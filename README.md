# Click & Munch

<!-- Logo placeholder: replace the path below with the actual logo file -->
<p align="center">
  <img src="./images/Logo.jpeg" alt="Click & Munch Logo" width="250" />
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

The architecture is built around independent microservices—authentication, restaurant management, geolocation, menu management, order lifecycle, reservations, notifications, ratings, and checkout orchestration—connected through a centralized API Gateway that acts as the single public entry point for both REST traffic and the realtime STOMP/WebSocket channel that pushes kitchen events to chefs the moment a waiter places an order. Asynchronous events flow between services via RabbitMQ. This design ensures scalability, fault isolation, and the ability to evolve each service independently as the platform grows.

**General-purpose languages used in the code base:**

- **Java** for the Spring Boot backend microservices and API Gateway.
- **Python** for the CheckoutService implemented with FastAPI.
- **TypeScript** for the web dashboard and the mobile application code.
- **JavaScript** for frontend and tooling configuration files in the workspace.

---

## 3. Architectural Structures

---

### 3.1 Component-and-Connector (C&C) Structure

#### C&C View

![Component-and-Connector (C&C) View](./images/C&C%20Diagram%20ClickAndMunch-Final.png)

#### Description of Architectural Elements and Relations

**Components (Services):**

| Component | Technology | Responsibility |
|-----------|-----------|----------------|
| **API Gateway** | Spring Cloud Gateway WebFlux (Java 21, Netty) | Single public ingress on `:8080`. Routes REST traffic, enforces JWT on protected routes, handles CORS, and proxies HTTP→WebSocket upgrades for the kitchen channel. |
| **AuthService** | Spring Boot 4, JDBC, PostgreSQL | User registration, login, JWT generation, and user lookup. |
| **RestaurantService** | Spring Boot 4, JDBC, PostgreSQL | Restaurant CRUD, owner validation (via AuthService), nearby search (via GeoService), menu aggregation (via MenuService). |
| **GeoService** | Spring Boot 4, JDBC, PostGIS | Geospatial location storage, proximity queries, distance and ETA calculation. Internal-only. |
| **MenuService** | Spring Boot 4, MongoDB | Menu category and item management per restaurant. |
| **OrderService** | Spring Boot 4, JDBC, PostgreSQL, STOMP, RabbitMQ | Order lifecycle state machine. One DB row per ordered unit for per-unit notes. Publishes events to RabbitMQ; pushes realtime events over STOMP. |
| **ReservationService** | Spring Boot 4, JDBC, PostgreSQL, RabbitMQ | Table reservations, capacity management, order linking. Publishes events to RabbitMQ. |
| **NotificationService** | Spring Boot 4, JDBC, PostgreSQL, RabbitMQ, SSE, Telegram Bot API | Consumes events from RabbitMQ, persists notifications, streams to clients via SSE, and forwards alerts to Telegram via `TelegramWorker`. |
| **RatingService** | Spring Boot 4, JDBC, PostgreSQL | Restaurant and waiter ratings with aggregate score summaries. |
| **CheckoutService** | **Python 3.12 / FastAPI / httpx** | Saga orchestrator: validates reservation → creates order → links reservation. No own database. |
| **RabbitMQ** | RabbitMQ 3 (AMQP) | Async event bus decoupling OrderService/ReservationService from NotificationService. |
| **Web Dashboard** | React 19, TypeScript, Vite, TailwindCSS | Admin panel; Chef Kitchen page subscribes to STOMP realtime channel. |
| **Mobile App** | React Native, Expo SDK 54, Zustand, React Query | Customer-facing browsing, reservations, and ordering. |

**Connectors (Relations):**

