import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    """Application configuration"""
    
    # Database
    DATABASE_URL: str = os.getenv(
        "DATABASE_URL",
        "postgresql://mike:secret@localhost:5437/table_db"
    )
    
    # Server
    SERVER_PORT: int = int(os.getenv("SERVER_PORT", "8085"))
    SERVER_HOST: str = os.getenv("SERVER_HOST", "0.0.0.0")
    
    # App
    APP_NAME: str = "TableService"
    APP_VERSION: str = "1.0.0"
    
    class Config:
        env_file = ".env"

settings = Settings()
