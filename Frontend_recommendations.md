# Frontend Recommendations — Click & Munch

> **Audience**: Frontend developers working on the **Dashboard** (React/Vite) and **Mobile** (Expo/React Native) apps.
> **Goal**: Guide integration with the backend API through the **API Gateway** at `http://localhost:8080`.

---

## 1. API Gateway Endpoint Map

All client requests go through the **API Gateway** on port `8080`. Below is the complete routing map:

| Gateway Path | Backend Service | Internal Path | Auth Required |
|---|---|---|---|
| `/auth/**` | AuthService :8081 | `/api/auth/**` | No |
| `/restaurant/**` | RestaurantService :8082 | `/api/restaurants/**` | Yes (JWT) |
| `/menu/**` | MenuService :8084 | `/api/menus/**` | Yes (JWT) |
| `/order/**` | OrderService :8085 | `/api/orders/**` | Yes (JWT) |
| `/reservation/**` | ReservationService :8086 | `/api/reservations/**` | Yes (JWT) |
| `/checkout/**` | CheckoutService :8089 | `/api/checkout/**` | Yes (JWT) |
| `/rating/**` | RatingService :8088 | `/api/ratings/**` | Yes (JWT) |
| `/notification/**` | NotificationService :8087 | `/api/notifications/**` | Yes (JWT) |

> **Note**: GeoService (:8083) is internal only — called by RestaurantService and ReservationService. Do NOT call it from the frontend.

---

## 2. Authentication Flow

### Login
```
POST /auth/login
Body: { "username": "...", "password": "..." }
Response: { "message": "Login successful", "data": "<JWT_TOKEN>" }
```

### Register (Customer — auto-approved)
```
POST /auth/register
Body: { "name": "...", "email": "...", "username": "...", "password": "...", "role": "CUSTOMER" }
Response: { "message": "User registered successfully", "data": null }
```

### Register (Restaurant Manager — requires admin approval)
```
POST /auth/register
Body: {
  "name": "...", "email": "...", "username": "...", "password": "...",
  "role": "RESTAURANT_MANAGER",
  "phone": "...", "address": "...", "governmentId": "...", "profileImageUrl": "..."
}
Response: { "message": "Registration submitted. Awaiting admin approval.", "data": null }
```
> **Note**: WAITER and CHEF cannot self-register. They must use the staff invite flow (see below).

### Staff Invite Flow (Manager sends invite → Staff completes registration)
```
# Step 1 — Manager creates invite:
POST /auth/staff-invite
Body: { "restaurantId": 5, "email": "waiter@email.com", "role": "WAITER" }
Response: { "message": "Staff invite created...", "data": "<INVITE_TOKEN>" }

# Step 2 — Staff member completes registration with the token:
POST /auth/register/staff
Body: {
  "inviteToken": "<INVITE_TOKEN>",
  "name": "Full Name", "username": "waiter1",
  "password": "secret123",
  "governmentId": "ABC123456",
  "profileImageUrl": "https://...",
  "address": "123 Main St",
  "phone": "+50612345678"
}
Response: { "message": "Staff registration completed. Awaiting approval.", "data": null }
```

**Frontend recommendations:**
- Manager dashboard: show a "Send Staff Invite" form (email + role dropdown: WAITER/CHEF)
- Staff registration: create a separate `/register/staff?token=...` page/screen that accepts the invite token
- Invite tokens expire in 7 days
- Staff remain in PENDING_APPROVAL until approved

### Admin Approval (Global Admin approves managers + staff)
```
GET /auth/users/pending                    # List all users awaiting approval
PUT /auth/users/{userId}/approve           # Approve a user
PUT /auth/users/{userId}/reject            # Reject a user
```

**Frontend recommendations:**
- Admin dashboard: add a "Pending Approvals" section listing users with status = PENDING_APPROVAL
- Show name, email, role, governmentId, address for each pending user
- Approve/Reject buttons per row

### JWT Token Claims
The JWT now includes `userId`, `username`, and `role`:
```json
{ "sub": "username", "userId": 42, "role": "CUSTOMER", "iat": ..., "exp": ... }
```

### JWT Usage
- Store the token securely (Dashboard: httpOnly cookie or memory; Mobile: SecureStore, never AsyncStorage)
- Send on every protected request: `Authorization: Bearer <token>`
- Token expires in **1 hour** — detect 401 responses and redirect to login
- **Login** is blocked for PENDING_APPROVAL and REJECTED users (API returns descriptive error message)

