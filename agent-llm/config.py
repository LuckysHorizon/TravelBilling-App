from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore",
        protected_namespaces=(),
    )

    # LLM Provider
    groq_api_key_fallback: str  # Reads GROQ_API_KEY_FALLBACK env var
    llm_model: str = "llama-3.3-70b-versatile"
    llm_temperature: float = 0.3
    llm_max_tokens: int = 2048
    llm_timeout_ms: int = 15000

    # Rate limiting
    rate_limit_requests_per_minute: int = 30
    rate_limit_tokens_per_minute: int = 40000

    # Service
    service_port: int = 8001
    log_level: str = "INFO"


settings = Settings()
