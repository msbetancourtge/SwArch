import type { Restaurant, RestaurantStatus } from '@/lib/types';

// TODO: Conectar con backend vía API Gateway (puerto 8080)
// Endpoint sugerido: GET /restaurant/owner/{ownerId}
// Nota: para usar ese endpoint se requiere ownerId real del usuario autenticado.

const RESTAURANT_API_BASE = import.meta.env.VITE_API_GATEWAY_BASE ?? 'http://localhost:8080';
const USE_MOCK_RESTAURANTS = (import.meta.env.VITE_USE_MOCK_RESTAURANTS ?? 'true') === 'true';

let mockRestaurants: Restaurant[] = [
  {
    id: 'RST-3001',
    name: 'Urban Bistro',
    category: 'Bistró',
    city: 'Bogotá',
    status: 'Activo',
    rating: 4.7,
    createdAt: '2025-10-20',
  },
  {
    id: 'RST-3002',
    name: 'Café Andino',
    category: 'Cafetería',
    city: 'Medellín',
    status: 'Activo',
    rating: 4.5,
    createdAt: '2025-09-10',
  },
  {
    id: 'RST-3003',
    name: 'Chef House',
    category: 'Internacional',
    city: 'Bogotá',
    status: 'Inactivo',
    rating: 3.9,
    createdAt: '2025-08-15',
  },
];

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const normalizeApiRestaurant = (item: any): Restaurant => ({
  id: item.id != null ? String(item.id) : '',
  name: item.name ?? 'Sin nombre',
  category: item.category ?? item.type ?? 'General',
  city: item.city ?? item.location ?? 'Sin ciudad',
  status: (item.status === 'INACTIVE' ? 'Inactivo' : 'Activo') as RestaurantStatus,
  rating: typeof item.rating === 'number' ? item.rating : undefined,
  createdAt: item.createdAt ?? item.submittedAt ?? new Date().toISOString(),
});

export const restaurantService = {
  async getAll(): Promise<Restaurant[]> {
    if (USE_MOCK_RESTAURANTS) {
      await delay(250);
      return [...mockRestaurants];
    }

    try {
      // Mientras no exista ownerId en sesión del dashboard, se deja listo para integración.
      const res = await fetch(`${RESTAURANT_API_BASE}/restaurant`, {
        method: 'GET',
      });

      if (!res.ok) throw new Error(`HTTP ${res.status}`);

      const data = await res.json();
      const rows = Array.isArray(data) ? data : Array.isArray(data?.data) ? data.data : [];
      return rows.map(normalizeApiRestaurant);
    } catch (error) {
      console.error('Error loading restaurants from API, using mock data:', error);
      return [...mockRestaurants];
    }
  },
};

