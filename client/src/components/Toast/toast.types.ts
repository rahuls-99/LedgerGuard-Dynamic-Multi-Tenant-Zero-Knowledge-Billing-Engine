import type { ReactNode } from 'react';

export type ToastType =
  | 'success'
  | 'error'
  | 'rollback'
  | 'warning'
  | 'info';

export interface ToastAction {
  label: string;
  onClick: () => void;
}

export interface ToastOptions {
  title: string;
  message?: string;
  id?: string;
  traceId?: string;
  action?: ToastAction;
}

export interface ToastItem extends ToastOptions {
  id: string;
  type: ToastType;
  duration: number;
}

export interface ToastContextValue {
  success: (options: ToastOptions) => void;
  error: (options: ToastOptions) => void;
  rollback: (options: ToastOptions) => void;
  warning: (options: ToastOptions) => void;
  info: (options: ToastOptions) => void;
  dismiss: (id: string) => void;
  clear: () => void;
}

export interface ToastProviderProps {
  children: ReactNode;
}