### User Info
```
GET /auth/users/{userId}
Response: { "id", "name", "username", "email", "role", "approvalStatus", "phone", "bio", "profileImageUrl", "address", "governmentId" }
```

### Update Profile (Waiter/Staff profiles)
```
PUT /auth/users/{userId}/profile
Body: { "phone": "...", "bio": "...", "profileImageUrl": "...", "address": "...", "governmentId": "..." }
```

### Get Users by Role
```
GET /auth/users/role/{role}
→ e.g., GET /auth/users/role/WAITER — lists all waiters
```

---

## 3. Customer Flow — Screen-by-Screen

### 3.1 Nearby Places / Browse Screen
```
# Get nearby restaurants (sorted by distance)
GET /restaurant/nearby?lat={lat}&lng={lng}&radius={meters}

# Filter by type (RESTAURANT, BAR, PUB, DISCO, GASTRO_BAR, CAFE)
GET /restaurant/nearby?lat={lat}&lng={lng}&radius=5000&type=PUB
```

**Frontend recommendations:**
- Use browser's `navigator.geolocation` (dashboard) or `expo-location` (mobile) to get user coordinates
- For the **map view**, use a library like **Leaflet** (dashboard) or **react-native-maps** (mobile) with **OpenStreetMap** tiles — no API key needed
- Display restaurant cards with distance + rating summary (see RatingService below)
- Implement infinite scroll / pagination on the restaurant list

### 3.2 Restaurant Detail Screen
```
GET /restaurant/{id}                        # Restaurant info (includes placeType)
GET /restaurant/{id}/hours                  # Operating hours by day-of-week
GET /menu/{restaurantId}                    # Full menu with categories and items
GET /rating/restaurant/{restaurantId}       # All reviews
GET /rating/restaurant/{restaurantId}/summary  # { averageScore, totalRatings }
GET /restaurant/{id}/tables/available?partySize=4  # Available tables
```

**Frontend recommendations:**
- Show menu items grouped by category
- Filter menu items by time availability: items have `availableFrom`/`availableTo` — grey out or hide unavailable items based on current time
- Show preparation time (`preparationMinutes`) next to each item
- Merge rating summary (stars + count) into the restaurant header

### 3.3 Cart & Checkout
Build the cart client-side (Zustand store on mobile, React Context or Zustand on dashboard).

```
POST /checkout/
Body: {
  "customerId": 1,
  "customerName": "John",
  "restaurantId": 5,
  "restaurantName": "La Parrilla",
  "items": [
    { "menuItemId": "abc123", "productName": "Burger", "quantity": 2, "unitPrice": 9.99 }
  ],
  "channel": "InPerson",       # or "Online"
  "reservationId": null,        # link to existing reservation if applicable
  "notes": "No onions",
  "paymentMethod": "CARD"       # CARD, CASH, TRANSFER
}
Response: { "orderId", "reservationId", "total", "status", "message", "paymentMethod" }
```

**Frontend recommendations:**
- Validate cart is not empty before submitting
- Show running total and item count in a floating cart badge
- After checkout success, navigate to order tracking screen

### 3.4 Reservations
```
POST /reservation/
Body: { "customerId", "customerName", "restaurantId", "restaurantName", "reservationDate", "reservationTime", "partySize", "notes" }

GET /reservation/customer/{customerId}       # My reservations
GET /reservation/restaurant/{id}/available-tables?partySize=4  # Available tables for the party
PUT /reservation/{id}/assign-table?tableId=3  # Assign a table

# Suggested available times based on table occupation:
GET /reservation/restaurant/{restaurantId}/suggested-times?date=2025-01-15&partySize=4
Response: {
  "restaurantId": 5,
  "date": "2025-01-15",
  "partySize": 4,
  "availableSlots": [
    { "time": "12:00", "availableTables": 3 },
    { "time": "12:30", "availableTables": 2 },
    { "time": "13:00", "availableTables": 4 }
  ]
}

# Check-in (customer/staff confirms arrival):
PUT /reservation/{id}/check-in
```

