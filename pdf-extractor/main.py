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

from fastapi import FastAPI, HTTPException, Request, File, UploadFile, Form
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from config import settings
from extractor import extract_from_pdf, extract_from_bytes
from models import ExtractRequest, ExtractResponse, PassengerRecord

# ── Logging ───────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper(), logging.INFO),
    format="%(asctime)s  %(levelname)-8s  %(name)s  %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)


# ── App lifecycle ─────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    log.info("╔══════════════════════════════════════════════════╗")
    log.info("║  PDF Extraction Service — Starting              ║")
    log.info("╠══════════════════════════════════════════════════╣")
    log.info("║  Environment : %s", settings.app_env)
    log.info("║  Port        : %d", settings.service_port)
    log.info("║  Groq Model  : %s", settings.groq_model)
    log.info("║  Rate Limit  : %d req/min", settings.groq_rate_limit_per_minute)
    log.info("║  CORS Origins: %s", settings.cors_allowed_origins)
    log.info("╚══════════════════════════════════════════════════╝")

    if settings.is_production:
        log.info("Running in PRODUCTION mode — debug endpoints disabled")
    else:
        log.info("Running in DEVELOPMENT mode — debug endpoints enabled")

    yield
    log.info("PDF Extraction Service shutting down")


app = FastAPI(
    title="TravelBilling PDF Extraction Service",
    description="Renders PDF pages and extracts ticket data using Groq vision AI",
    version="1.0.0",
    lifespan=lifespan,
    # Disable interactive docs in production for security
    docs_url="/docs" if settings.is_development else None,
    redoc_url="/redoc" if settings.is_development else None,
)

# ── CORS Middleware ───────────────────────────────────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)


# ── Routes ────────────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    """Health check — Java calls this on startup to verify service is up."""
    return {
        "status": "ok",
        "model": settings.groq_model,
        "environment": settings.app_env,
    }


# ── Debug endpoint (development only) ────────────────────────────────────────
if settings.is_development:
    @app.post("/extract-debug")
    async def extract_debug(request: Request):
        """Debug endpoint: shows exactly what the Python server receives from Java.
        Only registered in development mode — returns 404 in production.
        """
        body_bytes = await request.body()
        content_type = request.headers.get("content-type", "NONE")
        log.info("DEBUG: content-type=%s body-len=%d body=%.500s",
                 content_type, len(body_bytes), body_bytes.decode("utf-8", errors="replace"))
        return {
            "content_type": content_type,
            "body_length": len(body_bytes),
            "body_preview": body_bytes.decode("utf-8", errors="replace")[:500],
        }


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


@app.post("/extract-upload", response_model=ExtractResponse)
async def extract_upload(
    file: UploadFile = File(..., description="PDF file to extract data from"),
    company_id: int = Form(..., description="Company ID from auth context"),
):
    """
    Multipart upload extraction endpoint (Docker-safe).

    Java backend streams the PDF bytes directly instead of sending a filesystem
    path, so this works across separate Docker containers.
    """
    log.info(
        "Extract upload: file=%s company=%d content_type=%s",
        file.filename, company_id, file.content_type,
    )

    pdf_bytes = await file.read()
    if len(pdf_bytes) == 0:
        raise HTTPException(status_code=400, detail="Empty PDF file")

    log.info("Received %d bytes for extraction", len(pdf_bytes))

    try:
        result = await extract_from_bytes(pdf_bytes)

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
    # In production, don't leak error details
    detail = "Internal server error" if settings.is_production else str(exc)
    return JSONResponse(
        status_code=500,
        content={"status": "FAILED", "error": detail},
    )


# ── Entry point ───────────────────────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=settings.service_port,
        reload=settings.is_development,  # Hot-reload only in dev
        log_level=settings.log_level.lower(),
    )
