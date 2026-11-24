
import React from 'react';
import './Toast.css';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

interface ToastProps {
    message: string;
    type?: ToastType;
    onClose: () => void;
}

export const Toast: React.FC<ToastProps> = ({
    message,
    type = 'info',
    onClose,
}) => {
    return (
        <div
            className={`toast toast--${type}`}
            role="status"
            aria-live="polite"
        >
            <span className="toast__message">{message}</span>

            <button
                type="button"
                className="toast__close"
                onClick={onClose}
                aria-label="Close notification"
            >
                ×
            </button>
        </div>
    );
};

export default Toast;
