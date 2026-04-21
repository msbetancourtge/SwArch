// Auth Service - Conexión con backend AuthService

const AUTH_API_BASE = import.meta.env.VITE_AUTH_API_BASE ?? "http://localhost:8081";

// Tipos
interface ApiResponse<T> {
  message: string;
  data: T | null;
}

interface LoginResponseData {
  token: string;
}

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

// Storage
const TOKEN_KEY = 'auth_token';
const USER_KEY = 'auth_user';

// =======================
// 🔐 SESSION
// =======================

export function saveSession(token: string, user: { username: string; role: string }) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function getSession() {
  const token = localStorage.getItem(TOKEN_KEY);
  const userStr = localStorage.getItem(USER_KEY);
  if (!token || !userStr) return null;

  try {
    return { token, user: JSON.parse(userStr) };
  } catch {
    return null;
  }
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function isAuthenticated(): boolean {
  return !!getSession();
}

// =======================
// 🔍 JWT HELPERS
// =======================

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

export function getCurrentUserRole(): string | null {
  const session = getSession();
  if (!session) return null;

  const decoded = decodeJwtPayload(session.token);
  return decoded?.role ?? null;
}

export function getCurrentUserId(): number | null {
  const session = getSession();
  if (!session) return null;

  const decoded = decodeJwtPayload(session.token);
  return decoded?.userId ?? null;
}

export function getCurrentUsername(): string | null {
  const session = getSession();
  if (!session) return null;

  const decoded = decodeJwtPayload(session.token);
  return decoded?.sub ?? null;
}

export function getCurrentUserName(): string | null {
  const session = getSession();
  if (!session) return null;

  const decoded = decodeJwtPayload(session.token);
  return decoded?.name ?? null;
}

export function getCurrentUserInitials(): string {
  const name = getCurrentUserName();
  if (!name) return 'U';

  const parts = name.trim().split(' ').filter(Boolean);

  if (parts.length > 1) {
    return parts.map(p => p[0].toUpperCase()).join('').slice(0, 2);
  }

  return parts[0][0].toUpperCase();
}

// =======================
// 🏠 RESTAURANTE
// =======================

export async function getOwnerRestaurantId(): Promise<number | null> {
  const session = getSession();
  if (!session || session.user.role !== 'RESTAURANT_MANAGER') return null;

  const userId = getCurrentUserId();
  if (!userId) return null;

  const apiBase = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

  try {
    const res = await fetch(`${apiBase}/restaurant/admin/${userId}`, {
      headers: { Authorization: `Bearer ${session.token}` },
    });

    if (!res.ok) return null;

    const restaurants = await res.json();
    return restaurants[0]?.id ?? null;
  } catch {
    return null;
  }
}

// =======================
// 🔑 LOGIN
// =======================

export async function login(
  username: string,
  password: string
): Promise<{ success: boolean; message: string; user?: { username: string; role: string }, token?: string }> {

  try {
    const res = await fetch(`${AUTH_API_BASE}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password } as LoginRequest),
    });

    const data: ApiResponse<LoginResponseData> = await res.json();

    if (!res.ok || !data.data) {
      return { success: false, message: data.message || 'Error al iniciar sesión' };
    }

    const { token } = data.data;

    const decoded = decodeJwtPayload(token);
    const role = decoded?.role ?? 'USER';
    const user = decoded?.sub ?? username;

    saveSession(token, { username: user, role });

    return {
      success: true,
      message: data.message,
      user: { username: user, role },
      token
    };

  } catch (error) {
    console.error('Login error:', error);

    // fallback dev
    await new Promise(r => setTimeout(r, 500));

    if (username === 'admin' && password === 'admin123') {
      const mockUser = { username: 'admin', role: 'ADMIN' };
      const mockToken = 'mock-token-' + Date.now();

      saveSession(mockToken, mockUser);

      return {
        success: true,
        message: 'Login dev',
        user: mockUser,
        token: mockToken
      };
    }

    return { success: false, message: 'Credenciales inválidas' };
  }
}

// =======================
// 📝 REGISTER
// =======================

export async function register(
  name: string,
  email: string,
  username: string,
  password: string
): Promise<{ success: boolean; message: string }> {

  try {
    const res = await fetch(`${AUTH_API_BASE}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, username, password } as RegisterRequest),
    });

    const data: ApiResponse<null> = await res.json();

    if (!res.ok) {
      return { success: false, message: data.message || 'Error al registrar' };
    }

    return { success: true, message: data.message || 'Cuenta creada' };

  } catch (error) {
    console.error('Register error:', error);
    return { success: false, message: 'Error de conexión' };
  }
}

// =======================
// 🚪 LOGOUT
// =======================

export function logout() {
  clearSession();
}