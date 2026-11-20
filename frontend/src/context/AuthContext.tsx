
import {
  createContext,
  useContext,
  useState,
  useCallback,
  useMemo,
  useEffect,
  type ReactNode,
} from "react";

import { jwtDecode } from "jwt-decode";

import { loginRequest, fetchUserProfile } from "../services/authService";
import type { User } from "../types/Auth";

import {
  getToken,
  getUser,
  setSession,
  clearSession,
} from "../services/storage";

import { isTokenExpired } from "../utils/tokenUtils";

interface JwtPayload {
  exp?: number;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (
    orgId: string,
    email: string,
    password: string
  ) => Promise<void>;
  verifyUser: () => Promise<void>;
  logout: () => void;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(() => getUser());
  const [token, setToken] = useState<string | null>(() => getToken());

  // Login
  const login = useCallback(
    async (
      orgId: string,
      email: string,
      password: string
    ): Promise<void> => {
      const data = await loginRequest(orgId, email, password);

      setSession(data.token, data.user);

      setToken(data.token);
      setUser(data.user);
    },
    []
  );

  // Verify user with backend
  const verifyUser = useCallback(async (): Promise<void> => {
    try {
      const currentToken = getToken();

      if (!currentToken || isTokenExpired(currentToken)) {
        throw new Error("Token expired");
      }

      const profile = await fetchUserProfile();

      setUser(profile);
      setSession(currentToken, profile);
    } catch (error) {
      clearSession();
      setToken(null);
      setUser(null);

      throw error;
    }
  }, []);

  // Logout
  const logout = useCallback(() => {
    clearSession();
    setToken(null);
    setUser(null);
  }, []);

  // Check user role
  const hasRole = useCallback(
    (role: string): boolean => user?.role === role,
    [user]
  );

  // Automatic logout when token expires
  useEffect(() => {
    if (!token) return;

    try {
      const decoded = jwtDecode<JwtPayload>(token);

      if (!decoded.exp || isTokenExpired(token)) {
        logout();
        return;
      }

      const remainingTime = decoded.exp * 1000 - Date.now();

      const timeoutId = window.setTimeout(() => {
        logout();
      }, remainingTime);

      return () => window.clearTimeout(timeoutId);
    } catch {
      logout();
    }
  }, [token, logout]);

  // Sync logout across browser tabs
  useEffect(() => {
    const handleStorageChange = (event: StorageEvent) => {
      if (event.key === "token" && !event.newValue) {
        setToken(null);
        setUser(null);
      }
    };

    window.addEventListener("storage", handleStorageChange);

    return () => {
      window.removeEventListener("storage", handleStorageChange);
    };
  }, []);

  const value = useMemo(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token && user),
      login,
      verifyUser,
      logout,
      hasRole,
    }),
    [user, token, login, verifyUser, logout, hasRole]
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}
