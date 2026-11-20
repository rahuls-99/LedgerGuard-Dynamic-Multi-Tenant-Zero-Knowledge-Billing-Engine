
import { useEffect, useState } from "react";
import { Navigate, Outlet } from "react-router-dom";

import { useAuth } from "../context/AuthContext";

interface ProtectedRouteProps {
  requiredRole?: string;
}

export default function ProtectedRoute({
  requiredRole,
}: ProtectedRouteProps) {
  const { token, user, verifyUser, logout } = useAuth();

  const [status, setStatus] = useState<
    "checking" | "allowed" | "denied"
  >("checking");

  useEffect(() => {
    let cancelled = false;

    const checkAuthentication = async () => {
      if (!token) {
        setStatus("denied");
        return;
      }

      setStatus("checking");

      try {
        await verifyUser();

        if (!cancelled) {
          setStatus("allowed");
        }
      } catch {
        if (!cancelled) {
          logout();
          setStatus("denied");
        }
      }
    };

    checkAuthentication();

    return () => {
      cancelled = true;
    };
  }, [token, verifyUser, logout]);

  if (status === "checking") {
    return (
      <p className="loading-message">
        Verifying your account...
      </p>
    );
  }

  if (status === "denied") {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && user?.role !== requiredRole) {
    return <Navigate to="/forbidden" replace />;
  }

  return <Outlet />;
}
