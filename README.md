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
# Laboratorio 6 — Reliability

**Curso:** Arquitectura de Software · 2026-I  
**Plataforma:** ClickMunch (entrega de comida a domicilio)

---

## 4.1 Información del Equipo

| # | Nombre |
|--|---|
| 1 | Michael Stiven Betancourt Gelves |
| 2 | Santiago Bejarano Ariza |
| 3 | Santiago Suaza Montalvo|
| 4 | Julian David Ruiz Ramos |
| 5 | Manuel Felipe Espinosa Español |
| 6 | Manuel Santiago Mori Ardila |

---

## 4.2 Vistas Arquitectónicas

### Vista 1 — Cluster Pattern (Kubernetes)

```
┌──────────────────────────────────────────────────────────────────────┐
│                    MINIKUBE NODE (VM local)                          │
│                                                                      │
│  Namespace: clickmunch                                               │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Service: apigateway (NodePort :30080)                      │    │
│  │  ┌──────────────────┐    ┌──────────────────┐              │    │
│  │  │  Pod: apigateway  │    │  Pod: apigateway  │  replicas=2 │    │
│  │  │  (réplica 1)     │    │  (réplica 2)     │              │    │
│  │  │  :8080           │    │  :8080           │              │    │
│  │  └──────────────────┘    └──────────────────┘              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                       │ ClusterIP                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Service: authservice (ClusterIP :8081)                     │    │
│  │  ┌──────────────────┐    ┌──────────────────┐              │    │
│  │  │  Pod: authservice │    │  Pod: authservice │  replicas=2 │    │
│  │  │  (réplica 1)     │    │  (réplica 2)     │  HOT SPARE  │    │
│  │  │  :8081           │    │  :8081           │              │    │
│  │  └──────────────────┘    └──────────────────┘              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                       │ ClusterIP                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Service: auth-db (ClusterIP :5432)                         │    │
│  │  ┌──────────────────────────────────────────┐              │    │
│  │  │  StatefulSet: auth-db (PostgreSQL 16)    │              │    │
│  │  │  PVC: auth-db-pvc (1Gi)                 │              │    │
│  │  └──────────────────────────────────────────┘              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└───────────────────────────────────────┬──────────────────────────────┘
                                        │ NodePort :30080
                               ┌────────┴─────────┐
                               │  Cliente / Browser│
                               │  (host machine)  │
                               └──────────────────┘
```

**Leyenda:**
- El **Control Plane** de Minikube (scheduler, controller-manager, etcd, API server) gestiona el nodo internamente.
- Las flechas de carga entre el Service y los Pods representan el balanceo de carga round-robin de kube-proxy.
- Los Pods de APIGateway enrutan hacia servicios externos (los demás microservicios del proyecto) a través de `host.docker.internal`, que los alcanza en su Docker Compose en la máquina host.

---

### Vista 2 — Active Redundancy / Hot Spare (AuthService)

```
              ┌─────────────────────────────────────────────────┐
              │  K8s Service: authservice (ClusterIP, :8081)    │
              │  Algoritmo de balanceo: round-robin (kube-proxy)│
              └────────────┬────────────────────┬───────────────┘
                           │                    │
              ─────────────────────────────────────────────────
              SOLICITUDES  │  (todos los requests│  se reparten)
              ─────────────────────────────────────────────────
                           │                    │
              ┌────────────▼──────────┐  ┌──────▼─────────────┐
              │  Pod authservice-1    │  │  Pod authservice-2  │
              │  (ACTIVO)             │  │  (HOT SPARE)        │
              │                       │  │                      │
              │  • Procesa logins     │  │  • Procesa logins   │
              │  • Procesa registers  │  │  • Procesa registers│
              │  • Emite JWT tokens   │  │  • Emite JWT tokens │
              │                       │  │                      │
              │  Estado compartido ──────────────────────────► │
              │  (PostgreSQL auth-db) │  │  (mismo auth-db)    │
              └───────────────────────┘  └─────────────────────┘
                           │                    │
                           └─────────┬──────────┘
                                     │ ambos leen/escriben
                                     ▼
                          ┌──────────────────────┐
                          │  StatefulSet: auth-db │
                          │  (PostgreSQL 16)       │
                          │  PVC: auth-db-pvc     │
                          └──────────────────────┘

  ─────────────────────────────────────────────────────────────────
  ESCENARIO DE FALLO: authservice-1 cae (OOM / crash)
  ─────────────────────────────────────────────────────────────────

  1. Liveness probe falla → kube-proxy remueve el endpoint en <5 s
  2. authservice-2 absorbe el 100% del tráfico (0 ms de downtime visible)
  3. Controller manager detecta réplicas < 2 → agenda nuevo pod
  4. Nuevo pod alcanza estado Ready en ~90 s
  5. PodDisruptionBudget (minAvailable: 1) garantiza que nunca
     ambas réplicas estén inactivas simultáneamente
```

