// Auth via API Gateway (frontend should not call services directly)
const API_GATEWAY_BASE =
  import.meta.env.VITE_API_GATEWAY_URL ??
  import.meta.env.VITE_API_URL ??
  "http://localhost:8080";

// Tipos de respuesta del backend
interface ApiResponse<T> {
  message: string;
  data: T | null;
}

type LoginResponseData = string | { token: string };

interface RegisterRequest {
  name: string;
  email: string;
  username: string;
  password: string;
  role: string;
}

interface LoginRequest {
  username: string;
  password: string;
}

// Storage keys
const TOKEN_KEY = 'auth_token';
const USER_KEY = 'auth_user';

// Guardar sesión
export function saveSession(token: string, user: { username: string; role: string }) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

// Obtener sesión
export function getSession() {
  const token = localStorage.getItem(TOKEN_KEY);
  const userStr = localStorage.getItem(USER_KEY);
  if (!token || !userStr) return null;
  if (token === 'undefined' || token === 'null' || token.startsWith('mock-token-')) {
    clearSession();
    return null;
  }
  try {
    return { token, user: JSON.parse(userStr) };
  } catch {
    clearSession();
    return null;
  }
}

function extractJwtToken(data: LoginResponseData | null): string | null {
  if (!data) return null;
  if (typeof data === 'string') return data;
  if (typeof data.token === 'string') return data.token;
  return null;
}

// Cerrar sesión
export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

// Verificar si hay sesión activa
export function isAuthenticated(): boolean {
  return !!getSession();
}

export function decodeJwtPayload(token: string): any | null {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(base64);
    return JSON.parse(decoded);
  } catch (error) {
    console.error("Error decoding JWT:", error);
    return null;
  }
}

// Obtener el userId del usuario actual desde el token
export function getCurrentUserId(): number | null {
  const session = getSession();
  if (!session) return null;
  const decoded = decodeJwtPayload(session.token);
  return decoded?.userId ?? null;
}

// Obtener el restaurantId del manager actual
export async function getOwnerRestaurantId(): Promise<number | null> {
  const session = getSession();
  if (!session || session.user.role !== 'RESTAURANT_MANAGER') return null;

  const userId = getCurrentUserId();
  if (!userId) return null;

  const apiBase = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
  try {
    const res = await fetch(`${apiBase}/restaurant/owner/${userId}`, {
      headers: { Authorization: `Bearer ${session.token}` },
    });
    if (!res.ok) return null;
    const restaurants = await res.json();
    return restaurants[0]?.id ?? null;
  } catch {
    return null;
  }
}

// Login
export async function login(username: string, password: string): Promise<{ success: boolean; message: string; user?: { username: string; role: string } }> {
  try {
    const res = await fetch(`${API_GATEWAY_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password } as LoginRequest),
    });

    const data: ApiResponse<LoginResponseData> = await res.json();

    const token = extractJwtToken(data.data);

    if (!res.ok || !token) {
      return { success: false, message: data.message || 'Error al iniciar sesión' };
    }

    const decoded = decodeJwtPayload(token);
    const role = decoded?.role ?? 'USER';
    const user = decoded?.sub ?? username;
    saveSession(token, { username: user, role });

    return { success: true, message: data.message, user: { username: user, role } };
  } catch (error) {
    console.error('Login error:', error);
    return { success: false, message: 'Error de conexión con el servidor' };
  }
}

// Register (rol RESTAURANT_MANAGER por defecto para dashboard)
export async function register(
  name: string,
  email: string,
  username: string,
  password: string
): Promise<{ success: boolean; message: string }> {
  try {
    const res = await fetch(`${API_GATEWAY_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name,
        email,
        username,
        password,
        role: 'RESTAURANT_MANAGER', // Rol fijo para dashboard MVP
      } as RegisterRequest),
    });

    const data: ApiResponse<null> = await res.json();

    if (!res.ok) {
      return { success: false, message: data.message || 'Error al registrar' };
    }

    return { success: true, message: data.message || 'Cuenta creada exitosamente' };
  } catch (error) {
    console.error('Register error:', error);
    return { success: false, message: 'Error de conexión con el servidor' };
  }
}

// Logout
export function logout() {
  clearSession();
}


