// Tipos para Productos
export type ProductStatus = 'Borrador' | 'Pendiente' | 'Publicado';

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  status: ProductStatus;
  image: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductDTO {
  name: string;
  description: string;
  price: number;
  category: string;
  image: string;
}

export interface UpdateProductDTO extends Partial<CreateProductDTO> {
  id: string;
  status?: ProductStatus;
}

// Tipos para Órdenes
export type OrderStatus = 'Preparing' | 'Ready' | 'Served' | 'Delivered' | 'Cancelled';
export type OrderChannel = 'Reservation' | 'In-person';

export interface OrderItem {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Order {
  id: string;
  customer: string;
  customerId?: string;
  restaurant: string;
  restaurantId?: string;
  items: OrderItem[];
  eta: string;
  status: OrderStatus;
  total: number;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  channel: OrderChannel;
}

export interface CreateOrderDTO {
  customerId: string;
  restaurantId: string;
  items: Omit<OrderItem, 'subtotal'>[];
  channel: OrderChannel;
  notes?: string;
}

export interface UpdateOrderDTO {
  id: string;
  status?: OrderStatus;
  eta?: string;
  notes?: string;
}

export interface Restaurant {
  id: string;
  name: string;
  image: string;
  rating: number;
  deliveryTime: string;
  price: string;
  badge?: string;
  category?: string;
  city?: string;
  status?: 'Activo' | 'Inactivo';
  latitude: number;
  longitude: number;
}

// Categorías de productos
export const PRODUCT_CATEGORIES = [
  'Bebidas',
  'Ensaladas',
  'Platos fuertes',
  'Postres',
  'Aperitivos',
  'Sopas',
  'Carnes',
  'Pescados',
  'Vegetariano',
  'Vegano',
] as const;

export type ProductCategory = typeof PRODUCT_CATEGORIES[number];

// Tipos para Kitchen / Chef portal (conectados al OrderService real)
export type KitchenOrderStatus = 'PENDING' | 'IN_PREPARATION' | 'READY' | 'DELIVERED' | 'CANCELLED';

// Each KitchenOrderItem represents ONE ordered unit. Two burgers with
// different instructions come as two entries. The UI groups visually by
// (itemName, notes) when rendering the kitchen card.
export interface KitchenOrderItem {
  id: number;
  itemName: string;
  notes: string | null;
}

export interface KitchenOrder {
  id: number;
  restaurantId: number;
  tableNumber: number;
  status: KitchenOrderStatus;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  items: KitchenOrderItem[];
}
