
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { TransactionRow } from './TransactionRow';
import type { Transaction } from '../types/transaction';

const sampleTransaction: Transaction = {
    transactionId: 'TXN-123456789012345',
    type: 'PAYMENT',
    amount: '2500.00',
    currency: 'INR',
    status: 'SUCCESS',
    createdAt: '2026-11-23T10:30:00.000Z',
    failureReason: null,
};

describe('TransactionRow Component', () => {
    const handleToggle = vi.fn();
    const writeText = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();

        writeText.mockResolvedValue(undefined);

        Object.defineProperty(navigator, 'clipboard', {
            value: { writeText },
            configurable: true,
        });
    });

    it('displays transaction information', () => {
        render(
            <table>
                <tbody>
                    <TransactionRow
                        transaction={sampleTransaction}
                        expanded={false}
                        onToggle={handleToggle}
                        variant="table"
                    />
                </tbody>
            </table>
        );

        expect(screen.getByText('PAYMENT')).toBeInTheDocument();
        expect(screen.getByText('Success')).toBeInTheDocument();
        expect(screen.getByText(/TXN-1234/)).toBeInTheDocument();
    });

    it('calls onToggle when table row is clicked', () => {
        const { container } = render(
            <table>
                <tbody>
                    <TransactionRow
                        transaction={sampleTransaction}
                        expanded={false}
                        onToggle={handleToggle}
                        variant="table"
                    />
                </tbody>
            </table>
        );

        fireEvent.click(container.querySelector('.transaction-row')!);

        expect(handleToggle).toHaveBeenCalledTimes(1);
    });

    it('expands table row with Enter key', () => {
        const { container } = render(
            <table>
                <tbody>
                    <TransactionRow
                        transaction={sampleTransaction}
                        expanded={false}
                        onToggle={handleToggle}
                        variant="table"
                    />
                </tbody>
            </table>
        );

        fireEvent.keyDown(container.querySelector('.transaction-row')!, {
            key: 'Enter',
        });

        expect(handleToggle).toHaveBeenCalledTimes(1);
    });

    it('expands table row with Space key', () => {
        const { container } = render(
            <table>
                <tbody>
                    <TransactionRow
                        transaction={sampleTransaction}
                        expanded={false}
                        onToggle={handleToggle}
                        variant="table"
                    />
                </tbody>
            </table>
        );

        fireEvent.keyDown(container.querySelector('.transaction-row')!, {
            key: ' ',
        });

        expect(handleToggle).toHaveBeenCalledTimes(1);
    });

    it('displays full transaction details when expanded', () => {
        render(
            <table>
                <tbody>
                    <TransactionRow
                        transaction={sampleTransaction}
                        expanded={true}
                        onToggle={handleToggle}
                        variant="table"
                    />
                </tbody>
            </table>
        );

        expect(
            screen.getByText('TXN-123456789012345')
        ).toBeInTheDocument();

        expect(screen.getByText('UTC Time:')).toBeInTheDocument();
        expect(screen.getByText('Local Time:')).toBeInTheDocument();
    });

    it('copies transaction ID without expanding the row', async () => {
        render(
            <table>
                <tbody>
                    <TransactionRow
                        transaction={sampleTransaction}
                        expanded={false}
                        onToggle={handleToggle}
                        variant="table"
                    />
                </tbody>
            </table>
        );

        fireEvent.click(
            screen.getByRole('button', {
                name: 'Copy full transaction ID',
            })
        );

        expect(writeText).toHaveBeenCalledWith(
            'TXN-123456789012345'
        );

        expect(handleToggle).not.toHaveBeenCalled();
    });

    it('displays failure reason for failed transactions', () => {
        const failedTransaction: Transaction = {
            ...sampleTransaction,
            status: 'FAILED',
            failureReason: 'Insufficient funds',
        };

        render(
            <table>
                <tbody>
                    <TransactionRow
                        transaction={failedTransaction}
                        expanded={true}
                        onToggle={handleToggle}
                        variant="table"
                    />
                </tbody>
            </table>
        );

        expect(
            screen.getByText('Insufficient funds')
        ).toBeInTheDocument();
    });

    it('handles missing failure reason safely', () => {
        const failedTransaction: Transaction = {
            ...sampleTransaction,
            status: 'FAILED',
            failureReason: null,
        };

        render(
            <table>
                <tbody>
                    <TransactionRow
                        transaction={failedTransaction}
                        expanded={true}
                        onToggle={handleToggle}
                        variant="table"
                    />
                </tbody>
            </table>
        );

        expect(
            screen.getByText('No reason provided')
        ).toBeInTheDocument();
    });

    it('handles unknown transaction status safely', () => {
        const unknownTransaction: Transaction = {
            ...sampleTransaction,
            status: 'PROCESSING',
        };

        render(
            <table>
                <tbody>
                    <TransactionRow
                        transaction={unknownTransaction}
                        expanded={false}
                        onToggle={handleToggle}
                        variant="table"
                    />
                </tbody>
            </table>
        );

        expect(screen.getByText('PROCESSING')).toBeInTheDocument();
    });


    it('supports card variant and displays transaction details', () => {
        render(
            <TransactionRow
                transaction={sampleTransaction}
                expanded={false}
                onToggle={handleToggle}
                variant="card"
            />
        );

        expect(screen.getByText('TXN-1234...')).toBeInTheDocument();
        expect(screen.getByText('Success')).toBeInTheDocument();
        expect(screen.getByText('+₹2,500.00')).toBeInTheDocument();

        fireEvent.click(
            screen.getByRole('button', {
                name: /TXN-1234/,
            })
        );
        expect(handleToggle).toHaveBeenCalledTimes(1);
    });

});