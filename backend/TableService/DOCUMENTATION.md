# TableService - Complete Documentation

## Overview

**TableService** is a Python-based microservice for managing restaurant tables (mesas) in the Click & Munch ecosystem. It handles table occupancy status, customer assignments, reservations, and cleaning states with persistent data storage.

**Port**: 8085  
**Database**: PostgreSQL (5437)  
**Framework**: FastAPI  
**Deployment**: Docker + Docker Compose

---

## Architecture Overview

```
                    API Gateway (8080)
                           │
                           ↓
                  ┌─────────────────┐
                  │  TableService   │
                  │   (Port 8085)   │
                  └────────┬────────┘
                           │
                    ┌──────┴──────┐
                    ↓             ↓
              Routes          Service
              (APIs)         (Logic)
                    ↓             ↓
              Repository ─── Database
                         PostgreSQL
                         5437
```

### Current Status: **Standalone Component**
The service operates independently with no external service dependencies. It can be integrated with other microservices as needed.

---

## File Structure & Responsibilities

### 1. **main.py** - FastAPI Application Entry Point
**Purpose**: Initializes and configures the FastAPI application.

**What it does**:
- Creates the FastAPI app with lifespan management
- Adds CORS middleware for cross-origin requests
- Registers API routes from `routes.py`
- Sets up health check endpoint
- Manages startup/shutdown lifecycle

**Key endpoints**:
```python
GET  /              # Root endpoint
GET  /health        # Health check
GET  /docs          # Interactive API documentation (Swagger)
POST /api/tables/** # All table management endpoints
```

**Dependencies**: 
- FastAPI
- CORS middleware
- Routes module

---

### 2. **config.py** - Configuration Management
**Purpose**: Centralizes all application configuration settings.

**What it does**:
- Reads environment variables
- Provides default values for local development
- Uses Pydantic's BaseSettings for validation
- Loads `.env` file if present

**Configuration variables**:
```python
DATABASE_URL = "postgresql://mike:secret@table-db:5432/table_db"  # From env or default
SERVER_PORT = 8085                                                # From env or default
SERVER_HOST = "0.0.0.0"                                          # From env or default
APP_NAME = "TableService"
APP_VERSION = "1.0.0"
```

**How to use**: Import `settings` and access properties:
```python
from config import settings
print(settings.DATABASE_URL)
print(settings.SERVER_PORT)
```

**Environment Variables** (set in docker-compose.yml):
- `DATABASE_URL`: PostgreSQL connection string
- `SERVER_PORT`: Port to run FastAPI on
- `SERVER_HOST`: Host binding (0.0.0.0 = all interfaces)

---

### 3. **database.py** - Database Connection & Initialization
**Purpose**: Manages database connectivity and session lifecycle.

**What it does**:
- Creates SQLAlchemy engine with PostgreSQL
- Provides session factory (SessionLocal)
- Implements dependency injection for DB sessions
- Auto-creates tables on startup

**Key functions**:

```python
get_db()        # FastAPI dependency - yields DB sessions
init_db()       # Called on app startup to create all tables
engine          # SQLAlchemy engine for raw PostgreSQL connection
SessionLocal     # Factory for creating DB sessions
Base             # ORM declarative base class
```

**How it works**:
1. Engine connects to PostgreSQL via `DATABASE_URL`
2. `SessionLocal` factory creates new sessions
3. `get_db()` used as FastAPI dependency injection
4. `init_db()` called at startup to create schema

**Connection pooling**: Enabled with `pool_pre_ping=True` (checks if connection is alive before use)

---

### 4. **models.py** - SQLAlchemy ORM Models
**Purpose**: Defines the database schema through Python classes.

**What it does**:
- Defines `Table` entity mapping to PostgreSQL table
- Defines `TableStatus` enum for state management
- All columns with types and constraints

