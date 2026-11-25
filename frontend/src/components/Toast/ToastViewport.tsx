import type { ToastItem } from './toast.types';
import Toast from './Toast';

interface ToastViewportProps {
  toasts: ToastItem[];
  onDismiss: (id: string) => void;
}

export default function ToastViewport({
  toasts,
  onDismiss,
}: ToastViewportProps): React.ReactElement {
  const visibleToasts = toasts.slice(0, 3);

  return (
    <>
      <div
        className="toast-live-region"
        role="status"
        aria-live="polite"
        aria-atomic="false"
      >
        {visibleToasts
          .filter((toast) => toast.type === 'success' || toast.type === 'info')
          .map((toast) => (
            <div key={toast.id}>
              {toast.title} {toast.message}
            </div>
          ))}
      </div>

      <div
        className="toast-live-region"
        role="alert"
        aria-live="assertive"
        aria-atomic="false"
      >
        {visibleToasts
          .filter(
            (toast) =>
              toast.type === 'error' ||
              toast.type === 'rollback' ||
              toast.type === 'warning'
          )
          .map((toast) => (
            <div key={toast.id}>
              {toast.title} {toast.message}
            </div>
          ))}
      </div>

      <section
        className="toast-viewport"
        aria-label="Notifications"
      >
        {visibleToasts.map((toast) => (
          <Toast
            key={toast.id}
            toast={toast}
            onDismiss={onDismiss}
          />
        ))}
      </section>
    </>
  );
}