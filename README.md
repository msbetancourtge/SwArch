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

For restaurant owners and managers, the platform provides a comprehensive dashboard to manage restaurant profiles, define and update menu categories and items (including images and pricing), and monitor incoming orders in real time. The system supports role-based access for different staff members (managers, waiters, chefs), ensuring that each team member sees only the information relevant to their responsibilities.

The architecture is built around independent microservices—authentication, restaurant management, geolocation, menu management, order processing, reservation scheduling, and checkout orchestration—connected through a centralized API Gateway. This design ensures scalability, fault isolation, and the ability to evolve each service independently as the platform grows.

---

## 3. Architectural Structures

### Component-and-Connector (C&C) Structure

#### C&C View

```
┌────────────────────────────────────────────────────────────┐
│                            Clients                         │
│   ┌──────────────────┐            ┌──────────────────┐     │
│   │  Mobile App      │            │  Web Dashboard   │     │
│   │  (Expo / React   │            │  (React + Vite)  │     │
│   │   Native)        │            │  Port 5173       │     │
│   └────────┬─────────┘            └────────┬─────────┘     │
└────────────┼───────────────────────────────┼───────────────┘
             │           HTTP / REST         │
             └───────────────┬───────────────┘
                             │
                             ▼
               ┌──────────────────────────┐
               │       API Gateway        │
               │    (Spring Cloud MVC)    │
               │       Port 8080          │
               │  ┌────────────────────┐  │
               │  │ JWT Auth Filter    │  │
               │  │ Route Rewriting    │  │
               │  │ CORS Handling      │  │
               │  └────────────────────┘  │
               └┬──┬──┬──┬──┬──┬──┬──┬───┘
                │  │  │  │  │  │  │  │
    ┌───────────┘  │  │  │  │  │  │  └──────────────┐
    │   ┌──────────┘  │  │  │  │  └──────────┐      │
    │   │   ┌─────────┘  │  │  └───────┐     │      │
    ▼   ▼   ▼            ▼  ▼          ▼     ▼      ▼
┌──────┐┌────────┐┌────┐┌───────┐┌───────┐┌──────┐┌────────┐┌────────┐
│ Auth ││Restaur.││Menu││ Order ││Reserv.││Check-││ Rating ││Notif.  │
│ Svc  ││Service ││Svc ││Service││Service││ out  ││Service ││Service │
│:8081 ││ :8082  ││8084││ :8085 ││ :8086 ││:8089 ││ :8088  ││ :8087  │
└──┬───┘└─┬───┬──┘└─┬──┘└──┬────┘└──┬────┘└┬─┬─┬┘└──┬─────┘└──┬────┘
   │      │   │     │      │        │      │ │ │    │          ▲
   ▼      │   ▼     ▼      │        │      │ │ │    ▼          │
┌─────┐   │┌─────┐┌──────┐ │        │      │ │ │ ┌───────┐┌───────────┐
│auth │   ││ Geo ││menu  │ │        │      │ │ │ │rating ││notif.     │
│_db  │   ││Serv.││_db   │ │        │      │ │ │ │_db    ││_db        │
│PgSQL│   ││:8083││Mongo │ │        │      │ │ │ │PgSQL  ││PgSQL      │
│:5433│   │└──┬──┘│:27018│ │        │      │ │ │ │:5440  ││:5441      │
└─────┘   │   │   └──────┘ │        │      │ │ │ └───────┘└───────────┘
          ▼   ▼            │        │      │ │ │
   ┌──────────┐┌──────────┐│        │      │ │ │
   │restaur.  ││  geo_db  ││        │      │ │ │
   │  _db     ││  PostGIS ││        │      │ │ │
   │PostgreSQL││  :5435   ││        │      │ │ │
   │  :5434   │└──────────┘│        │      │ │ │
   └──────────┘            │        │      │ │ │
                           ▼        ▼      │ │ │
              ┌─────────────────────────┐   │ │ │
              │   RabbitMQ (AMQP)       │   │ │ │
              │   Port 5672 / 15672     │   │ │ │
              │                         │   │ │ │
              │  Exchange:              │   │ │ │
              │   clickmunch.events     │   │ │ │
              │   (topic)               │   │ │ │
              │                         │   │ │ │
              │  Queues:                │   │ │ │
              │   notification.order    │   │ │ │
              │   notification.reserv.  │───┼─┼─┘
              └─────────────────────────┘   │ │
                 ▲               ▲          │ │
                 │               │          │ │
          publish│        publish│          │ │
        order.*  │   reservation.*          │ │
                 │               │          │ │
           OrderService    ReservationSvc   │ │
                                            │ │
                             Calls via REST─┘ │
                           (Menu,Order,Reserv.)│
                                ┌──────────────┘
                                │
                                ▼
                          ┌──────────┐ ┌──────────┐ ┌──────────┐
                          │ order_db │ │reserv_db │ │ menu_db  │
                          │  PgSQL   │ │  PgSQL   │ │  MongoDB │
                          │  :5436   │ │  :5437   │ │  :27018  │
                          └──────────┘ └──────────┘ └──────────┘
```