| From | To | Protocol | Description |
|------|----|----------|-------------|
| Mobile / Dashboard | API Gateway | HTTP/REST | All REST enters on port 8080. |
| Dashboard Chef view | API Gateway | WebSocket/STOMP | `ws://gateway:8080/ws/kitchen` |
| API Gateway | AuthService | HTTP | `/auth/**` → `/api/auth/**` (public) |
| API Gateway | RestaurantService | HTTP | `/restaurant/**` (JWT-protected) |
| API Gateway | MenuService | HTTP | `/menu/**` (JWT-protected) |
| API Gateway | OrderService | HTTP + WebSocket | `/order/**` + `/ws/kitchen` proxy |
| API Gateway | ReservationService | HTTP | `/reservation/**` (JWT-protected) |
| API Gateway | CheckoutService | HTTP | `/checkout/**` (JWT-protected) |
| API Gateway | RatingService | HTTP | `/rating/**` (JWT-protected) |
| API Gateway | NotificationService | HTTP + SSE | `/notification/**` (JWT-protected) |
| CheckoutService | ReservationService | HTTP | Validate & link reservation |
| CheckoutService | OrderService | HTTP | `POST /api/orders` |
| RestaurantService | AuthService | HTTP | Owner identity validation |
| RestaurantService | GeoService | HTTP | Location creation and nearby search |
| RestaurantService | MenuService | HTTP | Menu aggregation |
| OrderService | RabbitMQ | AMQP | Publish `ORDER_CREATED`, `ORDER_STATUS_CHANGED` |
| ReservationService | RabbitMQ | AMQP | Publish `RESERVATION_CREATED` |
| RabbitMQ | NotificationService | AMQP | Event delivery |
| NotificationService → AuthService | HTTP | REST | `NotificationEventConsumer` fetches `telegramChatId` per user before dispatching Telegram events |
| NotificationService (TelegramWorker) | Telegram Bot API | HTTPS/REST | Delivers alerts via `POST /bot{token}/sendMessage` — only component aware of Telegram |
| Each service | Own DB | JDBC / MongoDB | Each service owns exactly one database |

#### Description of Architectural Styles

| Style | Where Applied | Description |
|-------|---------------|-------------|
| **Microservices** | Entire backend | The backend is decomposed into independently deployable services, each with its own bounded responsibility and data ownership. |
| **Client-Server** | Frontend ↔ Backend | The mobile app and dashboard act as clients that consume backend capabilities through the API Gateway. |
| **Layered Architecture** | Each microservice | Each service separates presentation, business logic, and data access responsibilities to reduce coupling. |
| **Event-Driven Architecture** | Order, Reservation, Notification flows | Domain events propagate asynchronously through RabbitMQ to decouple producers from consumers. |

#### Description of Architectural Patterns

| Pattern | Where Applied | Description |
|---------|---------------|-------------|
| **API Gateway** | APIGateway | A single entry point handles routing, JWT enforcement, CORS, and WebSocket proxying. |
| **Saga (Orchestration)** | CheckoutService | Checkout coordinates a multi-step transaction across ReservationService and OrderService and handles partial failure points. |
| **Publish/Subscribe** | RabbitMQ bus | OrderService and ReservationService publish events; NotificationService subscribes asynchronously. |
| **Pipe-and-Filter** | Gateway pipeline | Requests pass through authentication, path rewriting, and forwarding stages before reaching downstream services. |
| **State Machine** | OrderService | Order status transitions are constrained to valid lifecycle changes only. |
| **Repository** | Data layer | Spring Data repositories encapsulate persistence concerns behind repository interfaces. |
| **Mediator** | NotificationService + RabbitMQ | RabbitMQ acts as the central mediator between the core system and external channels. `TelegramWorker` is the only component aware of the Telegram API; all other services publish generic events, achieving full interoperability decoupling. |

---

### 3.2 Deployment Structure

#### Deployment View

All backend components run as Docker containers on a single host within the `appnet` bridge network.

![Deployment View](./images/Deployment_View.png)

#### Description of Architectural Elements and Relations

