
import React from 'react';
import type { Transaction } from '../types/transaction';
import { StatusChip } from './StatusBadge';
import './TransactionRow.css';
export interface TransactionRowProps {
    transaction: Transaction;
    expanded: boolean;
    onToggle: () => void;
    variant: 'table' | 'card';
}

// Format amounts without losing precision for very large values
function formatAmount(amount: string, currency: string): string {
    const match = amount.trim().match(/^([+-]?)(\d+)(?:\.(\d+))?$/);

    if (!match) {
        return `${currency} ${amount}`;
    }

    const sign = match[1] === '-' ? '-' : '+';
    const integerPart = match[2];
    const decimalPart = (match[3] || '').padEnd(2, '0');

    let groupedInteger: string;

    try {
        groupedInteger = BigInt(integerPart).toLocaleString('en-IN');
    } catch {
        return `${sign}${currency} ${amount}`;
    }

    try {
        const formatter = new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency,
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
        });

        const parts = formatter.formatToParts(0);
        const currencySymbol =
            parts.find((part) => part.type === 'currency')?.value || currency;

        return `${sign}${currencySymbol}${groupedInteger}.${decimalPart}`;
    } catch {
        return `${sign}${currency} ${groupedInteger}.${decimalPart}`;
    }
}

function formatDate(date: string): string {
    const value = new Date(date);

    if (Number.isNaN(value.getTime())) {
        return 'Invalid date';
    }

    return value.toLocaleDateString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
    });
}

function TransactionDetails({
    transaction,
}: {
    transaction: Transaction;
}) {
    const date = new Date(transaction.createdAt);
    const validDate = !Number.isNaN(date.getTime());

    return (
        <div className="transaction-details">
            <p>
                <strong>Full Transaction ID:</strong>{' '}
                {transaction.transactionId}
            </p>

            <p>
                <strong>UTC Time:</strong>{' '}
                {validDate ? date.toISOString() : 'Invalid date'}
            </p>

            <p>
                <strong>Local Time:</strong>{' '}
                {validDate ? date.toLocaleString() : 'Invalid date'}
            </p>

            {(transaction.status === 'FAILED' ||
                transaction.status === 'ROLLED_BACK') && (
                    <p className="transaction-details__reason">
                        <strong>Failure Reason:</strong>{' '}
                        {transaction.failureReason || 'No reason provided'}
                    </p>
                )}
        </div>
    );
}

const TransactionRowComponent: React.FC<TransactionRowProps> = ({
    transaction,
    expanded,
    onToggle,
    variant,
}) => {
    const shortId =
        transaction.transactionId.length > 12
            ? `${transaction.transactionId.slice(0, 8)}...`
            : transaction.transactionId;

    const handleCopy = async (
        event: React.MouseEvent<HTMLButtonElement>
    ) => {
        event.stopPropagation();

        try {
            await navigator.clipboard.writeText(
                transaction.transactionId
            );
        } catch {
            // Clipboard may be unavailable in some browsers.
        }
    };

    const handleKeyDown = (
        event: React.KeyboardEvent<HTMLTableRowElement>
    ) => {
        if (event.target !== event.currentTarget) {
            return;
        }

        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            onToggle();
        }
    };

    if (variant === 'card') {
        return (
            <article className="transaction-card">
                <button
                    type="button"
                    className="transaction-card__toggle"
                    aria-expanded={expanded}
                    onClick={onToggle}
                >
                    <span className="transaction-card__id">
                        {shortId}
                    </span>

                    <strong className="transaction-card__amount">
                        {formatAmount(
                            transaction.amount,
                            transaction.currency
                        )}
                    </strong>

                    <StatusChip status={transaction.status} />

                    <span>{formatDate(transaction.createdAt)}</span>
                </button>

                <button
                    type="button"
                    className="transaction-copy-button"
                    onClick={handleCopy}
                    aria-label="Copy full transaction ID"
                >
                    Copy ID
                </button>

                {expanded && (
                    <TransactionDetails transaction={transaction} />
                )}
            </article>
        );
    }

    return (
        <>
            <tr
                className="transaction-row"
                tabIndex={0}
                aria-expanded={expanded}
                onClick={onToggle}
                onKeyDown={handleKeyDown}
            >
                <td>
                    <span>{shortId}</span>{' '}
                    <button
                        type="button"
                        className="transaction-copy-button"
                        onClick={handleCopy}
                        aria-label="Copy full transaction ID"
                    >
                        Copy
                    </button>
                </td>

                <td>{transaction.type}</td>

                <td className="transaction-row__amount">
                    {formatAmount(
                        transaction.amount,
                        transaction.currency
                    )}
                </td>

                <td>
                    <StatusChip status={transaction.status} />
                </td>

                <td>{formatDate(transaction.createdAt)}</td>
            </tr>

            {expanded && (
                <tr className="transaction-row__details">
                    <td colSpan={5}>
                        <TransactionDetails transaction={transaction} />
                    </td>
                </tr>
            )}
        </>
    );
};

export const TransactionRow = React.memo(TransactionRowComponent);