**Frontend recommendations:**
- When user selects a date and party size, fetch suggested times and display them as selectable time slots
- Highlight times with more available tables in green
- Show a "Check In" button for confirmed reservations. Note: the system auto-releases reservations 10 minutes after the reserved time if no check-in occurs (status changes to `NoShow` and the table is freed)
- **Reservation statuses**: `Pendiente` → `Confirmada` → `CheckedIn` → `Completada` (or `Cancelada` / `NoShow`)

### 3.5 Order Tracking
```
GET /order/{orderId}                         # Full order with status
GET /order/customer/{customerId}             # Order history
```

**Order statuses flow**: `Pending` → `SentToKitchen` → `Preparing` → `Ready` → `Delivered` → `Cancelled`

### 3.6 Rating After Order
```
POST /rating/restaurant
Body: { "customerId", "customerName", "restaurantId", "restaurantName", "orderId", "score": 1-5, "review": "..." }

POST /rating/waiter
Body: { "customerId", "customerName", "waiterId", "waiterName", "restaurantId", "orderId", "score": 1-5, "comment": "..." }
```

---

## 4. Manager Portal — Screen-by-Screen

### 4.1 Dashboard Overview
Aggregate these calls on the manager dashboard home:
```
GET /restaurant/{restaurantId}
GET /order/restaurant/{restaurantId}          # All orders for this restaurant
GET /reservation/restaurant/{restaurantId}    # All reservations
GET /rating/restaurant/{restaurantId}/summary # Rating overview
```

### 4.2 Table Management
```
POST /restaurant/{restaurantId}/tables        # Create table: { "tableNumber", "seats" }
GET  /restaurant/{restaurantId}/tables        # All tables with status
GET  /restaurant/{restaurantId}/tables/available?partySize=2
PUT  /restaurant/tables/{tableId}/status      # Body: { "status": "AVAILABLE|OCCUPIED|RESERVED" }
DELETE /restaurant/tables/{tableId}
```

**Frontend recommendations:**
- Visual table grid / layout with color-coded statuses (green=available, red=occupied, yellow=reserved)
- Real-time updates via SSE notifications for status changes

### 4.3 Operating Hours
```
POST /restaurant/{restaurantId}/hours         # { "dayOfWeek": 1-7, "openTime": "09:00", "closeTime": "23:00" }
GET  /restaurant/{restaurantId}/hours         # Returns all 7 days
PUT  /restaurant/hours/{hoursId}              # Update specific day
DELETE /restaurant/hours/{hoursId}
```

### 4.4 Staff Management
```
POST /restaurant/{restaurantId}/staff         # { "userId", "role": "WAITER|CHEF" }
GET  /restaurant/{restaurantId}/staff         # All staff
GET  /restaurant/{restaurantId}/staff/WAITER  # Filter by role
PUT  /restaurant/staff/{assignmentId}/deactivate
DELETE /restaurant/staff/{assignmentId}
```

**Staff onboarding flow:**
1. Manager sends invite: `POST /auth/staff-invite` { restaurantId, email, role }
2. Staff completes registration via invite token
3. Admin (or manager) approves the user: `PUT /auth/users/{userId}/approve`
4. Once approved, manager adds staff to restaurant: `POST /restaurant/{restaurantId}/staff` { userId, role }

### 4.5 Restaurant Admin Management (Multi-Admin)
```
POST /restaurant/{restaurantId}/admins        # { "userId" } — add another admin to a restaurant
GET  /restaurant/{restaurantId}/admins        # List all admins for a restaurant
DELETE /restaurant/{restaurantId}/admins/{userId}  # Remove admin
GET  /restaurant/admin/{userId}               # All restaurants where user is an admin
```

**Frontend recommendations:**
- A restaurant can have multiple admins; managers can invite other users as admins
- Show the list of admins in restaurant settings with the ability to add/remove
- The `GET /restaurant/admin/{userId}` endpoint should be used on the manager dashboard home to list all restaurants the user manages

### 4.6 Reservation Management
```
GET /reservation/restaurant/{restaurantId}
GET /reservation/restaurant/{restaurantId}/date/{YYYY-MM-DD}
PUT /reservation/{id}/status                  # { "status": "Confirmada|Cancelada|Completada" }
PUT /reservation/{id}/assign-table?tableId=3
PUT /reservation/{id}/check-in                # Mark customer as arrived
GET /reservation/restaurant/{restaurantId}/suggested-times?date=2025-01-15&partySize=4
```

