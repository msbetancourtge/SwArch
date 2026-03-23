from sqlalchemy import Column, Integer, String, Enum, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from database import Base
from datetime import datetime
import enum

class TableStatus(str, enum.Enum):
    """Enum for table status"""
    DISPONIBLE = "disponible"
    OCUPADA = "ocupada"
    LIMPIANDO = "limpiando"
    RESERVADA = "reservada"


class Table(Base):
    """Table entity"""
    __tablename__ = "tables"
    
    id = Column(Integer, primary_key=True, index=True)
    restaurant_id = Column(Integer, nullable=False, index=True)
    numero = Column(String(50), nullable=False)
    capacidad = Column(Integer, nullable=False)
    status = Column(
        Enum(TableStatus),
        default=TableStatus.DISPONIBLE,
        nullable=False
    )
    cliente = Column(String(100), nullable=True)
    ocupada_desde = Column(DateTime, nullable=True)
    reservada_hasta = Column(DateTime, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)
    
    def __repr__(self):
        return f"<Table {self.numero} - {self.status.value}>"