---

## 4.3 Guía Técnica — Parte A: Cluster Pattern

### 1. Descripción del Patrón y Tácticas de Confiabilidad

El **Cluster Pattern** consiste en agrupar múltiples nodos de cómputo bajo una capa de gestión unificada que actúa como un único sistema lógico ante los clientes. Kubernetes implementa este patrón mediante tres abstracciones principales:

| Abstracción | Función |
|---|---|
| **Pod** | Unidad mínima de despliegue; encapsula uno o más contenedores |
| **Deployment** | Declara el estado deseado (réplicas, imagen, estrategia de actualización); el Controller Manager lo reconcilia continuamente |
| **Service** | Endpoint estable y único que balancea el tráfico entre los Pods activos |

Las **tácticas de confiabilidad** que soporta:

- **Detección de fallos** — Liveness y Readiness Probes detectan pods no saludables y los eliminan del pool de tráfico automáticamente.
- **Repuesto redundante** (Redundant Spare) — Múltiples réplicas aseguran que la caída de una instancia no interrumpa el servicio.
- **Balanceo de carga** — kube-proxy distribuye solicitudes entre réplicas sanas, maximizando el uso de recursos y evitando puntos calientes.
- **Auto-healing** — El Controller Manager reconcilia constantemente el estado real con el deseado: si un Pod muere, se crea uno nuevo.
- **Rolling Updates** — Las actualizaciones de imagen se aplican reemplazando Pods uno a uno sin downtime.

### 2. Tipo de Clúster Implementado

El despliegue del **APIGateway** corresponde a un clúster **Active/Active**:

> Todas las réplicas (2 por defecto, escalable a N) sirven tráfico simultáneamente. El Service de Kubernetes actúa como load balancer, distribuyendo cada petición HTTP/WebSocket entrante a cualquiera de los Pods disponibles de forma round-robin.

**Justificación:** El APIGateway es completamente *stateless* — solo verifica el JWT y enruta la solicitud al microservicio correspondiente. No mantiene sesiones ni estado en memoria entre peticiones. Esto hace que cualquier réplica pueda atender cualquier request sin coordinación, lo que es la condición necesaria y suficiente para un clúster Active/Active.

### 3. Pasos de Implementación

#### Prerrequisitos

```bash
# Instalar Minikube (Windows)
winget install Kubernetes.minikube

# Iniciar el cluster local
minikube start --cpus=4 --memory=6144

# Verificar que el cluster está activo
kubectl cluster-info
```

#### Paso 1 — Construir imágenes dentro de Minikube

```bash
# Apuntar Docker al daemon interno de Minikube (evita push a registry externo)
eval $(minikube docker-env)        # Linux/Mac
# En PowerShell Windows:
# minikube docker-env | Invoke-Expression

# Construir las imágenes necesarias
bash k8s/scripts/build-images-minikube.sh
```

#### Paso 2 — Desplegar todo el stack

```bash
bash k8s/scripts/deploy.sh
```

El script aplica los manifiestos en orden: Namespace → ConfigMap → Secret → auth-db → AuthService → APIGateway.

#### Paso 3 — Verificar el despliegue

```bash
kubectl get pods -n clickmunch
kubectl get svc  -n clickmunch
```

Salida esperada:

