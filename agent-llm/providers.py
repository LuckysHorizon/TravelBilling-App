from __future__ import annotations

import json
import logging
import time
from typing import Any, AsyncGenerator

import httpx

from config import settings
from models import ChatMessage, ChatResponse

logger = logging.getLogger(__name__)

GROQ_BASE_URL = "https://api.groq.com/openai/v1"


class ProviderError(Exception):
    """Base exception for provider errors."""

    def __init__(self, message: str, status_code: int | None = None, retryable: bool = False):
        super().__init__(message)
        self.status_code = status_code
        self.retryable = retryable


class RateLimitError(ProviderError):
    """Raised when the provider returns 429."""

    def __init__(self, message: str, retry_after: float | None = None):
        super().__init__(message, status_code=429, retryable=True)
        self.retry_after = retry_after


class AuthenticationError(ProviderError):
    """Raised when the provider returns 401/403."""

    def __init__(self, message: str):
        super().__init__(message, status_code=401, retryable=False)


class ServerError(ProviderError):
    """Raised when the provider returns 5xx."""

    def __init__(self, message: str, status_code: int):
        super().__init__(message, status_code=status_code, retryable=True)


class GroqProvider:
    """Groq LLM provider using the OpenAI-compatible API."""

    NAME = "groq"

    def __init__(self, api_key: str) -> None:
        self.api_key = api_key
        self.name = self.NAME
        self.base_url = GROQ_BASE_URL
        self._client: httpx.AsyncClient | None = None

    @property
    def client(self) -> httpx.AsyncClient:
        if self._client is None or self._client.is_closed:
            self._client = httpx.AsyncClient(
                base_url=self.base_url,
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                },
                timeout=httpx.Timeout(
                    connect=5.0,
                    read=settings.llm_timeout_ms / 1000.0,
                    write=5.0,
                    pool=10.0,
                ),
            )
        return self._client

    def _build_payload(
        self,
        messages: list[ChatMessage],
        tools: list[dict[str, Any]] | None,
        temperature: float | None,
        max_tokens: int | None,
        stream: bool = False,
    ) -> dict[str, Any]:
        payload: dict[str, Any] = {
            "model": settings.llm_model,
            "messages": [self._format_message(m) for m in messages],
            "temperature": temperature if temperature is not None else settings.llm_temperature,
            "max_tokens": max_tokens if max_tokens is not None else settings.llm_max_tokens,
            "stream": stream,
        }

        if tools:
            payload["tools"] = tools
            payload["tool_choice"] = "auto"

        return payload

    @staticmethod
    def _format_message(msg: ChatMessage) -> dict[str, Any]:
        formatted: dict[str, Any] = {"role": msg.role}

        if msg.content is not None:
            formatted["content"] = msg.content

        if msg.tool_calls is not None:
            formatted["tool_calls"] = msg.tool_calls

        if msg.tool_call_id is not None:
            formatted["tool_call_id"] = msg.tool_call_id

        # Ensure content key exists for tool role messages
        if msg.role == "tool" and "content" not in formatted:
            formatted["content"] = ""

        return formatted

    def _handle_error_response(self, response: httpx.Response) -> None:
        status = response.status_code

        try:
            body = response.json()
            error_msg = body.get("error", {}).get("message", response.text)
        except (json.JSONDecodeError, ValueError):
            error_msg = response.text

        if status == 429:
            retry_after = response.headers.get("retry-after")
            raise RateLimitError(
                f"Groq rate limit exceeded: {error_msg}",
                retry_after=float(retry_after) if retry_after else None,
            )

        if status in (401, 403):
            raise AuthenticationError(f"Groq authentication failed: {error_msg}")

        if status >= 500:
            raise ServerError(f"Groq server error: {error_msg}", status_code=status)

        raise ProviderError(
            f"Groq API error ({status}): {error_msg}",
            status_code=status,
            retryable=False,
        )

    async def chat(
        self,
        messages: list[ChatMessage],
        tools: list[dict[str, Any]] | None = None,
        temperature: float | None = None,
        max_tokens: int | None = None,
    ) -> ChatResponse:
        """Send a non-streaming chat completion request."""
        payload = self._build_payload(messages, tools, temperature, max_tokens, stream=False)
        start = time.perf_counter()

        try:
            response = await self.client.post("/chat/completions", json=payload)
        except httpx.TimeoutException as exc:
            raise ProviderError(
                f"Groq request timed out: {exc}", retryable=True
            ) from exc
        except httpx.ConnectError as exc:
            raise ProviderError(
                f"Groq connection failed: {exc}", retryable=True
            ) from exc

        latency_ms = (time.perf_counter() - start) * 1000.0

        if response.status_code != 200:
            self._handle_error_response(response)

        data = response.json()
        choice = data["choices"][0]
        message = choice["message"]
        usage = data.get("usage", {})

        tool_calls = message.get("tool_calls")
        if tool_calls:
            tool_calls = [
                {
                    "id": tc["id"],
                    "type": tc["type"],
                    "function": {
                        "name": tc["function"]["name"],
                        "arguments": tc["function"]["arguments"],
                    },
                }
                for tc in tool_calls
            ]

        return ChatResponse(
            content=message.get("content"),
            role=message.get("role", "assistant"),
            tool_calls=tool_calls,
            model=data.get("model", settings.llm_model),
            provider=self.name,
            prompt_tokens=usage.get("prompt_tokens", 0),
            completion_tokens=usage.get("completion_tokens", 0),
            latency_ms=round(latency_ms, 2),
        )

    async def chat_stream(
        self,
        messages: list[ChatMessage],
        tools: list[dict[str, Any]] | None = None,
        temperature: float | None = None,
        max_tokens: int | None = None,
    ) -> AsyncGenerator[str, None]:
        """Send a streaming chat completion request, yielding SSE-formatted chunks."""
        payload = self._build_payload(messages, tools, temperature, max_tokens, stream=True)

        try:
            async with self.client.stream(
                "POST", "/chat/completions", json=payload
            ) as response:
                if response.status_code != 200:
                    await response.aread()
                    self._handle_error_response(response)

                accumulated_tool_calls: dict[int, dict[str, Any]] = {}
                prompt_tokens = 0
                completion_tokens = 0

                async for raw_line in response.aiter_lines():
                    line = raw_line.strip()
                    if not line:
                        continue

                    if line.startswith("data: "):
                        line = line[6:]

                    if line == "[DONE]":
                        # Emit any accumulated tool calls
                        for _idx in sorted(accumulated_tool_calls.keys()):
                            tc = accumulated_tool_calls[_idx]
                            event = json.dumps({"type": "tool_call", "tool_call": tc})
                            yield f"data: {event}\n\n"

                        done_event = json.dumps(
                            {
                                "type": "done",
                                "usage": {
                                    "prompt_tokens": prompt_tokens,
                                    "completion_tokens": completion_tokens,
                                },
                            }
                        )
                        yield f"data: {done_event}\n\n"
                        return

                    try:
                        chunk = json.loads(line)
                    except json.JSONDecodeError:
                        continue

                    # Extract usage if present (Groq includes it in the final chunk)
                    if "usage" in chunk and chunk["usage"]:
                        prompt_tokens = chunk["usage"].get("prompt_tokens", prompt_tokens)
                        completion_tokens = chunk["usage"].get(
                            "completion_tokens", completion_tokens
                        )

                    if not chunk.get("choices"):
                        continue

                    delta = chunk["choices"][0].get("delta", {})

                    # Text content
                    if delta.get("content"):
                        event = json.dumps(
                            {"type": "text", "content": delta["content"]}
                        )
                        yield f"data: {event}\n\n"

                    # Tool call deltas — accumulate fragments
                    if delta.get("tool_calls"):
                        for tc_delta in delta["tool_calls"]:
                            idx = tc_delta.get("index", 0)
                            if idx not in accumulated_tool_calls:
                                accumulated_tool_calls[idx] = {
                                    "id": tc_delta.get("id", ""),
                                    "type": tc_delta.get("type", "function"),
                                    "function": {
                                        "name": "",
                                        "arguments": "",
                                    },
                                }
                            tc_ref = accumulated_tool_calls[idx]
                            if tc_delta.get("id"):
                                tc_ref["id"] = tc_delta["id"]
                            if tc_delta.get("type"):
                                tc_ref["type"] = tc_delta["type"]
                            fn = tc_delta.get("function", {})
                            if fn.get("name"):
                                tc_ref["function"]["name"] += fn["name"]
                            if fn.get("arguments"):
                                tc_ref["function"]["arguments"] += fn["arguments"]

        except httpx.TimeoutException as exc:
            raise ProviderError(
                f"Groq stream timed out: {exc}", retryable=True
            ) from exc
        except httpx.ConnectError as exc:
            raise ProviderError(
                f"Groq stream connection failed: {exc}", retryable=True
            ) from exc

    async def close(self) -> None:
        """Close the underlying HTTP client."""
        if self._client and not self._client.is_closed:
            await self._client.aclose()
