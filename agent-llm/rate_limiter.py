from __future__ import annotations

import asyncio
import logging
import time
from dataclasses import dataclass, field

from config import settings

logger = logging.getLogger(__name__)

WINDOW_SECONDS = 60.0


@dataclass
class _UsageRecord:
    timestamp: float
    request_count: int = 1
    tokens: int = 0


@dataclass
class _ProviderBucket:
    records: list[_UsageRecord] = field(default_factory=list)

    def prune(self, now: float) -> None:
        """Remove records older than the sliding window."""
        cutoff = now - WINDOW_SECONDS
        self.records = [r for r in self.records if r.timestamp >= cutoff]

    @property
    def total_requests(self) -> int:
        return sum(r.request_count for r in self.records)

    @property
    def total_tokens(self) -> int:
        return sum(r.tokens for r in self.records)


class RateLimiter:
    """In-memory sliding-window rate limiter per provider."""

    def __init__(
        self,
        requests_per_minute: int | None = None,
        tokens_per_minute: int | None = None,
    ) -> None:
        self._requests_per_minute = (
            requests_per_minute
            if requests_per_minute is not None
            else settings.rate_limit_requests_per_minute
        )
        self._tokens_per_minute = (
            tokens_per_minute
            if tokens_per_minute is not None
            else settings.rate_limit_tokens_per_minute
        )
        self._buckets: dict[str, _ProviderBucket] = {}
        self._lock = asyncio.Lock()

    def _ensure_bucket(self, provider: str) -> _ProviderBucket:
        if provider not in self._buckets:
            self._buckets[provider] = _ProviderBucket()
        return self._buckets[provider]

    async def can_make_request(self, provider: str) -> bool:
        """Check whether the provider is within rate limits."""
        async with self._lock:
            bucket = self._ensure_bucket(provider)
            bucket.prune(time.monotonic())

            if bucket.total_requests >= self._requests_per_minute:
                logger.warning(
                    "Rate limit: provider %s hit %d requests/min (limit %d)",
                    provider,
                    bucket.total_requests,
                    self._requests_per_minute,
                )
                return False

            if bucket.total_tokens >= self._tokens_per_minute:
                logger.warning(
                    "Rate limit: provider %s hit %d tokens/min (limit %d)",
                    provider,
                    bucket.total_tokens,
                    self._tokens_per_minute,
                )
                return False

            return True

    async def record_usage(
        self, provider: str, prompt_tokens: int = 0, completion_tokens: int = 0
    ) -> None:
        """Record a completed request and its token usage."""
        async with self._lock:
            bucket = self._ensure_bucket(provider)
            bucket.records.append(
                _UsageRecord(
                    timestamp=time.monotonic(),
                    request_count=1,
                    tokens=prompt_tokens + completion_tokens,
                )
            )
            logger.debug(
                "Recorded usage for %s: +%d tokens (window: %d req, %d tok)",
                provider,
                prompt_tokens + completion_tokens,
                bucket.total_requests,
                bucket.total_tokens,
            )

    async def reserve_request(self, provider: str) -> None:
        """Reserve a slot for a request before it completes (count only, no tokens)."""
        async with self._lock:
            bucket = self._ensure_bucket(provider)
            bucket.records.append(
                _UsageRecord(timestamp=time.monotonic(), request_count=1, tokens=0)
            )
