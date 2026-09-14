import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./useAuth";

export function PublicOnlyRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return null;
  }

  if (user !== null) {
    return (
      <Navigate to={user.onboardingCompleted ? "/" : "/onboarding"} replace />
    );
  }

  return <Outlet />;
}
