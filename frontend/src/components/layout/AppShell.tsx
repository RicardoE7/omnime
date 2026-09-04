import type { ReactNode } from "react";
import Sidebar from "./Sidebar";
import BottomNav from "./BottomNav";

type AppShellProps = {
  children: ReactNode;
};

const AppShell = ({ children }: AppShellProps) => {
  return (
    <div className="min-h-screen lg:flex">
      <Sidebar />

      <main className="min-w-0 flex-1 p-8 pb-24 lg:pb-8">
        {children}
      </main>

      <BottomNav />
    </div>
  );
};

export default AppShell;