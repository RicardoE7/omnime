import type { ReactNode } from "react";
import omnimeMark from "../../assets/brand/omnime-mark.png";

type AuthLayoutProps = {
  children: ReactNode;
};

export function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <main className="min-h-screen bg-background text-text-primary lg:grid lg:grid-cols-[1.1fr_0.9fr]">
      <section className="relative hidden overflow-hidden border-r border-border bg-surface lg:flex lg:flex-col lg:justify-between lg:p-12">
        <div className="flex items-center gap-3">
          <img src={omnimeMark} alt="" className="h-9 w-9 object-contain" />

          <span className="text-xl font-semibold tracking-tight">OMNIME</span>
        </div>

        <div className="max-w-xl">
          <p className="mb-4 text-label text-accent">ALL ANIME. FOR ME.</p>

          <h1 className="text-display">
            Discover anime that actually fits your taste.
          </h1>

          <p className="mt-6 max-w-lg text-body text-text-secondary">
            Omnime learns what you enjoy, what you don't, and why — then uses
            that understanding to help you find what to watch next.
          </p>
        </div>

        <p className="text-caption text-text-muted">
          AI-powered anime discovery built around you.
        </p>
      </section>

      <section className="flex min-h-screen items-center justify-center px-4 py-10 sm:px-6 lg:px-10">
        <div className="w-full max-w-[430px]">
          <div className="mb-10 flex items-center gap-3 lg:hidden">
            <img src={omnimeMark} alt="" className="h-8 w-8 object-contain" />

            <span className="text-xl font-semibold tracking-tight">OMNIME</span>
          </div>

          {children}
        </div>
      </section>
    </main>
  );
}
