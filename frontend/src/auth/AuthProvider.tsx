import { useEffect, useState, type ReactNode } from "react";
import {
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
  updateAdultAnimePreference as updateAdultAnimePreferenceRequest,
} from "../api/auth";
import type { AuthUser, LoginRequest, RegisterRequest } from "../types/auth";
import { AuthContext } from "./AuthContext";

type AuthProviderProps = {
  children: ReactNode;
};

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadCurrentUser() {
      try {
        const currentUser = await getCurrentUser();
        setUser(currentUser);
      } catch (error) {
        console.error("Failed to load current user:", error);
        setUser(null);
      } finally {
        setLoading(false);
      }
    }

    void loadCurrentUser();
  }, []);

  async function register(request: RegisterRequest) {
    const registeredUser = await registerRequest(request);
    setUser(registeredUser);
  }

  async function login(request: LoginRequest) {
    const authenticatedUser = await loginRequest(request);
    setUser(authenticatedUser);
  }

  async function logout() {
    await logoutRequest();
    setUser(null);
  }

  async function updateAdultAnimePreference(includeAdultAnime: boolean) {
    const updatedUser =
      await updateAdultAnimePreferenceRequest(includeAdultAnime);

    setUser(updatedUser);
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        register,
        login,
        logout,
        updateAdultAnimePreference,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
