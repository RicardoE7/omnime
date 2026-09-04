import AppShell from "./components/layout/AppShell";
import { useEffect, useState } from "react";
import { getHealth } from "./api/client";

function App() {
  const [backendStatus, setBackendStatus] = useState("Checking backend...");

  useEffect(() => {
    getHealth()
      .then((message) => {
        setBackendStatus(message);
      })
      .catch(() => {
        setBackendStatus("Backend unavailable");
      });
  }, []);

  return (
    <AppShell>
      <h1 className="text-page-title">Home</h1>
      <p className="mt-2 text-body text-text-secondary">{backendStatus}</p>
    </AppShell>
  );
}

export default App;
