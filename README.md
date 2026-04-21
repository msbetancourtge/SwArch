# Click & Munch

<!-- Logo placeholder: replace the path below with the actual logo file -->
<p align="center">
  <img src="./Logo.jpeg" alt="Click & Munch Logo" width="250" />
</p>

## 1. Team

**Team Name:** 1a

| # | Full Name |
|---|-----------|
| 1 | Michael Stiven Betancourt Gelves |
| 2 | Santiago Bejarano Ariza |
| 3 | Santiago Suaza Montalvo|
| 4 | Julian David Ruiz Ramos |
| 5 | Manuel Felipe Espinosa Español |
| 6 | Manuel Santiago Mori Ardila |

---

## 2. Software System

**Name:** Click & Munch

**Description:**

Click & Munch is a digital platform designed to streamline the dining experience at restaurants and bars. The system allows customers to browse nearby establishments, explore their menus, and place pre-orders before arriving at the venue. By enabling customers to reserve tables and submit their food and drink selections in advance, Click & Munch significantly reduces waiting times upon arrival—meals and beverages can be prepared ahead of time so that service begins almost immediately when the customer is seated.

For restaurant owners and managers, the platform provides a comprehensive dashboard to manage restaurant profiles, define and update menu categories and items (including images and pricing), and monitor incoming orders in real time. Chefs see new orders appear instantly on a kitchen board and advance them through a strict state machine (PENDING → IN_PREPARATION → READY → DELIVERED), with per-unit special instructions (e.g. "sin lechuga") so two of the same dish in one order can have different preparations. The system supports role-based access for different staff members (managers, waiters, chefs), ensuring that each team member sees only the information relevant to their responsibilities.

The architecture is built around independent microservices—authentication, restaurant management, geolocation, menu management, and order lifecycle—connected through a centralized API Gateway for REST traffic, plus a dedicated realtime WebSocket channel (STOMP) for pushing kitchen events to chefs the moment a waiter places an order. This design ensures scalability, fault isolation, and the ability to evolve each service independently as the platform grows.

---

## 3. Architectural Structures

### Component-and-Connector (C&C) Structure

#### C&C View

```
┌────────────────────────────────────────────────────────┐
│                          Clients                       │
│   ┌──────────────────┐          ┌──────────────────┐   │
│   │  Mobile App      │          │  Web Dashboard   │   │
│   │  (Expo / React   │          │  (React + Vite)  │   │
│   │   Native)        │          │  Port 5173       │   │
│   └────────┬─────────┘          └────────┬─────────┘   │
└────────────┼─────────────────────────────┼─────────────┘
             │         HTTP / REST         │   WebSocket
             └──────────────┬──────────────┘   (STOMP, kitchen)
                            │                         │
                            ▼                         │
              ┌─────────────────────────┐             │
              │      API Gateway        │             │
              │    (Spring Cloud MVC)   │             │
              │      Port 8080          │             │
              │  ┌───────────────────┐  │             │
              │  │ JWT Auth Filter   │  │             │
              │  │ Route Rewriting   │  │             │
              │  │ CORS Handling     │  │             │
              │  └───────────────────┘  │             │
              └──┬──────┬──────┬──────┬─┘             │
                 │      │      │      │               │
        ┌────────┘      │      │      └────────┐      │
        ▼               ▼      ▼               ▼      │
┌───────────┐  ┌──────────────┐  ┌───────────┐  ┌────────────┐
│AuthService│  │  Restaurant  │  │MenuService│  │OrderService│◀┘
│ Port 8081 │  │  Service     │  │ Port 8084 │  │ Port 8085  │
│           │  │  Port 8082   │  │           │  │  + WS /ws  │
└─────┬─────┘  └──┬─────┬────-┘  └─────┬─────┘  └──────┬─────┘
      │           │     │              │               │
      ▼           │     ▼              ▼               ▼
┌───────────┐     │  ┌──────────┐   ┌───────────┐   ┌───────────┐
│ auth_db   │     │  │GeoService│   │ menu_db   │   │ order_db  │
│ PostgreSQL│     │  │Port 8083 │   │ MongoDB 7 │   │PostgreSQL │
│ Port 5433 │     │  └────┬─────┘   │ Port 27018│   │ Port 5436 │
└───────────┘     │       │         └───────────┘   └───────────┘
                  ▼       ▼
          ┌─────────────┐ ┌───────────┐
          │restaurant_db│ │  geo_db   │
          │ PostgreSQL  │ │  PostGIS  │
          │ Port 5434   │ │ Port 5435 │
          └─────────────┘ └───────────┘
```

#### Architectural Styles Used