| Element | Type | Description |
|---------|------|-------------|
| **Host Machine** | Execution environment | Runs Docker Engine; Docker Compose orchestrates all containers. |
| **appnet** | Docker bridge network | Private virtual network; containers communicate via DNS service names (e.g., `http://authservice:8081`). |
| **apigateway** | Container | Only externally reachable service endpoint; binds host port 8080. |
| **`*service` containers** | Containers | `expose`d (visible inside `appnet`) but not `port`-published to the host. |
| **`*-db` containers** | Containers | Backing stores; host ports published for dev inspection only. |
| **rabbitmq** | Container | AMQP broker; ports 5672 and 15672 published to the host. |
| **Health checks** | Dependency mechanism | Every container declares a health check; `depends_on: condition: service_healthy` prevents startup race conditions. |

#### Description of Architectural Patterns

| Pattern | Description |
|---------|-------------|
| **Containerisation** | Every component is a Docker image; environment parity between dev and production. |
| **Service discovery via DNS** | Docker Compose `appnet` resolves container names; inter-service URLs are environment variables. |
| **Single-edge ingress** | Only the API Gateway is published externally, shrinking the attack surface. |
| **Externalized configuration** | DB URLs, credentials, and inter-service URLs injected via Docker Compose environment variables (Twelve-Factor App). |

---

### 3.3 Layered Structure

#### Layered View

The system is organized as a five-layer horizontal stack, from client applications down to the data stores:

![Layered View](./images/Layered_View.png)

#### Description of Architectural Elements and Relations

| Layer | Element | Responsibility |
|-------|---------|----------------|
| **Layer 0 — Presentation** | Mobile App, Web Dashboard | Client applications that consume the platform; render the UI and issue requests. No business logic. |
| **Layer 1 — Edge (Secure Channel)** | API Gateway | Single public entry point; terminates TLS (HTTPS/WSS), routes traffic, and enforces JWT and CORS. |
| **Layer 2 — Business Services** | Auth, Restaurant, Geo, Menu, Order, Reservation, Checkout, Rating, Notification | Independently deployable microservices that hold all business logic and own their data. |
| **Layer 3 — Asynchronous Messaging** | RabbitMQ (`clickmunch.events` topic) | Event bus that decouples producers (Order, Reservation) from consumers (Notification). |
| **Layer 4 — Data Stores** | PostgreSQL ×6, PostGIS, MongoDB | Backing databases; one per service, never accessed across service boundaries. |

#### Description of Architectural Patterns

| Pattern | Description |
|---------|-------------|
| **Layered (N-Tier)** | Strict unidirectional dependency: presentation → business → data → infrastructure. No layer skipping. |
| **DTO pattern** | Java records and Pydantic models are immutable transfer objects; entities are never exposed in API responses. |
| **Repository pattern** | Persistence mechanism (PostgreSQL, MongoDB) is hidden behind interfaces and swappable without touching business logic. |
| **Dependency Injection** | Spring IoC (Java) and FastAPI's dependency system (Python) wire components at startup, enabling unit-testable mock substitution. |

---

### 3.4 Decomposition Structure

#### Decomposition View

The system is decomposed into six functional domains grouped by business capability, plus a shared infrastructure layer:

![Decomposition View ](./images/Decomposition_View.png)

#### Description of Architectural Elements and Relations

| Domain | Services | Responsibility Boundary |
|--------|----------|------------------------|
| **Presentation** | Web Dashboard, Mobile App | Client applications consumed by end users; contain no business logic. |
| **Ingress** | API Gateway | All client-facing concerns: routing and security. No business logic. |
| **Identity & Access** | AuthService | User identity and JWT issuance. Consumed by the gateway and RestaurantService. |
| **Venue Management** | RestaurantService, GeoService, MenuService (×3 behind nginx-menu LB) | Venue profile, location, and menu. GeoService is internal-only (never reached directly by clients). |
| **Ordering** | CheckoutService ★, OrderService, ReservationService | End-to-end purchase flow. CheckoutService (Python) is the saga entry point. |
| **Customer Engagement** | NotificationService, RatingService | Realtime alerts and post-experience reviews. |
| **Infrastructure** | RabbitMQ | Shared event bus; decouples producers from consumers. |

