"""
PDF Extraction Pipeline
=======================
Renders PDF pages to PNG images using PyMuPDF, then calls Groq's
vision model (Llama 3.2 Vision) to extract structured ticket data.

Strategy: Call Groq once per page → merge passenger + fare data.

Pipeline:
  Page 1..N-1  →  passenger + route info  (individual calls)
  Page N       →  fare / payment info     (separate call)
  Merge        →  combine passengers with fare data
"""

import asyncio
import base64
import json
import re
import time
import logging
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import Any

import fitz  # PyMuPDF
import httpx

from config import settings

log = logging.getLogger(__name__)

# ── Render DPI ────────────────────────────────────────────────────────────────
RENDER_DPI = 180

# ── Rate limiter ──────────────────────────────────────────────────────────────
_rate_limit_interval = 60.0 / settings.groq_rate_limit_per_minute
_last_call_time: float = 0.0
_rate_limit_lock = asyncio.Lock()


async def _rate_limit():
    """Simple token-bucket rate limiter. Async-safe."""
    global _last_call_time
    async with _rate_limit_lock:
        now = time.monotonic()
        wait = _rate_limit_interval - (now - _last_call_time)
        if wait > 0:
            log.debug("Rate limiter: waiting %.2fs", wait)
            await asyncio.sleep(wait)
        _last_call_time = time.monotonic()


# ── System prompts ────────────────────────────────────────────────────────────

# Prompt for each page — extracts passengers AND fare if visible
PAGE_PROMPT = """You are extracting data from ONE page of a travel invoice.

RESPOND WITH ONLY A SINGLE JSON OBJECT. Your entire response must start with { and end with }.
Do NOT include markdown, code fences, explanations, thinking, or any text outside the JSON.

JSON schema (use null for any field you cannot confidently extract):
{
  "page_type": "PASSENGER" | "FARE" | "MIXED",
  "pnr_number": string or null,
  "operator_name": string or null,
  "travel_date": "yyyy-MM-dd" or null,
  "origin": string or null,
  "destination": string or null,
  "ticket_type": "FLIGHT" | "BUS" | "TRAIN" | null,
  "passengers": [{"passenger_name": "string"}],
  "fare": {"base_fare_total": number, "total_amount": number} or null
}

CRITICAL RULES:
1. travel_date = the actual journey/flight/departure date, NOT the booking/issue/purchase date.
   If the document shows "Date of Booking: 15 Jan" and "Flight Date: 18 Jan", travel_date is "2025-01-18".
2. pnr_number = the actual alphanumeric booking reference code (e.g. "H4TB7Q", "EYC8NQ").
   NEVER return field labels like "PNR", "Number", "Reference", or "Booking Reference" as the pnr_number value.
3. passenger_name = the actual person's name. NEVER return labels like "Passenger", "Name", or "Traveller".
4. operator_name = the actual airline/bus/train company name. NEVER return labels like "Operator" or "Carrier".
5. origin and destination = actual city or airport names. NEVER return labels like "Origin" or "Destination".
6. If you cannot confidently identify any field's actual value, return null for that field.
7. Do NOT guess or fabricate values.
8. fare.base_fare_total = airfare/base fare BEFORE taxes (total for ALL passengers).
9. fare.total_amount = grand total INCLUDING all taxes (total for ALL passengers).
10. passengers array should be empty [] if no passenger names are visible on this page.

Example:
{"page_type":"PASSENGER","pnr_number":"H4TB7Q","operator_name":"IndiGo","travel_date":"2025-02-13","origin":"Raipur","destination":"Delhi","ticket_type":"FLIGHT","passengers":[{"passenger_name":"Mr HASAN REZA"}],"fare":null}"""


# ── Main extraction functions ────────────────────────────────────────────────

async def extract_from_pdf(file_path: str) -> dict[str, Any]:
    """Full pipeline: PDF file -> rendered images -> Groq vision (per page) -> merge -> structured JSON."""
    page_images = _render_pdf_to_images(file_path)
    log.info("Rendered %d page(s) from %s", len(page_images), Path(file_path).name)
    return await _extract_from_images(page_images)


