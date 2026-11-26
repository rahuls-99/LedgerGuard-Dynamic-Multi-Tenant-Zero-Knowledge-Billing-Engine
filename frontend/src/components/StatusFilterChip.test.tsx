
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { StatusFilterChip } from './StatusFilterChip';

describe('StatusFilterChip Component', () => {
    it('displays the status label', () => {
        render(
            <StatusFilterChip
                status="SUCCESS"
                selected={false}
                onToggle={() => { }}
            />
        );

        expect(screen.getByText('SUCCESS')).toBeInTheDocument();
    });

    it('shows selected state correctly', () => {
        render(
            <StatusFilterChip
                status="SUCCESS"
                selected={true}
                onToggle={() => { }}
            />
        );

        expect(
            screen.getByRole('checkbox', { checked: true })
        ).toBeInTheDocument();
    });

    it('shows unselected state correctly', () => {
        render(
            <StatusFilterChip
                status="FAILED"
                selected={false}
                onToggle={() => { }}
            />
        );

        expect(
            screen.getByRole('checkbox', { checked: false })
        ).toBeInTheDocument();
    });

    it('calls onToggle when clicked', () => {
        const handleToggle = vi.fn();

        render(
            <StatusFilterChip
                status="PENDING"
                selected={false}
                onToggle={handleToggle}
            />
        );

        fireEvent.click(screen.getByRole('checkbox'));

        expect(handleToggle).toHaveBeenCalledWith('PENDING');
        expect(handleToggle).toHaveBeenCalledTimes(1);
    });

    it('displays the count when provided', () => {
        render(
            <StatusFilterChip
                status="SUCCESS"
                selected={false}
                count={12}
                onToggle={() => { }}
            />
        );

        expect(screen.getByText('12')).toBeInTheDocument();
    });

    it('formats status labels containing underscores', () => {
        render(
            <StatusFilterChip
                status="ROLLED_BACK"
                selected={false}
                onToggle={() => { }}
            />
        );

        expect(screen.getByText('ROLLED BACK')).toBeInTheDocument();
    });

    it('supports keyboard activation with Enter', async () => {
        const handleToggle = vi.fn();
        const user = userEvent.setup();

        render(
            <StatusFilterChip
                status="SUCCESS"
                selected={false}
                onToggle={handleToggle}
            />
        );

        const chip = screen.getByRole('checkbox');

        chip.focus();
        await user.keyboard('{Enter}');

        expect(handleToggle).toHaveBeenCalledWith('SUCCESS');
        expect(handleToggle).toHaveBeenCalledTimes(1);
    });
});