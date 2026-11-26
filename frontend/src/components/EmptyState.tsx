
import React from 'react';
import './EmptyState.css';

export type EmptyStateVariant =
    | 'no-data'
    | 'no-results'
    | 'first-time';

interface EmptyStateProps {
    icon?: string;
    title: string;
    description: string;
    variant?: EmptyStateVariant;
    actionLabel?: string;
    onAction?: () => void;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
    icon = '📭',
    title,
    description,
    variant = 'no-data',
    actionLabel,
    onAction,
}) => {
    const displayIcon =
        variant === 'no-results' ? '🔍' :
            variant === 'first-time' ? '✨' :
                icon;

    return (
        <div className="empty-state" role="status">
            <div className="empty-state__icon" aria-hidden="true">
                {displayIcon}
            </div>

            <h3>{title}</h3>

            <p>{description}</p>

            {actionLabel && onAction && (
                <button
                    type="button"
                    className="empty-state__button"
                    onClick={onAction}
                >
                    {actionLabel}
                </button>
            )}
        </div>
    );
};
