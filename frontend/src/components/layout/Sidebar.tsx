import { NavLink } from "react-router-dom";
import { IconUserCircle } from "@tabler/icons-react";
import { navItems } from "./navigation";
import omnimeMark from "../../assets/brand/omnime-mark.png";

const Sidebar = () => {
  return (
    <aside className="hidden h-screen w-64 shrink-0 flex-col border-r border-border bg-surface px-4 py-6 lg:sticky lg:top-0 lg:flex">
      <div className="flex items-center gap-3 px-3 pb-8">
        <img
          src={omnimeMark}
          alt=""
          className="h-8 w-8 shrink-0 object-contain"
        />

        <span className="text-xl font-semibold tracking-[0.18em] text-text-primary">
          OMNIME
        </span>
      </div>

      <nav
        aria-label="Primary navigation"
        className="flex flex-1 flex-col gap-2"
      >
        {navItems.map((item) => {
          const Icon = item.icon;

          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === "/"}
              className={({ isActive }) =>
                [
                  "flex min-h-11 items-center gap-3 rounded-control px-3 text-label transition-colors",
                  isActive
                    ? "bg-accent-subtle text-accent"
                    : "text-text-secondary hover:bg-surface-interactive hover:text-text-primary",
                ].join(" ")
              }
            >
              <Icon size={20} stroke={1.75} aria-hidden="true" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      <nav
        aria-label="Account navigation"
        className="border-t border-border pt-4"
      >
        <NavLink
          to="/account"
          className={({ isActive }) =>
            [
              "flex min-h-11 items-center gap-3 rounded-control px-3 text-label transition-colors",
              isActive
                ? "bg-accent-subtle text-accent"
                : "text-text-secondary hover:bg-surface-interactive hover:text-text-primary",
            ].join(" ")
          }
        >
          <IconUserCircle size={20} stroke={1.75} aria-hidden="true" />
          <span>Account</span>
        </NavLink>
      </nav>
    </aside>
  );
};

export default Sidebar;
