import './SubmissionStatus.css';

export type SubmissionState =
    | 'idle'
    | 'submitting'
    | 'retrying'
    | 'success'
    | 'failed'
    | 'unknown';

interface SubmissionStatusProps {
    status: SubmissionState;
    message?: string;
    onRetry?: () => void;
    onViewLedger?: () => void;
    onEdit?: () => void;
}

const statusDetails: Record<
    SubmissionState,
    { icon: string; label: string }
> = {
    idle: {
        icon: '○',
        label: 'Ready to submit',
    },
    submitting: {
        icon: '◌',
        label: 'Submitting transaction...',
    },
    retrying: {
        icon: '↻',
        label: 'Still processing. Please keep this page open.',
    },
    success: {
        icon: '✓',
        label: 'Transaction completed successfully.',
    },
    failed: {
        icon: '✕',
        label: 'Transaction failed.',
    },
    unknown: {
        icon: '?',
        label: 'We could not confirm the result. Retrying is safe.',
    },
};

export function SubmissionStatus({
    status,
    message,
    onRetry,
    onViewLedger,
    onEdit,
}: SubmissionStatusProps) {
    const details = statusDetails[status];

    return (
        <section
            className={`submission-status submission-status--${status}`}
            aria-live="polite"
            aria-atomic="true"
        >
            <div className="submission-status__content">
                <span
                    className="submission-status__icon"
                    aria-hidden="true"
                >
                    {details.icon}
                </span>

                <div className="submission-status__text">
                    <p className="submission-status__message">
                        {message || details.label}
                    </p>
                </div>
            </div>

            {status === 'failed' || status === 'unknown' ? (
                <div className="submission-status__actions">
                    {onRetry && (
                        <button type="button" onClick={onRetry}>
                            Retry
                        </button>
                    )}

                    {onEdit && (
                        <button type="button" onClick={onEdit}>
                            Edit
                        </button>
                    )}
                </div>
            ) : null}

            {status === 'success' && onViewLedger && (
                <div className="submission-status__actions">
                    <button type="button" onClick={onViewLedger}>
                        View in ledger
                    </button>
                </div>
            )}
        </section>
    );
}