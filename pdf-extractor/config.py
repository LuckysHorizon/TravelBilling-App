"""
TravelBilling PDF Extractor — Configuration
=============================================
Centralized config using Pydantic Settings.

Environment is controlled by the APP_ENV variable:
  - "development" (default): verbose logging, debug endpoints enabled
  - "production": strict mode, debug endpoints disabled, tight CORS
"""

from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Literal


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore",
        protected_namespaces=(),
    )

    # ── Environment ───────────────────────────────────────────
    app_env: Literal["development", "production"] = "development"

    # ── Groq API ──────────────────────────────────────────────
    groq_api_key: str
    groq_api_url: str = "https://api.groq.com/openai/v1/chat/completions"
    groq_model: str = "meta-llama/llama-4-scout-17b-16e-instruct"
    groq_timeout_seconds: int = 60
    groq_rate_limit_per_minute: int = 30

    # ── Service ───────────────────────────────────────────────
    service_port: int = 8000
    log_level: str = "INFO"

    # ── CORS ──────────────────────────────────────────────────
    cors_allowed_origins: str = "*"  # Tightened in production via env var

    # ── Derived Properties ────────────────────────────────────

    @property
    def is_production(self) -> bool:
        return self.app_env == "production"

    @property
    def is_development(self) -> bool:
        return self.app_env == "development"

    @property
    def cors_origins_list(self) -> list[str]:
        """Parse comma-separated origins into a list."""
        if self.cors_allowed_origins == "*":
            return ["*"]
        return [o.strip() for o in self.cors_allowed_origins.split(",") if o.strip()]


settings = Settings()