**Table Entity**:
```python
Table:
  - id (Primary Key)
  - restaurant_id (FK to restaurants)
  - numero (String: "Mesa 1", "Table A", etc.)
  - capacidad (Integer: seating capacity)
  - status (Enum: disponible, ocupada, limpiando, reservada)
  - cliente (String: customer ID/number)
  - ocupada_desde (DateTime: when occupied)
  - reservada_hasta (DateTime: reservation end time)
  - created_at (DateTime: creation timestamp)
  - updated_at (DateTime: last modification timestamp)
```

**TableStatus Enum**:
```python
DISPONIBLE = "disponible"  # Available for guests
OCUPADA = "ocupada"        # Currently occupied
LIMPIANDO = "limpiando"    # Being cleaned
RESERVADA = "reservada"    # Reserved for future time
```

**Database migrations**: Tables auto-created on app startup via `Base.metadata.create_all()`

---

### 5. **schemas.py** - Pydantic DTOs (Data Transfer Objects)
**Purpose**: Defines request/response validation and serialization.

**What it does**:
- Validates incoming HTTP requests
- Serializes database objects to JSON responses
- Provides OpenAPI documentation
- Type hints for IDE support

**Key schemas**:

```python
# Input schemas (what client sends)
TableCreate                # POST /api/tables
OcuparTableRequest        # POST /api/tables/{id}/occupy
ReservarTableRequest      # POST /api/tables/{id}/reserve

# Output schemas (what API returns)
TableResponse             # Single table response
TablesListResponse        # Multiple tables grouped by status
TableStatusResponse       # Status summary (counts)

# Enum
TableStatus               # Same as models.TableStatus
```

**Example usage**:
```python
# Client sends this (validated against OcuparTableRequest)
{"numero_cliente": "Cliente #101"}

# API returns this (serialized from TableResponse)
{
  "id": 1,
  "numero": "Mesa 1",
  "capacidad": 4,
  "status": "ocupada",
  "cliente": "Cliente #101",
  "ocupada_desde": "2026-03-18T19:30:00",
  "restaurant_id": 1,
  ...
}
```

---

### 6. **repository.py** - Data Access Layer (Repository Pattern)
**Purpose**: Encapsulates all database queries and operations.

**What it does**:
- Abstracts database logic from business logic
- Provides CRUD operations for Table entity
- Implements query methods for filtering
- Manages transactions and commits

**Key methods**:

```python
# CRUD Operations
create()                    # Create new table
get_by_id()                # Get single table
get_by_restaurant()        # Get all tables for restaurant
delete()                   # Delete table

# Status Queries
get_disponibles()          # Get available tables
get_ocupadas()             # Get occupied tables
get_limpiando()            # Get tables being cleaned
get_reservadas()           # Get reserved tables

# State Modifications
ocupar()                   # Mark as occupied + assign client
liberar()                  # Mark for cleaning
finalizar_limpieza()       # Mark as available
reservar()                 # Mark as reserved

# Reporting
count_by_status()          # Count tables per status
```

**Design Pattern**: Repository Pattern
- Separates data access from business logic
- Easy to test (mock repository)
- Easy to switch database implementations

---

### 7. **service.py** - Business Logic Layer
**Purpose**: Implements application logic and business rules.

**What it does**:
- Calls repository for data access
- Validates business rules
- Coordinates complex operations
- Handles errors gracefully

**Key methods** (all take `db: Session` parameter):

```python
create_table()             # Create new table with validation
get_table()                # Retrieve table details
get_all_tables()           # List all tables for restaurant
get_tables_status()        # Get tables grouped by status
get_status_summary()       # Get status counts

# Operations
ocupar_mesa()              # Occupy table (validate availability)
liberar_mesa()             # Mark for cleaning
finalizar_limpieza()       # Complete cleaning
reservar_mesa()            # Reserve table (validate time)
delete_table()             # Delete table
```

**Business Logic Examples**:
```python
# occupar_mesa() checks:
- Table exists?
- Status is DISPONIBLE?

# reservar_mesa() checks:
- Table exists?
- Status is DISPONIBLE?
- Reservation time is valid?
```

---

