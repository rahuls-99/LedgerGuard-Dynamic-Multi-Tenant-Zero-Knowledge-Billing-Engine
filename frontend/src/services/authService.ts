
import api from "./api";

import type {
  LoginRequest,
  LoginResponse,
  User,
} from "../types/Auth";

// Login API
export const loginRequest = async (
  orgId: string,
  email: string,
  password: string
): Promise<LoginResponse> => {
  const payload: LoginRequest = {
    orgId,
    email,
    password,
  };

  const response = await api.post<LoginResponse>(
    "/real-auth/login",
    payload
  );

  return response.data;
};

// Get logged-in user profile
export const fetchUserProfile = async (): Promise<User> => {
  const response = await api.get<User>("/real-auth/me");

  return response.data;
};
