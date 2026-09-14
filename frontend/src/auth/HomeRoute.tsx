import { Navigate } from "react-router-dom";
import { useAuth } from "./useAuth";
import { HomePage } from "../pages/HomePage";

export function HomeRoute() {
  const { user } = useAuth();

  if (user && !user.onboardingCompleted) {
    return <Navigate to="/onboarding" replace />;
  }

  return <HomePage />;
}