async def extract_from_bytes(pdf_bytes: bytes) -> dict[str, Any]:
    """Full pipeline: PDF bytes -> rendered images -> Groq vision (per page) -> merge -> structured JSON."""
    page_images = _render_bytes_to_images(pdf_bytes)
    log.info("Rendered %d page(s) from uploaded bytes (%d bytes)", len(page_images), len(pdf_bytes))
    return await _extract_from_images(page_images)


async def _extract_from_images(page_images: list[str]) -> dict[str, Any]:
    """Shared extraction pipeline: rendered page images -> Groq vision -> merge -> JSON."""
    start = time.monotonic()
    total_pages = len(page_images)

    if total_pages == 0:
        raise ValueError("PDF has no pages to extract")

    # Call Groq for each page
    page_results = []
    total_usage = {"prompt_tokens": 0, "completion_tokens": 0}
    failed_pages = 0

    for i, image_b64 in enumerate(page_images):
        page_num = i + 1
        log.info("Processing page %d/%d", page_num, total_pages)

        raw_response, usage = await _call_groq_single(
            image_b64,
            page_num=page_num,
            total_pages=total_pages,
        )
        if not raw_response:
            log.warning("Page %d failed to extract; skipping.", page_num)
            failed_pages += 1
            continue

        total_usage["prompt_tokens"] += usage.get("prompt_tokens", 0)
        total_usage["completion_tokens"] += usage.get("completion_tokens", 0)

        page_data = _parse_page_response(raw_response, page_num)
        if page_data:
            page_results.append(page_data)
        else:
            log.warning("Page %d parsing failed; skipping.", page_num)
            failed_pages += 1

    log.info("Extraction completed: %d/%d pages successfully processed", total_pages - failed_pages, total_pages)

    if not page_results:
        raise ValueError("No valid data extracted from any page")

    # Merge page results into final passenger records
    records = _merge_pages(page_results)

    elapsed_ms = int((time.monotonic() - start) * 1000)
    log.info("Extraction complete: %d record(s) in %dms", len(records), elapsed_ms)

    return {
        "records": records,
        "model_used": settings.groq_model,
        "prompt_tokens": total_usage["prompt_tokens"],
        "completion_tokens": total_usage["completion_tokens"],
        "processing_ms": elapsed_ms,
    }


# ── PDF rendering ─────────────────────────────────────────────────────────────

def _render_pdf_to_images(file_path: str) -> list[str]:
    """Render every page of the PDF to a base64-encoded PNG string."""
    path = Path(file_path)
    if not path.exists():
        raise FileNotFoundError(f"PDF not found at: {file_path}")
    if not path.suffix.lower() == ".pdf":
        raise ValueError(f"Not a PDF file: {file_path}")

    doc = fitz.open(str(path))
    images: list[str] = []

    try:
        mat = fitz.Matrix(RENDER_DPI / 72, RENDER_DPI / 72)
        for page_num in range(len(doc)):
            page = doc[page_num]
            pix = page.get_pixmap(matrix=mat, alpha=False)
            png_bytes = pix.tobytes("png")
            images.append(base64.b64encode(png_bytes).decode("ascii"))
            log.debug(
                "Page %d: %dx%d px, %d bytes",
                page_num + 1, pix.width, pix.height, len(png_bytes),
            )
    finally:
        doc.close()

    return images


def _render_bytes_to_images(pdf_bytes: bytes) -> list[str]:
    """Render every page of PDF bytes to a base64-encoded PNG string."""
    if not pdf_bytes:
        raise ValueError("Empty PDF bytes")

    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    images: list[str] = []

    try:
        mat = fitz.Matrix(RENDER_DPI / 72, RENDER_DPI / 72)
        for page_num in range(len(doc)):
            page = doc[page_num]
            pix = page.get_pixmap(matrix=mat, alpha=False)
            png_bytes = pix.tobytes("png")
            images.append(base64.b64encode(png_bytes).decode("ascii"))
            log.debug(
                "Page %d: %dx%d px, %d bytes",
                page_num + 1, pix.width, pix.height, len(png_bytes),
            )
    finally:
        doc.close()

    return images


# ── Groq API call (single image) ─────────────────────────────────────────────

