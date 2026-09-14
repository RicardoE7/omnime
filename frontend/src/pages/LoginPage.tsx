import { useState } from "react";
import { Link } from "react-router-dom";
import { AuthLayout } from "../components/auth/AuthLayout";
import { useAuth } from "../auth/useAuth";

export function LoginPage() {
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    setError(null);
    setSubmitting(true);

    try {
      await login({
        email,
        password,
      });
    } catch {
      setError("Invalid email or password.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout>
      <div>
        <p className="text-label text-accent">WELCOME BACK</p>

        <h1 className="mt-2 text-page-title">Log in to Omnime</h1>

        <p className="mt-3 text-body-sm text-text-secondary">
          Pick up where you left off and keep discovering anime built around
          your taste.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="mt-8 space-y-5">
        <div>
          <label htmlFor="email" className="text-label text-text-primary">
            Email
          </label>

          <input
            id="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="mt-2 min-h-11 w-full rounded-md border border-border bg-surface px-3 text-body text-text-primary outline-none transition focus:border-border-accent focus:ring-2 focus:ring-accent/30"
          />
        </div>

        <div>
          <div className="flex items-center justify-between gap-4">
            <label htmlFor="password" className="text-label text-text-primary">
              Password
            </label>

            <Link
              to="/forgot-password"
              className="text-body-sm text-accent hover:text-accent-hover"
            >
              Forgot password?
            </Link>
          </div>

          <input
            id="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="mt-2 min-h-11 w-full rounded-md border border-border bg-surface px-3 text-body text-text-primary outline-none transition focus:border-border-accent focus:ring-2 focus:ring-accent/30"
          />
        </div>

        {error && (
          <p role="alert" className="text-body-sm text-danger">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="min-h-11 w-full rounded-md bg-accent px-4 text-label text-white transition hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-60"
        >
          {submitting ? "Logging in..." : "Log in"}
        </button>
      </form>

      <p className="mt-6 text-center text-body-sm text-text-secondary">
        New to Omnime?{" "}
        <Link
          to="/register"
          className="font-semibold text-accent hover:text-accent-hover"
        >
          Create an account
        </Link>
      </p>
    </AuthLayout>
  );
}
