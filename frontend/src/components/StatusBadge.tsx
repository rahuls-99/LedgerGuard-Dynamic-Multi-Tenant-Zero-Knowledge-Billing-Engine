
import React from 'react';
import './StatusBadge.css';

export type BadgeStatus =
    | 'SUCCESS'
    | 'FAILED'
    | 'PENDING'
    | 'ROLLED_BACK';

export interface StatusChipProps {
    status: string;
    size?: 'sm' | 'md';
    showIcon?: boolean;
    showTooltip?: boolean;
}

export interface StatusBadgeProps {
    status: BadgeStatus;
}

const STATUS_DETAILS: Record<
    BadgeStatus,
    {
        label: string;
        icon: string;
        className: string;
        tooltip: string;
    }
> = {
    SUCCESS: {
        label: 'Success',
        icon: '✓',
        className: 'success',
        tooltip: 'Success: the transaction was completed successfully.',
    },
    FAILED: {
        label: 'Failed',
        icon: '×',
        className: 'failed',
        tooltip: 'Failed: the charge was not completed.',
    },
    PENDING: {
        label: 'Pending',
        icon: '◷',
        className: 'pending',
        tooltip: 'Pending: the transaction is still being processed.',
    },
    ROLLED_BACK: {
        label: 'Rolled Back',
        icon: '↶',
        className: 'rolled-back',
        tooltip: 'Rolled back: the amount was reversed.',
    },
};

export const StatusChip: React.FC<StatusChipProps> = ({
    status,
    size = 'md',
    showIcon = true,
    showTooltip = true,
}) => {
    const tooltipId = React.useId();

    const details = STATUS_DETAILS[status as BadgeStatus];

    const label = details?.label || status || 'Unknown';
    const icon = details?.icon || '?';
    const className = details?.className || 'unknown';
    const tooltip =
        details?.tooltip || 'Unknown transaction status.';

    return (
        <span
            className={`status-badge status-badge--${className} status-badge--${size}`}
            tabIndex={showTooltip ? 0 : undefined}
            aria-describedby={showTooltip ? tooltipId : undefined}
        >
            {showIcon && (
                <span
                    className="status-badge__icon"
                    aria-hidden="true"
                >
                    {icon}
                </span>
            )}

            <span className="status-badge__label">
                {label}
            </span>

            {showTooltip && (
                <span
                    id={tooltipId}
                    className="status-badge__tooltip"
                    role="tooltip"
                >
                    {tooltip}
                </span>
            )}
        </span>
    );
};

// Backward compatibility for existing components
export const StatusBadge: React.FC<StatusBadgeProps> = ({
    status,
}) => {
    return <StatusChip status={status} />;
};
