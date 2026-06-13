from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse

from config import settings
from llm_client import LLMClient, NoHealthyProviderError
from models import ChatRequest, ChatResponse, HealthResponse, ProviderStatus

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------
logging.basicConfig(
    level=settings.log_level.upper(),
    format="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
)
logger = logging.getLogger("agent-llm")

# ---------------------------------------------------------------------------
# Application lifespan
# ---------------------------------------------------------------------------
llm_client: LLMClient


@asynccontextmanager
async def lifespan(_app: FastAPI) -> AsyncGenerator[None, None]:
    global llm_client  # noqa: PLW0603
    llm_client = LLMClient()
    logger.info(
        "agent-llm service starting — model=%s, provider=groq, port=%d",
        settings.llm_model,
        settings.service_port,
    )
    yield
    logger.info("agent-llm service shutting down")
    await llm_client.close()


# ---------------------------------------------------------------------------
# FastAPI app
# ---------------------------------------------------------------------------
app = FastAPI(
    title="agent-llm",
    description="LLM abstraction layer for the Agentic AI assistant",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---------------------------------------------------------------------------
# POST /v1/chat — non-streaming chat completion
# ---------------------------------------------------------------------------
@app.post("/v1/chat", response_model=ChatResponse)
async def chat(request: ChatRequest) -> ChatResponse:
    """Send a chat completion request and return a JSON response."""
    try:
        response = await llm_client.chat(
            messages=request.messages,
            tools=request.tools,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
        )
        return response
    except NoHealthyProviderError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except Exception as exc:
        logger.exception("Unexpected error in /v1/chat")
        raise HTTPException(status_code=500, detail=str(exc)) from exc


# ---------------------------------------------------------------------------
# POST /v1/chat/stream — streaming chat completion (SSE)
# ---------------------------------------------------------------------------
@app.post("/v1/chat/stream")
async def chat_stream(request: ChatRequest) -> StreamingResponse:
    """Stream a chat completion via Server-Sent Events."""

    async def _event_generator() -> AsyncGenerator[str, None]:
        try:
            async for chunk in llm_client.chat_stream(
                messages=request.messages,
                tools=request.tools,
                temperature=request.temperature,
                max_tokens=request.max_tokens,
            ):
                yield chunk
        except NoHealthyProviderError as exc:
            import json

            error_event = json.dumps({"type": "error", "error": str(exc)})
            yield f"data: {error_event}\n\n"
        except Exception as exc:
            import json

            logger.exception("Unexpected error in /v1/chat/stream")
            error_event = json.dumps({"type": "error", "error": str(exc)})
            yield f"data: {error_event}\n\n"

    return StreamingResponse(
        _event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


# ---------------------------------------------------------------------------
# GET /health — service health + provider status
# ---------------------------------------------------------------------------
@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    """Return overall service health and individual provider statuses."""
    providers = await llm_client.get_health_status()

    # Overall status: healthy if any provider is healthy
    if any(p.status == ProviderStatus.HEALTHY for p in providers.values()):
        overall = "healthy"
    elif any(p.status == ProviderStatus.DEGRADED for p in providers.values()):
        overall = "degraded"
    else:
        overall = "unhealthy"

    return HealthResponse(
        status=overall,
        model=settings.llm_model,
        provider="groq",
        providers=providers,
    )


# ---------------------------------------------------------------------------
# GET /providers — list all providers and their health
# ---------------------------------------------------------------------------
@app.get("/providers")
async def list_providers() -> dict:
    """List all configured providers with health details."""
    providers = await llm_client.get_health_status()
    return {
        "providers": [
            {
                "name": name,
                "model": settings.llm_model,
                "status": health_info.status.value,
                "consecutive_failures": health_info.consecutive_failures,
                "last_success": (
                    health_info.last_success.isoformat()
                    if health_info.last_success
                    else None
                ),
                "last_failure": (
                    health_info.last_failure.isoformat()
                    if health_info.last_failure
                    else None
                ),
                "backoff_until": (
                    health_info.backoff_until.isoformat()
                    if health_info.backoff_until
                    else None
                ),
            }
            for name, health_info in providers.items()
        ]
    }


# ---------------------------------------------------------------------------
# Entrypoint
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=settings.service_port,
        log_level=settings.log_level.lower(),
    )