```
NAME                           READY   STATUS    RESTARTS   AGE
apigateway-7d8f9b6c4-k2xpq    1/1     Running   0          2m
apigateway-7d8f9b6c4-n8wvr    1/1     Running   0          2m
authservice-5c6b7d8e9-j4mnp   1/1     Running   0          3m
authservice-5c6b7d8e9-p7qst   1/1     Running   0          3m
auth-db-0                     1/1     Running   0          4m

NAME          TYPE        CLUSTER-IP      PORT(S)          AGE
apigateway    NodePort    10.96.45.12     80:30080/TCP     2m
authservice   ClusterIP   10.96.112.34    8081/TCP         3m
auth-db       ClusterIP   10.96.200.5     5432/TCP         4m
```

### 4. Manifiestos YAML

#### `k8s/apigateway/deployment.yaml` (fragmento clave)

```yaml
spec:
  replicas: 2                         # Active/Active cluster
  strategy:
    type: RollingUpdate               # Actualizaciones sin downtime
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  template:
    spec:
      containers:
        - name: apigateway
          image: clickmunch/apigateway:latest
          imagePullPolicy: Never      # Imagen local de Minikube
          ports:
            - containerPort: 8080
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 45
            periodSeconds: 15
```

#### `k8s/apigateway/service.yaml`

```yaml
spec:
  type: NodePort
  selector:
    app: apigateway
  ports:
    - port: 80
      targetPort: 8080
      nodePort: 30080
```

### 5. Evidencia de Self-Healing y Escalado

#### 5.1 Self-Healing

```bash
# Listar pods actuales
kubectl get pods -n clickmunch -l app=apigateway

# Eliminar un pod (simula fallo inesperado)
kubectl delete pod <nombre-del-pod> -n clickmunch

# Observar en tiempo real cómo Kubernetes crea uno nuevo
kubectl get pods -n clickmunch -l app=apigateway --watch
```

Salida esperada (el pod `Terminating` se reemplaza por un `ContainerCreating` en segundos):

```
NAME                          READY   STATUS        RESTARTS   AGE
apigateway-7d8f9b6c4-k2xpq   1/1     Terminating   0          10m
apigateway-7d8f9b6c4-n8wvr   1/1     Running       0          10m
apigateway-7d8f9b6c4-x9abc   0/1     Pending       0          2s
apigateway-7d8f9b6c4-x9abc   0/1     ContainerCreating  0    4s
apigateway-7d8f9b6c4-x9abc   1/1     Running       0          35s
```

#### 5.2 Escalado

```bash
# Escalar a 4 réplicas
kubectl scale deployment apigateway --replicas=4 -n clickmunch

# Verificar
kubectl get pods -n clickmunch -l app=apigateway

# Volver a 2
kubectl scale deployment apigateway --replicas=2 -n clickmunch
```

---

## 4.4 Guía Técnica — Parte B: Patrón de Redundancia

### 1. Descripción del Patrón

Se implementa **Active Redundancy (Hot Spare)**, que corresponde a la táctica **Redundant Spare** dentro del grupo *Recover from Faults > Preparation and Repair* del catálogo de tácticas de confiabilidad de Len Bass.

**Definición:** Todas las instancias del grupo de protección (activas y spare) **reciben y procesan las mismas solicitudes en paralelo** en todo momento. El estado se mantiene sincronizado porque todas las réplicas comparten el mismo origen de verdad (la base de datos PostgreSQL). Ante la falla de una instancia, las demás continúan operando sin interrupción y sin necesitar un proceso de sincronización, ya que estaban completamente actualizadas.

**Componente seleccionado: AuthService**

El AuthService es el microservicio más crítico de la plataforma: ninguna operación autenticada puede ejecutarse si este servicio falla. Al ser un servicio **sin estado en memoria** (toda la persistencia vive en PostgreSQL), es el candidato ideal para Active Redundancy: múltiples réplicas pueden procesar solicitudes de login y registro de forma completamente independiente.

### 2. Escenario de Calidad

