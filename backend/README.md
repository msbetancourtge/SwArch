# Click & Munch Backend

This backend is a microservices architecture with a single API Gateway entry point for frontend communication. Each microservice owns its data and uses a dedicated PostgreSQL database (GeoService uses PostGIS).

## Architecture

- Microservices: `AuthService`, `RestaurantService`, `GeoService`, `MenuService`
- Databases:
  - AuthService → PostgreSQL (`auth_db` at localhost:5433)
  - RestaurantService → PostgreSQL (`restaurant_db` at localhost:5434)
  - GeoService → PostGIS (`geo_db` at localhost:5435)
  - MenuService → PostgreSQL (`menu_db` at localhost:5436)
- API Gateway: `APIGateway` (exposes a unified, public interface to the frontend)
- Service Ports:
  - APIGateway: 8080
  - AuthService: 8081
  - RestaurantService: 8082
  - GeoService: 8083
  - MenuService: 8084
- Routing (Gateway → Services):
  - `/auth/**` → AuthService (`/api/auth/**`)
  - `/restaurant/**` → RestaurantService (`/api/restaurants/**`)
  - `/menu/**` → MenuService (`/api/menus/**`)

The gateway rewrites incoming paths to each service’s internal API. For example, `/auth/register` → `/api/auth/register` in AuthService.

## Design Patterns & Practices

- API Gateway Pattern: Central ingress that routes to internal services; path rewriting and CORS handled in the gateway.
- Filter: `JwtAuthenticationFilter` in the gateway guards protected routes (e.g., restaurant/menu) while auth routes remain public.
- JWT-based Authentication: Token generation/validation encapsulated in `JwtTokenUtil`.
- Layered Architecture:
  - Controller (HTTP endpoints)
  - Service (business logic)
  - Repository (data access via Spring Data)
  - DTOs (request/response models between layers)
- Repository Pattern: Spring Data repositories like `UserRepository` abstract persistence.
- Builder Pattern: Entities (e.g., `User`) use Lombok `@Builder` for construction.
- Dependency Injection: Spring-managed components (`@Service`, `@RestController`, `@Bean`).
- Client/Integration Pattern: `RestaurantService` uses `AuthClient` to query user details; services call each other via HTTP.

## API Gateway (Single Point of Access)

All frontend traffic goes through the gateway at `http://localhost:8080`.

- Public:
  - `/auth/**` → forwarded to AuthService; no JWT required.
- Protected (JWT required via gateway filter):
  - `/restaurant/**` → forwarded to RestaurantService
  - `/menu/**` → forwarded to MenuService

Note: GeoService routes are consumed internally by other services and are not directly exposed via the gateway.

## Service Endpoints

Below are the internal service endpoints (the gateway maps external requests to these). Use the gateway paths from the frontend.

### AuthService (internal base: `/api/auth`)
- `POST /api/auth/login` → Login and receive a token (if applicable).
- `POST /api/auth/register` → Register a new user.
- `GET /api/auth/users/{userId}` → Get user info by ID.
- Password Reset (base: `/auth/password-reset`)
  - `POST /auth/password-reset/request` → Request a reset token by email.
  - `POST /auth/password-reset/confirm` → Confirm reset with token and new password.

Gateway mappings:
- `/auth/login` → `/api/auth/login`
- `/auth/register` → `/api/auth/register`
- `/auth/users/{userId}` → `/api/auth/users/{userId}`
- For password reset, ensure controller base aligns with gateway path rewriting (recommended: `/api/auth/password-reset/**`).

### RestaurantService (internal base: `/api/restaurants`)
- `POST /api/restaurants` → Create a restaurant.
- `GET /api/restaurants/{id}` → Get restaurant by ID.
- `GET /api/restaurants/owner/{ownerId}` → List restaurants for owner ID.
- `GET /api/restaurants/nearby` → Nearby search using GeoService.
- `GET /api/restaurants/{id}/details` → Aggregated restaurant details.

Gateway mappings:
- `/restaurant` → `/api/restaurants`
- `/restaurant/{id}` → `/api/restaurants/{id}`
- `/restaurant/owner/{ownerId}` → `/api/restaurants/owner/{ownerId}`
- `/restaurant/nearby` → `/api/restaurants/nearby`
- `/restaurant/{id}/details` → `/api/restaurants/{id}/details`

### MenuService (internal base: `/api/menus`)
Categories:
- `POST /api/menus/categories` → Create category.
- `GET /api/menus/categories/{categoryId}` → Get category.
- `PUT /api/menus/categories/{categoryId}` → Update category.
- `DELETE /api/menus/categories/{categoryId}` → Delete category.

Items:
- `POST /api/menus/categories/{categoryId}/items` → Create item in category.
- `GET /api/menus/items/{itemId}` → Get item.
- `PUT /api/menus/items/{itemId}` → Update item.
- `DELETE /api/menus/items/{itemId}` → Delete item.

Restaurants:
- `POST /api/menus` → Create full menu (categories + items) for a restaurant.
- `GET /api/menus/restaurants/{restaurantId}` → Get full menu by restaurant.
- `GET /api/menus/restaurants/{restaurantId}/items` → List items for a restaurant.
- `DELETE /api/menus/restaurants/{restaurantId}` → Delete all menu data for a restaurant.

Gateway mappings:
- `/menu/**` → `/api/menus/**`

### GeoService (internal base: `/api/geo`)
- `POST /api/geo/locations` → Create location (restaurant, etc.).
- `POST /api/geo/nearby` → Find nearby locations.
- `GET /api/geo/locations` → List all locations.

Typically consumed by RestaurantService; not exposed directly via the gateway.

## Running Locally

1. Start databases with docker compose:

```bash
cd ClickAndMunchApp/backend
docker compose up -d
```

2. Start services (in separate terminals or via your IDE):

```bash
# Gateway
cd ClickAndMunchApp/backend/APIGateway
./gradlew bootRun

# Auth
cd ClickAndMunchApp/backend/AuthService
./gradlew bootRun

# Restaurant
cd ClickAndMunchApp/backend/RestaurantService
./gradlew bootRun

# Geo
cd ClickAndMunchApp/backend/GeoService
./gradlew bootRun

# Menu
cd ClickAndMunchApp/backend/MenuService
./gradlew bootRun
```

3. Try a registration via the gateway:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Plinio Rodolfo",
    "email": "plinio@example.com",
    "username": "plinieichon",
    "password": "123987",
    "role": "CUSTOMER"
  }'
```

If you protect more routes, include `Authorization: Bearer <token>` on requests to `/restaurant/**` or `/menu/**`.