**Boundary rules:**
- Cross-domain communication uses only REST (sync) or RabbitMQ (async).
- Intra-domain REST calls are allowed (e.g., RestaurantService → GeoService); cross-database access is forbidden.
- The Ingress domain contains no business logic; it is a traffic director only.
- The diagram's infrastructure band also visually groups the per-service databases (PostgreSQL ×6, PostGIS, MongoDB — covered as Data Stores in §3.3). These are owned one-per-service and are *not* a shared domain; only RabbitMQ is shared infrastructure.

---

## 4. Security

---

### 4.1 Web Application Firewall (WAF) — Injection Attack Prevention

The WAF is implemented as a `GlobalFilter` inside the API Gateway with the highest filter priority. It inspects request paths, query parameters, and key headers against regex signature lists for SQLi, XSS, and Path Traversal, decoding URL-encoded payloads up to two passes to defeat percent-encoding evasion. Malicious traffic is rejected with HTTP 403 before JWT validation or downstream routing ever runs.

### Quality Scenario:

| Component | Answer |
| :--- | :--- |
| **Source** | Threat actor — External (Unknown) |
| **Stimulus** | SQL Injection / XSS / Path Traversal attempt on any endpoint |
| **Artifact** | API Gateway (WafFilter) |
| **Environment** | Normal Operation — Production |
| **Response** | Decode payload, match against attack signatures, and reject request with HTTP 403 |
| **Response Measure** | Recognize attack pattern < 200 ms; block request before it reaches any microservice |

---

#### Applied Architectural Pattern

**WAF (Web Application Firewall)** — A global filter screens every request at the edge using signature-based pattern matching. The WAF acts as the first line of defense, blocking known attack vectors (SQLi, XSS, Path Traversal) before any business logic is involved.

#### Applied Architectural Tactics

**Verify Message Integrity** — The WAF is the first stage in the API Gateway's filter pipeline. Requests flow through WAF → JWT validation → path rewriting → routing to the target microservice, each stage being independent and composable.

---

### 4.2 Secure Channel — TLS Termination at the API Gateway

TLS is terminated at the API Gateway, the only publicly reachable container. It presents a valid X.509 certificate to connecting clients; all internal `appnet` traffic stays on the private Docker bridge network. This keeps certificate management in one place and ensures no plaintext data is exposed on the external network.

### Quality Scenario:

| Component | Answer |
| :--- | :--- |
| **Source** | Threat actor — External (Network Eavesdropper / MITM) |
| **Stimulus** | Interception attempt on plaintext HTTP traffic between client and server |
| **Artifact** | API Gateway (TLS termination endpoint) |
| **Environment** | Normal Operation — Production |
| **Response** | Enforce TLS 1.2 / 1.3; reject plaintext connections; encrypt all data in transit |
| **Response Measure** | TLS handshake complete < 100 ms; zero plaintext bytes exposed to the external network |

---

#### Applied Architectural Pattern

**Secure Channel** — TLS 1.2 / 1.3 encrypts all traffic between external clients and the API Gateway, preventing eavesdropping and man-in-the-middle attacks. Modern cipher suites are enforced and deprecated protocols (SSLv3, TLS 1.0/1.1) are disabled.

#### Applied Architectural Tactics

**Encrypt Data** — Because only the API Gateway is published externally, TLS is applied once at the boundary. All downstream microservices communicate over the private `appnet` network without needing their own certificates.

---

## 5. Performance and Scalabillity

---

### 5.1 Load Balancer

The Load Balancer pattern was implemented to distribute incoming requests across multiple instances of the Menu service, preventing the saturation of a single node and improving the scalability and availability of the system.

### Quality Scenario:

| Attribute          | Description |
|--------------------|-------------|
| **Source**         | Clients/Users |
| **Stimulus**       | The users generate 100 menu requests per second (100 req/s) |
| **Artifact**       | Load Balancer and Menu Service instances |
| **Environment**    | Normal operation in a local development environment |
| **Response**       | The load balancer distributes incoming requests evenly across the available Menu Service instances |
| **Response Measure** | - Response time < 300 ms  <br> - CPU utilization < 80%  <br> - Error rate < 1% |

