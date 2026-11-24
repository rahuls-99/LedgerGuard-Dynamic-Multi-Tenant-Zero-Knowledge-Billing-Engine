import { useEffect, useId, useRef } from 'react';
import type { ReactNode } from 'react';
import { useFocusTrap } from '../../hooks/useFocusTrap';
import './ConfirmModal.css';

interface ConfirmModalProps {
    open: boolean;
    title: string;
    summary: ReactNode;
    confirmLabel: string;
    cancelLabel: string;
    onConfirm: () => void;
    onCancel: () => void;
    loading?: boolean;
    variant?: 'default' | 'danger';
}

export function ConfirmModal({
    open,
    title,
    summary,
    confirmLabel,
    cancelLabel,
    onConfirm,
    onCancel,
    loading = false,
    variant = 'default',
}: ConfirmModalProps) {
    const dialogRef = useRef<HTMLDivElement>(null);
    const cancelButtonRef = useRef<HTMLButtonElement>(null);
    const titleId = useId();
    const descriptionId = useId();

    useFocusTrap(dialogRef, open, cancelButtonRef);

    useEffect(() => {
        if (!open) {
            return;
        }

        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';

        return () => {
            document.body.style.overflow = previousOverflow;
        };
    }, [open]);

    useEffect(() => {
        if (!open || loading) {
            return;
        }

        const handleEscape = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                onCancel();
            }
        };

        document.addEventListener('keydown', handleEscape);

        return () => {
            document.removeEventListener('keydown', handleEscape);
        };
    }, [open, loading, onCancel]);

    if (!open) {
        return null;
    }

    const handleBackdropClick = (
        event: React.MouseEvent<HTMLDivElement>
    ) => {
        if (event.target === event.currentTarget && !loading) {
            onCancel();
        }
    };

    return (
        <div
            className="confirm-modal__backdrop"
            onMouseDown={handleBackdropClick}
        >
            <div
                ref={dialogRef}
                className={`confirm-modal confirm-modal--${variant}`}
                role="dialog"
                aria-modal="true"
                aria-labelledby={titleId}
                aria-describedby={descriptionId}
                tabIndex={-1}
            >
                <h2 id={titleId} className="confirm-modal__title">
                    {title}
                </h2>

                <div
                    id={descriptionId}
                    className="confirm-modal__summary"
                >
                    {summary}
                </div>

                <p className="confirm-modal__warning">
                    Please check the amount. This action will move money.
                </p>

                <div className="confirm-modal__actions">
                    <button
                        ref={cancelButtonRef}
                        type="button"
                        className="confirm-modal__cancel"
                        onClick={onCancel}
                        disabled={loading}
                    >
                        {cancelLabel}
                    </button>

                    <button
                        type="button"
                        className="confirm-modal__confirm"
                        onClick={onConfirm}
                        disabled={loading}
                    >
                        {loading && (
                            <span
                                className="confirm-modal__spinner"
                                aria-hidden="true"
                            />
                        )}
                        {loading ? 'Processing...' : confirmLabel}
                    </button>
                </div>
            </div>
        </div>
    );
}