**Frontend recommendations:**
- Show reservation status badges: Pendiente (yellow), Confirmada (blue), CheckedIn (green), Completada (gray), NoShow (red), Cancelada (strikethrough)
- The system auto-releases confirmed reservations 10 minutes past the reserved time if no check-in. You can poll status to reflect this in the UI
- Allow staff to manually check-in customers at arrival

### 4.6 Menu Management
```
POST /menu/{restaurantId}           # Create full menu
POST /menu/{restaurantId}/items     # Add individual item
PUT  /menu/{restaurantId}/items/{itemId}
DELETE /menu/{restaurantId}/items/{itemId}
```

Menu item fields include `availableFrom`, `availableTo` (time-based availability) and `preparationMinutes`.

### 4.7 Ratings Dashboard
```
GET /rating/restaurant/{restaurantId}               # All restaurant reviews
GET /rating/restaurant/{restaurantId}/summary        # Average + count
GET /rating/waiter/restaurant/{restaurantId}         # Waiter reviews for this restaurant
GET /rating/waiter/{waiterId}/summary                # Per-waiter average
```

---

## 5. Waiter Screen

### 5.1 My Orders
```
GET /order/waiter/{waiterId}          # Orders assigned to me
PUT /order/{orderId}/status           # Update status: { "status": "SentToKitchen|Ready|Delivered" }
```

### 5.2 Waiter Calls (Call Waiter button)
```
# Customer creates a call:
POST /order/waiter-call
Body: { "orderId", "tableId", "restaurantId", "message": "Need help" }

# Waiter fetches pending calls:
GET /order/waiter-calls/restaurant/{restaurantId}/pending

# Waiter acknowledges / resolves:
PUT /order/waiter-calls/{callId}/acknowledge
PUT /order/waiter-calls/{callId}/resolve
```

**Frontend recommendations:**
- Show a badge with pending call count
- Play a sound notification on new calls (use SSE stream for real-time)
- Allow quick resolve from the list

### 5.3 Tips & Comments
```
POST /order/{orderId}/tip
Body: { "tipAmount": 5.00, "waiterComment": "Great service!" }
```

### 5.4 Waiter Profile
```
GET  /auth/users/{userId}
PUT  /auth/users/{userId}/profile
Body: { "phone": "...", "bio": "I've been a waiter for 5 years...", "profileImageUrl": "https://..." }
```

---

## 6. Chef Screen

### 6.1 Kitchen Queue
```
GET /order/restaurant/{restaurantId}  # All orders → filter by status = SentToKitchen or Preparing
PUT /order/{orderId}/status           # Move to "Preparing" or "Ready"
```

**Frontend recommendations:**
- Kanban-style board: columns for **Sent to Kitchen** → **Preparing** → **Ready**
- Sort by `preparationMinutes` (shortest first) or by `createdAt` (oldest first)
- Allow drag-and-drop between columns
- Auto-refresh via polling (every 10s) or use the SSE notification stream

### 6.2 ETA
The `preparationMinutes` field on order items can be used to estimate completion time. Sum the max preparation time across items in the order.

---

## 7. Real-Time Notifications (SSE)

The backend provides a **Server-Sent Events** endpoint for real-time push notifications:

```
GET /notification/stream/{userId}
Content-Type: text/event-stream
```

### Usage (Dashboard — Web)
```typescript
const eventSource = new EventSource(`http://localhost:8080/notification/stream/${userId}`, {
  // Note: EventSource doesn't support Authorization header natively.
  // Use a library like eventsource-polyfill or pass token as query param
  // if your gateway supports it, or use polling as fallback.
});

eventSource.addEventListener('notification', (event) => {
  const notification = JSON.parse(event.data);
  // Show toast, update badge count, etc.
});
```

### Usage (Mobile — React Native)
Use the `react-native-sse` or `eventsource` polyfill package:
```typescript
import EventSource from 'react-native-sse';

