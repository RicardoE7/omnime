import homePreview from "../assets/omnime-home-preview.png";

type SneakPeekModalProps = {
  onEnter: () => void;
};

export function SneakPeekModal({ onEnter }: SneakPeekModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-background/85 backdrop-blur-md" />

      {/* Modal */}
      <div className="relative z-10 w-full max-w-6xl overflow-hidden rounded-modal border border-border-strong bg-surface shadow-2xl">
        <div className="grid lg:grid-cols-[0.85fr_1.35fr]">
          {/* Copy */}
          <div className="relative z-20 flex flex-col justify-center p-6 sm:p-8 lg:p-10">
            {/* Development badge */}
            <div className="mb-6">
              <span className="inline-flex rounded-full border border-border-accent bg-accent-subtle px-3 py-1.5 text-caption font-semibold tracking-[0.08em] text-accent-hover">
                IN DEVELOPMENT
              </span>
            </div>

            {/* Main copy */}
            <h1 className="text-page-title text-text-primary">
              Omnime is being built right now.
            </h1>

            <p className="mt-4 text-body text-text-secondary">
              You're looking at the live development build of an AI-powered
              anime discovery platform that learns what you enjoy and recommends
              what to watch next.
            </p>

            <p className="mt-4 text-body-sm text-text-muted">
              The foundation is live. Features are being added as development
              continues.
            </p>

            {/* Current development status */}
            <div className="mt-7 rounded-lg border border-border bg-surface-elevated p-4">
              <p className="text-caption font-semibold tracking-[0.06em] text-text-muted">
                CURRENTLY BUILDING
              </p>

              <div className="mt-2 flex items-center gap-2">
                <span className="text-accent">✦</span>

                <span className="text-label text-text-primary">
                  Authentication
                </span>
              </div>
            </div>

            {/* Enter build */}
            <button
              type="button"
              onClick={onEnter}
              className="mt-7 min-h-11 rounded-md bg-accent px-5 py-3 text-label text-white transition hover:bg-accent-hover focus:outline-none focus:ring-2 focus:ring-accent/30"
            >
              Enter Development Build →
            </button>

            <p className="mt-4 text-caption text-text-muted">
              Expect things to change. That's half the fun.
            </p>
          </div>

          {/* Desktop Home preview */}
          <div className="relative hidden min-h-[520px] overflow-hidden lg:block">
            {/* Fade screenshot into copy */}
            <div className="absolute inset-y-0 left-0 z-10 w-24 bg-gradient-to-r from-surface to-transparent" />

            <div className="absolute left-6 top-1/2 w-[900px] -translate-y-1/2">
              <div className="overflow-hidden rounded-xl border border-border-accent shadow-2xl">
                <img
                  src={homePreview}
                  alt="Preview of the planned Omnime home experience"
                  className="block w-full"
                />
              </div>
            </div>
          </div>

          {/* Mobile Home preview */}
          <div className="px-6 pb-6 sm:px-8 lg:hidden">
            <div className="overflow-hidden rounded-xl border border-border-accent">
              <img
                src={homePreview}
                alt="Preview of the planned Omnime home experience"
                className="block w-full"
              />
            </div>

            <p className="mt-3 text-center text-caption text-text-muted">
              A sneak peek at where Omnime is headed.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}