| Elemento | Descripción |
|---|---|
| **Fuente** | Fallo interno de infraestructura (proceso de la JVM) |
| **Estímulo** | Un Pod del AuthService termina inesperadamente por un error OutOfMemoryError durante la hora pico del almuerzo (12:00–14:00) |
| **Artefacto** | AuthService — microservicio de autenticación y autorización (puerto 8081) |
| **Entorno** | Operación normal con carga pico: ~500 solicitudes de login concurrentes distribuidas entre 2 réplicas activas |
| **Respuesta** | La liveness probe detecta el pod fallido en ≤ 30 s; kube-proxy elimina ese endpoint del pool; la réplica superviviente absorbe el 100% del tráfico de inmediato; el Controller Manager agenda un pod de reemplazo; el PodDisruptionBudget garantiza que nunca ambas réplicas caigan simultáneamente |
| **Medida de respuesta** | Cero solicitudes de autenticación perdidas (el spare ya estaba procesando tráfico activamente); tiempo de detección ≤ 30 s; tiempo hasta restaurar 2 réplicas ≤ 90 s; downtime perceptible por el usuario: 0 ms |

### 3. Pasos de Implementación

El patrón está implementado en tres capas complementarias:

#### Capa 1 — Deployment con 2 réplicas y anti-afinidad

El `authservice/deployment.yaml` configura:

```yaml
spec:
  replicas: 2
  strategy:
    rollingUpdate:
      maxUnavailable: 0    # Nunca reduce réplicas durante actualizaciones
      maxSurge: 1
  template:
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchLabels:
                    app: authservice
                topologyKey: kubernetes.io/hostname
```

La regla `podAntiAffinity` instruye al scheduler a colocar cada réplica en un **nodo físico diferente** cuando sea posible, garantizando redundancia a nivel de hardware (no solo de proceso).

#### Capa 2 — Probes de liveness y readiness

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
  initialDelaySeconds: 60
  periodSeconds: 15
  failureThreshold: 3        # 3 fallos → pod reiniciado

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 45
  periodSeconds: 10
  failureThreshold: 3        # 3 fallos → pod removido del Service
```

#### Capa 3 — PodDisruptionBudget

```yaml
# authservice/pdb.yaml
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: authservice
```

El PDB garantiza que durante operaciones de mantenimiento planeadas (`kubectl drain`), Kubernetes **nunca retire más de un pod simultáneamente**, preservando siempre el contrato del Hot Spare.

### 4. Fragmentos de Configuración

El `authservice/deployment.yaml` completo se encuentra en [k8s/authservice/deployment.yaml](authservice/deployment.yaml).

Puntos clave de configuración:

```yaml
# Estado compartido: ambas réplicas leen/escriben la misma BD
- name: SPRING_DATASOURCE_URL
  valueFrom:
    configMapKeyRef:
      name: clickmunch-config
      key: SPRING_DATASOURCE_URL

# JWT_SECRET idéntico en ambas → tokens son válidos en cualquier réplica
- name: JWT_SECRET
  valueFrom:
    secretKeyRef:
      name: clickmunch-secrets
      key: JWT_SECRET
```

**Por qué no se necesita sincronización de estado entre réplicas:**  
El AuthService no guarda estado en memoria entre peticiones. Cada login lee `users` de PostgreSQL y cada token emitido se verifica con el mismo `JWT_SECRET`. Dos réplicas con el mismo secret y la misma BD son funcionalmente idénticas — esto es la definición de Hot Spare sin overhead de sincronización.

### 5. Evidencia de Failover

```bash
# Script automatizado (corre ambas partes del demo):
bash k8s/scripts/simulate-failover.sh authservice
```

**Pasos manuales equivalentes:**

```bash
# 1. Ver las dos réplicas activas y sus nodos
kubectl get pods -n clickmunch -l app=authservice -o wide

# Salida:
# NAME                           READY   NODE       IP
# authservice-5c6b7d8e9-j4mnp   1/1     minikube   172.17.0.4
# authservice-5c6b7d8e9-p7qst   1/1     minikube   172.17.0.5

# 2. Eliminar una réplica (simula crash inesperado)
kubectl delete pod authservice-5c6b7d8e9-j4mnp -n clickmunch

# 3. Observar en tiempo real:
kubectl get pods -n clickmunch -l app=authservice --watch

