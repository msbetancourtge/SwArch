export type OrderStatus =
  | 'Preparing'
  | 'Ready'
  | 'Served'
  | 'Delivered'
  | 'Cancelled';

export type OrderChannel = 'Reservation' | 'In-person';

export interface OrderItem {
  id?: number;
  orderId?: number;
  menuItemId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Order {
  id: number;
  customerId: number;
  restaurantId: number;
  status: OrderStatus;
  channel: OrderChannel;
  notes?: string;
  eta?: string;
  total: number;
  items: OrderItem[];
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateOrderRequest {
  customerId: number;
  restaurantId: number;
  channel: OrderChannel;
  notes?: string;
  items: {
    menuItemId: string;
    productName: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
  }[];
  total: number;
}
