import { useCallback, useState } from 'react';
import { ToastContext } from './ToastContext';
import ToastViewport from './ToastViewport';
import type {
  ToastContextValue,
  ToastItem,
  ToastOptions,
  ToastProviderProps,
  ToastType,
} from './toast.types';

const TOAST_DURATIONS: Record<ToastType, number> = {
  success: 5000,
  error: 10000,
  rollback: 0,
  warning: 8000,
  info: 5000,
};

export function ToastProvider({
  children,
}: ToastProviderProps): React.ReactElement {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const addToast = useCallback(
    (type: ToastType, options: ToastOptions) => {
      const id = options.id ?? crypto.randomUUID();

      const newToast: ToastItem = {
        ...options,
        id,
        type,
        duration: TOAST_DURATIONS[type],
      };

      setToasts((current) => {
        const existingIndex = current.findIndex(
          (toast) => toast.id === id
        );

        if (existingIndex !== -1) {
          return current.map((toast) =>
            toast.id === id ? newToast : toast
          );
        }

        return [...current, newToast];
      });
    },
    []
  );

  const dismiss = useCallback((id: string) => {
    setToasts((current) =>
      current.filter((toast) => toast.id !== id)
    );
  }, []);

  const clear = useCallback(() => {
    setToasts([]);
  }, []);

  const success = useCallback(
    (options: ToastOptions) => addToast('success', options),
    [addToast]
  );

  const error = useCallback(
    (options: ToastOptions) => addToast('error', options),
    [addToast]
  );

  const rollback = useCallback(
    (options: ToastOptions) => addToast('rollback', options),
    [addToast]
  );

  const warning = useCallback(
    (options: ToastOptions) => addToast('warning', options),
    [addToast]
  );

  const info = useCallback(
    (options: ToastOptions) => addToast('info', options),
    [addToast]
  );

  const contextValue: ToastContextValue = {
    success,
    error,
    rollback,
    warning,
    info,
    dismiss,
    clear,
  };

  return (
    <ToastContext.Provider value={contextValue}>
      {children}
      <ToastViewport toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  );
}

export default ToastProvider;