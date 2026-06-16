# Interoperability in Click & Munch

## 1. Applied interoperability pattern

The Click & Munch project implements interoperability mainly in the backend using the **Mediator** pattern combined with a **message broker** (RabbitMQ).

### 1.1 Where it applies

- `NotificationService` is the component that materializes interoperability.
- `OrderService` and `ReservationService` publish domain events without knowing the external delivery channels.
- `RabbitMQ` acts as the central mediator between event producers and notification consumers.
- `TelegramWorker` is the only component that knows the Telegram API.
- `AuthService` is queried by `NotificationService` to obtain the user's `telegramChatId`.

### 1.2 How decoupling is achieved

- Core services (`OrderService`, `ReservationService`) publish generic events such as `order.created`, `order.status.changed`, `reservation.confirmed`, `reservation.cancelled`.
- These events are sent to the `clickmunch.events` exchange of type **topic**.
- `NotificationService` consumes the events and creates internal notifications (persistence + SSE).
- If the user has Telegram linked, `NotificationService` publishes an additional event with routing key `notification.send` to the `notification.telegram.queue`.
- `TelegramWorker` consumes that queue and performs the `HTTP POST` to `https://api.telegram.org/bot{token}/sendMessage`.

### 1.3 Pattern benefits in the project

- **Decoupled interoperability**: producers do not need knowledge of Telegram or other channels.
- **Extensibility**: adding a new notification channel (email, WhatsApp, SMS) only requires adding a new worker or consumer, without changing core services.
- **Resilience**: external channel failures do not directly affect the main order and reservation flows.
- **Separation of concerns**: `NotificationService` handles notification logic while `TelegramWorker` handles the external adapter.

### 1.4 Key design elements

- `clickmunch.events` → RabbitMQ topic exchange.
- `notification.order.queue`, `notification.reservation.queue`, `notification.telegram.queue` → durable queues.
- `NotificationEventConsumer` → queries `AuthService` to obtain the `telegramChatId` and creates the notification.
- `TelegramNotificationPublisher` → publishes abstract messages without Telegram logic.
- `TelegramWorker` → final adapter that knows the external Telegram API.

---

## 2. Interoperability quality scenario

| Attribute | Description |
|----------|-------------|
| **Source** | Domain events emitted by internal services (`OrderService`, `ReservationService`) and users with Telegram linked. |
| **Stimulus** | An order or reservation event is generated and the user has a valid `telegramChatId`. |
| **Artifact** | `NotificationService` interoperability pipeline. |
| **Environment** | Normal operation in the backend environment with RabbitMQ available and `NotificationService` deployed. |
| **Response** | The event is consumed and processed: the internal notification is stored, delivered via SSE, and the Telegram send request is enqueued. |
| **Response Measure** | - Event processed in < 500 ms.<br>- Published to `notification.telegram.queue` in < 200 ms.<br>- Producer service does not need changes to support the Telegram channel.<br>- Telegram errors do not block persistence or SSE delivery. |

### 2.1 Scenario justification

This scenario shows that the system's interoperability allows the business core to connect to an external channel (Telegram) without the order and reservation services knowing the external implementation. The quality value lies in the ability to integrate new channels with minimal system changes and maintain consistent user behavior even if the external channel fails.

### 2.2 Quality observations

- The system can be extended to other messaging channels with a new worker listening to a different queue.
- Interoperability depends on a centralized mediator (`RabbitMQ`), which makes this component critical for notification flow availability.
- Maintaining the event contract and the producer/consumer separation is key to preserving interoperability quality.
