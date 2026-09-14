import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { AuthLayout } from "../components/auth/AuthLayout";

export function RegisterPage() {
  const { register } = useAuth();

  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    setError(null);
    setSubmitting(true);

    try {
      await register({
        email,
        password,
        displayName: displayName.trim() || undefined,
      });
    } catch {
      setError("Unable to create your account.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout>
      <div>
        <p className="text-label text-accent">START DISCOVERING</p>

        <h1 className="mt-2 text-page-title">Create your Omnime account</h1>

        <p className="mt-3 text-body-sm text-text-secondary">
          Tell Omnime what you watch, what you love, and what doesn't work for
          you. We'll handle the discovery.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="mt-8 space-y-5">
        <div>
          <label htmlFor="displayName" className="text-label text-text-primary">
            Display name
          </label>

          <input
            id="displayName"
            type="text"
            autoComplete="name"
            maxLength={100}
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            className="mt-2 min-h-11 w-full rounded-md border border-border bg-surface px-3 text-body text-text-primary outline-none transition focus:border-border-accent focus:ring-2 focus:ring-accent/30"
          />

          <p className="mt-1 text-caption text-text-muted">Optional</p>
        </div>

        <div>
          <label htmlFor="email" className="text-label text-text-primary">
            Email
          </label>

          <input
            id="email"
            type="email"
            autoComplete="email"
            required
            maxLength={254}
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="mt-2 min-h-11 w-full rounded-md border border-border bg-surface px-3 text-body text-text-primary outline-none transition focus:border-border-accent focus:ring-2 focus:ring-accent/30"
          />
        </div>

        <div>
          <label htmlFor="password" className="text-label text-text-primary">
            Password
          </label>

          <input
            id="password"
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="mt-2 min-h-11 w-full rounded-md border border-border bg-surface px-3 text-body text-text-primary outline-none transition focus:border-border-accent focus:ring-2 focus:ring-accent/30"
          />

          <p className="mt-1 text-caption text-text-muted">
            At least 8 characters
          </p>
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
          {submitting ? "Creating account..." : "Create account"}
        </button>
      </form>

      <p className="mt-6 text-center text-body-sm text-text-secondary">
        Already have an account?{" "}
        <Link
          to="/login"
          className="font-semibold text-accent hover:text-accent-hover"
        >
          Log in
        </Link>
      </p>
    </AuthLayout>
  );
}
