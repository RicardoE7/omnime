import AppShell from "./components/layout/AppShell";
import { useCallback, useEffect, useState } from "react";
import { getHealth } from "./api/client";
import { SneakPeekModal } from "./components/SneakPeekModal";
import { BackendWakeup } from "./components/BackendWakeup";

type BackendState = "loading" | "waking" | "ready" | "failed";

function App() {
  const showSneakPeek = import.meta.env.VITE_SHOW_SNEAK_PEEK === "true";

  const [hasEnteredBuild, setHasEnteredBuild] = useState(
    () => sessionStorage.getItem("omnime-sneak-peek-entered") === "true",
  );

  const [backendState, setBackendState] = useState<BackendState>("loading");
  const [showBackendWakeup, setShowBackendWakeup] = useState(false);

  const checkBackend = useCallback(() => {
    setBackendState("loading");
    setShowBackendWakeup(false);

    const loadingTimer = window.setTimeout(() => {
      setShowBackendWakeup(true);
    }, 500);

    const wakingTimer = window.setTimeout(() => {
      setBackendState("waking");
      setShowBackendWakeup(true);
    }, 3000);

    const failureTimer = window.setTimeout(() => {
      setBackendState("failed");
      setShowBackendWakeup(true);
    }, 90000);

    getHealth()
      .then(() => {
        window.clearTimeout(loadingTimer);
        window.clearTimeout(wakingTimer);
        window.clearTimeout(failureTimer);

        setShowBackendWakeup(false);
        setBackendState("ready");
      })
      .catch(() => {
        window.clearTimeout(loadingTimer);
        window.clearTimeout(wakingTimer);
        window.clearTimeout(failureTimer);

        setShowBackendWakeup(true);
        setBackendState("failed");
      });

    return () => {
      window.clearTimeout(loadingTimer);
      window.clearTimeout(wakingTimer);
      window.clearTimeout(failureTimer);
    };
  }, []);

  useEffect(() => {
  const initialCheck = window.setTimeout(() => {
    checkBackend();
  }, 0);

  return () => {
    window.clearTimeout(initialCheck);
  };
}, [checkBackend]);

  function handleEnterBuild() {
    sessionStorage.setItem("omnime-sneak-peek-entered", "true");
    setHasEnteredBuild(true);
  }

  return (
    <>
      {showBackendWakeup && backendState !== "ready" && (
        <BackendWakeup state={backendState} onRetry={checkBackend} />
      )}

      {showSneakPeek && !hasEnteredBuild && (
        <SneakPeekModal onEnter={handleEnterBuild} />
      )}

      <AppShell>
        <h1 className="text-page-title">Home</h1>
      </AppShell>
    </>
  );
}

export default App;