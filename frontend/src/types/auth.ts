export type AuthUser = {
  id: string;
  email: string;
  displayName: string | null;
  onboardingCompleted: boolean;
  includeAdultAnime: boolean;
};

export type RegisterRequest = {
  email: string;
  password: string;
  displayName?: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type ForgotPasswordRequest = {
  email: string;
};

export type ResetPasswordRequest = {
  token: string;
  newPassword: string;
};