| Style | Where Applied | Description |
|-------|---------------|-------------|
| **Microservices** | Entire backend | The system is decomposed into six independently deployable services (AuthService, RestaurantService, GeoService, MenuService, OrderService, API Gateway), each owning its own database and communicating via REST. |
| **API Gateway** | APIGateway service | A single entry point routes all REST external traffic, performs path rewriting, handles CORS, and enforces JWT authentication before forwarding requests to downstream services. |
| **Layered Architecture** | Each microservice | Every service follows a Controller → Service → Repository layering, separating HTTP handling, business logic, and data access concerns. |
| **Client-Server** | Frontend ↔ Backend | The mobile app and web dashboard act as clients that consume the backend's RESTful API through the gateway. |
| **Pipe-and-Filter** | Gateway request pipeline | Incoming requests pass through a pipeline of filters (JWT authentication, path rewriting, URI resolution) before reaching the target service handler. |
| **Publish/Subscribe** | OrderService kitchen events | OrderService publishes `ORDER_CREATED` and `ORDER_STATUS_CHANGED` events to `/topic/kitchen/{restaurantId}` over STOMP/WebSocket. Chef clients subscribe to their restaurant's topic and receive updates in real time without polling. |
| **State Machine** | OrderService order lifecycle | Orders transition through a strict state machine (PENDING → IN_PREPARATION → READY → DELIVERED, plus CANCELLED) enforced at the service layer; invalid transitions are rejected. |

#### Architectural Elements and Relations

**Components (Services):**

| Component | Responsibility | Technology |
|-----------|---------------|------------|
| **API Gateway** | Central REST ingress; routes, rewrites paths, enforces JWT on protected routes, handles CORS. | Spring Cloud Gateway Server MVC, Java 21 |
| **AuthService** | User registration, login, JWT token generation, password reset, user lookup. | Spring Boot 4, Spring Security, Spring Data JDBC, PostgreSQL |
| **RestaurantService** | Restaurant CRUD, owner validation, nearby search orchestration, restaurant details aggregation. | Spring Boot 4, Spring Data JDBC, PostgreSQL |
| **GeoService** | Geospatial storage and proximity queries for restaurant locations. | Spring Boot 4, Spring Data JDBC, PostGIS |
| **MenuService** | Menu category and item management (CRUD), full menu creation per restaurant. | Spring Boot 4, Spring Data MongoDB, MongoDB |
| **OrderService** | Order lifecycle management (create, retrieve, state-machine transitions) and realtime kitchen event fan-out. One row per ordered unit so per-unit notes (e.g. "sin lechuga") can differ in the same order. | Spring Boot 4, Spring Data JDBC, Spring WebSocket (STOMP), PostgreSQL |
| **Web Dashboard** | Admin panel for restaurant, product, and kitchen management. Chef Kitchen page subscribes to the realtime channel. | React 19, TypeScript, Vite, TailwindCSS, @stomp/stompjs |
| **Mobile App** | Customer-facing app for browsing restaurants, menus, and ordering. | React Native, Expo SDK 54, Zustand, React Query |

**Connectors (Relations):**

| From | To | Protocol | Description |
|------|----|----------|-------------|
| Mobile App / Dashboard | API Gateway | HTTP/REST | All REST client traffic enters through port 8080. |
| API Gateway | AuthService | HTTP/REST | Forwards `/auth/**` → `/api/auth/**` (public). |
| API Gateway | RestaurantService | HTTP/REST | Forwards `/restaurant/**` → `/api/restaurants/**` (JWT-protected). |
| API Gateway | MenuService | HTTP/REST | Forwards `/menu/**` → `/api/menus/**` (JWT-protected). |
| API Gateway | OrderService | HTTP/REST | Forwards `/order/**` → `/api/orders/**` (JWT-protected). |
| Web Dashboard (Chef) | OrderService | WebSocket/STOMP | Direct connection `ws://orderservice:8085/ws/kitchen` for realtime kitchen events. See "Realtime channel" below for why this bypasses the gateway. |
| RestaurantService | AuthService | HTTP/REST | Validates owner identity via `AuthClient`. |
| RestaurantService | GeoService | HTTP/REST | Creates locations and queries nearby restaurants via `GeoClient`. |
| RestaurantService | MenuService | HTTP/REST | Fetches menu data for restaurant details via `MenuClient`. |
| AuthService | auth_db | JDBC | PostgreSQL database for users and credentials. |
| RestaurantService | restaurant_db | JDBC | PostgreSQL database for restaurant records. |
| GeoService | geo_db | JDBC | PostGIS database for geospatial location data. |
| MenuService | menu_db | MongoDB Driver | MongoDB database for menu categories and items. |
| OrderService | order_db | JDBC | PostgreSQL database for orders and order_items (one row per unit). |

#### Realtime channel (WebSocket)

When a waiter places a new order via `POST /order` (HTTP REST, through the gateway), the `OrderService`:

1. Persists the order and its items (one row per ordered unit).
2. Publishes a `ORDER_CREATED` event to `/topic/kitchen/{restaurantId}` via STOMP.

