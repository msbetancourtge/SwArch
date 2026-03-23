from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from database import get_db
from service import TableService
from schemas import (
    TableCreate, TableResponse, TableStatusResponse, TablesListResponse,
    OcuparTableRequest, ReservarTableRequest
)
from typing import List
import logging

logger = logging.getLogger(__name__)

# Create router
router = APIRouter(prefix="/api/tables", tags=["tables"])


@router.post("", response_model=TableResponse, status_code=status.HTTP_201_CREATED)
def create_table(table: TableCreate, db: Session = Depends(get_db)):
    """Create a new table"""
    try:
        result = TableService.create_table(db, table)
        return result
    except Exception as e:
        logger.error(f"Error creating table: {e}")
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{table_id}", response_model=TableResponse)
def get_table(table_id: int, db: Session = Depends(get_db)):
    """Get table by ID"""
    table = TableService.get_table(db, table_id)
    if not table:
        raise HTTPException(status_code=404, detail="Table not found")
    return table


@router.get("/restaurant/{restaurant_id}", response_model=List[TableResponse])
def get_restaurant_tables(restaurant_id: int, db: Session = Depends(get_db)):
    """Get all tables for a restaurant"""
    tables = TableService.get_all_tables(db, restaurant_id)
    return tables


@router.get("/restaurant/{restaurant_id}/status", response_model=TablesListResponse)
def get_restaurant_status(restaurant_id: int, db: Session = Depends(get_db)):
    """Get all tables grouped by status for a restaurant"""
    return TableService.get_tables_status(db, restaurant_id)


@router.get("/restaurant/{restaurant_id}/summary", response_model=TableStatusResponse)
def get_status_summary(restaurant_id: int, db: Session = Depends(get_db)):
    """Get status summary for a restaurant"""
    return TableService.get_status_summary(db, restaurant_id)


@router.post("/{table_id}/occupy", response_model=TableResponse)
def occupy_table(table_id: int, request: OcuparTableRequest, db: Session = Depends(get_db)):
    """Occupy a table"""
    table = TableService.ocupar_mesa(db, table_id, request)
    if not table:
        raise HTTPException(status_code=404, detail="Table not found or not available")
    return table


@router.post("/{table_id}/release", response_model=TableResponse)
def release_table(table_id: int, db: Session = Depends(get_db)):
    """Release a table (mark for cleaning)"""
    table = TableService.liberar_mesa(db, table_id)
    if not table:
        raise HTTPException(status_code=404, detail="Table not found")
    return table


@router.post("/{table_id}/finish-cleaning", response_model=TableResponse)
def finish_cleaning(table_id: int, db: Session = Depends(get_db)):
    """Finish cleaning a table"""
    table = TableService.finalizar_limpieza(db, table_id)
    if not table:
        raise HTTPException(status_code=404, detail="Table not found or not being cleaned")
    return table


@router.post("/{table_id}/reserve", response_model=TableResponse)
def reserve_table(table_id: int, request: ReservarTableRequest, db: Session = Depends(get_db)):
    """Reserve a table"""
    table = TableService.reservar_mesa(db, table_id, request.reservada_hasta)
    if not table:
        raise HTTPException(status_code=404, detail="Table not found or not available")
    return table


@router.delete("/{table_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_table(table_id: int, db: Session = Depends(get_db)):
    """Delete a table"""
    success = TableService.delete_table(db, table_id)
    if not success:
        raise HTTPException(status_code=404, detail="Table not found")
    return None
