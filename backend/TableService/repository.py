from sqlalchemy.orm import Session
from models import Table, TableStatus
from typing import List, Optional
from datetime import datetime
import logging

logger = logging.getLogger(__name__)


class TableRepository:
    """Data access layer for tables"""
    
    @staticmethod
    def create(db: Session, numero: str, capacidad: int, restaurant_id: int) -> Table:
        """Create a new table"""
        table = Table(
            numero=numero,
            capacidad=capacidad,
            restaurant_id=restaurant_id
        )
        db.add(table)
        db.commit()
        db.refresh(table)
        logger.info(f"Table {table.id} created for restaurant {restaurant_id}")
        return table
    
    @staticmethod
    def get_by_id(db: Session, table_id: int) -> Optional[Table]:
        """Get table by ID"""
        return db.query(Table).filter(Table.id == table_id).first()
    
    @staticmethod
    def get_by_restaurant(db: Session, restaurant_id: int) -> List[Table]:
        """Get all tables for a restaurant"""
        return db.query(Table).filter(
            Table.restaurant_id == restaurant_id
        ).order_by(Table.numero).all()
    
    @staticmethod
    def get_disponibles(db: Session, restaurant_id: int) -> List[Table]:
        """Get available tables for a restaurant"""
        return db.query(Table).filter(
            Table.restaurant_id == restaurant_id,
            Table.status == TableStatus.DISPONIBLE
        ).all()
    
    @staticmethod
    def get_ocupadas(db: Session, restaurant_id: int) -> List[Table]:
        """Get occupied tables for a restaurant"""
        return db.query(Table).filter(
            Table.restaurant_id == restaurant_id,
            Table.status == TableStatus.OCUPADA
        ).order_by(Table.ocupada_desde).all()
    
    @staticmethod
    def get_limpiando(db: Session, restaurant_id: int) -> List[Table]:
        """Get tables being cleaned"""
        return db.query(Table).filter(
            Table.restaurant_id == restaurant_id,
            Table.status == TableStatus.LIMPIANDO
        ).all()
    
    @staticmethod
    def get_reservadas(db: Session, restaurant_id: int) -> List[Table]:
        """Get reserved tables"""
        return db.query(Table).filter(
            Table.restaurant_id == restaurant_id,
            Table.status == TableStatus.RESERVADA
        ).all()
    
    @staticmethod
    def update_status(db: Session, table_id: int, status: TableStatus) -> Optional[Table]:
        """Update table status"""
        table = TableRepository.get_by_id(db, table_id)
        if not table:
            return None
        
        table.status = status
        table.updated_at = datetime.utcnow()
        db.commit()
        db.refresh(table)
        return table
    
    @staticmethod
    def ocupar(db: Session, table_id: int, numero_cliente: str) -> Optional[Table]:
        """Mark table as occupied"""
        table = TableRepository.get_by_id(db, table_id)
        if not table:
            return None
        
        table.status = TableStatus.OCUPADA
        table.cliente = numero_cliente
        table.ocupada_desde = datetime.utcnow()
        table.updated_at = datetime.utcnow()
        db.commit()
        db.refresh(table)
        logger.info(f"Table {table_id} occupied by client {numero_cliente}")
        return table
    
    @staticmethod
    def liberar(db: Session, table_id: int) -> Optional[Table]:
        """Mark table as cleaning"""
        table = TableRepository.get_by_id(db, table_id)
        if not table:
            return None
        
        table.status = TableStatus.LIMPIANDO
        table.updated_at = datetime.utcnow()
        db.commit()
        db.refresh(table)
        logger.info(f"Table {table_id} marked for cleaning")
        return table
    
    @staticmethod
    def finalizar_limpieza(db: Session, table_id: int) -> Optional[Table]:
        """Mark table as available"""
        table = TableRepository.get_by_id(db, table_id)
        if not table:
            return None
        
        table.status = TableStatus.DISPONIBLE
        table.cliente = None
        table.ocupada_desde = None
        table.updated_at = datetime.utcnow()
        db.commit()
        db.refresh(table)
        logger.info(f"Table {table_id} cleaned and ready for use")
        return table
    
    @staticmethod
    def reservar(db: Session, table_id: int, reservada_hasta: datetime) -> Optional[Table]:
        """Reserve table"""
        table = TableRepository.get_by_id(db, table_id)
        if not table:
            return None
        
        table.status = TableStatus.RESERVADA
        table.reservada_hasta = reservada_hasta
        table.updated_at = datetime.utcnow()
        db.commit()
        db.refresh(table)
        logger.info(f"Table {table_id} reserved until {reservada_hasta}")
        return table
    
    @staticmethod
    def delete(db: Session, table_id: int) -> bool:
        """Delete a table"""
        table = TableRepository.get_by_id(db, table_id)
        if not table:
            return False
        
        db.delete(table)
        db.commit()
        logger.info(f"Table {table_id} deleted")
        return True
    
    @staticmethod
    def count_by_status(db: Session, restaurant_id: int) -> dict:
        """Count tables by status"""
        total = db.query(Table).filter(Table.restaurant_id == restaurant_id).count()
        disponibles = db.query(Table).filter(
            Table.restaurant_id == restaurant_id,
            Table.status == TableStatus.DISPONIBLE
        ).count()
        ocupadas = db.query(Table).filter(
            Table.restaurant_id == restaurant_id,
            Table.status == TableStatus.OCUPADA
        ).count()
        limpiando = db.query(Table).filter(
            Table.restaurant_id == restaurant_id,
            Table.status == TableStatus.LIMPIANDO
        ).count()
        reservadas = db.query(Table).filter(
            Table.restaurant_id == restaurant_id,
            Table.status == TableStatus.RESERVADA
        ).count()
        
        return {
            "total": total,
            "disponibles": disponibles,
            "ocupadas": ocupadas,
            "limpiando": limpiando,
            "reservadas": reservadas
        }