---

#### Applied Architectural Pattern and Tactics

The primary tactic implemented is **Maintain Multiple Copies of Computation** and **Increase Additional Computational Resources**, as incoming requests are distributed across multiple instances of the menu service.

---

### 5.2 Throttling

The Throttling (rate limiting) pattern was implemented to control the flow of requests to the Menu service, protecting the system against traffic spikes and abuse (for example, denial-of-service attacks or malicious clients).

### Quality Scenario:

We use 2 scenarios, one for limiting requests made by a single user and another for global requests.


| Attribute          | Description |
|--------------------|-------------|
| **Source**         | Clients/Users |
| **Stimulus**       | A single IP address sends more than 25 req/second |
| **Artifact**       | Throttler (NGINX) and Menu Service instances |
| **Environment**    | Normal operation in a local development environment |
| **Response**       | The throttler drops individual excess requests breaking the Per-IP limit. Dropped requests are instantly rejected at the proxy layer with an HTTP 503 Service Unavailable response, protecting the backend |
| **Response Measure** | - Total traffic forwarded to menu service instances per user is 25 requests per second (15 base rate + 10 burst queue)  <br> - 0% of excess traffic reaches the backend instances  <br> - Throttler response time for rejected requests < 10 ms (dropped immediately) |

| Attribute          | Description |
|--------------------|-------------|
| **Source**         | Clients/Users |
| **Stimulus**       | Concurrent users generate more than 1100 req/second |
| **Artifact**       | Throttler (NGINX) and Menu Service instances |
| **Environment**    | Normal operation in a local development environment |
| **Response**       | The throttler drops global excess requests breaking the Global limit. Dropped requests are instantly rejected at the proxy layer with an HTTP 503 Service Unavailable response, protecting the backend |
| **Response Measure** | - Total traffic forwarded to menu service instances is 1100 requests per second (1000 base rate + 100 burst queue)  <br> - 0% of excess traffic reaches the backend instances  <br> - Throttler response time for rejected requests < 10 ms (dropped immediately) |

---

#### Applied Architectural Pattern and Tactics

The primary tactic implemented is **Limit Request Rate** and **Protect System Against Overload**, as the rate of incoming requests is restricted both per-client and globally, dropping the excess immediately at the proxy layer (NGINX) to prevent the menu service from becoming saturated.


### Testing Analysis and Results

A test is performed in JMeter once the Load Balancer has been implemented.

The load test did not yield a performance curve with a clearly defined inflection point (knee), due to the limited number of measurement points.

However, it is estimated that the knee occurs below 2000 concurrent users. Up to that threshold, the system maintained stable response times and an acceptable throughput.

At that point, response times spiked and the error rate reached 6%, demonstrating resource saturation (request queues, increased latency, and general degradation).