### 8. **routes.py** - HTTP Endpoints (Controllers)
**Purpose**: Defines REST API endpoints and HTTP handling.

**What it does**:
- Maps HTTP methods to service logic
- Handles request/response serialization
- Returns proper HTTP status codes
- Throws appropriate HTTP exceptions

**Endpoint Structure**:

```
POST   /api/tables
       Create new table
       Input: TableCreate
       Output: TableResponse (201)

GET    /api/tables/{table_id}
       Get table by ID
       Output: TableResponse (200) or 404

GET    /api/tables/restaurant/{restaurant_id}
       Get all tables for restaurant
       Output: List[TableResponse]

GET    /api/tables/restaurant/{restaurant_id}/summary
       Get status counts
       Output: TableStatusResponse

GET    /api/tables/restaurant/{restaurant_id}/status
       Get tables grouped by status
       Output: TablesListResponse

POST   /api/tables/{table_id}/occupy
       Mark table as occupied
       Input: OcuparTableRequest
       Output: TableResponse (200) or 404/400

POST   /api/tables/{table_id}/release
       Mark for cleaning
       Output: TableResponse (200) or 404

POST   /api/tables/{table_id}/finish-cleaning
       Mark as available
       Output: TableResponse (200) or 404

POST   /api/tables/{table_id}/reserve
       Make reservation
       Input: ReservarTableRequest
       Output: TableResponse (200) or 404/400

DELETE /api/tables/{table_id}
       Delete table
       Output: 204 No Content
```

**HTTP Status Codes**:
- `200 OK`: Successful GET/POST
- `201 Created`: Successful POST (creation)
- `204 No Content`: Successful DELETE
- `400 Bad Request`: Invalid validation or operation
- `404 Not Found`: Resource doesn't exist

---

### 9. **docker-compose.yml** - Service Orchestration
**Purpose**: Defines multi-container orchestration for local development and testing.

**What it does**:
- Configures PostgreSQL database container
- Configures TableService Python container
- Sets environment variables
- Defines networking between services
- Manages volumes for data persistence
- Implements health checks

**Services defined**:

```yaml
table-db:
  - Image: postgres:16
  - Port: 5437 (maps to 5432 inside container)
  - Database: table_db
  - User: mike / Password: secret
  - Volume: table_data (persists data)
  - Health check: pg_isready

tableservice:
  - Build: . (current directory)
  - Port: 8085
  - Depends on: table-db
  - Environment variables: DATABASE_URL, SERVER_PORT, SERVER_HOST
  - Health check: HTTP GET /health
```

**Network**: appnet (bridge network) - allows services to communicate by hostname

**Volume**: table_data - persists PostgreSQL data between container restarts

---

### 10. **Dockerfile** - Container Image Definition
**Purpose**: Defines how to build the Docker image for TableService.

**What it does**:
- Uses Python 3.11-slim as base image
- Installs system dependencies (gcc, postgresql-client)
- Copies application code
- Installs Python packages from requirements.txt
- Exposes port 8085
- Defines health check
- Sets startup command

**Build stages**:
1. Base image: `python:3.11-slim`
2. Install system packages
3. Copy requirements.txt
4. pip install dependencies
5. Copy application code
6. Health check setup
7. Expose port 8085
8. Run uvicorn server

**Health check**: 
```bash
python -c "import requests; requests.get('http://localhost:8085/health')"
```
Runs every 10 seconds, 5-second timeout, 3 retries

---

### 11. **requirements.txt** - Python Dependencies
**Purpose**: Lists all Python packages needed for the service.

**Packages**:
- `fastapi==0.104.1` - Web framework
- `uvicorn==0.24.0` - ASGI server to run FastAPI
- `sqlalchemy==2.0.23` - ORM for database
- `psycopg2-binary==2.9.9` - PostgreSQL driver
- `pydantic==2.5.0` - Data validation
- `pydantic-settings==2.1.0` - Configuration management
- `python-dateutil==2.8.2` - Date/time utilities

**Installation**: `pip install -r requirements.txt`

