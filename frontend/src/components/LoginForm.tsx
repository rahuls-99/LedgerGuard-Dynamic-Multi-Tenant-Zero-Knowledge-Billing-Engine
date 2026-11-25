
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
      setValidationError("Please fill in all required fields.");
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

  const displayedError = validationError || error;

  return (
    <form
      className="login-form"
      onSubmit={handleSubmit}
      aria-busy={loading}
    >
      <h1>LedgerGuard Login</h1>

      <p>Fields marked with * are required.</p>

      <label htmlFor="orgId">
        Organization ID *
      </label>
      <input
        id="orgId"
        name="orgId"
        type="text"
        value={orgId}
        onChange={(e) => setOrgId(e.target.value)}
        placeholder="Enter Organization ID"
        required
        aria-required="true"
        disabled={loading}
        aria-invalid={Boolean(validationError && !orgId.trim())}
        aria-describedby={
          validationError && !orgId.trim()
            ? "login-error"
            : undefined
        }
      />

      <label htmlFor="email">
        Email *
      </label>
      <input
        id="email"
        name="email"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="Enter your email"
        autoComplete="username"
        required
        aria-required="true"
        disabled={loading}
        aria-invalid={Boolean(
          validationError &&
          (!email.trim() ||
            !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()))
        )}
        aria-describedby={
          validationError &&
          (!email.trim() ||
            !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()))
            ? "login-error"
            : undefined
        }
      />

      <label htmlFor="password">
        Password *
      </label>
      <input
        id="password"
        name="password"
        type={showPassword ? "text" : "password"}
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Enter your password"
        autoComplete="current-password"
        required
        aria-required="true"
        disabled={loading}
        aria-invalid={Boolean(validationError && !password)}
        aria-describedby={
          validationError && !password
            ? "login-error"
            : undefined
        }
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

      {displayedError && (
        <p
          id="login-error"
          className="error-message"
          role="alert"
          aria-live="assertive"
        >
          {displayedError}
        </p>
      )}

      <button type="submit" disabled={loading}>
        {loading ? (
          <span role="status">Logging in, please wait...</span>
        ) : (
          "Login"
        )}
      </button>
    </form>
  );
}