async def _call_groq_single(
    image_b64: str,
    page_num: int,
    total_pages: int,
    max_retries: int = 5,
) -> tuple[str | None, dict]:
    """
    POST to Groq vision API with ONE page image.
    Returns (raw_content_string, usage_dict).
    """
    user_content = [
        {
            "type": "image_url",
            "image_url": {"url": f"data:image/png;base64,{image_b64}"},
        },
        {
            "type": "text",
            "text": f"This is page {page_num} of {total_pages} of a travel document. "
                    f"Extract all visible information from this page.",
        },
    ]

    payload = {
        "model": settings.groq_model,
        "max_tokens": 2048,
        "temperature": 0.0,
        "stream": False,
        "messages": [
            {"role": "system", "content": PAGE_PROMPT},
            {"role": "user", "content": user_content},
        ],
    }

    headers = {
        "Authorization": f"Bearer {settings.groq_api_key}",
        "Content-Type": "application/json",
    }

    import random
    delay = 4.0
    max_delay = 20.0
    
    # Bound the entire extraction time to avoid backend timeouts
    # Leave a 5s buffer from the absolute timeout
    start_time = time.monotonic()
    deadline = start_time + max(10, settings.groq_timeout_seconds - 5.0)

    async with httpx.AsyncClient(
        timeout=httpx.Timeout(settings.groq_timeout_seconds, connect=10.0)
    ) as client:
        for attempt in range(1, max_retries + 1):
            if time.monotonic() > deadline:
                log.error("Page %d: Bounded time exceeded before attempt %d", page_num, attempt)
                break

            await _rate_limit()

            log.info(
                "Calling Groq page %d/%d (attempt %d/%d)",
                page_num, total_pages, attempt, max_retries,
            )
            call_start = time.monotonic()

            try:
                response = await client.post(
                    settings.groq_api_url,
                    json=payload,
                    headers=headers,
                )
            except httpx.RequestError as e:
                log.error("Groq request failed on page %d: %s", page_num, e)
                if time.monotonic() + delay > deadline:
                    break
                await asyncio.sleep(delay)
                delay = min(delay * 2, max_delay)
                continue

            elapsed = int((time.monotonic() - call_start) * 1000)
            log.info("Groq responded %d in %dms (page %d)", response.status_code, elapsed, page_num)

            if response.status_code == 200:
                data = response.json()
                content = data["choices"][0]["message"]["content"]
                usage = data.get("usage", {})
                log.debug("Page %d content (%.100s...)", page_num, content)
                return content, usage

            if response.status_code in (429, 503):
                retry_after = response.headers.get("retry-after")
                if retry_after and retry_after.isdigit():
                    wait_time = float(retry_after)
                    log.warning("Groq %d on page %d — respect Retry-After header: %.1fs", response.status_code, page_num, wait_time)
                else:
                    jitter = random.uniform(0.0, 1.0)
                    wait_time = delay + jitter
                    log.warning("Groq %d on page %d — backing off %.1fs before retry", response.status_code, page_num, wait_time)
                
                if time.monotonic() + wait_time > deadline:
                    log.error("Page %d: Wait time %.1fs would exceed deadline. Aborting retries.", page_num, wait_time)
                    break
                    
                await asyncio.sleep(wait_time)
                delay = min(delay * 2, max_delay)
                continue

            log.error("Groq returned %d on page %d: %.200s", response.status_code, page_num, response.text)
            return None, {}

    log.error("Groq failed after %d attempts (or timeout) for page %d", attempt, page_num)
    return None, {}


# ── Field validation ──────────────────────────────────────────────────────────

# Values that are field labels, not actual data
_GARBAGE_PNR = {
    "pnr", "number", "reference", "booking reference", "booking ref",
    "pnr number", "pnr no", "ref", "confirmation", "booking",
}
_GARBAGE_NAMES = {
    "passenger", "name", "traveller", "traveler", "passenger name",
    "pax", "pax name",
}
_GARBAGE_LOCATIONS = {
    "origin", "destination", "from", "to", "departure", "arrival",
    "source", "city", "airport",
}
_GARBAGE_OPERATORS = {
    "operator", "carrier", "airline", "operator name", "carrier name",
}


