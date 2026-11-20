
import axios from "axios";
import { getToken, clearSession } from "./storage";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Attach token to every API request
api.interceptors.request.use(
  (config) => {
    const token = getToken();

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Handle API response errors
api.interceptors.response.use(
  (response) => response,

  (error) => {
    const status = error.response?.status;
    const requestUrl = error.config?.url || "";

    // Don't redirect when login itself fails
    const isLoginRequest = requestUrl.includes("/real-auth/login");

    if (status === 401 && !isLoginRequest) {
      clearSession();

      if (window.location.pathname !== "/login") {
        window.location.replace("/login");
      }
    }

    if (status === 403) {
      console.error("Access Denied: Cross-Tenant Action Not Allowed");
    }

    if (!error.response) {
      console.error("Cannot reach server. Please check your connection.");
    }

    return Promise.reject(error);
  }
);

export default api;
