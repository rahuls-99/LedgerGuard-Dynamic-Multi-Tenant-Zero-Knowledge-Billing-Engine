
import { useEffect, useState } from "react";
import api from "../services/api";
import { useAuth } from "../context/AuthContext";

export default function LedgerPage() {
  const { user } = useAuth();

  const [invoices, setInvoices] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!user?.orgId) {
      setLoading(false);
      setError("Organization information not found.");
      return;
    }

    let cancelled = false;

    const fetchInvoices = async () => {
      try {
        setLoading(true);
        setError("");

        const response = await api.get("/invoices", {
          headers: {
            "X-Tenant-ID": "alpha",
            "X-Org-Id": user.orgId,
          },
        });

        if (!cancelled) {
          setInvoices(response.data);
        }
      } catch (error: unknown) {
        if (!cancelled) {
          const err = error as {
            response?: {
              status?: number;
              data?: {
                message?: string;
                error?: string;
              };
            };
          };

          const status = err.response?.status;
          const message =
            err.response?.data?.message ||
            err.response?.data?.error;

          if (status === 401) {
            setError("Session expired. Please login again.");
          } else if (status === 403) {
            setError(
              "Access Denied: Cross-Tenant Action Not Allowed."
            );
          } else if (status === 400) {
            setError(
              message || "Bad Request. Please check request headers."
            );
          } else if (status === 500) {
            setError(
              message || "Backend Server Error. Please contact the backend team."
            );
          } else {
            setError(
              message || "Unable to load invoices. Please try again."
            );
          }
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchInvoices();

    return () => {
      cancelled = true;
    };
  }, [user?.orgId]);

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>LedgerGuard</h1>
      </header>

      <main
        className="dashboard-content"
        aria-busy={loading}
      >
        <h2>Transaction Ledger</h2>
        <p>View and manage your transactions here.</p>

        {loading && (
          <p role="status" aria-live="polite">
            Loading invoices, please wait...
          </p>
        )}

        {error && (
          <p className="error-message" role="alert">
            {error}
          </p>
        )}

        {invoices !== null && !loading && (
          <section>
            <h3>Invoice Data</h3>
            <pre className="invoice-data">
              {JSON.stringify(invoices, null, 2)}
            </pre>
          </section>
        )}
      </main>
    </div>
  );
}
