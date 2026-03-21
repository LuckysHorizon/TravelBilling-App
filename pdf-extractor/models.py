from pydantic import BaseModel, Field
from typing import Optional
from decimal import Decimal


class ExtractRequest(BaseModel):
    """Sent by Java to request extraction."""
    file_path: str = Field(
        ...,
        description="Absolute path to the PDF on the shared filesystem",
    )
    company_id: int = Field(..., description="Company ID from JWT context")


class PassengerRecord(BaseModel):
    """One record = one DB row in Java's tickets table."""
    pnr_number: Optional[str] = Field(None, max_length=20)
    passenger_name: Optional[str] = None
    travel_date: Optional[str] = Field(None, description="yyyy-MM-dd format")
    operator_name: Optional[str] = None
    origin: Optional[str] = None
    destination: Optional[str] = None
    base_fare: Optional[float] = None
    total_amount: Optional[float] = None
    ticket_type: str = Field("UNKNOWN", description="FLIGHT | BUS | TRAIN")
    confidence: float = Field(0.0, ge=0.0, le=1.0)


class ExtractResponse(BaseModel):
    """Returned to Java after successful extraction."""
    model_config = {"protected_namespaces": ()}

    status: str                          # "SUCCESS" | "PARTIAL" | "FAILED"
    total_passengers: int
    records: list[PassengerRecord]
    model_used: str
    prompt_tokens: int
    completion_tokens: int
    processing_ms: int
    error: Optional[str] = None
