import { NavLink } from "react-router-dom";
import { navItems } from "./navigation";

const BottomNav = () => {
  return (
    <nav
      aria-label="Primary navigation"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-surface px-2 lg:hidden"
    >
      <div className="grid grid-cols-5">
        {navItems.map((item) => {
          const Icon = item.icon;

          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === "/"}
              className={({ isActive }) =>
                [
                  "flex min-h-16 flex-col items-center justify-center gap-1 text-caption transition-colors",
                  isActive
                    ? "text-accent"
                    : "text-text-muted hover:text-text-primary",
                ].join(" ")
              }
            >
              <Icon size={20} stroke={1.75} aria-hidden="true" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </div>
    </nav>
  );
};

export default BottomNav;
