import type { KitchenOrder } from '@/lib/types';

const ORDER_API_BASE = import.meta.env.VITE_ORDER_API_BASE ?? "http://localhost:8085";

interface ApiResponse<T> {
  message: string;
  data: T | null;
}

export const kitchenOrderService = {
  async getKitchenOrders(restaurantId: number): Promise<KitchenOrder[]> {
    const res = await fetch(`${ORDER_API_BASE}/api/orders/kitchen/${restaurantId}`);
    const json: ApiResponse<KitchenOrder[]> = await res.json();
    return json.data ?? [];
  },

  async updateOrderStatus(orderId: number, status: string): Promise<KitchenOrder | null> {
    const res = await fetch(`${ORDER_API_BASE}/api/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    });
    const json: ApiResponse<KitchenOrder> = await res.json();
    if (!res.ok || !json.data) {
      throw new Error(json.message || 'Error updating order status');
    }
    return json.data;
  },

  async getOrder(orderId: number): Promise<KitchenOrder | null> {
    const res = await fetch(`${ORDER_API_BASE}/api/orders/${orderId}`);
    const json: ApiResponse<KitchenOrder> = await res.json();
    return json.data ?? null;
  },
};
