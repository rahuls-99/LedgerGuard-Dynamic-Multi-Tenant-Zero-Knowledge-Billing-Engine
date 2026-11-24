
import './Toast.css';
import { useEffect, useRef, useState } from 'react';
import type { ToastItem } from './toast.types';

interface ToastProps {
  toast: ToastItem;
  onDismiss: (id: string) => void;
}

const TOAST_ICONS = {
  success: '✓',
  error: '✕',
  rollback: '↶',
  warning: '⚠',
  info: 'ⓘ',
};

export default function Toast({
  toast,
  onDismiss,
}: ToastProps): React.ReactElement {
  const [paused, setPaused] = useState(false);
  const [remaining, setRemaining] = useState(toast.duration);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const startedAtRef = useRef<number>(0);

  useEffect(() => {
    setRemaining(toast.duration);
  }, [toast.id, toast.duration]);

  useEffect(() => {
    if (remaining <= 0 || paused || toast.duration === 0) {
      return;
    }

    startedAtRef.current = Date.now();

    timerRef.current = setTimeout(() => {
      onDismiss(toast.id);
    }, remaining);

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, [remaining, paused, toast.duration, toast.id, onDismiss]);

  const pauseTimer = (): void => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);

      const elapsed = Date.now() - startedAtRef.current;

      setRemaining((current) =>
        Math.max(0, current - elapsed)
      );
    }

    setPaused(true);
  };

  const resumeTimer = (): void => {
    setPaused(false);
  };

  const handleKeyDown = (
    event: React.KeyboardEvent<HTMLElement>
  ): void => {
    if (event.key === 'Escape') {
      onDismiss(toast.id);
    }
  };

  const copyTraceId = async (): Promise<void> => {
    if (!toast.traceId) return;

    try {
      await navigator.clipboard.writeText(toast.traceId);
    } catch {
      console.error('Unable to copy trace ID');
    }
  };

  return (
    <article
      className={`toast toast-${toast.type}`}
      onMouseEnter={pauseTimer}
      onMouseLeave={resumeTimer}
      onFocus={pauseTimer}
      onBlur={resumeTimer}
      onKeyDown={handleKeyDown}
    >
      <span className="toast-icon" aria-hidden="true">
        {TOAST_ICONS[toast.type]}
      </span>

      <div className="toast-content">
        <strong>{toast.title}</strong>

        {toast.message && <p>{toast.message}</p>}

        {toast.traceId && (
          <button
            type="button"
            className="toast-copy-button"
            onClick={copyTraceId}
          >
            Copy trace ID
          </button>
        )}

        {toast.action && (
          <button
            type="button"
            className="toast-action-button"
            onClick={toast.action.onClick}
          >
            {toast.action.label}
          </button>
        )}
      </div>

      <button
        type="button"
        className="toast-close"
        aria-label="Close notification"
        onClick={() => onDismiss(toast.id)}
      >
        ×
      </button>
    </article>
  );
}