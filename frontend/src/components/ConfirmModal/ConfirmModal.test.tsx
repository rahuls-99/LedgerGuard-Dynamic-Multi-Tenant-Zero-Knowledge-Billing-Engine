
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ConfirmModal } from './ConfirmModal';

describe('ConfirmModal', () => {
    const defaultProps = {
        open: true,
        title: 'Confirm Payment',
        summary: <p>Payment amount: ₹500</p>,
        confirmLabel: 'Confirm',
        cancelLabel: 'Cancel',
        onConfirm: vi.fn(),
        onCancel: vi.fn(),
    };

    it('renders the modal with its title and summary', () => {
        render(<ConfirmModal {...defaultProps} />);

        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByText('Confirm Payment')).toBeInTheDocument();
        expect(screen.getByText('Payment amount: ₹500')).toBeInTheDocument();
    });

    it('does not render when closed', () => {
        render(<ConfirmModal {...defaultProps} open={false} />);

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('calls onConfirm when confirm is clicked', () => {
        const onConfirm = vi.fn();

        render(<ConfirmModal {...defaultProps} onConfirm={onConfirm} />);

        fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));

        expect(onConfirm).toHaveBeenCalledTimes(1);
    });

    it('calls onCancel when cancel is clicked', () => {
        const onCancel = vi.fn();

        render(<ConfirmModal {...defaultProps} onCancel={onCancel} />);

        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(onCancel).toHaveBeenCalledTimes(1);
    });

    it('calls onCancel when Escape is pressed', () => {
        const onCancel = vi.fn();

        render(<ConfirmModal {...defaultProps} onCancel={onCancel} />);

        fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });

        expect(onCancel).toHaveBeenCalledTimes(1);
    });

    it('does not close when Escape is pressed during loading', () => {
        const onCancel = vi.fn();

        render(
            <ConfirmModal {...defaultProps} loading onCancel={onCancel} />
        );

        fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });

        expect(onCancel).not.toHaveBeenCalled();
    });

    it('disables buttons while loading', () => {
        render(<ConfirmModal {...defaultProps} loading />);

        expect(screen.getByRole('button', { name: 'Processing...' }))
            .toBeDisabled();

        expect(screen.getByRole('button', { name: 'Cancel' }))
            .toBeDisabled();
    });
});
