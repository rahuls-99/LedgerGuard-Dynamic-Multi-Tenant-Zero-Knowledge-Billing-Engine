
import type { User } from "../types/Auth";

export const getToken = (): string | null => {
  return localStorage.getItem("token");
};

export const getUser = (): User | null => {
  try {
    const savedUser = localStorage.getItem("user");

    return savedUser ? (JSON.parse(savedUser) as User) : null;
  } catch {
    localStorage.removeItem("user");
    return null;
  }
};

export const setSession = (token: string, user: User): void => {
  localStorage.setItem("token", token);
  localStorage.setItem("user", JSON.stringify(user));
};

export const clearSession = (): void => {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
};
