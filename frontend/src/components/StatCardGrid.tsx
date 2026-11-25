import React from 'react';
import './StatCardGrid.css';
import StatCard from './StatCard';
import { StatCardSkeleton } from './StatCardSkeleton';
import ErrorState from './ErrorState';
import type { DashboardStats } from '../types/dashboard';
import {
    formatCurrency,
    formatNumber,
    formatRelativeTime,
} from '../utils/format';

interface StatCardGridProps {
    stats: DashboardStats | null;
    loading: boolean;
    error: string | null;
    onRetry: () => void;
    traceId?: string;
}

const StatCardGrid: React.FC<StatCardGridProps> = ({
    stats,
    loading,
    error,
    onRetry,
    traceId,
}) => {
    if (loading) {
        return (
            <div className="stat-card-grid" aria-busy="true" aria-label="Loading dashboard statistics">
                {[1, 2, 3, 4].map((item) => (
                    <StatCardSkeleton key={item} />
                ))}
            </div>
        );
    }

    if (error) {
        return (
            <ErrorState
                title="Unable to load dashboard statistics"
                message={error}
                traceId={traceId}
                onRetry={onRetry}
                variant="server"
            />
        );
    }

    const safeStats: DashboardStats = stats ?? {
        totalUsers: 0,
        totalTransactions: 0,
        totalWalletBalance: '0.00',
        currency: 'INR',
        lastTransactionAt: null,
    };

    const lastTransaction = safeStats.lastTransactionAt
        ? formatRelativeTime(safeStats.lastTransactionAt)
        : 'No transactions yet';

    return (
        <div
            className="stat-card-grid"
            role="group"
            aria-label="Dashboard statistics"
            aria-busy="false"
        >
            <StatCard
                title="Total Users"
                value={formatNumber(safeStats.totalUsers)}
                ariaLabel={`Total Users: ${safeStats.totalUsers}`}
            />

            <StatCard
                title="Total Transactions"
                value={formatNumber(safeStats.totalTransactions)}
                ariaLabel={`Total Transactions: ${safeStats.totalTransactions}`}
            />

            <StatCard
                title="Wallet Balance"
                value={formatCurrency(
                    safeStats.totalWalletBalance,
                    safeStats.currency
                )}
            />

            <StatCard
                title="Last Transaction"
                value={lastTransaction}
                hint={
                    safeStats.lastTransactionAt
                        ? 'Most recent transaction'
                        : 'Your transaction activity will appear here'
                }
            />
        </div>
    );
};

export default StatCardGrid;