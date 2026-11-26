
import React from 'react';
import './ErrorState.css';

export type ErrorStateVariant =
    | 'network'
    | 'server'
    | 'forbidden'
    | 'generic';

interface ErrorStateProps {
    title: string;
    message: string;
    traceId?: string;
    onRetry?: () => void;
    variant?: ErrorStateVariant;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
    title,
    message,
    traceId,
    onRetry,
    variant = 'generic',
}) => {
    const handleCopyTraceId = async () => {
        if (!traceId) return;

        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(traceId);
            } else {
                const textArea = document.createElement('textarea');
                textArea.value = traceId;
                textArea.style.position = 'fixed';
                textArea.style.opacity = '0';

                document.body.appendChild(textArea);
                textArea.select();

                const copied = document.execCommand('copy');
                document.body.removeChild(textArea);

                if (!copied) {
                    throw new Error('Copy command failed');
                }
            }

            alert('Trace ID copied successfully!');
        } catch (error) {
            console.error('Unable to copy trace ID:', error);
            alert('Unable to copy. Please copy the Trace ID manually.');
        }
    };

    return (
        <div className={`error-state error-state--${variant}`} role="alert">
            <div className="error-state__icon" aria-hidden="true">
                {variant === 'network' ? '🌐' :
                    variant === 'server' ? '⚠️' :
                        variant === 'forbidden' ? '🔒' : '❌'}
            </div>

            <h3>{title}</h3>

            <p>{message}</p>

            {traceId && (
                <div className="error-state__trace">
                    <small>Trace ID: {traceId}</small>

                    <button type="button" onClick={handleCopyTraceId}>
                        Copy Trace ID
                    </button>
                </div>
            )}

            {onRetry && (
                <button
                    type="button"
                    className="error-state__retry"
                    onClick={onRetry}
                >
                    Try again
                </button>
            )}
        </div>
    );
};

export default ErrorState;