# Salida esperada:
# authservice-5c6b7d8e9-j4mnp   1/1   Terminating   0   5m   ← pod caído
# authservice-5c6b7d8e9-p7qst   1/1   Running       0   5m   ← HOT SPARE activo, absorbe tráfico
# authservice-5c6b7d8e9-r8uvw   0/1   Pending       0   3s   ← nuevo pod siendo creado
# authservice-5c6b7d8e9-r8uvw   1/1   Running       0   78s  ← restaurado

# 4. Probar que el servicio nunca dejó de responder (en otra terminal)
MINIKUBE_IP=$(minikube ip)
while true; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    http://$MINIKUBE_IP:30080/auth/actuator/health)
  echo "$(date +%T) — HTTP $STATUS"
  sleep 1
done
# Resultado esperado: nunca aparece un status diferente a 200
```

### 6. Recomendaciones para Otros Equipos

**Recomendación 1 — Asegúrate de que el servicio sea truly stateless antes de elegir Hot Spare**

Active Redundancy solo funciona sin sincronización si todas las réplicas comparten el mismo estado persistente (una BD) y no guardan estado conversacional en memoria. Si tu servicio tiene caché en memoria, sesiones locales o conexiones WebSocket con estado, necesitas implementar un mecanismo de sincronización explícito (Redis Pub/Sub, sticky sessions, etc.) o considerar Passive Redundancy.

**Recomendación 2 — Configura el PodDisruptionBudget desde el inicio, no como afterthought**

Sin un PDB, un `kubectl drain` de mantenimiento puede eliminar simultáneamente todos los pods del Deployment si caben en el mismo nodo, dejando el servicio sin ninguna réplica por el tiempo que tarda en levantarse el nuevo pod. El PDB con `minAvailable: 1` cuesta cero recursos adicionales y garantiza que el operador de mantenimiento nunca viole el contrato de disponibilidad del Hot Spare sin querer.

---

## 4.5 Guía Técnica — Parte C: Cold Spare (NotificationService)

### 1. Descripción del Patrón

Se implementa **Passive Redundancy (Cold Spare)**, que corresponde a la táctica **Redundant Spare** dentro del grupo *Recover from Faults > Preparation and Repair* del catálogo de tácticas de confiabilidad de Len Bass.

**Definición:** Solo **una instancia** del servicio está activa y procesando solicitudes en todo momento. No hay instancia spare pre-encendida. Cuando la instancia activa falla, el orquestador (Kubernetes) crea una nueva instancia (el "spare frío" se enciende), que debe pasar por todo el ciclo de arranque antes de poder procesar tráfico. Durante este periodo de arranque, el servicio está **no disponible** — pero gracias a RabbitMQ como buffer de mensajes, **no se pierde ningún evento**.

**Componente seleccionado: NotificationService**

El NotificationService es un servicio **no-crítico para la operación de negocio**: los pedidos, pagos y reservaciones pueden funcionar normalmente sin notificaciones. Esto lo hace el candidato ideal para Cold Spare: se acepta un periodo de inactividad de ~60-90 s a cambio de menor consumo de recursos (una sola instancia en vez de dos).

### 2. Comparación: Cold Spare vs Hot Spare

| Aspecto | Hot Spare (AuthService) | Cold Spare (NotificationService) |
|---|---|---|
| **Réplicas activas** | 2 (ambas procesan tráfico) | 1 (sin spare pre-encendida) |
| **Consumo de CPU/RAM** | 2× (doble de recursos) | 1× (recursos mínimos) |
| **Downtime durante fallo** | 0 ms | ~60-90 s (arranque del spare) |
| **PDB** | `minAvailable: 1` | `maxUnavailable: 1` |
| **Anti-afinidad de pod** | Sí (distribuir en nodos) | No (1 sola réplica) |
| **Pérdida de mensajes** | Ninguna | Ninguna (RabbitMQ los retiene) |
| **Sincronización de estado** | Continua (BD compartida) | No necesaria |
| **Caso de uso ideal** | Servicios críticos (auth, pagos) | Servicios tolerantes a latencia |

### 3. Escenario de Calidad

| Elemento | Descripción |
|---|---|
| **Fuente** | Fallo interno de infraestructura (proceso de la JVM) |
| **Estímulo** | El único Pod activo del NotificationService termina inesperadamente por un OutOfMemoryError durante la hora pico del almuerzo (12:00–14:00) |
| **Artefacto** | NotificationService — microservicio de notificaciones y streaming SSE (puerto 8087) |
| **Entorno** | Operación normal con carga: 1 instancia activa, 0 instancias spare pre-encendidas, ~200 eventos de pedidos/reservaciones por hora encolados en RabbitMQ |
| **Respuesta** | La liveness probe detecta el pod fallido en ≤ 45 s; el Controller Manager agenda un nuevo pod (el spare frío se enciende); RabbitMQ retiene todos los mensajes de `notification.order.queue` y `notification.reservation.queue` sin pérdida; el nuevo pod arranca, se conecta a PostgreSQL y RabbitMQ, y procesa los mensajes acumulados |
| **Medida de respuesta** | **RTO ≤ 90 s** (tiempo de arranque del pod); **RPO = 0** (ningún mensaje perdido); durante el RTO las peticiones REST devuelven HTTP 503; las conexiones SSE se desconectan pero los clientes reconectan automáticamente y reciben las notificaciones acumuladas |

### 4. Vista Arquitectónica

```
              ┌───────────────────────────────────────────────────────────────┐
              │  K8s Service: notificationservice (ClusterIP, :8087)          │
              │  Patrón: Cold Spare (Passive Redundancy)                     │
              └──────────────────────────┬────────────────────────────────────┘
                                         │
                          ┌──────────────▼──────────────┐
                          │  Pod notificationservice-1  │
                          │  (ACTIVO — única instancia) │
                          │                              │
                          │  • Consume eventos RabbitMQ │
                          │  • REST /api/notifications  │
                          │  • SSE /stream/{userId}     │
                          │  • Persiste a PostgreSQL    │
                          └──────────────┬──────────────┘
                                         │
                    ┌────────────────────┼────────────────────┐
                    │                    │                     │
                    ▼                    ▼                     ▼
         ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐
         │  notification-db │  │    RabbitMQ       │  │  APIGateway  │
         │  (PostgreSQL 16) │  │  (Message Broker) │  │  (proxy)     │
         │  PVC: 1Gi       │  │  Colas durables:  │  │              │
         └──────────────────┘  │  • order.queue    │  └──────────────┘
                               │  • reservation.q  │
                               └──────────────────┘

  ─────────────────────────────────────────────────────────────────────────
  ESCENARIO DE FALLO: notificationservice-1 cae (OOM / crash)
  ─────────────────────────────────────────────────────────────────────────

  Tiempo   Evento                                          Estado
  ──────   ──────────────────────────────────────────────   ──────────────
  T+0 s    Pod crashea por OOM                             ⛔ 0/1 Running
  T+15 s   Liveness probe falla (1er intento)              ⛔ 0/1 Running
  T+30 s   Liveness probe falla (2do intento)              ⛔ 0/1 Running
  T+45 s   Liveness probe falla (3er intento) → restart    🔄 Restarting
  T+45 s   RabbitMQ acumula mensajes en colas durables     📨 Buffering
  T+50 s   Nuevo pod en estado ContainerCreating           🔄 Creating
  T+90 s   Spring Boot listo, readiness probe pasa         ✅ 1/1 Running
  T+90 s   Mensajes acumulados de RabbitMQ se procesan     📧 Delivering
  T+90 s   Service endpoint restaurado, REST disponible    ✅ Available

  RTO total: ~90 s | RPO: 0 mensajes perdidos