---

### 12. **.gitignore** - Git Ignore Rules
**Purpose**: Specifies files that shouldn't be committed to version control.

**Ignored patterns**:
- Python cache and compiled files (`__pycache__/`, `*.pyc`)
- Virtual environments (`venv/`, `ENV/`, `env/`)
- IDE files (`.vscode/`, `.idea/`)
- Environment files (`.env`)
- Testing artifacts (`.pytest_cache/`, `.coverage`)
- Log files (`*.log`)

---

### 13. **README.md** - User Documentation
**Purpose**: Provides quick start guide and API documentation for developers.

**Contents**:
- Feature overview
- Architecture diagram
- Docker setup instructions
- API endpoints reference
- Example curl requests
- Testing instructions
- Integration guidelines

---

## Data Flow

### Scenario: Occupy a Table

```
1. Client sends HTTP request:
   POST /api/tables/1/occupy
   {"numero_cliente": "Cliente #101"}
                    ↓
2. routes.py receives request:
   - Validates JSON against OcuparTableRequest schema
   - Calls TableService.ocupar_mesa()
                    ↓
3. service.py executes business logic:
   - Checks table exists via repository.get_by_id()
   - Checks status is DISPONIBLE
   - Calls repository.ocupar()
                    ↓
4. repository.py updates database:
   - Updates table.status = OCUPADA
   - Sets table.cliente = "Cliente #101"
   - Sets table.ocupada_desde = now()
   - Commits to PostgreSQL
                    ↓
5. service.py converts response:
   - Creates TableResponse from updated table
                    ↓
6. routes.py returns HTTP response:
   {
     "id": 1,
     "numero": "Mesa 1",
     "status": "ocupada",
     "cliente": "Cliente #101",
     "ocupada_desde": "2026-03-18T19:30:00",
     ...
   }
```

---

## Integration Points with Other Services

### Potential Integrations

#### 1. **RestaurantService Integration**
**When**: Restaurant needs to manage its tables

**Flow**:
```
RestaurantService (8082)
         ↓
TableService (8085)
```

**Communication**:
```python
# RestaurantService would call:
POST   /api/tables
GET    /api/tables/restaurant/{restaurant_id}/status
POST   /api/tables/{id}/occupy
```

**Environment variable** (in docker-compose):
```yaml
RESTAURANT_SERVICE_URL: http://restaurantservice:8082
```

**Benefits**:
- Restaurants can manage their own tables
- Track occupancy in real-time
- Monitor table turnover

---

#### 2. **AuthService Integration** 
**When**: Need to verify client identity or staff permissions
**Current Status**: NOT IMPLEMENTED (standalone)

**Potential implementation**:
```python
# In routes.py, add JWT validation
from fastapi import Depends
from fastapi.security import HTTPBearer

security = HTTPBearer()

@router.post("/{table_id}/occupy")
async def occupy_table(
    table_id: int,
    request: OcuparTableRequest,
    token: HTTPAuthCredential = Depends(security),
    db: Session = Depends(get_db)
):
    # Validate token with AuthService
    # Then proceed with occupying table
    pass
```

**Environment variable needed**:
```yaml
AUTH_SERVICE_URL: http://authservice:8081
```

---

#### 3. **OrderService Integration** (Future)
**When**: Need to link orders to tables

**Concept**:
```
Table → Order
Mesa → Pedido

POST /api/orders
{
  "table_id": 1,
  "items": [...]
}
```

**Flow**:
1. OrderService calls TableService to get table info
2. OrderService creates order linked to table_id
3. OrderService can query table status

---

#### 4. **AnalyticsService Integration** (Future)
**When**: Need to track table metrics and performance

**Metrics to track**:
- Average occupancy time per table
- Table turnover rate
- Peak hours analysis
- Table utilization percentage

**API needed**:
```python
GET /api/tables/restaurant/{id}/analytics
    Returns time-series data for occupancy
```

---

## How to Add Integration with Another Service

