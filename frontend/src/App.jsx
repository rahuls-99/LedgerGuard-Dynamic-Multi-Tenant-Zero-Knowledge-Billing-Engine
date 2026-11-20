
import { lazy, Suspense } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
} from "react-router-dom";

import { AuthProvider } from "./context/AuthContext";
import LoginPage from "./pages/LoginPage";
import ProtectedRoute from "./components/ProtectedRoute";
import ErrorBoundary from "./ErrorBoundary";
import ForbiddenPage from "./pages/ForbiddenPage";

import "./App.css";

const DashboardPage = lazy(() => import("./pages/DashboardPage"));
const LedgerPage = lazy(() => import("./pages/LedgerPage"));
const AnalyticsPage = lazy(() => import("./pages/AnalyticsPage"));

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ErrorBoundary>
          <Suspense
            fallback={
              <p className="loading-message">
                Loading page...
              </p>
            }
          >
            <Routes>
              <Route path="/login" element={<LoginPage />} />

              <Route path="/forbidden" element={<ForbiddenPage />} />

              <Route element={<ProtectedRoute />}>
                <Route
                  path="/dashboard"
                  element={<DashboardPage />}
                />

                <Route
                  path="/ledger"
                  element={<LedgerPage />}
                />

                <Route
                  path="/analytics"
                  element={<AnalyticsPage />}
                />
              </Route>

              <Route
                path="/"
                element={<Navigate to="/dashboard" replace />}
              />

              <Route
                path="*"
                element={<Navigate to="/dashboard" replace />}
              />
            </Routes>
          </Suspense>
        </ErrorBoundary>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
