from pydantic import BaseModel, Field
from datetime import datetime
from enum import Enum
from typing import Optional, List


class TableStatus(str, Enum):
    """Table status enum"""
    DISPONIBLE = "disponible"
    OCUPADA = "ocupada"
    LIMPIANDO = "limpiando"
    RESERVADA = "reservada"


class TableBase(BaseModel):
    """Base table schema"""
    numero: str = Field(..., min_length=1, max_length=50)
    capacidad: int = Field(..., gt=0)
    restaurant_id: int = Field(..., gt=0)


class TableCreate(TableBase):
    """Schema for creating a table"""
    pass


class TableUpdate(BaseModel):
    """Schema for updating a table"""
    numero: Optional[str] = None
    capacidad: Optional[int] = None
    status: Optional[TableStatus] = None


class TableResponse(TableBase):
    """Schema for table response"""
    id: int
    status: TableStatus
    cliente: Optional[str] = None
    ocupada_desde: Optional[datetime] = None
    reservada_hasta: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime
    
    class Config:
        from_attributes = True


class OcuparTableRequest(BaseModel):
    """Request to occupy a table"""
    numero_cliente: str = Field(..., min_length=1, max_length=100)


class ReservarTableRequest(BaseModel):
    """Request to reserve a table"""
    reservada_hasta: datetime


class TableStatusResponse(BaseModel):
    """Response with table status summary"""
    total: int
    disponibles: int
    ocupadas: int
    limpiando: int
    reservadas: int


class TablesListResponse(BaseModel):
    """Response with list of tables"""
    restaurant_id: int
    total: int
    disponibles: List[TableResponse] = []
    ocupadas: List[TableResponse] = []
    limpiando: List[TableResponse] = []
    reservadas: List[TableResponse] = []
