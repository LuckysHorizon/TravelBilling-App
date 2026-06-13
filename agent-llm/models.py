from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Any

from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    role: str = Field(..., description="Message role: system, user, assistant, or tool")
    content: str | None = Field(None, description="Message text content")
    tool_calls: list[dict[str, Any]] | None = Field(
        None, description="Tool calls made by the assistant"
    )
    tool_call_id: str | None = Field(
        None, description="ID of the tool call this message responds to"
    )


class ChatRequest(BaseModel):
    messages: list[ChatMessage] = Field(..., min_length=1)
    tools: list[dict[str, Any]] | None = Field(
        None, description="Tool definitions for function calling"
    )
    temperature: float | None = Field(None, ge=0.0, le=2.0)
    max_tokens: int | None = Field(None, ge=1, le=32768)
    stream: bool = Field(False, description="Whether to stream the response via SSE")


class ChatResponse(BaseModel):
    content: str | None = None
    role: str = "assistant"
    tool_calls: list[dict[str, Any]] | None = None
    model: str
    provider: str
    prompt_tokens: int = 0
    completion_tokens: int = 0
    latency_ms: float = 0.0


class ProviderStatus(str, Enum):
    HEALTHY = "healthy"
    UNHEALTHY = "unhealthy"
    DEGRADED = "degraded"


class ProviderHealth(BaseModel):
    name: str
    status: ProviderStatus = ProviderStatus.HEALTHY
    last_success: datetime | None = None
    last_failure: datetime | None = None
    consecutive_failures: int = 0
    backoff_until: datetime | None = None


class HealthResponse(BaseModel):
    status: str
    model: str
    provider: str
    providers: dict[str, ProviderHealth]
