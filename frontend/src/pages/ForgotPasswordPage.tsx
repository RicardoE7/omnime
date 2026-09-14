import { useState } from "react";
import { Link } from "react-router-dom";
import { forgotPassword } from "../api/auth";
import { AuthLayout } from "../components/auth/AuthLayout";

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    setError(null);
    setSubmitting(true);

    try {
      await forgotPassword({ email });
      setSubmitted(true);
    } catch {
      setError("Unable to request a password reset.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout>
      <div>
        <h1 className="text-page-title text-text-primary">
          Reset your password
        </h1>

        <p className="mt-3 text-body text-text-secondary">
          Enter your email and we'll help you reset your password.
        </p>

        {submitted ? (
          <div className="mt-8 rounded-card border border-border bg-surface p-5">
            <p className="text-body text-text-primary">
              Check your email for password reset instructions.
            </p>

            <p className="mt-2 text-body-sm text-text-muted">
              If an Omnime account exists for that email, a reset link has been
              sent.
            </p>

            <Link
              to="/login"
              className="mt-5 inline-block text-label text-accent hover:text-accent-hover"
            >
              Back to login
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="mt-8">
            <label htmlFor="email" className="text-label text-text-secondary">
              Email
            </label>

            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
              maxLength={254}
              autoComplete="email"
              className="mt-2 min-h-11 w-full rounded-control border border-border bg-surface px-4 text-body text-text-primary outline-none transition focus:border-border-accent"
            />

            {error && <p className="mt-3 text-body-sm text-danger">{error}</p>}

            <button
              type="submit"
              disabled={submitting}
              className="mt-6 min-h-11 w-full rounded-control bg-accent px-4 text-label text-white transition hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-50"
            >
              {submitting ? "Sending..." : "Send reset instructions"}
            </button>

            <Link
              to="/login"
              className="mt-5 block text-center text-label text-text-secondary hover:text-text-primary"
            >
              Back to login
            </Link>
          </form>
        )}
      </div>
    </AuthLayout>
  );
}
