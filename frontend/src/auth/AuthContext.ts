import { createContext } from "react";
import type { AuthUser, LoginRequest, RegisterRequest } from "../types/auth";

export type AuthContextValue = {
  user: AuthUser | null;
  loading: boolean;
  register: (request: RegisterRequest) => Promise<void>;
  login: (request: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  updateAdultAnimePreference: (includeAdultAnime: boolean) => Promise<void>;
};

export const AuthContext = createContext<AuthContextValue | undefined>(
  undefined,
);
