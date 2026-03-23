# TableService

Microservice for managing restaurant tables (mesas), including occupancy status, client assignment, and timestamps.

## Features

- Create and manage restaurant tables
- Track table occupancy status (disponible, ocupada, limpiando, reservada)
- Assign clients to tables with timestamps
- Reserve tables for specific times
- Get table status summary and details
- Clean separation of concerns (repository, service, routes)

## Architecture

```
Routes (FastAPI endpoints)
    ↓
Service (Business logic)
    ↓
Repository (Data access)
    ↓
Database (PostgreSQL)
```

## Setup (With Docker - Recommended for Testing)

### Prerequisites
- Docker
- Docker Compose

### Quick Start with Docker

1. Navigate to TableService directory:
```bash
cd backend/TableService
```

2. Start the service and database:
```bash
docker-compose up --build
```

This will:
- Build the TableService image
- Start PostgreSQL database (port 5437)
- Start TableService (port 8085)
- Auto-create database tables on startup
- Health check both services

3. Access the service:
- API: `http://localhost:8085`
- API Documentation: `http://localhost:8085/docs`
- Health Check: `http://localhost:8085/health`

4. Stop the service:
```bash
docker-compose down
```

To remove database as well:
```bash
docker-compose down -v
```

---

## Setup (Local Development - Without Docker)

### Prerequisites
- Python 3.11+
- PostgreSQL 16+ (running separately)
- pip

### Installation

1. Create virtual environment:
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

2. Install dependencies:
```bash
pip install -r requirements.txt
```

3. Set environment variables (create `.env` file):
```
DATABASE_URL=postgresql://mike:secret@localhost:5437/table_db
SERVER_PORT=8085
SERVER_HOST=0.0.0.0
```

4. Ensure PostgreSQL is running with table_db database

5. Start the service:
```bash
python main.py
```

Service will be available at `http://localhost:8085`

## API Endpoints

All endpoints use the `/api/tables` base path.

### Table Management

- `POST /` - Create a new table
- `GET /{table_id}` - Get table by ID
- `GET /restaurant/{restaurant_id}` - Get all tables for restaurant
- `DELETE /{table_id}` - Delete a table

### Status Management

- `GET /restaurant/{restaurant_id}/summary` - Get status summary (count by status)
- `GET /restaurant/{restaurant_id}/status` - Get detailed status (tables grouped by status)

### Operations

- `POST /{table_id}/occupy` - Mark table as occupied
- `POST /{table_id}/release` - Mark table for cleaning
- `POST /{table_id}/finish-cleaning` - Mark table as available
- `POST /{table_id}/reserve` - Reserve table until specified time

## Docker Usage

### With Docker Compose (includes database)
```bash
docker-compose up --build
```

### Standalone Docker Run
```bash
docker build -t tableservice:latest .

docker run -p 8085:8085 \
  -e DATABASE_URL=postgresql://mike:secret@postgres:5432/table_db \
  --network appnet \
  tableservice:latest
```

### View logs
```bash
docker-compose logs -f tableservice
```

### Stop containers
```bash
docker-compose stop
```

## Models

### Table
- `id` (int) - Primary key
- `restaurant_id` (int) - Associated restaurant
- `numero` (str) - Table number/name
- `capacidad` (int) - Seating capacity
- `status` (enum) - Current status
- `Testing with Docker Compose

Once running (`docker-compose up`), you can test endpoints:

### Health check
```bash
curl http://localhost:8085/health
```

### Create table
```bash
curl -X POST http://localhost:8085/api/tables \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "Mesa 1",
    "capacidad": 4,
    "restaurant_id": 1
  }'
```

### Create another table
```bash
curl -X POST http://localhost:8085/api/tables \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "Mesa 2",
    "capacidad": 6,
    "restaurant_id": 1
  }'
```

### Occupy table
```bash
curl -X POST http://localhost:8085/api/tables/1/occupy \
  -H "Content-Type: application/json" \
  -d '{"numero_cliente": "Cliente #101"}'
```

### Get restaurant status (all tables grouped by status)
```bash
curl http://localhost:8085/api/tables/restaurant/1/status
```

### Get status summary (counts)
```bash
curl http://localhost:8085/api/tables/restaurant/1/summary
```

### Release table (mark for cleaning)
```bash
curl -X POST http://localhost:8085/api/tables/1/release
```

### Finish cleaning
```bash
curl -X POST http://localhost:8085/api/tables/1/finish-cleaning
```

### Reserve table
```bash
curl -X POST http://localhost:8085/api/tables/2/reserve \
  -H "Content-Type: application/json" \
  -d '{"reservada_hasta": "2026-03-18T20:00:00"}'
```

### Interactive API Documentation
Open in browser: **http://localhost:8085/docs**

This provides an interactive Swagger UI to test all endpoints. Get restaurant status
```bash
curl http://localhost:8085/api/tables/restaurant/1/status
```

### Release table
```bash
curl -X POST http://localhost:8085/api/tables/1/release
```

### Finish cleaning
```bash
curl -X POST http://localhost:8085/api/tables/1/finish-cleaning
```

## Integration with Other Services

Currently, this service operates independently. To integrate with other microservices:

1. Add API client for AuthService (verify user/client)
2. Add API client for RestaurantService (verify restaurant ownership)
3. Add service-to-service communication in routes guards
4. Update docker-compose configuration with service dependencies

