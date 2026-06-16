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

