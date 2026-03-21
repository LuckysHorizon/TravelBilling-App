from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore",
        protected_namespaces=(),
    )

    groq_api_key: str
    groq_api_url: str = "https://api.groq.com/openai/v1/chat/completions"
    groq_model: str = "meta-llama/llama-4-scout-17b-16e-instruct"
    groq_timeout_seconds: int = 60
    groq_rate_limit_per_minute: int = 30
    service_port: int = 8000


settings = Settings()