### Step 1: Update docker-compose.yml
```yaml
tableservice:
  environment:
    AUTH_SERVICE_URL: http://authservice:8081
    RESTAURANT_SERVICE_URL: http://restaurantservice:8082
  depends_on:
    authservice:
      condition: service_healthy
```

### Step 2: Create API Client
Create `clients/auth_client.py`:
```python
import httpx
from config import settings

class AuthClient:
    def __init__(self):
        self.base_url = settings.AUTH_SERVICE_URL
    
    async def verify_token(self, token: str):
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.base_url}/api/auth/verify",
                headers={"Authorization": f"Bearer {token}"}
            )
            return response.json()
```

### Step 3: Update Routes to Use Client
```python
from clients.auth_client import AuthClient

auth_client = AuthClient()

@router.post("/{table_id}/occupy")
async def occupy_table(
    table_id: int,
    request: OcuparTableRequest,
    token: str = Header(...),
    db: Session = Depends(get_db)
):
    # Verify with AuthService
    user = await auth_client.verify_token(token)
    if not user:
        raise HTTPException(status_code=401)
    
    # Proceed with service
    table = TableService.ocupar_mesa(db, table_id, request)
    return table
```

---

## Testing the Service

### With Docker Compose
```bash
cd backend/TableService
docker compose up --build
```

### API Documentation
```
http://localhost:8085/docs
```

### Sample Requests

**Create Table**:
```bash
curl -X POST http://localhost:8085/api/tables \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "Mesa 1",
    "capacidad": 4,
    "restaurant_id": 1
  }'
```

**Get Restaurant Status**:
```bash
curl http://localhost:8085/api/tables/restaurant/1/status
```

**Occupy Table**:
```bash
curl -X POST http://localhost:8085/api/tables/1/occupy \
  -H "Content-Type: application/json" \
  -d '{"numero_cliente": "Cliente #101"}'
```

---

## Performance Considerations

### Current Optimizations
1. **Connection Pooling**: SQLAlchemy pooling enabled
2. **Health Checks**: Both services verify health before use
3. **Indexed Columns**: restaurant_id, status indexed for fast queries

### Future Optimizations
1. **Caching**: Redis for frequently accessed data
2. **Async Operations**: Convert to async FastAPI/SQLAlchemy
3. **Database Indexing**: Add composite indexes on common queries
4. **API Rate Limiting**: Prevent table spam

---

## Deployment Scenarios

### Local Development
```bash
docker compose up --build
```
- Creates isolated environment
- Persists data in volumes
- Auto-creates schema

### Integration with Main Stack
Update `/backend/docker-compose.services.yml`:
```yaml
include:
  - path: ./TableService/docker-compose.yml
```

Then in APIGateway, add routes:
```yaml
- id: tableservice
  uri: http://tableservice:8085
  predicates:
    - Path=/table/**
  filters:
    - RewritePath=/table/(?<path>.*), /api/tables/$\{path}
```

---

## Troubleshooting

### TableService won't connect to database
**Issue**: `could not translate host name "table-db" to address`
**Fix**: Ensure both services on same network in docker-compose.yml

### Health check failing
**Issue**: Service container keeps restarting
**Fix**: Check logs: `docker compose logs tableservice`

### Port already in use
**Issue**: `Error: Port 8085 already in use`
**Fix**: 
```bash
docker compose down -v
docker compose up --build
```

---

## Summary

| Component | Purpose | Layer |
|-----------|---------|-------|
| main.py | FastAPI app setup | Application |
| routes.py | HTTP endpoints | Presentation |
| service.py | Business logic | Business |
| repository.py | Data access | Data |
| models.py | Database schema | Data |
| schemas.py | Request/response validation | Data |
| database.py | DB connection management | Infrastructure |
| config.py | Configuration management | Infrastructure |
| Dockerfile | Container definition | Deployment |
| docker-compose.yml | Multi-container orchestration | Deployment |

This architecture follows clean architecture principles with clear separation of concerns, making it easy to test, maintain, and extend.

