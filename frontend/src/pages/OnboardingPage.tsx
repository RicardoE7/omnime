import AppShell from "../components/layout/AppShell";

export function OnboardingPage() {
  return (
    <AppShell>
      <div className="mx-auto max-w-2xl py-12">
        <p className="text-label text-accent">WELCOME TO OMNIME</p>

        <h1 className="mt-2 text-page-title">Let's build your anime taste.</h1>

        <p className="mt-4 text-body text-text-secondary">
          Your account is ready. Omnime will use the anime you add to start
          learning what you enjoy.
        </p>

        <p className="mt-6 text-body-sm text-text-muted">
          Taste onboarding is coming next.
        </p>
      </div>
    </AppShell>
  );
}
