const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

if (!API_BASE_URL) {
  throw new Error("VITE_API_BASE_URL is not configured");
}

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