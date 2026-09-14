import type {
  AuthUser,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
} from "../types/auth";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

type CsrfResponse = {
  token: string;
};

async function getCsrfToken(): Promise<string> {
  const response = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error("Unable to obtain CSRF token.");
  }

  const csrf: CsrfResponse = await response.json();

  return csrf.token;
}

async function authenticatedRequest(
  path: string,
  options: RequestInit = {},
): Promise<Response> {
  const csrfToken = await getCsrfToken();

  return fetch(`${API_BASE_URL}${path}`, {
    ...options,
    credentials: "include",
    headers: {
      ...options.headers,
      "X-XSRF-TOKEN": csrfToken,
    },
  });
}

export async function register(request: RegisterRequest): Promise<AuthUser> {
  const response = await authenticatedRequest("/api/auth/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error("Registration failed.");
  }

  return response.json();
}

export async function login(request: LoginRequest): Promise<AuthUser> {
  const response = await authenticatedRequest("/api/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error("Login failed.");
  }

  return response.json();
}

export async function logout(): Promise<void> {
  const response = await authenticatedRequest("/api/auth/logout", {
    method: "POST",
  });

  if (!response.ok) {
    throw new Error("Logout failed.");
  }
}

export async function getCurrentUser(): Promise<AuthUser | null> {
  const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
    credentials: "include",
  });

  if (response.status === 401) {
    return null;
  }

  if (!response.ok) {
    throw new Error("Unable to load current user.");
  }

  return response.json();
}

export async function forgotPassword(
  request: ForgotPasswordRequest,
): Promise<void> {
  const response = await authenticatedRequest("/api/auth/forgot-password", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error("Password reset request failed.");
  }
}

export async function resetPassword(
  request: ResetPasswordRequest,
): Promise<void> {
  const response = await authenticatedRequest("/api/auth/reset-password", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error("Password reset failed.");
  }
}

export async function updateAdultAnimePreference(
  includeAdultAnime: boolean,
): Promise<AuthUser> {
  const response = await authenticatedRequest(
    "/api/auth/adult-anime-preference",
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ includeAdultAnime }),
    },
  );

  if (!response.ok) {
    throw new Error("Unable to update adult anime preference.");
  }

  return response.json() as Promise<AuthUser>;
}
