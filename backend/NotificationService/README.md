# NotificationService

Microservicio de notificaciones para Click & Munch. Responsable de consumir eventos de dominio desde RabbitMQ, persistirlos como notificaciones y entregarlos a los usuarios vía Server-Sent Events (SSE) y mensajes de Telegram.

---

## Arquitectura: Patrón Mediator con Message Broker

Este servicio implementa el atributo de calidad de **Interoperabilidad** mediante el patrón **Mediator**, donde RabbitMQ actúa como el mediador centralizado que desacopla totalmente al sistema core de cualquier canal de entrega externo.

```
┌──────────────────┐    routing key           ┌─────────────────────┐
│   OrderService   │ ──"notification.send"──▶ │  clickmunch.events  │
│ ReservationService│                          │   (TopicExchange)   │
│  (Emisores)      │                          │   [MEDIATOR]        │
└──────────────────┘                          └──────────┬──────────┘
  No saben nada                                          │
  de Telegram                                 binding: "notification.send"
                                                         │
                                ┌────────────────────────▼──────────────────────┐
                                │         notification.telegram.queue            │
                                └────────────────────────┬──────────────────────┘
                                                         │
                                                         ▼
                                          ┌──────────────────────────┐
                                          │      TelegramWorker      │
                                          │  (Consumidor + Adaptador)│
                                          │  ÚNICO componente que    │
                                          │  conoce la API Telegram  │
                                          └──────────────┬───────────┘
                                                         │  HTTP POST
                                                         ▼
                                          ┌──────────────────────────┐
                                          │    api.telegram.org      │
                                          │  /bot{token}/sendMessage │
                                          └──────────────────────────┘
```

### Por qué este diseño cumple el patrón Mediator

| Principio | Cómo se aplica |
|-----------|---------------|
| **Desacoplamiento** | `OrderService` y `ReservationService` publican eventos genéricos en el Exchange sin saber que existe Telegram. |
| **Mediador centralizado** | RabbitMQ es el único punto de coordinación entre emisores y consumidores. |
| **Conocimiento localizado** | Solo `TelegramWorker` conoce la URL, el token y el formato de la API de Telegram. |
| **Extensibilidad** | Agregar WhatsApp, email u otro canal = crear un nuevo Worker que escuche una nueva cola. El core no cambia. |

---

## Infraestructura RabbitMQ

Exchange único de tipo **Topic** al que se conectan todas las colas:

| Elemento | Nombre | Tipo |
|----------|--------|------|
| Exchange | `clickmunch.events` | Topic |
| Cola de órdenes | `notification.order.queue` | Durable |
| Cola de reservaciones | `notification.reservation.queue` | Durable |
| Cola de Telegram | `notification.telegram.queue` | Durable |

### Routing Keys

| Routing Key | Cola destino | Evento |
|-------------|-------------|--------|
| `order.created` | `notification.order.queue` | Orden creada |
| `order.status.changed` | `notification.order.queue` | Cambio de estado de orden |
| `reservation.confirmed` | `notification.reservation.queue` | Reservación confirmada |
| `reservation.cancelled` | `notification.reservation.queue` | Reservación cancelada |
| `notification.send` | `notification.telegram.queue` | Envío de mensaje a Telegram |

---

## Componentes del flujo Telegram

### 1. `TelegramNotificationEvent` — El contrato del mensaje

Record Java que representa el comando de notificación de forma abstracta. No contiene ninguna referencia a Telegram.

```java
public record TelegramNotificationEvent(
    String chatId,       // destinatario
    String message,      // contenido
    String originService,// quién originó el evento
    String eventType     // tipo de evento
) {}
```

### 2. `TelegramNotificationPublisher` — El Emisor

Publicado por el sistema core. Envía el evento al Exchange con la routing key `notification.send`. No contiene URLs, tokens ni lógica de HTTP.

```java
// Uso desde cualquier servicio del core:
telegramPublisher.sendNotification(
    chatId,          // ID de chat de Telegram del usuario
    message,         // texto a enviar
    "OrderService",  // nombre del servicio origen
    "ORDER_CREATED"  // tipo de evento
);
```

### 3. `TelegramWorker` — El Consumidor y Adaptador

Worker que escucha la cola `notification.telegram.queue`. Es el **único componente** del sistema con conocimiento de la API de Telegram.

Responsabilidades:
1. Consumir mensajes de RabbitMQ.
2. Transformar el `TelegramNotificationEvent` al formato JSON que requiere Telegram.
3. Ejecutar el `HTTP POST` hacia `api.telegram.org`.
4. Absorber errores de la API externa sin propagar al broker (evita reencolas infinitas).

```
TelegramWorker
├── @RabbitListener(queues = "notification.telegram.queue")
├── sendToTelegram(chatId, text)
│   └── POST https://api.telegram.org/bot{token}/sendMessage
│       └── { chat_id, text, parse_mode: "HTML" }
└── TelegramApiException (error handling interno)
```

### 4. Integración en `NotificationService`

El servicio core delega a Telegram de forma opcional. Si la solicitud incluye `telegramChatId`, encola el envío sin bloquear ni conocer el destino:

```java
// Solo si el request incluye telegramChatId:
if (request.telegramChatId() != null) {
    telegramPublisher.sendNotification(...); // fire-and-forget
}
```

---

## Estructura de archivos

