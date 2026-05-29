export type OrderStatus =
  | 'Pending'
  | 'SentToKitchen'
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
  customerName: string;
  restaurantId: number;
  restaurantName: string;
  status: OrderStatus;
  channel: OrderChannel;
  notes?: string;
  eta?: string;
  total: number;
  tableId?: number;
  waiterId?: number;
  tipAmount?: number;
  waiterComment?: string;
  preparationMinutes?: number;
  items: OrderItem[];
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateOrderRequest {
  customerId: number;
  customerName: string;
  restaurantId: number;
  restaurantName: string;
  channel: OrderChannel;
  notes?: string;
  items: {
    menuItemId: string;
    productName: string;
    quantity: number;
    unitPrice: number;
  }[];
}