def _sanitize_string_field(value: Any, garbage_set: set[str]) -> str | None:
    """Return None if value is missing, empty, or a known garbage label."""
    if value is None:
        return None
    s = str(value).strip()
    if not s or s.lower() == "null" or s.lower() == "n/a" or s.lower() == "none":
        return None
    if s.lower() in garbage_set:
        return None
    return s


def _sanitize_extracted_data(data: dict) -> dict:
    """Sanitize extracted fields to prevent garbage label values."""
    data["pnr_number"] = _sanitize_string_field(
        data.get("pnr_number"), _GARBAGE_PNR
    )
    data["operator_name"] = _sanitize_string_field(
        data.get("operator_name"), _GARBAGE_OPERATORS
    )
    data["origin"] = _sanitize_string_field(
        data.get("origin"), _GARBAGE_LOCATIONS
    )
    data["destination"] = _sanitize_string_field(
        data.get("destination"), _GARBAGE_LOCATIONS
    )

    # Sanitize passenger names
    passengers = data.get("passengers") or []
    clean_passengers = []
    for p in passengers:
        if isinstance(p, dict):
            name = _sanitize_string_field(
                p.get("passenger_name"), _GARBAGE_NAMES
            )
            if name:
                p["passenger_name"] = name
                clean_passengers.append(p)
    data["passengers"] = clean_passengers

    # Sanitize travel_date — reject if it looks like a label, not a date
    td = data.get("travel_date")
    if td is not None:
        td_str = str(td).strip()
        if not re.match(r'^\d{4}-\d{2}-\d{2}$', td_str):
            log.warning("travel_date '%s' is not yyyy-MM-dd format — setting to null", td_str)
            data["travel_date"] = None

    return data


def _safe_float(val: Any) -> float | None:
    """Safely convert a string/value to a float, returning None on failure."""
    if val is None:
        return None
    s = str(val).strip()
    if not s or s.lower() in ("null", "n/a", "none"):
        return None
    
    # Remove commas, currency symbols, and spaces
    s = re.sub(r'[^\d.-]', '', s)
    if not s:
        return None
        
    try:
        return float(s)
    except (ValueError, TypeError):
        return None


# ── Page response parsing ─────────────────────────────────────────────────────

def _parse_page_response(raw_content: str, page_num: int) -> dict | None:
    """Parse the JSON object from a single page's Groq response.
    Falls back to regex-based Markdown parsing if JSON fails."""
    clean = raw_content.strip()

    # Strip <think>...</think> blocks some models emit
    clean = re.sub(r'<think>[\s\S]*?</think>', '', clean).strip()

    # Strip markdown code fences
    fence_match = re.match(r'^```(?:json)?\s*([\s\S]*?)\s*```$', clean, re.DOTALL)
    if fence_match:
        clean = fence_match.group(1).strip()

    # Try to find JSON within the response (model may add text around it)
    json_match = re.search(r'\{[\s\S]*\}', clean)
    if json_match:
        try:
            data = json.loads(json_match.group())
            if isinstance(data, dict):
                data = _sanitize_extracted_data(data)
                data["_page_num"] = page_num
                log.info(
                    "Page %d: type=%s, passengers=%d, has_fare=%s, pnr=%s",
                    page_num,
                    data.get("page_type", "?"),
                    len(data.get("passengers") or []),
                    bool(data.get("fare")),
                    data.get("pnr_number"),
                )
                return data
        except json.JSONDecodeError as e:
            log.warning("Page %d: JSON decode failed: %s", page_num, e)

    # Fallback: parse Markdown/text response
    log.warning("Page %d: No valid JSON found. Attempting Markdown/text parsing...", page_num)
    result = _parse_markdown_response(clean, page_num)
    if result:
        result = _sanitize_extracted_data(result)
    return result