Chefs connected to the dashboard's Kitchen page are subscribed to that topic and see the card appear instantly, with no polling. The same mechanism publishes `ORDER_STATUS_CHANGED` whenever the chef advances an order (PENDING → IN_PREPARATION → READY → DELIVERED). A 60-second safety-net poll keeps the UI correct after reconnects or missed frames.

**Why WebSocket is not proxied through the gateway:** Spring Cloud Gateway Server MVC (the servlet flavor used here) does not transparently proxy the HTTP Upgrade handshake required for WebSockets; only the reactive flavor does. Rather than migrate the entire gateway to reactive for one feature, the project follows the common production pattern of separating REST and realtime edges (e.g. AWS API Gateway + AppSync, or nginx with distinct locations). If a single edge becomes desirable later, an nginx/traefik sidecar can be added in front to terminate both HTTP and WebSocket on one port.

---

## 4. Prototype

### Prerequisites

- **Docker** and **Docker Compose** (for the backend)
- **Node.js** ≥ 18 and **npm** (for the frontends)
- **Expo CLI** (for the mobile app): `npm install -g expo-cli`

### Backend

The current local compose stack brings up 6 core containers for the active flow: API Gateway, AuthService, RestaurantService, GeoService, MenuService, OrderService, 5 backing databases, and RabbitMQ for order events.

```bash
# 1. Clone the repository
git clone <repository-url>
cd ClickAndMunchApp

# 2. Start all backend services
cd backend
docker compose up --build -d

# 3. Verify all containers are healthy
docker compose ps
```

All core containers should show **"(healthy)"**. The API Gateway will be available at `http://localhost:8080` and the realtime kitchen WebSocket at `ws://localhost:8085/ws/kitchen`.

| Service | Port |
|---------|------|
| API Gateway | 8080 |
| AuthService | 8081 |
| RestaurantService | 8082 |
| GeoService | 8083 |
| MenuService | 8084 |
| OrderService (REST + WebSocket) | 8085 |
| RabbitMQ | 5672 |
| RabbitMQ Management | 15672 |
| auth_db (PostgreSQL) | 5433 |
| restaurant_db (PostgreSQL) | 5434 |
| geo_db (PostGIS) | 5435 |
| order_db (PostgreSQL) | 5436 |
| menu_db (MongoDB) | 27018 |

Gateway-exposed routes currently verified in this branch:

- `/auth/**` -> AuthService
- `/restaurant/**` -> RestaurantService
- `/menu/**` -> MenuService
- `/order/**` -> OrderService
- `/reservation/**` -> ReservationService
- `/checkout/**` -> CheckoutService
- `/rating/**` -> RatingService
- `/notification/**` -> NotificationService

Direct-only local endpoints currently verified:

- GeoService: `http://localhost:8083/api/geo/**`
- Password reset: `http://localhost:8081/auth/password-reset/**`
- RabbitMQ management: `http://localhost:15672/api/**`

**Quick test:** Register a user through the gateway:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "username": "testuser",
    "password": "123456",
    "role": "CUSTOMER"
  }'
```

Then log in and use the returned JWT for protected routes:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456"
  }'
```

**Place an order (as waiter, through the gateway):**

```bash
curl -X POST http://localhost:8080/order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt>" \
  -d '{
    "restaurantId": 1,
    "tableNumber": 5,
    "notes": "Mesa junto a la ventana",
    "items": [
      {"itemName": "Hamburguesa", "notes": "sin lechuga"},
      {"itemName": "Hamburguesa", "notes": "con todo"}
    ]
  }'
```

A chef subscribed to `/topic/kitchen/1` over the WebSocket receives the event immediately.

Useful verified reads through the gateway:

```bash
curl -H "Authorization: Bearer <jwt>" http://localhost:8080/restaurant/owner/1
curl -H "Authorization: Bearer <jwt>" http://localhost:8080/menu/restaurants/1
curl -H "Authorization: Bearer <jwt>" http://localhost:8080/order/kitchen/1
curl -H "Authorization: Bearer <jwt>" "http://localhost:8080/order/restaurant/1?status=PENDING"
```

If your local `order-db` volume was created before the latest OrderService model changes, restarting the stack is enough; OrderService now patches the legacy local schema on startup.

To stop the backend:

```bash
docker compose down
```

### Frontend — Web Dashboard

```bash
# From the project root
cd frontend/dashboard

# Install dependencies
npm install

# Start the development server
npm run dev
```

The dashboard will be available at `http://localhost:5173`. The kitchen view is at `/kitchen`.

### Frontend — Mobile App

```bash
# From the project root
cd frontend/mobile

# Install dependencies
npm install

# Start Expo
npx expo start
```

Then choose one of the options:
- Press **i** to open in the iOS Simulator
- Press **a** to open in the Android Emulator
- Scan the QR code with **Expo Go** on your phone
