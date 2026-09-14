import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { resetPassword } from "../api/auth";
import { AuthLayout } from "../components/auth/AuthLayout";

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [newPassword, setNewPassword] = useState("");
  const [confirmedPassword, setConfirmedPassword] = useState("");
  const [completed, setCompleted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    setError(null);

    if (!token) {
      setError("This password reset link is invalid.");
      return;
    }

    if (newPassword !== confirmedPassword) {
      setError("Passwords do not match.");
      return;
    }

    setSubmitting(true);

    try {
      await resetPassword({
        token,
        newPassword,
      });

      setCompleted(true);
    } catch {
      setError("This password reset link is invalid or expired.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout>
      <div>
        <h1 className="text-page-title text-text-primary">
          Choose a new password
        </h1>

        <p className="mt-3 text-body text-text-secondary">
          Enter a new password for your Omnime account.
        </p>

        {completed ? (
          <div className="mt-8 rounded-card border border-border bg-surface p-5">
            <p className="text-body text-text-primary">
              Your password has been changed.
            </p>

            <Link
              to="/login"
              className="mt-5 inline-block text-label text-accent hover:text-accent-hover"
            >
              Log in with your new password
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="mt-8">
            <label
              htmlFor="new-password"
              className="text-label text-text-secondary"
            >
              New password
            </label>

            <input
              id="new-password"
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              required
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
              className="mt-2 min-h-11 w-full rounded-control border border-border bg-surface px-4 text-body text-text-primary outline-none transition focus:border-border-accent"
            />

            <label
              htmlFor="confirm-password"
              className="mt-5 block text-label text-text-secondary"
            >
              Confirm new password
            </label>

            <input
              id="confirm-password"
              type="password"
              value={confirmedPassword}
              onChange={(event) => setConfirmedPassword(event.target.value)}
              required
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
              className="mt-2 min-h-11 w-full rounded-control border border-border bg-surface px-4 text-body text-text-primary outline-none transition focus:border-border-accent"
            />

            {error && <p className="mt-3 text-body-sm text-danger">{error}</p>}

            <button
              type="submit"
              disabled={submitting}
              className="mt-6 min-h-11 w-full rounded-control bg-accent px-4 text-label text-white transition hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-50"
            >
              {submitting ? "Changing password..." : "Change password"}
            </button>
          </form>
        )}
      </div>
    </AuthLayout>
  );
}
