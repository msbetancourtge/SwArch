from sqlalchemy.orm import Session
from repository import TableRepository
from schemas import TableCreate, TableResponse, TableStatusResponse, TablesListResponse, OcuparTableRequest
from models import TableStatus
from datetime import datetime
from typing import List, Optional
import logging

logger = logging.getLogger(__name__)


class TableService:
    """Business logic for table management"""
    
    @staticmethod
    def create_table(db: Session, table_data: TableCreate) -> TableResponse:
        """Create a new table"""
        table = TableRepository.create(
            db,
            numero=table_data.numero,
            capacidad=table_data.capacidad,
            restaurant_id=table_data.restaurant_id
        )
        return TableResponse.from_orm(table)
    
    @staticmethod
    def get_table(db: Session, table_id: int) -> Optional[TableResponse]:
        """Get table by ID"""
        table = TableRepository.get_by_id(db, table_id)
        if not table:
            return None
        return TableResponse.from_orm(table)
    
    @staticmethod
    def get_all_tables(db: Session, restaurant_id: int) -> List[TableResponse]:
        """Get all tables for a restaurant"""
        tables = TableRepository.get_by_restaurant(db, restaurant_id)
        return [TableResponse.from_orm(t) for t in tables]
    
    @staticmethod
    def get_tables_status(db: Session, restaurant_id: int) -> TablesListResponse:
        """Get all tables grouped by status"""
        disponibles = [TableResponse.from_orm(t) for t in TableRepository.get_disponibles(db, restaurant_id)]
        ocupadas = [TableResponse.from_orm(t) for t in TableRepository.get_ocupadas(db, restaurant_id)]
        limpiando = [TableResponse.from_orm(t) for t in TableRepository.get_limpiando(db, restaurant_id)]
        reservadas = [TableResponse.from_orm(t) for t in TableRepository.get_reservadas(db, restaurant_id)]
        
        return TablesListResponse(
            restaurant_id=restaurant_id,
            total=len(disponibles) + len(ocupadas) + len(limpiando) + len(reservadas),
            disponibles=disponibles,
            ocupadas=ocupadas,
            limpiando=limpiando,
            reservadas=reservadas
        )
    
    @staticmethod
    def get_status_summary(db: Session, restaurant_id: int) -> TableStatusResponse:
        """Get status summary for a restaurant"""
        counts = TableRepository.count_by_status(db, restaurant_id)
        return TableStatusResponse(**counts)
    
    @staticmethod
    def ocupar_mesa(db: Session, table_id: int, request: OcuparTableRequest) -> Optional[TableResponse]:
        """Occupy a table"""
        table = TableRepository.get_by_id(db, table_id)
        if not table:
            logger.warning(f"Table {table_id} not found")
            return None
        
        if table.status != TableStatus.DISPONIBLE:
            logger.warning(f"Table {table_id} is not available ({table.status.value})")
            return None
        
        table = TableRepository.ocupar(db, table_id, request.numero_cliente)
        return TableResponse.from_orm(table)
    
    @staticmethod
    def liberar_mesa(db: Session, table_id: int) -> Optional[TableResponse]:
        """Release a table (mark for cleaning)"""
        table = TableRepository.get_by_id(db, table_id)
        if not table:
            logger.warning(f"Table {table_id} not found")
            return None
        
        table = TableRepository.liberar(db, table_id)
        return TableResponse.from_orm(table)
    
    @staticmethod
    def finalizar_limpieza(db: Session, table_id: int) -> Optional[TableResponse]:
        """Finish cleaning a table"""
        table = TableRepository.get_by_id(db, table_id)
        if not table:
            logger.warning(f"Table {table_id} not found")
            return None
        
        if table.status != TableStatus.LIMPIANDO:
            logger.warning(f"Table {table_id} is not being cleaned")
            return None
        
        table = TableRepository.finalizar_limpieza(db, table_id)
        return TableResponse.from_orm(table)
    
    @staticmethod
    def reservar_mesa(db: Session, table_id: int, reservada_hasta: datetime) -> Optional[TableResponse]:
        """Reserve a table"""
        table = TableRepository.get_by_id(db, table_id)
        if not table:
            logger.warning(f"Table {table_id} not found")
            return None
        
        if table.status != TableStatus.DISPONIBLE:
            logger.warning(f"Table {table_id} is not available for reservation")
            return None
        
        table = TableRepository.reservar(db, table_id, reservada_hasta)
        return TableResponse.from_orm(table)
    
    @staticmethod
    def delete_table(db: Session, table_id: int) -> bool:
        """Delete a table"""
        return TableRepository.delete(db, table_id)