With this information, it is decided to implement the Throttler pattern, defining a global limit of 1100 req/s and a per-user (IP) limit of 25 req/s (per the group's criteria).
# Interoperability in Click & Munch

## 7. Applied interoperability pattern

The Click & Munch project implements interoperability mainly in the backend using the **Mediator** pattern combined with a **message broker** (RabbitMQ).

### 7.1 Where it applies

- `NotificationService` is the component that materializes interoperability.
- `OrderService` and `ReservationService` publish domain events without knowing the external delivery channels.
- `RabbitMQ` acts as the central mediator between event producers and notification consumers.
- `TelegramWorker` is the only component that knows the Telegram API.
- `AuthService` is queried by `NotificationService` to obtain the user's `telegramChatId`.

### 7.2 How decoupling is achieved

- Core services (`OrderService`, `ReservationService`) publish generic events such as `order.created`, `order.status.changed`, `reservation.confirmed`, `reservation.cancelled`.
- These events are sent to the `clickmunch.events` exchange of type **topic**.
- `NotificationService` consumes the events and creates internal notifications (persistence + SSE).
- If the user has Telegram linked, `NotificationService` publishes an additional event with routing key `notification.send` to the `notification.telegram.queue`.
- `TelegramWorker` consumes that queue and performs the `HTTP POST` to `https://api.telegram.org/bot{token}/sendMessage`.

### 7.3 Pattern benefits in the project

- **Decoupled interoperability**: producers do not need knowledge of Telegram or other channels.
- **Extensibility**: adding a new notification channel (email, WhatsApp, SMS) only requires adding a new worker or consumer, without changing core services.
- **Resilience**: external channel failures do not directly affect the main order and reservation flows.
- **Separation of concerns**: `NotificationService` handles notification logic while `TelegramWorker` handles the external adapter.

### 7.4 Mediator pattern description

The system uses the **Mediator** pattern to centralize communication between domain event producers and delivery channels. In this case, `RabbitMQ` acts as the mediator:

- Producers (`OrderService`, `ReservationService`) send generic domain events to the broker.
- The broker routes those events to one or more consumers without producers knowing the consumers.
- `NotificationService` receives events and decides whether to create internal notifications, send SSE, or publish a Telegram send request.
- `TelegramWorker` is the final consumer that knows the external Telegram API.

This keeps the core services focused on business logic while the mediator handles distribution and integration with external channels.

### 7.5 Design tactics applied

The following design tactics were applied to achieve interoperability quality:

- **Message-based decoupling**: events are transported through RabbitMQ rather than direct service-to-service calls.
- **Single responsibility**: notification creation, event routing, and external delivery are separated across different components.
- **Explicit channel isolation**: the Telegram integration is isolated in `TelegramWorker`, so external API changes stay localized.
- **Asynchronous delivery**: using queues prevents Telegram delivery latency or failure from blocking the core workflow.
- **Optional integration**: Telegram is optional; users without a linked chat ID still receive notifications through SSE.

### 7.6 Key design elements

- `clickmunch.events` → RabbitMQ topic exchange.
- `notification.order.queue`, `notification.reservation.queue`, `notification.telegram.queue` → durable queues.
- `NotificationEventConsumer` → queries `AuthService` to obtain the `telegramChatId` and creates the notification.
- `TelegramNotificationPublisher` → publishes abstract messages without Telegram logic.
- `TelegramWorker` → final adapter that knows the external Telegram API.

---

## 7.7 Interoperability quality scenario

| Attribute | Description |
|----------|-------------|
| **Source** | Domain events emitted by internal services (`OrderService`, `ReservationService`) and users with Telegram linked. |
| **Stimulus** | An order or reservation event is generated and the user has a valid `telegramChatId`. |
| **Artifact** | `NotificationService` interoperability pipeline. |
| **Environment** | Normal operation in the backend environment with RabbitMQ available and `NotificationService` deployed. |
| **Response** | The event is consumed and processed: the internal notification is stored, delivered via SSE, and the Telegram send request is enqueued. |
| **Response Measure** | - Event processed in < 500 ms.<br>- Published to `notification.telegram.queue` in < 200 ms.<br>- Producer service does not need changes to support the Telegram channel.<br>- Telegram errors do not block persistence or SSE delivery. |

### 7.8 Scenario justification

This scenario shows that the system's interoperability allows the business core to connect to an external channel (Telegram) without the order and reservation services knowing the external implementation. The quality value lies in the ability to integrate new channels with minimal system changes and maintain consistent user behavior even if the external channel fails.

### 7.9 Quality observations

- The system can be extended to other messaging channels with a new worker listening to a different queue.
- Interoperability depends on a centralized mediator (`RabbitMQ`), which makes this component critical for notification flow availability.
- Maintaining the event contract and the producer/consumer separation is key to preserving interoperability quality.

## 8. Prototype

### Prerequisites

- **Docker** and **Docker Compose** (for the backend)
- **Node.js** ≥ 18 and **npm** (for the frontends)
- **Expo CLI** (for the mobile app): `npm install -g expo-cli`

### Backend

The backend stack comprises 10 microservices, 8 backing databases, and 1 RabbitMQ broker — all orchestrated by Docker Compose (29 containers total).

```bash
# 1. Clone the repository
git clone <repository-url>
cd ClickAndMunchApp

# 2. Build and start all backend services
cd backend
docker compose up --build -d

# 3. Verify all containers are healthy
docker compose ps
```

All containers should show **"(healthy)"**. Public endpoints:

- REST API: `http://localhost:8080`
- Kitchen WebSocket: `ws://localhost:8080/ws/kitchen`
- RabbitMQ Management: `http://localhost:15672` (user: `mike` / `secret`)

| Service | Host port | Notes |
|---------|----------:|-------|
| API Gateway (REST + WebSocket) | **8080** | Only public edge |
| AuthService | — | Internal only |
| RestaurantService | — | Internal only |
| GeoService | — | Internal only |
| MenuService | — | Internal only |
| OrderService | — | Internal only (REST + WS) |
| ReservationService | — | Internal only |
| NotificationService | — | Internal only (REST + SSE) |
| RatingService | — | Internal only |
| CheckoutService ★ Python | — | Internal only |
| RabbitMQ (AMQP) | **5672** | |
| RabbitMQ Management UI | **15672** | |
| auth_db (PostgreSQL) | 5433 | Dev inspection |
| restaurant_db (PostgreSQL) | 5434 | Dev inspection |
| geo_db (PostGIS) | 5435 | Dev inspection |
| order_db (PostgreSQL) | 5436 | Dev inspection |
| menu_db (MongoDB) | 27018 | Dev inspection |
| reservation_db (PostgreSQL) | 5437 | Dev inspection |
| rating_db (PostgreSQL) | 5438 | Dev inspection |
| notification_db (PostgreSQL) | 5439 | Dev inspection |

**Gateway routes:**

| Route | Service |
|-------|---------|
| `/auth/**` | AuthService |
| `/restaurant/**` | RestaurantService |
| `/menu/**` | MenuService |
| `/order/**` | OrderService |
| `/ws/kitchen` | OrderService WebSocket |
| `/reservation/**` | ReservationService |
| `/checkout/**` | CheckoutService (Python/FastAPI) |
| `/rating/**` | RatingService |
| `/notification/**` | NotificationService |

**Quick test — register and log in:**

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","username":"testuser","password":"123456","role":"CUSTOMER"}'

curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
```

**Place an order (waiter flow):**

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

**Full checkout (customer flow via Python CheckoutService):**

```bash
curl -X POST http://localhost:8080/checkout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt>" \
  -d '{
    "customerId": 1,
    "customerName": "Test User",
    "restaurantId": 1,
    "restaurantName": "El Rincón",
    "items": [
      {"menuItemId": "abc123", "productName": "Hamburguesa", "quantity": 2, "unitPrice": 15000}
    ],
    "paymentMethod": "CARD"
  }'
```

**Other useful reads:**

```bash
curl -H "Authorization: Bearer <jwt>" http://localhost:8080/restaurant/owner/1
curl -H "Authorization: Bearer <jwt>" http://localhost:8080/menu/restaurants/1
curl -H "Authorization: Bearer <jwt>" http://localhost:8080/order/kitchen/1
curl -H "Authorization: Bearer <jwt>" "http://localhost:8080/order/restaurant/1?status=PENDING"
curl -H "Authorization: Bearer <jwt>" http://localhost:8080/notification
```

**Stop the backend:**

```bash
docker compose down
```

---

### Frontend — Web Dashboard

```bash
cd frontend/dashboard
npm install
npm run dev
```

Available at `http://localhost:5173`. Kitchen view at `/kitchen`.

---

### Frontend — Mobile App

```bash
cd frontend/mobile
npm install
npx expo start
```

- Press **i** — iOS Simulator
- Press **a** — Android Emulator
- Scan QR code with **Expo Go** on your phone