const es = new EventSource(`${API_URL}/notification/stream/${userId}`);
es.addEventListener('notification', (event) => {
  const data = JSON.parse(event.data);
  // Update Zustand store, show push notification
});
```

### REST Fallback (Polling)
If SSE is not feasible:
```
GET /notification/user/{userId}/unread       # Unread notifications
GET /notification/user/{userId}/unread-count  # Badge count
PUT /notification/{id}/read                   # Mark as read
PUT /notification/user/{userId}/read-all      # Mark all as read
```

### Notification Types
| Type | Trigger |
|---|---|
| `NEW_ORDER` | New order placed (auto-generated via RabbitMQ) |
| `ORDER_READY` | Chef marks order as Ready (auto-generated via RabbitMQ) |
| `ORDER_STATUS_CHANGED` | Any order status update (auto-generated via RabbitMQ) |
| `RESERVATION_CONFIRMED` | Manager confirms reservation (auto-generated via RabbitMQ) |
| `RESERVATION_CANCELLED` | Reservation cancelled (auto-generated via RabbitMQ) |
| `WAITER_CALL` | Customer calls waiter |
| `GENERAL` | Any other notification |

> **Note:** Notifications for order and reservation events are now created **automatically** via RabbitMQ async messaging. When an order is created or its status changes, OrderService publishes an event to the `clickmunch.events` exchange. Similarly, when a reservation is confirmed or cancelled, ReservationService publishes an event. NotificationService consumes these events and creates notifications automatically — no manual `POST /notification/` call is needed for these cases.

**Frontend recommendation:** When sending notifications from waiter/manager actions, call `POST /notification/` to create and push the notification:
```json
{
  "userId": 42,
  "restaurantId": 5,
  "type": "ORDER_READY",
  "title": "Order Ready",
  "message": "Your order #123 is ready for pickup at Table 4",
  "orderId": 123
}
```

---

## 8. State Management Recommendations

### Dashboard (React/Vite)
- **Auth**: React Context (`AuthContext`) with JWT token in memory
- **API calls**: Service functions in `src/lib/services/` using `fetch` with auth header
- **Server data**: Consider **React Query** / **TanStack Query** for caching and auto-refetch
- **Cart**: Local state or Zustand store (persists through page navigation)

### Mobile (Expo/React Native)
- **Auth tokens**: `expo-secure-store` — never use AsyncStorage for JWT
- **API layer**: Axios instance with JWT interceptor (already exists in `core/api/`)
- **Server state**: React Query with Axios
- **Client state**: Zustand stores per domain (cart, user, notifications)
- **Navigation**: Expo Router file-based routing

---

## 9. Port & Service Quick Reference

| Service | Port | Database | DB Port |
|---|---|---|---|
| API Gateway | 8080 | — | — |
| AuthService | 8081 | PostgreSQL (auth_db) | 5433 |
| RestaurantService | 8082 | PostgreSQL (restaurant_db) | 5434 |
| GeoService | 8083 | PostGIS (geo_db) | 5435 |
| MenuService | 8084 | MongoDB (menu_db) | 27018 |
| OrderService | 8085 | PostgreSQL (order_db) | 5436 |
| ReservationService | 8086 | PostgreSQL (reservation_db) | 5437 |
| NotificationService | 8087 | PostgreSQL (notification_db) | 5439 |
| RatingService | 8088 | PostgreSQL (rating_db) | 5438 |
| CheckoutService | 8089 | — (stateless orchestrator) | — |
| RabbitMQ (AMQP) | 5672 | — | — |
| RabbitMQ (Management UI) | 15672 | — | — |

---

## 10. Common Pitfalls & Tips

1. **Always go through the Gateway** (`:8080`), never call backend services directly from the client.
2. **Handle 401** globally — redirect to login and clear stored token.
3. **Date formats**: Use ISO 8601 (`2025-06-15`, `14:30:00`). Java `LocalDate`/`LocalTime` serialize this way by default.
4. **Menu item IDs** are MongoDB ObjectId strings (e.g., `"683a5f1e..."`), not numbers.
5. **Restaurant/Order/User IDs** are numeric `Long`.
6. **CORS** is configured on the Gateway — no need to handle it in individual services.
7. **Operating hours** use `dayOfWeek` as integer: 1=Monday ... 7=Sunday.
8. **Order channel**: `"InPerson"` for dine-in, `"Online"` for delivery/pickup.
9. **Async notifications**: Order and reservation notifications are generated automatically by the backend via RabbitMQ. You don't need to call `POST /notification/` for these events—just listen to the SSE stream or poll for new notifications.
