from __future__ import annotations

import logging
from typing import Any, AsyncGenerator

from config import settings
from health_manager import HealthManager
from models import ChatMessage, ChatResponse, ProviderHealth
from providers import GroqProvider, ProviderError
from rate_limiter import RateLimiter

logger = logging.getLogger(__name__)


class NoHealthyProviderError(Exception):
    """Raised when no provider is available to serve a request."""


class LLMClient:
    """Unified LLM client with health tracking and rate limiting."""

    def __init__(self) -> None:
        self.providers = [GroqProvider(settings.groq_api_key_fallback)]
        self.health_manager = HealthManager()
        self.rate_limiter = RateLimiter()

    async def chat(
        self,
        messages: list[ChatMessage],
        tools: list[dict[str, Any]] | None = None,
        temperature: float | None = None,
        max_tokens: int | None = None,
    ) -> ChatResponse:
        """Send a chat completion, trying each healthy provider in order."""
        errors: list[str] = []

        for provider in self.providers:
            if not await self.health_manager.is_healthy(provider.name):
                logger.info("Skipping unhealthy provider: %s", provider.name)
                errors.append(f"{provider.name}: unhealthy (in backoff)")
                continue

            if not await self.rate_limiter.can_make_request(provider.name):
                logger.info("Skipping rate-limited provider: %s", provider.name)
                errors.append(f"{provider.name}: rate limited")
                continue

            try:
                response = await provider.chat(
                    messages=messages,
                    tools=tools,
                    temperature=temperature,
                    max_tokens=max_tokens,
                )
                await self.health_manager.record_success(provider.name)
                await self.rate_limiter.record_usage(
                    provider.name,
                    prompt_tokens=response.prompt_tokens,
                    completion_tokens=response.completion_tokens,
                )
                return response

            except ProviderError as exc:
                logger.error("Provider %s failed: %s", provider.name, exc)
                await self.health_manager.record_failure(provider.name)
                errors.append(f"{provider.name}: {exc}")
                continue

        raise NoHealthyProviderError(
            f"All providers unavailable. Errors: {'; '.join(errors)}"
        )

    async def chat_stream(
        self,
        messages: list[ChatMessage],
        tools: list[dict[str, Any]] | None = None,
        temperature: float | None = None,
        max_tokens: int | None = None,
    ) -> AsyncGenerator[str, None]:
        """Stream chat completion, trying each healthy provider in order."""
        errors: list[str] = []

        for provider in self.providers:
            if not await self.health_manager.is_healthy(provider.name):
                logger.info("Skipping unhealthy provider: %s", provider.name)
                errors.append(f"{provider.name}: unhealthy (in backoff)")
                continue

            if not await self.rate_limiter.can_make_request(provider.name):
                logger.info("Skipping rate-limited provider: %s", provider.name)
                errors.append(f"{provider.name}: rate limited")
                continue

            try:
                await self.rate_limiter.reserve_request(provider.name)
                async for chunk in provider.chat_stream(
                    messages=messages,
                    tools=tools,
                    temperature=temperature,
                    max_tokens=max_tokens,
                ):
                    yield chunk

                await self.health_manager.record_success(provider.name)
                return

            except ProviderError as exc:
                logger.error("Provider %s stream failed: %s", provider.name, exc)
                await self.health_manager.record_failure(provider.name)
                errors.append(f"{provider.name}: {exc}")
                continue

        raise NoHealthyProviderError(
            f"All providers unavailable for streaming. Errors: {'; '.join(errors)}"
        )

    async def get_health_status(self) -> dict[str, ProviderHealth]:
        """Return health snapshots for every tracked provider."""
        # Ensure all registered providers have an entry
        for provider in self.providers:
            await self.health_manager.is_healthy(provider.name)
        return await self.health_manager.get_all_health()

    async def close(self) -> None:
        """Shut down all provider HTTP clients."""
        for provider in self.providers:
            await provider.close()