def _parse_markdown_response(text: str, page_num: int) -> dict | None:
    """
    Fallback parser for when NVIDIA returns Markdown instead of JSON.
    Extracts key fields using regex from bullet-point / text format.
    """
    result: dict = {
        "page_type": None,
        "pnr_number": None,
        "operator_name": None,
        "travel_date": None,
        "origin": None,
        "destination": None,
        "ticket_type": None,
        "passengers": [],
        "fare": None,
        "_page_num": page_num,
    }

    # Extract PNR
    pnr_match = re.search(r'PNR[:\s#]*\**\s*([A-Z0-9]{4,10})', text, re.IGNORECASE)
    if pnr_match:
        result["pnr_number"] = pnr_match.group(1).strip()

    # Extract operator
    for op in ["IndiGo", "Air India", "SpiceJet", "Vistara", "GoAir", "AirAsia"]:
        if op.lower() in text.lower():
            result["operator_name"] = op
            result["ticket_type"] = "FLIGHT"
            break

    # Extract date (yyyy-MM-dd or dd Mon yyyy or dd/MM/yyyy)
    date_match = re.search(r'(\d{4}-\d{2}-\d{2})', text)
    if date_match:
        result["travel_date"] = date_match.group(1)
    else:
        date_match = re.search(r'(\d{1,2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\w*\s+(\d{4})', text, re.IGNORECASE)
        if date_match:
            months = {"jan": "01", "feb": "02", "mar": "03", "apr": "04", "may": "05", "jun": "06",
                       "jul": "07", "aug": "08", "sep": "09", "oct": "10", "nov": "11", "dec": "12"}
            d, m, y = date_match.group(1), date_match.group(2).lower()[:3], date_match.group(3)
            result["travel_date"] = f"{y}-{months.get(m, '01')}-{int(d):02d}"

    # Extract origin and destination from sector (RPR-DEL, Raipur to Delhi, etc.)
    sector_match = re.search(r'(?:Sector|Route|From|Origin)[:\s]*\**\s*(\w[\w\s]*?)\s*(?:[-–→to]+)\s*(\w[\w\s]*?)(?:\s*\(|$|\n|,)', text, re.IGNORECASE)
    if sector_match:
        result["origin"] = sector_match.group(1).strip()
        result["destination"] = sector_match.group(2).strip()
    else:
        # Try airport codes
        code_match = re.search(r'([A-Z]{3})\s*[-–→]\s*([A-Z]{3})', text)
        if code_match:
            code_to_city = {"RPR": "Raipur", "DEL": "Delhi", "BOM": "Mumbai", "BLR": "Bengaluru",
                           "HYD": "Hyderabad", "MAA": "Chennai", "CCU": "Kolkata", "COK": "Kochi",
                           "GOI": "Goa", "JAI": "Jaipur", "PNQ": "Pune", "AMD": "Ahmedabad"}
            result["origin"] = code_to_city.get(code_match.group(1), code_match.group(1))
            result["destination"] = code_to_city.get(code_match.group(2), code_match.group(2))

    # Extract passenger names (look for Mr/Ms/Mrs patterns)
    name_matches = re.findall(r'(?:Mr|Ms|Mrs|Miss|Master)\s+[A-Z][A-Z\s]+', text)
    for name in name_matches:
        clean_name = name.strip()
        if len(clean_name) > 3:
            result["passengers"].append({"passenger_name": clean_name})

    # Extract fare amounts
    base_match = re.search(r'(?:Airfare|Base\s*Fare|Air\s*Fare)[:\s]*[\u20b9INR\s]*([0-9,]+\.?\d*)', text, re.IGNORECASE)
    total_match = re.search(r'(?:Total\s*(?:Charge|Fare|Amount)|Grand\s*Total|Amount\s*Payable)[:\s]*[\u20b9INR\s]*([0-9,]+\.?\d*)', text, re.IGNORECASE)

    if base_match or total_match:
        fare = {}
        if base_match:
            f = _safe_float(base_match.group(1))
            if f is not None:
                fare["base_fare_total"] = f
        if total_match:
            f = _safe_float(total_match.group(1))
            if f is not None:
                fare["total_amount"] = f
        if fare:
            result["fare"] = fare

    # Determine page type
    has_passengers = len(result["passengers"]) > 0
    has_fare = result["fare"] is not None
    if has_passengers and has_fare:
        result["page_type"] = "MIXED"
    elif has_passengers:
        result["page_type"] = "PASSENGER"
    elif has_fare:
        result["page_type"] = "FARE"
    else:
        result["page_type"] = "PASSENGER"  # default

    log.info(
        "Page %d (Markdown fallback): type=%s, passengers=%d, has_fare=%s, pnr=%s",
        page_num, result["page_type"], len(result["passengers"]),
        has_fare, result["pnr_number"],
    )

    # Return even if minimal data — the merge step will handle combining
    if result["pnr_number"] or result["passengers"] or result["fare"]:
        return result

    log.warning("Page %d: Markdown fallback found nothing useful", page_num)
    return None