```
NotificationService/
└── src/main/java/com/clickmunch/NotificationService/
    ├── config/
    │   ├── RabbitMQConfig.java          # Exchange, colas y bindings (incluye cola Telegram)
    │   └── TelegramProperties.java      # Propiedades tipadas: botToken, apiBaseUrl
    ├── dto/
    │   ├── CreateNotificationRequest.java  # Incluye campo opcional telegramChatId
    │   └── NotificationResponse.java
    ├── entity/
    │   ├── Notification.java
    │   └── NotificationType.java
    ├── event/
    │   ├── NotificationEventConsumer.java  # Consumidor de órdenes y reservaciones
    │   ├── OrderEvent.java
    │   ├── ReservationEvent.java
    │   ├── TelegramNotificationEvent.java  # Contrato del mensaje Telegram
    │   └── TelegramWorker.java             # Worker: consume cola y llama a Telegram
    ├── repository/
    │   └── NotificationRepository.java
    ├── service/
    │   ├── NotificationService.java             # Lógica de negocio + SSE
    │   └── TelegramNotificationPublisher.java   # Emisor: publica en RabbitMQ
    └── NotificationServiceApplication.java
```

---

## Configuración

### Variables de entorno requeridas

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `TELEGRAM_BOT_TOKEN` | Token del bot entregado por @BotFather | `1234567890:AAExxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx` |
| `SPRING_RABBITMQ_HOST` | Host de RabbitMQ | `rabbitmq` (Docker) / `localhost` |
| `SPRING_RABBITMQ_PORT` | Puerto AMQP | `5672` |
| `SPRING_RABBITMQ_USERNAME` | Usuario de RabbitMQ | `mike` |
| `SPRING_RABBITMQ_PASSWORD` | Contraseña de RabbitMQ | `secret` |
| `SPRING_DATASOURCE_URL` | URL de PostgreSQL | `jdbc:postgresql://notification-db:5432/notification_db` |

### `application.yaml`

```yaml
telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  api-base-url: ${TELEGRAM_API_BASE_URL:https://api.telegram.org}
```

---

## Cómo obtener las credenciales de Telegram

### 1. Crear el bot y obtener el token

1. Abre Telegram y busca **`@BotFather`**.
2. Envía `/newbot` y sigue las instrucciones (nombre y username del bot).
3. BotFather te entrega el token: `1234567890:AAExxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### 2. Obtener el chat_id del destinatario

1. Busca tu bot en Telegram y presiona **Start**.
2. Abre en el navegador:
   ```
   https://api.telegram.org/bot<TU_TOKEN>/getUpdates
   ```
3. En el JSON de respuesta, busca `message.chat.id` — ese es el `chat_id`.

### 3. Configurar la variable de entorno (Windows)

```powershell
# Persistente en el perfil del usuario:
[System.Environment]::SetEnvironmentVariable("TELEGRAM_BOT_TOKEN", "TU_TOKEN", "User")
```

```bash
# Linux / macOS:
export TELEGRAM_BOT_TOKEN="TU_TOKEN"
```

---

## Cómo disparar una notificación a Telegram

Incluye el campo `telegramChatId` en cualquier petición de creación de notificación:

```bash
curl -X POST http://localhost:8080/notification \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt>" \
  -d '{
    "userId": 1,
    "restaurantId": 1,
    "type": "GENERAL",
    "title": "Pedido listo",
    "message": "Tu pedido #42 está listo para recoger.",
    "telegramChatId": "7184207241"
  }'
```

Flujo resultante:
1. `NotificationService` persiste la notificación en PostgreSQL.
2. Envía el evento por SSE a los clientes web suscritos.
3. Publica un `TelegramNotificationEvent` en RabbitMQ (`notification.send`).
4. `TelegramWorker` consume el mensaje y hace `POST` a la API de Telegram.
5. El usuario recibe el mensaje en su chat de Telegram.

---

## Prueba de conectividad directa

Verificar que el token y el chat_id son válidos sin levantar el stack completo:

```powershell
# PowerShell
$token = $env:TELEGRAM_BOT_TOKEN
$body = @{ chat_id = "TU_CHAT_ID"; text = "Prueba de conectividad" } | ConvertTo-Json
Invoke-RestMethod -Uri "https://api.telegram.org/bot$token/sendMessage" `
  -Method Post -ContentType "application/json" -Body $body
```

```bash
# bash / curl
curl -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage" \
  -H "Content-Type: application/json" \
  -d '{"chat_id": "TU_CHAT_ID", "text": "Prueba de conectividad"}'
```

Respuesta esperada: `{"ok": true, ...}`

---

## Ejecución con Docker

```bash
# Desde backend/
TELEGRAM_BOT_TOKEN=tu_token docker compose up --build notificationservice rabbitmq
```

O agregando `TELEGRAM_BOT_TOKEN` al archivo `.env` del proyecto:

```env
TELEGRAM_BOT_TOKEN=YOUR_BOT_TOKEN_HERE
```

---

## Manejo de errores

| Escenario | Comportamiento |
|-----------|---------------|
| API de Telegram devuelve 4xx/5xx | `TelegramApiException` es capturada y logueada. El mensaje **no** se reencola (error externo, no del broker). |
| RabbitMQ no disponible al publicar | Spring AMQP lanza excepción; el flujo HTTP del NotificationService continúa (SSE y BD no se ven afectados). |
| `telegramChatId` no incluido en el request | El publisher no se invoca. Comportamiento idéntico al original. |
| Token inválido | Telegram devuelve `{"ok": false, "error_code": 401}`. Se loguea y absorbe. |