#### Architectural Styles Used

| Style | Where Applied | Description |
|-------|---------------|-------------|
| **Microservices** | Entire backend | The system is decomposed into ten independently deployable services (AuthService, RestaurantService, GeoService, MenuService, OrderService, ReservationService, CheckoutService, RatingService, NotificationService, and API Gateway), each owning its own database (where applicable) and communicating via REST and asynchronous messaging. |
| **API Gateway** | APIGateway service | A single entry point routes all external traffic, performs path rewriting, handles CORS, and enforces JWT authentication before forwarding requests to downstream services. |
| **Saga Orchestrator** | CheckoutService | The checkout flow coordinates multiple services (Menu validation, Order creation, Reservation linking) through a centralized orchestrator, ensuring a consistent multi-step transaction without distributed locks. |
| **Event-Driven / Async Messaging** | OrderService, ReservationService → NotificationService | Domain events (order created, status changed, reservation confirmed/cancelled) are published to a RabbitMQ topic exchange and consumed asynchronously by NotificationService to create user notifications. Decouples producers from consumers and improves resilience. |
| **Layered Architecture** | Each microservice | Every service follows a Controller → Service → Repository layering, separating HTTP handling, business logic, and data access concerns. |
| **Client-Server** | Frontend ↔ Backend | The mobile app and web dashboard act as clients that consume the backend's RESTful API through the gateway. |
| **Pipe-and-Filter** | Gateway request pipeline | Incoming requests pass through a pipeline of filters (JWT authentication, path rewriting, URI resolution) before reaching the target service handler. |

#### Architectural Elements and Relations

**Components (Services):**

| Component | Responsibility | Technology |
|-----------|---------------|------------|
| **API Gateway** | Central ingress point; routes, rewrites paths, enforces JWT on protected routes, handles CORS. | Spring Cloud Gateway Server MVC, Java 21 |
| **AuthService** | User registration (with approval workflow), login, JWT token generation, password reset, staff invite flow, admin approval/rejection. | Spring Boot 4, Spring Security, Spring Data JDBC, PostgreSQL |
| **RestaurantService** | Restaurant CRUD, owner validation, nearby search orchestration, restaurant details aggregation, restaurant cards/profiles, table management, operating hours, staff assignments, multi-admin management. | Spring Boot 4, Spring Data JDBC, PostgreSQL |
| **GeoService** | Geospatial storage and proximity queries for restaurant locations. | Spring Boot 4, Spring Data JDBC, PostGIS |
| **MenuService** | Menu category and item management (CRUD), full menu creation per restaurant, item availability and prep time tracking. | Spring Boot 4, Spring Data MongoDB, MongoDB |
| **OrderService** | Order lifecycle management (CRUD), status tracking (Preparing → Ready → Served → Delivered/Cancelled), order items, waiter calls, tips, add items to existing orders. Publishes async events to RabbitMQ on order creation and status changes. | Spring Boot 4, Spring Data JDBC, PostgreSQL, Spring AMQP |
| **ReservationService** | Reservation scheduling, party size management, status tracking (Pendiente → Confirmada → CheckedIn → Completada/Cancelada/NoShow), order linking, suggested available times, 10-min auto-release for no-shows, check-in. Publishes async events to RabbitMQ on confirmation and cancellation. | Spring Boot 4, Spring Data JDBC, PostgreSQL, Spring AMQP |
| **CheckoutService** | Saga Orchestrator — validates cart items, creates orders via OrderService, links reservations. Supports tips, delivery fees, and discounts. Stateless (no database). | Spring Boot 4, RestClient |
| **RatingService** | Restaurant and waiter ratings, rating summaries with averages and counts per entity. | Spring Boot 4, Spring Data JDBC, PostgreSQL |
| **NotificationService** | User notifications with type-based filtering (ORDER, RESERVATION, PROMOTION, SYSTEM), mark as read. Consumes async events from RabbitMQ (order and reservation queues) to auto-generate notifications. | Spring Boot 4, Spring Data JDBC, PostgreSQL, Spring AMQP |
| **RabbitMQ** | Message broker for asynchronous inter-service communication. Hosts the `clickmunch.events` topic exchange with routing-key-based bindings to notification queues. | RabbitMQ 3 (Management) |
| **Web Dashboard** | Admin panel for restaurant and product management. | React 19, TypeScript, Vite, TailwindCSS |
| **Mobile App** | Customer-facing app for browsing restaurants, menus, and ordering. | React Native, Expo SDK 54, Zustand, React Query |