```

### 5. Pasos de Implementación

#### Capa 1 — Deployment con 1 réplica (Cold Spare)

El `notificationservice/deployment.yaml` configura:

```yaml
spec:
  replicas: 1                       # COLD SPARE: solo 1 instancia activa
  strategy:
    rollingUpdate:
      maxUnavailable: 1             # Se acepta downtime temporal
      maxSurge: 1
  template:
    metadata:
      labels:
        redundancy: cold-spare      # Etiqueta documental
```

A diferencia del Hot Spare (`replicas: 2`), aquí solo hay una instancia. No se configura `podAntiAffinity` porque con 1 réplica no tiene sentido distribuir entre nodos.

#### Capa 2 — Probes de liveness y readiness

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8087
  initialDelaySeconds: 60         # Spring Boot necesita ~45-60 s
  periodSeconds: 15
  failureThreshold: 3             # 3 fallos → pod reiniciado

readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8087
  initialDelaySeconds: 45         # Más agresivo para minimizar RTO
  periodSeconds: 10
  failureThreshold: 3
```

#### Capa 3 — PodDisruptionBudget

```yaml
# notificationservice/pdb.yaml
spec:
  maxUnavailable: 1               # Cold Spare: se permite desalojar el pod
  selector:
    matchLabels:
      app: notificationservice
```

