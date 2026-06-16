# Interoperabilidad en Click & Munch

## 1. Patrón de interoperabilidad aplicado

El proyecto Click & Munch implementa la interoperabilidad principalmente en el backend a través del patrón **Mediator** combinado con un **broker de mensajes** (RabbitMQ).

### 1.1 ¿Dónde se aplica?

- `NotificationService` es el componente que materializa la interoperabilidad.
- `OrderService` y `ReservationService` publican eventos de dominio sin conocer los canales de entrega externos.
- `RabbitMQ` actúa como mediador central entre los emisores de eventos y los consumidores de notificaciones.
- `TelegramWorker` es el único componente que conoce la API de Telegram.
- `AuthService` es consultado por el `NotificationService` para obtener el `telegramChatId` del usuario.

### 1.2 Cómo se logra el desacoplamiento

- Los servicios core (`OrderService`, `ReservationService`) publican eventos genéricos como `order.created`, `order.status.changed`, `reservation.confirmed`, `reservation.cancelled`.
- Estos eventos pasan al exchange `clickmunch.events` de tipo **topic**.
- `NotificationService` consume los eventos y genera notificaciones internas (persistencia + SSE).
- Si el usuario tiene Telegram vinculado, `NotificationService` publica un evento adicional con routing key `notification.send` hacia la cola `notification.telegram.queue`.
- `TelegramWorker` consume esa cola y realiza el `HTTP POST` a `https://api.telegram.org/bot{token}/sendMessage`.

### 1.3 Beneficios del patrón en el proyecto

- **Interoperabilidad sin acoplamiento**: los productores no requieren conocimiento de Telegram ni de otros canales.
- **Extensibilidad**: agregar un nuevo canal de notificación (email, WhatsApp, SMS) solo requiere añadir un nuevo worker o consumidor, sin cambiar los servicios core.
- **Resiliencia**: fallos en el canal externo no afectan directamente el flujo principal de órdenes y reservaciones.
- **Separación de responsabilidades**: `NotificationService` gestiona la lógica de notificaciones y `TelegramWorker` se encarga del adaptador externo.

### 1.4 Elementos clave del diseño

- `clickmunch.events` → Exchange topic de RabbitMQ.
- `notification.order.queue`, `notification.reservation.queue`, `notification.telegram.queue` → colas durables.
- `NotificationEventConsumer` → consulta `AuthService` para obtener el `telegramChatId` y crea la notificación.
- `TelegramNotificationPublisher` → publica mensajes abstractos sin lógica de Telegram.
- `TelegramWorker` → adaptador final que conoce la API externa de Telegram.

---

## 2. Escenario de calidad de interoperabilidad

| Atributo | Descripción |
|----------|-------------|
| **Fuente** | Eventos de dominio emitidos por servicios internos (`OrderService`, `ReservationService`) y usuarios con Telegram vinculado. |
| **Estímulo** | Se genera un evento de orden o reservación y el usuario tiene un `telegramChatId` válido. |
| **Artefacto** | Pipeline de interoperabilidad: RabbitMQ `clickmunch.events`, `NotificationService`, `TelegramWorker`, `AuthService`. |
| **Ambiente** | Operación normal en el entorno de backend con RabbitMQ disponible y `NotificationService` desplegado. |
| **Respuesta** | El evento se consume y se procesa: se guarda la notificación interna, se entrega por SSE y se encola el envío a Telegram. |
| **Medida de respuesta** | - Evento procesado en < 500 ms.
- Publicación en `notification.telegram.queue` en < 200 ms.
- El servicio productor no requiere cambios para soportar el canal Telegram.
- Error en Telegram no bloquea la persistencia o la entrega SSE. |

### 2.1 Justificación del escenario

Este escenario demuestra que la interoperabilidad del sistema permite conectar el core de negocio con un canal externo (Telegram) sin que los servicios de orden y reservación conozcan la implementación externa. El valor de calidad reside en la capacidad de integrar nuevos canales con mínima modificación del sistema y en mantener un comportamiento coherente hacia el usuario aun si el canal externo falla.

### 2.2 Observaciones de calidad

- El sistema puede ampliarse a otros canales de mensajería con un nuevo worker que escuche una cola diferente.
- La interoperabilidad depende de un mediador centralizado (`RabbitMQ`), lo que hace que este componente sea crítico para la disponibilidad del flujo de notificaciones.
- Mantener el contrato del evento y la separación entre productor y consumidor es clave para preservar la calidad de interoperabilidad.
