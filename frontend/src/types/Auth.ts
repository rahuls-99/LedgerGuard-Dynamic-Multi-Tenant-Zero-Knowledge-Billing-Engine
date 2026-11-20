
export interface User {
  userId: string;
  orgId: string;
  role: string;
}

export interface LoginRequest {
  orgId: string;
  email: string;
  password: string;
}

export interface LoginResponse {
  success: boolean;
  token: string;
  user: User;
}

export interface ApiErrorResponse {
  success: false;
  message: string;
}
