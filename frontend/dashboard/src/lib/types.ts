// ... (Todo tu código anterior se mantiene igual arriba)

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
  // 🔥 Campos añadidos/asegurados
  availableFrom: string;
  availableTo: string;
  
  preparationMinutes?: string; 
}

export interface CreateProductDTO {
  name: string;
  description: string;
  price: number;
  category: string;
  image: string;
  // 🔥 Campos añadidos/asegurados
  availableFrom: string;
  availableTo: string;
  
  preparationMinutes?: string;
}

export interface UpdateProductDTO extends Partial<CreateProductDTO> {
  id: string;
  status?: ProductStatus;
}

// Tipos para Órdenes

export type OrderChannel = 'Reservation' | 'In-person';




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

export interface Table {
  id: number;
  restaurantId: number;
  tableNumber: string;
  seats: number;
  status: 'AVAILABLE' | 'OCCUPIED' | 'RESERVED' | 'CLEANING';
}

export interface TableApiResponse {
  id: number;
  restaurantId: number;
  tableNumber: string;
  seats: number;
  status: string; 
}
export interface OperatingHours {
  id?: number;
  dayOfWeek: 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
  openTime: string; // Formato "HH:mm"
  closeTime: string; // Formato "HH:mm"
  isOpen: boolean;
}

// --- LO QUE HACÍA FALTA PARA EL SERVICIO DE MENÚ ---

/**
 * Representa la estructura exacta que devuelve el MenuService (Spring Boot)
 * para un ítem individual.
 */
export interface BackendMenuItem {
  id: string;
  categoryId: string;
  name: string;
  description: string;
  price: number;
  imageUrl: string;
  // 🔥 Campos añadidos para coincidir con el nuevo payload
  availableFrom: string;
  availableTo: string;
  isAvailable: boolean;
  preparationMinutes?: string;
}

/**
 * Representa la estructura de categoría que maneja el microservicio.
 * El campo 'category' es el Enum de Java.
 */
export interface BackendMenuCategory {
  id: string;
  restaurantId: number;
  category: "ENTRADA" | "PLATO" | "POSTRE" | "BEBIDA" | "ENSALADA" | "ADICIONAL";
}

/**
 * Respuesta del endpoint /api/menus/restaurants/{restaurantId}
 * que devuelve el menú completo organizado.
 */
export interface MenuRestaurantResponse {
  restaurantId: number;
  categories: {
    id: string;
    name: string; // El nombre de la categoría (ej: "Bebidas")
    restaurantId: number;
    items: BackendMenuItem[];
  }[];
}

/**
 * Estructura para crear o actualizar categorías
 */
export interface MenuCategoryRequest {
  restaurantId: number;
  category: string;
}

/**
 * Estructura para crear o actualizar items según el controlador
 */
export interface MenuItemRequest {
  name: string;
  description: string;
  price: number;
  imageUrl?: string;
  // 🔥 Campos añadidos para el fetch del service
  availableFrom: string;
  availableTo: string;
  isAvailable: boolean;
  preparationMinutes?: number;
}
export interface RatingSummary {
  entityId: number;
  averageScore: number;
  totalRatings: number;
}
export interface IndividualRating {
  id: number;
  customerId: number;
  customerName: string;
  restaurantId: number;
  restaurantName: string;
  orderId: number;
  score: number;      // Calificación (1-5)
  review: string;     // Comentario del cliente
  createdAt: string;  // Formato ISO (2026-04-20T...)
}
export type OrderStatus =
  | "Pending"
  | "SentToKitchen"
  | "Preparing"
  | "Ready"
  | "Served"
  | "Delivered"
  | "Cancelled";

export type OrderItem = {
  id: number;
  menuItemId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
};

export type Order = {
  id: number;
  customerId: number;
  customerName: string;
  restaurantId: number;
  restaurantName: string;

  status: OrderStatus;
  channel: string;
  notes: string;
  eta: string;

  total: number;
  tableId: number | null;
  waiterId: number | null;

  tipAmount: number | null;
  waiterComment: string | null;

  preparationMinutes: number;

  items: OrderItem[];

  createdAt: string;
  updatedAt: string;
};