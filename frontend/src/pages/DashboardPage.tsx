
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>LedgerGuard</h1>

        <button type="button" onClick={handleLogout}>
          Logout
        </button>
      </header>

      <main className="dashboard-content">
        <h2>Welcome to LedgerGuard Dashboard</h2>

        {user && (
          <div className="user-details">
            <p><strong>User ID:</strong> {user.userId}</p>
            <p><strong>Organization:</strong> {user.orgId}</p>
            <p><strong>Role:</strong> {user.role}</p>
          </div>
        )}

        <nav
          className="dashboard-navigation"
          aria-label="Dashboard navigation"
        >
          <Link to="/ledger">Ledger</Link>
          <Link to="/analytics">Analytics</Link>
        </nav>
      </main>
    </div>
  );
}
