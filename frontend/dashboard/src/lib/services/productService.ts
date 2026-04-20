import { getSession } from '@/lib/auth';
import type { 
  Product, 
  CreateProductDTO, 
  UpdateProductDTO,   
  BackendMenuItem,
  BackendMenuCategory
} from '@/lib/types';


const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
// Según tu Gateway, la ruta de entrada es /menu
const BASE_PATH = `${API_BASE}/menu`; 

const FRONTEND_TO_BACKEND_CATEGORY: Record<string, string> = {
  'Bebidas': 'BEBIDA',
  'Ensaladas': 'ENSALADA',
  'Platos fuertes': 'PLATO',
  'Postres': 'POSTRE',
  'Aperitivos': 'ENTRADA',
  'Sopas': 'ENTRADA',
  'Carnes': 'PLATO',
  'Pescados': 'PLATO',
  'Vegetariano': 'PLATO',
  'Vegano': 'ADICIONAL',
};

const BACKEND_TO_FRONTEND_CATEGORY: Record<string, string> = {
  'BEBIDA': 'Bebidas',
  'ENSALADA': 'Ensaladas',
  'PLATO': 'Platos fuertes',
  'POSTRE': 'Postres',
  'ENTRADA': 'Aperitivos',
  'ADICIONAL': 'Vegano',
};

const categoryCache = new Map<string, BackendMenuCategory>();

function authHeaders(): Record<string, string> {
  const session = getSession();
  return {
    'Content-Type': 'application/json',
    ...(session ? { Authorization: `Bearer ${session.token}` } : {}),
  };
}

function toProduct(item: BackendMenuItem, categoryName: string): Product {
  return {
    id: item.id,
    name: item.name,
    description: item.description,
    price: Number(item.price),
    category: categoryName,
    status: 'Publicado',
    image: item.imageUrl || '',
    createdAt: '', 
    updatedAt: '',
    availableFrom: item.availableFrom,
    availableTo: item.availableTo,    
    preparationMinutes: item.preparationMinutes
  };
}

export const productService = {
  /**
   * GET /menu/restaurants/{id}/items 
   * (El Gateway lo convierte en /api/menus/restaurants/{id}/items)
   */
  async getAll(restaurantId: number): Promise<Product[]> {
    const res = await fetch(`${BASE_PATH}/restaurants/${restaurantId}/items`, {
      headers: authHeaders(),
    });
    
    if (!res.ok) return [];
    const items: BackendMenuItem[] = await res.json();

    const categoryIds = [...new Set(items.map(i => i.categoryId))];
    await Promise.all(categoryIds.map(async (id) => {
      if (!categoryCache.has(id)) {
        // GET /menu/categories/{id}
        const cRes = await fetch(`${BASE_PATH}/categories/${id}`, { headers: authHeaders() });
        if (cRes.ok) categoryCache.set(id, await cRes.json());
      }
    }));

    return items.map(item => {
      const cat = categoryCache.get(item.categoryId);
      const name = cat ? (BACKEND_TO_FRONTEND_CATEGORY[cat.category] || cat.category) : 'Sin categoría';
      return toProduct(item, name);
    });
  },

  /**
   * POST /menu/categories/...
   */
  async create(data: CreateProductDTO, restaurantId: number): Promise<Product> {
    const backendEnum = FRONTEND_TO_BACKEND_CATEGORY[data.category] || 'ADICIONAL';
    
    let categoryId = '';
    const cachedCat = Array.from(categoryCache.values())
      .find(c => c.restaurantId === restaurantId && c.category === backendEnum);

    if (cachedCat) {
      categoryId = cachedCat.id;
    } else {
      const catRes = await fetch(`${BASE_PATH}/categories`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({ restaurantId, category: backendEnum })
      });
      if (!catRes.ok) throw new Error('Error al crear categoría');
      const newCat: BackendMenuCategory = await catRes.json();
      categoryCache.set(newCat.id, newCat);
      categoryId = newCat.id;
    }
    const payload = {
      name: data.name,
      description: data.description,
      price: data.price,
      imageUrl: data.image,
      availableFrom: data.availableFrom,
      availableTo: data.availableTo,
      isAvailable: true,
      preparationMinutes: data.preparationMinutes ? parseInt(data.preparationMinutes) : undefined
    };
    console.log('Payload para creación:', payload);
    const res = await fetch(`${BASE_PATH}/categories/${categoryId}/items`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({
        name: data.name,
        description: data.description,
        price: data.price,
        imageUrl: data.image,
        availableFrom: data.availableFrom,
        availableTo: data.availableTo,
        preparationMinutes: data.preparationMinutes
        
      }),
    });

    if (!res.ok) throw new Error('Error al crear el producto');
    const newItem: BackendMenuItem = await res.json();
    return toProduct(newItem, data.category);
  },

  /**
   * PUT /menu/items/{id}
   */
  async update(data: UpdateProductDTO): Promise<Product | null> {
    const res = await fetch(`${BASE_PATH}/items/${data.id}`, {
      method: 'PUT',
      headers: authHeaders(),
      body: JSON.stringify({
        name: data.name,
        description: data.description,
        price: data.price,
        imageUrl: data.image,
        availableFrom: data.availableFrom,
        availableTo: data.availableTo,
        preparationMinutes: data.preparationMinutes
      }),
    });

    if (!res.ok) return null;
    const updatedItem: BackendMenuItem = await res.json();
    
    const cat = categoryCache.get(updatedItem.categoryId);
    const catName = cat ? (BACKEND_TO_FRONTEND_CATEGORY[cat.category] || cat.category) : 'Sin categoría';
    
    return toProduct(updatedItem, catName);
  },

  /**
   * DELETE /menu/items/{id}
   */
  async delete(id: string): Promise<boolean> {
    const res = await fetch(`${BASE_PATH}/items/${id}`, {
      method: 'DELETE',
      headers: authHeaders(),
    });
    return res.ok;
  },

  async getById(id: string): Promise<Product | null> {
    const res = await fetch(`${BASE_PATH}/items/${id}`, { headers: authHeaders() });
    if (!res.ok) return null;
    const item: BackendMenuItem = await res.json();
    
    const catRes = await fetch(`${BASE_PATH}/categories/${item.categoryId}`, { headers: authHeaders() });
    const cat: BackendMenuCategory = await catRes.json();
    
    return toProduct(item, BACKEND_TO_FRONTEND_CATEGORY[cat.category] || cat.category);
  }
};