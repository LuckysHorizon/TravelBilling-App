"""
TravelBilling PDF Extraction Microservice
==========================================
FastAPI service that receives a PDF file path from Java,
renders pages using PyMuPDF, calls Groq vision API,
and returns structured ticket data as JSON.

Run:  uvicorn main:app --host 0.0.0.0 --port 8000 --reload
"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from config import settings
from extractor import extract_from_pdf
from models import ExtractRequest, ExtractResponse, PassengerRecord

# ── Logging ───────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(name)s  %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)


# ── App lifecycle ─────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    log.info("PDF Extraction Service starting on port %d", settings.service_port)
    log.info("Groq model: %s", settings.groq_model)
    log.info("Rate limit: %d req/min", settings.groq_rate_limit_per_minute)
    yield
    log.info("PDF Extraction Service shutting down")


app = FastAPI(
    title="TravelBilling PDF Extraction Service",
    description="Renders PDF pages and extracts ticket data using Groq vision AI",
    version="1.0.0",
    lifespan=lifespan,
)

# Allow Java backend to call this service
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)


# ── Routes ────────────────────────────────────────────────────────────────────

@app.post("/extract-debug")
async def extract_debug(request: Request):
    """Debug endpoint: shows exactly what the Python server receives from Java."""
    body_bytes = await request.body()
    content_type = request.headers.get("content-type", "NONE")
    log.info("DEBUG: content-type=%s body-len=%d body=%.500s",
             content_type, len(body_bytes), body_bytes.decode("utf-8", errors="replace"))
    return {
        "content_type": content_type,
        "body_length": len(body_bytes),
        "body_preview": body_bytes.decode("utf-8", errors="replace")[:500],
    }

@app.get("/health")
async def health():
    """Health check — Java calls this on startup to verify service is up."""
    return {"status": "ok", "model": settings.groq_model}


@app.post("/extract", response_model=ExtractResponse)
async def extract(request: ExtractRequest):
    """
    Main extraction endpoint.

    Java POSTs:
      { "file_path": "D:\\...\\uuid_ticket.pdf", "company_id": 21 }

    Returns structured extraction result with passenger records.
    """
    log.info(
        "Extract request: file=%s company=%d",
        request.file_path, request.company_id,
    )

    try:
        result = await extract_from_pdf(request.file_path)

    except FileNotFoundError as e:
        log.error("File not found: %s", e)
        raise HTTPException(status_code=404, detail=str(e))

    except ValueError as e:
        log.error("Extraction failed: %s", e)
        raise HTTPException(status_code=422, detail=str(e))

    except Exception as e:
        log.exception("Unexpected error during extraction")
        raise HTTPException(status_code=500, detail=f"Extraction error: {str(e)}")

    # Map raw dicts to Pydantic models for response validation
    records = []
    for rec in result["records"]:
        try:
            records.append(PassengerRecord(**rec))
        except Exception as e:
            log.warning("Record validation failed: %s — %s", rec, e)
            continue

    if not records:
        raise HTTPException(
            status_code=422,
            detail="Extraction returned no valid passenger records",
        )

    # Determine status
    has_nulls = any(
        r.pnr_number is None or r.origin is None or r.destination is None
        for r in records
    )
    status = "PARTIAL" if has_nulls else "SUCCESS"

    log.info("Returning %d record(s) with status=%s", len(records), status)

    return ExtractResponse(
        status=status,
        total_passengers=len(records),
        records=records,
        model_used=result["model_used"],
        prompt_tokens=result["prompt_tokens"],
        completion_tokens=result["completion_tokens"],
        processing_ms=result["processing_ms"],
    )


# ── Error handlers ────────────────────────────────────────────────────────────

@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(
        status_code=exc.status_code,
        content={"status": "FAILED", "error": exc.detail},
    )


@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    log.exception("Unhandled exception")
    return JSONResponse(
        status_code=500,
        content={"status": "FAILED", "error": "Internal server error"},
    )


# ── Entry point ───────────────────────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=settings.service_port,
        reload=True,
        log_level="info",
    )
