import type { Order, OrderStatus } from "@/lib/types";

const API = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

// 🔐 Si usas auth, puedes adaptar esto
const getHeaders = () => {
  const token = localStorage.getItem("auth_token");
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
};

// 🎯 Estados activos
const ACTIVE_STATUS: OrderStatus[] = [
  "Pending",
  "SentToKitchen",
  "Preparing",
  "Ready",
  "Served",
];

// ⏱️ Util: tiempo en minutos desde creación
export const getMinutes = (date: string) => {
  const created = new Date(date);
  const now = new Date();
  return Math.floor((now.getTime() - created.getTime()) / 60000);
};

// 🎨 Util: color por tiempo
export const getTimeColor = (min: number) => {
  if (min < 10) return "text-green-600";
  if (min < 20) return "text-yellow-500";
  return "text-red-600";
};

// 🔄 Flujo de estados
export const getNextStatus = (status: OrderStatus): OrderStatus | null => {
  switch (status) {
    case "Pending":
      return "SentToKitchen";
    case "SentToKitchen":
      return "Preparing";
    case "Preparing":
      return "Ready";
    case "Ready":
      return "Served";
    case "Served":
      return "Delivered";
    default:
      return null;
  }
};

// 🍽️ Validar si una mesa está libre
export const isTableFree = (tableId: number, orders: Order[]) => {
  return !orders.some(
    (o) =>
      o.tableId === tableId &&
      ACTIVE_STATUS.includes(o.status)
  );
};

export const orderService = {
  
  // 📋 Obtener todas las órdenes del restaurante
  async getByRestaurant(restaurantId: number): Promise<Order[]> {
    const res = await fetch(`${API}/order/restaurant/${restaurantId}`, {
      headers: getHeaders(),
    });

    if (!res.ok) throw new Error("Error cargando órdenes");
    return res.json();
  },

  // 🔥 Obtener solo órdenes activas (helper frontend)
  async getActive(restaurantId: number): Promise<Order[]> {
    const data = await this.getByRestaurant(restaurantId);
    return data.filter((o) => ACTIVE_STATUS.includes(o.status));
  },

  // 🔄 Cambiar estado de la orden
  async updateStatus(orderId: number, status: OrderStatus): Promise<void> {
    const res = await fetch(`${API}/order/${orderId}/status`, {
      method: "PUT",
      headers: getHeaders(),
      body: JSON.stringify({ status }),
    });

    if (!res.ok) {
      const err = await res.text();
      throw new Error(err || "Error actualizando estado");
    }
  },

  // 🍽️ Asignar mesa (con validación previa opcional)
  async assignTable(orderId: number, tableId: number): Promise<void> {
    const res = await fetch(`${API}/order/${orderId}/assign-table?tableId=${tableId}`, {
      method: "PUT",
      headers: getHeaders(),
      
    });

    if (!res.ok) {
      const err = await res.text();
      throw new Error(err || "Error asignando mesa");
    }
  },

  // 🔍 Obtener una orden por ID
  async getById(orderId: number): Promise<Order | null> {
    const res = await fetch(`${API}/order/${orderId}`, {
      headers: getHeaders(),
    });

    if (!res.ok) return null;
    return res.json();
  },

  // 🧪 Debug helper (opcional)
  logOrder(order: Order) {
    console.log("📦 ORDER:", order);
  },
  async getHistory(restaurantId: number): Promise<Order[]> {
  const res = await fetch(`${API}/order/restaurant/${restaurantId}`, {
    headers: getHeaders(),
  });

  if (!res.ok) throw new Error("Error cargando historial");

  const data: Order[] = await res.json();

  return data.filter(
    (o) => o.status === "Delivered" || o.status === "Cancelled"
  );
}
};