El PDB usa `maxUnavailable: 1` (en vez del `minAvailable: 1` del Hot Spare), porque el Cold Spare acepta periodos de indisponibilidad temporal.

#### Capa 4 — RabbitMQ como buffer de mensajes

RabbitMQ es el componente clave que hace viable el Cold Spare para este servicio:

```yaml
# Las colas son DURABLES (persisten a disco)
@Bean
public Queue orderQueue() {
    return new Queue(ORDER_QUEUE, true);  // true = durable
}
```

Cuando el pod del NotificationService cae, RabbitMQ retiene los mensajes en sus colas durables (`notification.order.queue`, `notification.reservation.queue`). Al arrancar el spare, los `@RabbitListener` reconectan automáticamente y procesan todos los mensajes acumulados.

### 6. Fragmentos de Configuración Clave

```yaml
# Deployment: variables de entorno del cold spare
env:
  # BD propia del NotificationService (K8s internal)
  - name: SPRING_DATASOURCE_URL
    valueFrom:
      configMapKeyRef:
        name: clickmunch-config
        key: NOTIFICATION_DATASOURCE_URL    # jdbc:postgresql://notification-db:5432/notification_db

  # RabbitMQ (K8s internal — buffer de mensajes)
  - name: SPRING_RABBITMQ_HOST
    valueFrom:
      configMapKeyRef:
        name: clickmunch-config
        key: SPRING_RABBITMQ_HOST           # "rabbitmq"
```

**¿Por qué Cold Spare funciona sin perder mensajes?**
1. Las colas de RabbitMQ son **durables** (`true`) — persisten a disco incluso si RabbitMQ reinicia.
2. Los `@RabbitListener` del NotificationService implementan **auto-reconnect** (comportamiento por defecto de Spring AMQP).
3. Al arrancar el spare, Spring se conecta a RabbitMQ y consume todos los mensajes pendientes en orden FIFO.

### 7. Evidencia de Failover

A continuación se presenta un video demostrativo que evidencia el funcionamiento del patrón **Cold Spare** (cómo el orquestador recupera la instancia del `NotificationService` tras un fallo y cómo RabbitMQ actúa como buffer para garantizar que no haya pérdida de mensajes):

<video src="./images/Cold%20Spare%20Notification%20Service.mov" controls width="100%"></video>

*En caso de que el reproductor no sea compatible con tu visor de Markdown, puedes acceder al archivo de video directamente aquí:*  
👉 **[Ver Video de Demostración (Cold Spare)](./images/Cold%20Spare%20Notification%20Service.mov)**

```bash
# Script automatizado:
bash k8s/scripts/simulate-failover.sh notificationservice
```

**Pasos manuales equivalentes:**

