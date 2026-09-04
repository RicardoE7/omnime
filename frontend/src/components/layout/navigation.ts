import {
  IconBookmark,
  IconBrain,
  IconCirclePlus,
  IconEyeCheck,
  IconHome,
} from "@tabler/icons-react";

export const navItems = [
  {
    label: "Home",
    path: "/",
    icon: IconHome,
  },
  {
    label: "Add Anime",
    path: "/add",
    icon: IconCirclePlus,
  },
  {
    label: "Watched",
    path: "/watched",
    icon: IconEyeCheck,
  },
  {
    label: "Saved",
    path: "/saved",
    icon: IconBookmark,
  },
  {
    label: "My Taste",
    path: "/my-taste",
    icon: IconBrain,
  },
];