**Connectors (Relations):**

| From | To | Protocol | Description |
|------|----|----------|-------------|
| Mobile App / Dashboard | API Gateway | HTTP/REST | All client requests enter through port 8080. |
| API Gateway | AuthService | HTTP/REST | Forwards `/auth/**` → `/api/auth/**` (public). |
| API Gateway | RestaurantService | HTTP/REST | Forwards `/restaurant/**` → `/api/restaurants/**` (JWT-protected). |
| API Gateway | MenuService | HTTP/REST | Forwards `/menu/**` → `/api/menus/**` (JWT-protected). |
| API Gateway | OrderService | HTTP/REST | Forwards `/order/**` → `/api/orders/**` (JWT-protected). |
| API Gateway | ReservationService | HTTP/REST | Forwards `/reservation/**` → `/api/reservations/**` (JWT-protected). |
| API Gateway | CheckoutService | HTTP/REST | Forwards `/checkout/**` → `/api/checkout/**` (JWT-protected). |
| API Gateway | RatingService | HTTP/REST | Forwards `/rating/**` → `/api/ratings/**` (JWT-protected). |
| API Gateway | NotificationService | HTTP/REST | Forwards `/notification/**` → `/api/notifications/**` (JWT-protected). |
| RestaurantService | AuthService | HTTP/REST | Validates owner identity via `AuthClient`. |
| RestaurantService | GeoService | HTTP/REST | Creates locations and queries nearby restaurants via `GeoClient`. |
| RestaurantService | MenuService | HTTP/REST | Fetches menu data for restaurant details via `MenuClient`. |
| AuthService | auth_db | JDBC | PostgreSQL database for users and credentials. |
| RestaurantService | restaurant_db | JDBC | PostgreSQL database for restaurant records. |
| GeoService | geo_db | JDBC | PostGIS database for geospatial location data. |
| MenuService | menu_db | MongoDB Driver | MongoDB database for menu categories and items. |
| OrderService | order_db | JDBC | PostgreSQL database for orders and order items. |
| ReservationService | reservation_db | JDBC | PostgreSQL database for reservations. |
| ReservationService | RestaurantService | HTTP/REST | Fetches tables and operating hours for suggested times via `RestaurantClient`. |
| RatingService | rating_db | JDBC | PostgreSQL database for ratings. |
| NotificationService | notification_db | JDBC | PostgreSQL database for notifications. |
| CheckoutService | OrderService | HTTP/REST | Creates orders via `OrderClient`. |
| CheckoutService | ReservationService | HTTP/REST | Validates and links reservations via `ReservationClient`. |
| CheckoutService | MenuService | HTTP/REST | Validates menu items via `MenuClient`. |
| OrderService | RabbitMQ | AMQP | Publishes `order.created` and `order.status.changed` events to the `clickmunch.events` topic exchange. |
| ReservationService | RabbitMQ | AMQP | Publishes `reservation.confirmed` and `reservation.cancelled` events to the `clickmunch.events` topic exchange. |
| RabbitMQ | NotificationService | AMQP | Routes events to `notification.order.queue` and `notification.reservation.queue`. NotificationService consumes them and creates user notifications. |

---

## 4. Prototype

### Prerequisites

- **Docker** and **Docker Compose** (for the backend)
- **Node.js** ≥ 18 and **npm** (for the frontends)
- **Expo CLI** (for the mobile app): `npm install -g expo-cli`

### Backend

The entire backend (10 microservices + 8 databases + 1 message broker) runs in Docker containers.

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

All 19 containers should show **"(healthy)"**. The API Gateway will be available at `http://localhost:8080`.

| Service | Port |
|---------|----- |
| API Gateway | 8080 |
| AuthService | 8081 |
| RestaurantService | 8082 |
| GeoService | 8083 |
| MenuService | 8084 |
| OrderService | 8085 |
| ReservationService | 8086 |
| NotificationService | 8087 |
| RatingService | 8088 |
| CheckoutService | 8089 |
| RabbitMQ (AMQP) | 5672 |
| RabbitMQ (Management UI) | 15672 |
| auth_db (PostgreSQL) | 5433 |
| restaurant_db (PostgreSQL) | 5434 |
| geo_db (PostGIS) | 5435 |
| order_db (PostgreSQL) | 5436 |
| reservation_db (PostgreSQL) | 5437 |
| menu_db (MongoDB) | 27018 |
| rating_db (PostgreSQL) | 5440 |
| notification_db (PostgreSQL) | 5441 |

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

The dashboard will be available at `http://localhost:5173`.

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
