type BackendWakeupProps = {
  state: "loading" | "waking" | "failed";
  onRetry: () => void;
};

export function BackendWakeup({
  state,
  onRetry,
}: BackendWakeupProps) {
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-background/95 px-4 backdrop-blur-sm">
      <div className="w-full max-w-md text-center">
        <p className="text-card-title tracking-[0.18em] text-text-primary">
          OMNIME
        </p>

        {state === "failed" ? (
          <>
            <div className="mx-auto mt-8 flex h-10 w-10 items-center justify-center rounded-full border border-danger/40 bg-danger/10 text-subheading text-danger">
              !
            </div>

            <h1 className="mt-6 text-section-title text-text-primary">
              Omnime didn't wake up.
            </h1>

            <p className="mt-3 text-body-sm text-text-secondary">
              The development server couldn't be reached. This can occasionally
              happen while the build is in progress.
            </p>

            <button
              type="button"
              onClick={onRetry}
              className="mt-7 min-h-11 rounded-md bg-accent px-6 py-3 text-label text-white transition hover:bg-accent-hover focus:outline-none focus:ring-2 focus:ring-accent/30"
            >
              Try Again
            </button>
          </>
        ) : (
          <>
            <div
              className="mx-auto mt-8 h-10 w-10 animate-spin rounded-full border-2 border-border-strong border-t-accent"
              aria-hidden="true"
            />

            <h1 className="mt-6 text-section-title text-text-primary">
              {state === "loading"
                ? "Loading Omnime..."
                : "Waking up Omnime..."}
            </h1>

            {state === "waking" && (
              <>
                <p className="mt-3 text-body-sm text-text-secondary">
                  This development build uses a free hosting environment. After
                  a period of inactivity, the backend may need a moment to wake
                  up.
                </p>

                <p className="mt-3 text-caption text-text-muted">
                  Usually under a minute.
                </p>

                <div className="mt-6 flex items-center justify-center gap-2">
                  <span className="h-2 w-2 animate-pulse rounded-full bg-accent" />

                  <span className="text-metadata text-text-muted">
                    Connecting to development server
                  </span>
                </div>
              </>
            )}
          </>
        )}
      </div>
    </div>
  );
}
