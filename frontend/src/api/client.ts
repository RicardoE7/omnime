const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export async function getHealth(signal?: AbortSignal): Promise<string> {
  const response = await fetch(`${API_BASE_URL}/api/health`, {
    credentials: "include",
    signal,
  });

  if (!response.ok) {
    throw new Error(`Health check failed with status ${response.status}`);
  }

  return response.text();
}