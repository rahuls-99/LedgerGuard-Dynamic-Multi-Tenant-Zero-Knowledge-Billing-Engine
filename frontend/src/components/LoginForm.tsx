
import { useState, type FormEvent } from "react";

interface LoginFormProps {
  onSubmit: (
    orgId: string,
    email: string,
    password: string
  ) => Promise<void>;
  loading?: boolean;
  error?: string;
}

export default function LoginForm({
  onSubmit,
  loading = false,
  error = "",
}: LoginFormProps) {
  const [orgId, setOrgId] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [validationError, setValidationError] = useState("");

  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault();
    setValidationError("");

    const cleanOrgId = orgId.trim();
    const cleanEmail = email.trim();

    if (!cleanOrgId || !cleanEmail || !password) {
      setValidationError("Please fill in all fields.");
      return;
    }

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailPattern.test(cleanEmail)) {
      setValidationError("Please enter a valid email address.");
      return;
    }

    try {
      await onSubmit(cleanOrgId, cleanEmail, password);
    } catch {
      // Parent component handles login errors.
    }
  };

  return (
    <form className="login-form" onSubmit={handleSubmit}>
      <h2>LedgerGuard Login</h2>

      <label htmlFor="orgId">Organization ID</label>
      <input
        id="orgId"
        type="text"
        value={orgId}
        onChange={(e) => setOrgId(e.target.value)}
        placeholder="Enter Organization ID"
        required
        disabled={loading}
      />

      <label htmlFor="email">Email</label>
      <input
        id="email"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="Enter your email"
        required
        disabled={loading}
      />

      <label htmlFor="password">Password</label>
      <input
        id="password"
        type={showPassword ? "text" : "password"}
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Enter your password"
        required
        disabled={loading}
      />

      <label className="show-password">
        <input
          type="checkbox"
          checked={showPassword}
          onChange={(e) => setShowPassword(e.target.checked)}
          disabled={loading}
        />
        Show Password
      </label>

      {validationError && (
        <p className="error-message">{validationError}</p>
      )}

      {error && (
        <p className="error-message">{error}</p>
      )}

      <button type="submit" disabled={loading}>
        {loading ? "Logging in..." : "Login"}
      </button>
    </form>
  );
}
