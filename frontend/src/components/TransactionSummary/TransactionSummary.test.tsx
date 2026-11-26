import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { TransactionSummary } from './TransactionSummary';

describe('TransactionSummary', () => {
    const defaultProps = {
        type: 'Payment',
        wallet: 'Main Wallet',
        amount: '1234.50',
        currency: 'INR',
        description: 'Monthly subscription payment',
    };

    it('displays the transaction type', () => {
        render(<TransactionSummary {...defaultProps} />);

        expect(screen.getByText('Payment')).toBeInTheDocument();
    });

    it('displays the wallet name', () => {
        render(<TransactionSummary {...defaultProps} />);

        expect(screen.getByText('Main Wallet')).toBeInTheDocument();
    });

    it('displays the formatted transaction amount', () => {
        render(<TransactionSummary {...defaultProps} />);

        expect(screen.getByText(/1,234\.50/)).toBeInTheDocument();
    });

    it('displays the currency', () => {
        render(<TransactionSummary {...defaultProps} />);

        expect(screen.getByText('INR')).toBeInTheDocument();
    });

    it('displays the transaction description', () => {
        render(<TransactionSummary {...defaultProps} />);

        expect(
            screen.getByText('Monthly subscription payment'),
        ).toBeInTheDocument();
    });

    it('displays fallback text when description is empty', () => {
        render(
            <TransactionSummary
                {...defaultProps}
                description=""
            />,
        );

        expect(
            screen.getByText('No description provided'),
        ).toBeInTheDocument();
    });

    it('sets the description as a tooltip', () => {
        render(<TransactionSummary {...defaultProps} />);

        expect(
            screen.getByText('Monthly subscription payment'),
        ).toHaveAttribute('title', 'Monthly subscription payment');
    });

    it('displays all transaction summary labels', () => {
        render(<TransactionSummary {...defaultProps} />);

        expect(screen.getByText('Transaction Type')).toBeInTheDocument();
        expect(screen.getByText('Wallet')).toBeInTheDocument();
        expect(screen.getByText('Amount')).toBeInTheDocument();
        expect(screen.getByText('Currency')).toBeInTheDocument();
        expect(screen.getByText('Description')).toBeInTheDocument();
    });
});