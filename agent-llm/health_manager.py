from __future__ import annotations

import asyncio
import logging
from datetime import datetime, timezone

from models import ProviderHealth, ProviderStatus

logger = logging.getLogger(__name__)

BASE_BACKOFF_SECONDS = 60.0
MAX_BACKOFF_SECONDS = 1800.0  # 30 minutes
DEGRADED_THRESHOLD = 2  # consecutive failures before marking degraded


class HealthManager:
    """Tracks provider health with exponential backoff on failures."""

    def __init__(self) -> None:
        self._states: dict[str, ProviderHealth] = {}
        self._lock = asyncio.Lock()

    def _ensure_provider(self, provider: str) -> ProviderHealth:
        if provider not in self._states:
            self._states[provider] = ProviderHealth(
                name=provider,
                status=ProviderStatus.HEALTHY,
            )
        return self._states[provider]

    async def is_healthy(self, provider: str) -> bool:
        """Check whether a provider is available (not in backoff)."""
        async with self._lock:
            state = self._ensure_provider(provider)

            if state.status == ProviderStatus.UNHEALTHY and state.backoff_until:
                now = datetime.now(tz=timezone.utc)
                if now < state.backoff_until:
                    return False
                # Backoff window expired — allow a retry
                logger.info(
                    "Provider %s backoff expired, allowing retry", provider
                )
                state.status = ProviderStatus.DEGRADED

            return True

    async def record_success(self, provider: str) -> None:
        """Record a successful request, resetting failure tracking."""
        async with self._lock:
            state = self._ensure_provider(provider)
            state.status = ProviderStatus.HEALTHY
            state.last_success = datetime.now(tz=timezone.utc)
            state.consecutive_failures = 0
            state.backoff_until = None
            logger.debug("Provider %s marked healthy", provider)

    async def record_failure(self, provider: str) -> None:
        """Record a failed request and compute exponential backoff."""
        async with self._lock:
            state = self._ensure_provider(provider)
            now = datetime.now(tz=timezone.utc)
            state.last_failure = now
            state.consecutive_failures += 1

            if state.consecutive_failures >= DEGRADED_THRESHOLD:
                state.status = ProviderStatus.UNHEALTHY
            else:
                state.status = ProviderStatus.DEGRADED

            # Exponential backoff: 60s * 2^(failures-1), capped at 1800s
            backoff = min(
                BASE_BACKOFF_SECONDS * (2 ** (state.consecutive_failures - 1)),
                MAX_BACKOFF_SECONDS,
            )
            from datetime import timedelta

            state.backoff_until = now + timedelta(seconds=backoff)
            logger.warning(
                "Provider %s failure #%d — backoff %.0fs until %s",
                provider,
                state.consecutive_failures,
                backoff,
                state.backoff_until.isoformat(),
            )

    async def get_provider_health(self, provider: str) -> ProviderHealth:
        """Return a snapshot of a provider's health state."""
        async with self._lock:
            return self._ensure_provider(provider).model_copy()

    async def get_all_health(self) -> dict[str, ProviderHealth]:
        """Return health snapshots for every tracked provider."""
        async with self._lock:
            return {
                name: state.model_copy() for name, state in self._states.items()
            }
