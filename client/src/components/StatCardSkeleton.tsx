import React from 'react';
import { Skeleton } from './Skeleton';

export interface StatCardSkeletonProps {
    showIcon?: boolean;
}

export const StatCardSkeleton: React.FC<StatCardSkeletonProps> = ({
    showIcon = true,
}) => {
    return (
        <div
            className="stat-card-skeleton"
            aria-busy="true"
            aria-label="Loading statistic card"
            style={{
                display: 'flex',
                flexDirection: 'column',
                gap: '12px',
                padding: '16px',
                borderRadius: '10px',
                border: '1px solid var(--border)',
                background: 'var(--bg)',
                boxShadow: 'var(--shadow)',
                textAlign: 'left',
            }}
        >
            <div
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '12px',
                }}
            >
                <Skeleton variant="line" width="55%" height={14} />
                {showIcon && <Skeleton variant="circle" width={32} height={32} />}
            </div>
            <Skeleton variant="line" width="40%" height={28} />
            <Skeleton variant="line" width="70%" height={13} />
        </div>
    );
};