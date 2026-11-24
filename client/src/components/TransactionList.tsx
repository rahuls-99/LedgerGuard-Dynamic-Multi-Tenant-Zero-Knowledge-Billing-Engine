import React from 'react';
import type { Transaction } from '../types/transaction';
import { TransactionRow } from './TransactionRow';
import { EmptyState } from './EmptyState';
import ErrorState from './ErrorState';
import { TableSkeleton } from './TableSkeleton';
import { useMediaQuery } from '../hooks/useMediaQuery';
import './TransactionList.css';

export interface TransactionListProps {
    transactions: Transaction[];
    loading?: boolean;
    error?: string | null;
    traceId?: string;
    onRetry?: () => void;
}

export const TransactionList: React.FC<TransactionListProps> = ({
    transactions,
    loading = false,
    error = null,
    traceId,
    onRetry,
}) => {
    const isMobile = useMediaQuery('(max-width: 767px)');
    const [expandedId, setExpandedId] = React.useState<string | null>(null);

    const handleToggle = (transactionId: string) => {
        setExpandedId((currentId) =>
            currentId === transactionId ? null : transactionId
        );
    };

    if (error) {
        return (
            <ErrorState
                title="Unable to load transactions"
                message={error}
                traceId={traceId}
                onRetry={onRetry}
            />
        );
    }

    if (loading) {
        return (
            <div className="transaction-list__loading">
                <table className="data-table">
                    <TableSkeleton rows={5} columns={5} />
                </table>
            </div>
        );
    }

    if (transactions.length === 0) {
        return (
            <EmptyState
                icon="📄"
                title="No transactions found"
                description="There are no transactions to display."
                variant="no-data"
            />
        );
    }

    if (isMobile) {
        return (
            <div className="transaction-list transaction-list--mobile">
                {transactions.map((transaction) => (
                    <TransactionRow
                        key={transaction.transactionId}
                        transaction={transaction}
                        variant="card"
                        expanded={expandedId === transaction.transactionId}
                        onToggle={() =>
                            handleToggle(transaction.transactionId)
                        }
                    />
                ))}
            </div>
        );
    }

    return (
        <div className="transaction-list transaction-list--desktop">
            <table className="data-table transaction-list__table">
                <thead>
                    <tr>
                        <th scope="col">Transaction ID</th>
                        <th scope="col">Type</th>
                        <th scope="col">Amount</th>
                        <th scope="col">Status</th>
                        <th scope="col">Date</th>
                    </tr>
                </thead>

                <tbody>
                    {transactions.map((transaction) => (
                        <TransactionRow
                            key={transaction.transactionId}
                            transaction={transaction}
                            variant="table"
                            expanded={expandedId === transaction.transactionId}
                            onToggle={() =>
                                handleToggle(transaction.transactionId)
                            }
                        />
                    ))}
                </tbody>
            </table>
        </div>
    );
};