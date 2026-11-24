
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { SubmissionStatus } from './SubmissionStatus';

describe('SubmissionStatus', () => {
    it('displays the idle state', () => {
        render(<SubmissionStatus status="idle" />);

        expect(screen.getByText('Ready to submit')).toBeInTheDocument();
    });

    it('displays the submitting state', () => {
        render(<SubmissionStatus status="submitting" />);

        expect(
            screen.getByText('Submitting transaction...'),
        ).toBeInTheDocument();
    });

    it('displays the retrying state', () => {
        render(<SubmissionStatus status="retrying" />);

        expect(
            screen.getByText(
                'Still processing. Please keep this page open.',
            ),
        ).toBeInTheDocument();
    });

    it('displays the success state', () => {
        render(<SubmissionStatus status="success" />);

        expect(
            screen.getByText('Transaction completed successfully.'),
        ).toBeInTheDocument();
    });

    it('displays the failed state', () => {
        render(<SubmissionStatus status="failed" />);

        expect(
            screen.getByText('Transaction failed.'),
        ).toBeInTheDocument();
    });

    it('displays the unknown state', () => {
        render(<SubmissionStatus status="unknown" />);

        expect(
            screen.getByText(
                'We could not confirm the result. Retrying is safe.',
            ),
        ).toBeInTheDocument();
    });

    it('displays a custom message when provided', () => {
        render(
            <SubmissionStatus
                status="success"
                message="Payment received!"
            />,
        );

        expect(screen.getByText('Payment received!')).toBeInTheDocument();
    });

    it('calls onRetry when Retry is clicked', () => {
        const onRetry = vi.fn();

        render(
            <SubmissionStatus
                status="failed"
                onRetry={onRetry}
            />,
        );

        fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

        expect(onRetry).toHaveBeenCalledTimes(1);
    });

    it('calls onEdit when Edit is clicked', () => {
        const onEdit = vi.fn();

        render(
            <SubmissionStatus
                status="failed"
                onEdit={onEdit}
            />,
        );

        fireEvent.click(screen.getByRole('button', { name: 'Edit' }));

        expect(onEdit).toHaveBeenCalledTimes(1);
    });

    it('calls onViewLedger when View in ledger is clicked', () => {
        const onViewLedger = vi.fn();

        render(
            <SubmissionStatus
                status="success"
                onViewLedger={onViewLedger}
            />,
        );

        fireEvent.click(
            screen.getByRole('button', { name: 'View in ledger' }),
        );

        expect(onViewLedger).toHaveBeenCalledTimes(1);
    });

    it('does not display action buttons for submitting state', () => {
        render(
            <SubmissionStatus
                status="submitting"
                onRetry={vi.fn()}
                onEdit={vi.fn()}
                onViewLedger={vi.fn()}
            />,
        );

        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('has a polite live region for screen readers', () => {
        render(<SubmissionStatus status="success" />);

        const statusSection = screen
            .getByText('Transaction completed successfully.')
            .closest('section');

        expect(statusSection).toHaveAttribute('aria-live', 'polite');
        expect(statusSection).toHaveAttribute('aria-atomic', 'true');
    });
});
