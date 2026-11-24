
import React from 'react';
import './StatusFilterChip.css';

export interface StatusFilterChipProps {
    status: string;
    selected: boolean;
    onToggle: (status: string) => void;
    count?: number;
}

export const StatusFilterChip: React.FC<StatusFilterChipProps> = ({
    status,
    selected,
    onToggle,
    count,
}) => {
    const label = status.replace(/_/g, ' ');

    return (
        <button
            type="button"
            className={`status-filter-chip ${selected ? 'status-filter-chip--selected' : ''
                }`}
            role="checkbox"
            aria-checked={selected}
            aria-label={`${label} filter${count !== undefined ? `, ${count} transactions` : ''
                }`}
            onClick={() => onToggle(status)}
        >
            <span>{label}</span>

            {count !== undefined && (
                <span
                    className="status-filter-chip__count"
                    aria-hidden="true"
                >
                    {count}
                </span>
            )}
        </button>
    );
};
