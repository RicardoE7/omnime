import AppShell from "./components/layout/AppShell";
import { useCallback, useEffect, useRef, useState } from "react";
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

  const backendControllerRef = useRef<AbortController | null>(null);

  const checkBackend = useCallback(() => {
    backendControllerRef.current?.abort();

    const controller = new AbortController();
    backendControllerRef.current = controller;

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
      controller.abort();
      setBackendState("failed");
      setShowBackendWakeup(true);
    }, 90000);

    getHealth(controller.signal)
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

        if (!controller.signal.aborted) {
          setShowBackendWakeup(true);
          setBackendState("failed");
        }
      });

    return () => {
      window.clearTimeout(loadingTimer);
      window.clearTimeout(wakingTimer);
      window.clearTimeout(failureTimer);
      controller.abort();
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
