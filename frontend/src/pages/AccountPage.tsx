import AppShell from "../components/layout/AppShell";
import { useAuth } from "../auth/useAuth";

export function AccountPage() {
  const { user, logout, updateAdultAnimePreference } = useAuth();

  if (!user) {
    return null;
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-2xl py-12">
        <p className="text-label text-accent">ACCOUNT</p>

        <h1 className="mt-2 text-page-title">Your account</h1>

        <p className="mt-3 text-body text-text-secondary">
          Manage your Omnime account and preferences.
        </p>

        <div className="mt-8 rounded-card border border-border bg-surface p-6">
          <div>
            <p className="text-label text-text-muted">Display name</p>

            <p className="mt-1 text-body text-text-primary">
              {user.displayName ?? "Not set"}
            </p>
          </div>

          <div className="mt-6">
            <p className="text-label text-text-muted">Email</p>

            <p className="mt-1 text-body text-text-primary">{user.email}</p>
          </div>

          <div className="mt-6 border-t border-border pt-6">
            <div className="flex items-center justify-between gap-6">
              <div>
                <p className="text-label text-text-primary">
                  Include adult anime
                </p>

                <p className="mt-1 text-body-sm text-text-muted">
                  Allow adult anime to appear in your Omnime recommendations.
                </p>
              </div>

              <input
                type="checkbox"
                checked={user.includeAdultAnime}
                onChange={(event) =>
                  void updateAdultAnimePreference(event.target.checked)
                }
                className="h-5 w-5 cursor-pointer accent-accent"
                aria-label="Include adult anime"
              />
            </div>
          </div>
        </div>

        <button
          type="button"
          onClick={() => void logout()}
          className="mt-6 min-h-11 rounded-control border border-border-strong px-4 text-label text-text-secondary transition hover:bg-surface-interactive hover:text-text-primary"
        >
          Log out
        </button>
      </div>
    </AppShell>
  );
}
