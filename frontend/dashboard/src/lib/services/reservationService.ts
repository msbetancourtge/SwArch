import type { Reservation, ReservationStatus } from "@/lib/types";

const API_GATEWAY_BASE = import.meta.env.VITE_API_GATEWAY_BASE ?? "http://localhost:8080";

type CreateReservationData = {
  customerId: number;
  customerName: string;
  restaurantId: number;
  restaurantName: string;
  reservationDate: string;
  reservationTime: string;
  partySize: number;
  notes?: string;
};

const authHeaders = (extra: Record<string, string> = {}) => {
  const token = localStorage.getItem("auth_token");
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extra,
  };
};

const normalizeReservation = (item: any): Reservation => ({
  id: Number(item.id),
  customerId: Number(item.customerId),
  customerName: item.customerName ?? "Cliente",
  restaurantId: Number(item.restaurantId),
  restaurantName: item.restaurantName ?? `Restaurante ${item.restaurantId ?? ""}`.trim(),
  reservationDate: item.reservationDate,
  reservationTime: String(item.reservationTime ?? "").slice(0, 5),
  partySize: Number(item.partySize ?? 1),
  status: item.status as ReservationStatus,
  notes: item.notes ?? null,
  orderId: item.orderId ?? null,
  tableId: item.tableId ?? null,
  checkedInAt: item.checkedInAt ?? null,
  createdAt: item.createdAt ?? null,
});

export const reservationService = {
  async getByRestaurant(restaurantId: number): Promise<Reservation[]> {
    const response = await fetch(`${API_GATEWAY_BASE}/reservation/restaurant/${restaurantId}`, {
      headers: authHeaders(),
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const data = await response.json();
    return Array.isArray(data) ? data.map(normalizeReservation) : [];
  },

  async create(data: CreateReservationData): Promise<Reservation> {
    const response = await fetch(`${API_GATEWAY_BASE}/reservation`, {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(data),
    });
    const text = await response.text();
    if (!response.ok) throw new Error(text || "Error creating reservation");
    return normalizeReservation(JSON.parse(text));
  },

  async assignTable(reservationId: number, tableId: number): Promise<Reservation> {
    const response = await fetch(`${API_GATEWAY_BASE}/reservation/${reservationId}/assign-table?tableId=${tableId}`, {
      method: "PUT",
      headers: authHeaders(),
    });
    const text = await response.text();
    if (!response.ok) throw new Error(text || "Error assigning table");
    return normalizeReservation(JSON.parse(text));
  },

  async updateStatus(reservationId: number, status: ReservationStatus): Promise<Reservation> {
    const response = await fetch(`${API_GATEWAY_BASE}/reservation/${reservationId}/status`, {
      method: "PUT",
      headers: authHeaders(),
      body: JSON.stringify({ status }),
    });
    const text = await response.text();
    if (!response.ok) throw new Error(text || "Error updating reservation status");
    return normalizeReservation(JSON.parse(text));
  },
};