# ── Multi-page merge ─────────────────────────────────────────────────────────

def _merge_pages(page_results: list[dict]) -> list[dict]:
    """
    Merge per-page extractions into final per-passenger records.

    Strategy:
    1. Collect all passengers from PASSENGER / MIXED pages
    2. Collect fare data from FARE / MIXED pages
    3. Collect shared fields (PNR, route, date, operator) from any page
    4. Divide total fares equally among passengers
    """
    # Collect shared fields from any page that has them
    shared = {
        "pnr_number": None,
        "operator_name": None,
        "travel_date": None,
        "origin": None,
        "destination": None,
        "ticket_type": None,
    }
    for page in page_results:
        for key in shared:
            val = page.get(key)
            if val is not None and str(val).lower() != "null" and str(val).strip():
                shared[key] = str(val).strip()

    # Collect passengers
    all_passengers: list[str] = []
    for page in page_results:
        page_type = (page.get("page_type") or "").upper()
        if page_type in ("PASSENGER", "MIXED", ""):
            passengers = page.get("passengers") or []
            for p in passengers:
                name = p.get("passenger_name") if isinstance(p, dict) else None
                if name and str(name).strip() and str(name).lower() != "null":
                    all_passengers.append(str(name).strip())

    # Collect fare data
    base_fare_total: float | None = None
    total_amount_all: float | None = None
    for page in page_results:
        fare = page.get("fare")
        if fare and isinstance(fare, dict):
            bf = fare.get("base_fare_total")
            ta = fare.get("total_amount")
            fbf = _safe_float(bf)
            if fbf is not None:
                base_fare_total = fbf
            fta = _safe_float(ta)
            if fta is not None:
                total_amount_all = fta

    # If no passengers found, create one "Unknown" record
    if not all_passengers:
        log.warning("No passengers found in any page. Creating single unknown record.")
        all_passengers = ["Unknown Passenger"]

    num_passengers = len(all_passengers)
    log.info(
        "Merge: %d passengers, base_fare_total=%s, total_amount=%s, shared=%s",
        num_passengers, base_fare_total, total_amount_all, shared,
    )

    # Divide fares equally among passengers
    per_person_base = None
    per_person_total = None
    if base_fare_total is not None:
        per_person_base = float(
            Decimal(str(base_fare_total / num_passengers)).quantize(
                Decimal("0.01"), rounding=ROUND_HALF_UP
            )
        )
    if total_amount_all is not None:
        per_person_total = float(
            Decimal(str(total_amount_all / num_passengers)).quantize(
                Decimal("0.01"), rounding=ROUND_HALF_UP
            )
        )

    # Build final records
    records: list[dict] = []
    for name in all_passengers:
        record = {
            "pnr_number": shared["pnr_number"],
            "passenger_name": name,
            "travel_date": shared["travel_date"],
            "operator_name": shared["operator_name"],
            "origin": shared["origin"],
            "destination": shared["destination"],
            "base_fare": per_person_base,
            "total_amount": per_person_total,
            "ticket_type": shared["ticket_type"] or "UNKNOWN",
            "confidence": 0.92 if per_person_total is not None else 0.6,
        }
        records.append(record)

    log.info("Final merged records: %d", len(records))
    for i, r in enumerate(records):
        log.info(
            "  [%d] %s — PNR=%s, %s->%s, base=%s, total=%s",
            i, r["passenger_name"], r["pnr_number"],
            r["origin"], r["destination"],
            r["base_fare"], r["total_amount"],
        )

    return records
