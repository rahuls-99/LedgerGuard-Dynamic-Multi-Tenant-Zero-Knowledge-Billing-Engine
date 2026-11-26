
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { TransactionList } from './TransactionList';
import { useMediaQuery } from '../hooks/useMediaQuery';
import type { Transaction } from '../types/transaction';

vi.mock('../hooks/useMediaQuery', () => ({
    useMediaQuery: vi.fn(),
}));

const mockUseMediaQuery = vi.mocked(useMediaQuery);

const sampleTransaction: Transaction = {
    transactionId: 'TXN-123456789012345',
    type: 'PAYMENT',
    amount: '2500.00',
    currency: 'INR',
    status: 'SUCCESS',
    createdAt: '2026-11-23T10:30:00.000Z',
    failureReason: null,
};

describe('TransactionList Component', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockUseMediaQuery.mockReturnValue(false);
    });

    it('displays loading skeleton', () => {
        render(
            <TransactionList
                transactions={[]}
                loading={true}
            />
        );

        expect(screen.getByRole('table')).toBeInTheDocument();
    });

    it('displays empty state when no transactions exist', () => {
        render(
            <TransactionList transactions={[]} />
        );

        expect(
            screen.getByText('No transactions found')
        ).toBeInTheDocument();
    });

    it('displays error message when loading fails', () => {
        render(
            <TransactionList
                transactions={[]}
                error="Unable to connect to server"
                traceId="TRACE-123"
            />
        );

        expect(
            screen.getByText('Unable to connect to server')
        ).toBeInTheDocument();

        expect(screen.getByText(/TRACE-123/)).toBeInTheDocument();
    });

    it('displays transactions in desktop table layout', () => {
        mockUseMediaQuery.mockReturnValue(false);

        render(
            <TransactionList
                transactions={[sampleTransaction]}
            />
        );

        expect(
            screen.getByRole('columnheader', {
                name: 'Transaction ID',
            })
        ).toBeInTheDocument();

        expect(screen.getByText('PAYMENT')).toBeInTheDocument();
        expect(
            document.querySelector('.transaction-list--desktop')
        ).toBeInTheDocument();
    });

    it('displays transactions in mobile card layout', () => {
        mockUseMediaQuery.mockReturnValue(true);

        render(
            <TransactionList
                transactions={[sampleTransaction]}
            />
        );

        expect(
            document.querySelector('.transaction-list--mobile')
        ).toBeInTheDocument();

        expect(
            screen.getByRole('button', {
                name: /TXN-1234/,
            })
        ).toBeInTheDocument();
    });

    it('expands transaction details when clicked on desktop', () => {
        mockUseMediaQuery.mockReturnValue(false);

        render(
            <TransactionList
                transactions={[sampleTransaction]}
            />
        );

        fireEvent.click(screen.getByText('PAYMENT'));

        expect(
            screen.getByText('TXN-123456789012345')
        ).toBeInTheDocument();
    });

    it('expands transaction details when clicked on mobile', () => {
        mockUseMediaQuery.mockReturnValue(true);

        render(
            <TransactionList
                transactions={[sampleTransaction]}
            />
        );

        fireEvent.click(
            screen.getByRole('button', {
                name: /TXN-1234/,
            })
        );

        expect(
            screen.getByText('TXN-123456789012345')
        ).toBeInTheDocument();
    });
});