```bash
# 1. Ver la única réplica activa
kubectl get pods -n clickmunch -l app=notificationservice -o wide

# Salida:
# NAME                                   READY   NODE       IP
# notificationservice-6a7b8c9d0-x1y2z   1/1     minikube   172.17.0.8

# 2. Eliminar el pod (simula crash inesperado)
kubectl delete pod notificationservice-6a7b8c9d0-x1y2z -n clickmunch

# 3. Observar en tiempo real:
kubectl get pods -n clickmunch -l app=notificationservice --watch

# Salida esperada:
# notificationservice-6a7b8c9d0-x1y2z   1/1   Terminating          0   10m  ← pod caído
# (0 pods Running durante ~60-90 s — esto es el downtime aceptado del Cold Spare)
# notificationservice-6a7b8c9d0-a3b4c   0/1   Pending              0   2s   ← spare creándose
# notificationservice-6a7b8c9d0-a3b4c   0/1   ContainerCreating    0   5s
# notificationservice-6a7b8c9d0-a3b4c   1/1   Running              0   78s  ← spare activo

# 4. Verificar que los mensajes acumulados se procesaron:
kubectl logs -n clickmunch -l app=notificationservice --tail=20

# Resultado esperado: logs mostrando "Received order event: ..." para los
# mensajes que se acumularon en RabbitMQ durante el downtime.
```

### 8. Recomendaciones para Otros Equipos

**Recomendación 1 — Usa Cold Spare solo si tienes un buffer de mensajes**

El Cold Spare funciona aquí porque RabbitMQ actúa como buffer durante el downtime. Si tu servicio recibe solo tráfico REST síncrono (sin cola de mensajes), los clientes recibirán errores 503 durante el RTO. Evalúa si tu caso de uso tolera ese periodo de indisponibilidad.

**Recomendación 2 — Mide y monitorea el RTO real**

El script `simulate-failover.sh` mide el RTO (Recovery Time Objective) automáticamente. Ejecuta esta simulación regularmente para verificar que el tiempo de arranque no se ha degradado (por ejemplo, por dependencias adicionales o aumento del dataset).

**Recomendación 3 — Cold Spare no es sinónimo de "sin protección"**

Aunque el spare está apagado, Kubernetes garantiza el reinicio automático del pod. La diferencia con Hot Spare es solo el **tiempo de recuperación** (90 s vs 0 ms), no la **capacidad de recuperarse**. Ambos patrones son válidos según la criticidad del servicio.

---

## Apéndice — Estructura de Archivos

```
k8s/
├── namespace.yaml                    # Namespace: clickmunch
├── configmap.yaml                    # URLs de servicios y config no-sensible
├── secret.yaml                       # Plantilla (usar create-secret.sh)
├── apigateway/
│   ├── deployment.yaml               # Part A: 2 réplicas, Active/Active cluster
│   └── service.yaml                  # NodePort :30080
├── authservice/
│   ├── deployment.yaml               # Part B: Hot Spare, anti-affinity
│   ├── service.yaml                  # ClusterIP :8081
│   └── pdb.yaml                      # PodDisruptionBudget (minAvailable: 1)
├── auth-db/
│   ├── pvc.yaml                      # PersistentVolumeClaim 1Gi
│   ├── statefulset.yaml              # PostgreSQL 16
│   └── service.yaml                  # ClusterIP :5432
├── notificationservice/
│   ├── deployment.yaml               # Part C: Cold Spare, 1 réplica
│   ├── service.yaml                  # ClusterIP :8087
│   └── pdb.yaml                      # PDB (maxUnavailable: 1)
├── notification-db/
│   ├── pvc.yaml                      # PersistentVolumeClaim 1Gi
│   ├── statefulset.yaml              # PostgreSQL 16
│   └── service.yaml                  # ClusterIP :5432
├── rabbitmq/
│   ├── deployment.yaml               # RabbitMQ 3 con management UI
│   └── service.yaml                  # ClusterIP :5672 + :15672
└── scripts/
    ├── build-images-minikube.sh      # Construye imágenes en el daemon de Minikube
    ├── create-secret.sh              # Crea Secret desde backend/.env
    ├── deploy.sh                     # Aplica todos los manifiestos en orden
    └── simulate-failover.sh          # Demo de self-healing y failover
```


# 7.Interoperability in Click & Munch

## Applied interoperability pattern

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

### Scenario justification

This scenario shows that the system's interoperability allows the business core to connect to an external channel (Telegram) without the order and reservation services knowing the external implementation. The quality value lies in the ability to integrate new channels with minimal system changes and maintain consistent user behavior even if the external channel fails.

### Quality observations

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
