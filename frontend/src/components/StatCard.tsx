import React from 'react';
import './StatCard.css';

export interface StatCardProps {
    title: string;
    value: string | number;
    hint?: string;
    ariaLabel?: string;
}

const StatCard: React.FC<StatCardProps> = ({
    title,
    value,
    hint,
    ariaLabel,
}) => {
    return (
        <div
            className="stat-card"
            role="group"
            aria-label={ariaLabel || `${title}: ${value}`}
        >
            <h3 className="stat-card__title">{title}</h3>

            <p className="stat-card__value">{value}</p>

            {hint && (
                <p className="stat-card__hint">{hint}</p>
            )}
        </div>
    );
};

export default StatCard;