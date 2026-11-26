
import React from 'react';
import './ErrorBoundaryFallback.css';

interface ErrorBoundaryFallbackProps {
    message?: string;
    onRetry: () => void;
}

export const ErrorBoundaryFallback: React.FC<ErrorBoundaryFallbackProps> = ({
    message = 'Something went wrong while displaying this page.',
    onRetry,
}) => {
    return (
        <div className="error-boundary-fallback" role="alert">
            <h2>Oops! Something went wrong.</h2>

            <p>{message}</p>

            <div className="error-boundary-fallback__actions">
                <button type="button" onClick={onRetry}>
                    Try again
                </button>

                <button
                    type="button"
                    onClick={() => window.location.reload()}
                >
                    Reload page
                </button>
            </div>
        </div>
    );
};

export default ErrorBoundaryFallback;
