# Para el punto de C&C en: descripcion de patrones utilizados
## Load Balance

Se implementó el patrón Load Balancer para distribuir las solicitudes entrantes entre múltiples instancias del servicio Menu, evitando la saturación de un solo nodo y mejorando la escalabilidad y disponibilidad del sistema.

### Implementación

- Se desplegaron 3 instancias del servicio menu-service.

- Se configuró NGINX como balanceador de carga (reverse proxy) con estrategia Round Robin.

- El API Gateway fue modificado para enviar todo el tráfico del menú al balanceador, no directamente a una instancia.

### Resultados clave

- Testing usando la herramienta JMeter

- Throughput estable hasta ≈500 req/s (con 500 usuarios concurrentes).

- Tiempo de respuesta promedio < 10 ms hasta los 500 usuarios.

- Punto de saturación (knee) identificado por debajo de 2000 usuarios concurrentes, donde la latencia se disparó a ~5000 ms y la tasa de error alcanzó el 6%.

### Tradeoffs

- Ventajas: Mayor escalabilidad horizontal, disponibilidad y distribución equitativa de carga.

- Desventajas: Complejidad adicional en la infraestructura (gestión de NGINX, múltiples contenedores) y necesidad de configurar health checks para evitar enrutar a instancias fallidas.

### Throttling

Se implementó el patrón Throttling (limitación de tasa) para controlar el flujo de peticiones hacia el servicio Menu, protegiendo el sistema contra picos de tráfico y abusos (por ejemplo, ataques de denegación de servicio o clientes maliciosos).

### Implementación
- Se configuró NGINX con dos niveles de limitación:

    - Límite global: 1000 r/s sobre una zona compartida global_limit, aplicable a todo el tráfico agregado.

    - Límite por IP: 15 r/s por dirección cliente, con zona menu_limit de 10 MB para almacenar estados.

- Se habilitó burst + nodelay en ambos niveles (burst=100 para el global, burst=10 para IP) para permitir ráfagas cortas sin retrasar la respuesta inmediata.

- El algoritmo subyacente es el Leaky Bucket (cubo con fugas), que suaviza la tasa de salida.

### Resultados clave
- Por cliente (IP): no puede superar 25 peticiones por segundo (tasa base de 15 r/s + burst de 10 con nodelay).

- Sistema global: no excederá 1100 peticiones por segundo (tasa base de 1000 r/s + burst de 100 con nodelay).

- Estos valores fueron seleccionados por el equipo con base en los resultados de las pruebas de carga realizadas en la implementación del Load Balancer, que mostraron que el sistema mantiene estabilidad y tiempos de respuesta aceptables por debajo de <2000 usuarios concurrentes.

### Tradeoffs
- Ventajas: Protege el backend contra sobrecarga; mejora la disponibilidad para clientes legítimos; mitiga ataques por denegación de servicio; el uso de nodelay evita latencias excesivas durante ráfagas.

- Desventajas: Puede rechazar peticiones legítimas durante picos reales de tráfico; la memoria compartida (10 MB) tiene un límite de entradas; requiere ajuste fino de tasas (15r/s por IP) según los patrones reales de uso.



# Siguiente seccion Quality Attributes
## Performance and Scalability

### Escenarios de Performance

1. Load Balance

| Attribute          | Description |
|--------------------|-------------|
| **Source**         | Clients/Users |
| **Stimulus**       | The users generate 100 menu requests per second (100 req/s) |
| **Artifact**       | Load Balancer and Menu Service instances |
| **Environment**    | Normal operation in a local development environment |
| **Response**       | The load balancer distributes incoming requests evenly across the available Menu Service instances |
| **Response Measure** | - Response time < 300 ms  <br> - CPU utilization < 80%  <br> - Error rate < 1% |

2. Throttling

Se usan 2 escenarios, uno para limitar requests por usuario y otro, para requests globales.


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

### Patrones y Tacticas Aplicadas

**Load Balancer Pattern**

La táctica principal implementada es **Mantener Múltiples Copias de Computación** y **Incrementar Recursos Adicionales de Computación**, pues se distribuyen requests entrantes sobre multiples instancias del servicio de menu.

**Throttling Pattern**

La táctica principal implementada es **Limitar el caudal de peticiones** y **Proteger el sistema contra sobrecarga**, pues se restringe la tasa de solicitudes entrantes por cliente y a nivel global, descartando el exceso inmediatamente en la capa de proxy (NGINX) para evitar que el servicio de menú se sature.

### Analisis del testing y resultados

Se realiza un test en JMeter una vez implementado el Load Balancer

La prueba de carga no arrojó una curva de rendimiento con un punto de inflexión (knee) claramente definido, debido al número limitado de puntos de medición. 

No obstante, se estima que el knee ocurre por debajo de 2000 usuarios concurrentes. Hasta ese umbral, el sistema mantuvo tiempos de respuesta estables y una capacidad de procesamiento (throughput) aceptable. 

En ese punto, los tiempos de respuesta se dispararon y la tasa de error alcanzó el 6 %, evidenciando saturación de recursos (colas de peticiones, mayor latencia y degradación general).

Con esta información se decida implementar el patrón Throttler definiendo un límite de 1100 req/s globales y un límite por usuario (IP) de 25 req/s (por criterio del grupo)