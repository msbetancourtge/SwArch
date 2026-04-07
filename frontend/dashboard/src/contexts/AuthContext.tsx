// AuthContext.tsx - Context para manejar el estado de autenticación global
import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import * as auth from '@/lib/auth';

interface User {
  username: string;
  role: string;
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  restaurantId: number | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [restaurantId, setRestaurantId] = useState<number | null>(null);

  // Verificar autenticación al cargar
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const session = auth.getSession();
        if (session) {
          setUser(session.user);
          if (session.user.role === 'RESTAURANT_MANAGER') {
            const id = await auth.getOwnerRestaurantId();
            setRestaurantId(id);
          }
        }
      } catch (error) {
        console.error('Error verificando autenticación:', error);
        auth.clearSession();
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };

    checkAuth();
  }, []);

  const login = async (username: string, password: string) => {
    const result = await auth.login(username, password);
    if (result.success && result.user) {
      setUser(result.user);
      if (result.user.role === 'RESTAURANT_MANAGER') {
        const id = await auth.getOwnerRestaurantId();
        setRestaurantId(id);
      }
    } else {
      throw new Error(result.message);
    }
  };

  const logout = () => {
    auth.clearSession();
    setUser(null);
    setRestaurantId(null);
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: user !== null,
    isLoading,
    restaurantId,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// Hook personalizado para usar el contexto
export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth debe usarse dentro de un AuthProvider');
  }
  return context;
}
