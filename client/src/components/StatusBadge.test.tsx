
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatusChip } from './StatusBadge';

describe('StatusChip Component', () => {

    it('displays Success status correctly', () => {
        render(<StatusChip status="SUCCESS" />);

        expect(screen.getByText('Success')).toBeInTheDocument();
    });

    it('displays Failed status correctly', () => {
        render(<StatusChip status="FAILED" />);

        expect(screen.getByText('Failed')).toBeInTheDocument();
    });

    it('displays Pending status correctly', () => {
        render(<StatusChip status="PENDING" />);

        expect(screen.getByText('Pending')).toBeInTheDocument();
    });

    it('displays Rolled Back status correctly', () => {
        render(<StatusChip status="ROLLED_BACK" />);

        expect(screen.getByText('Rolled Back')).toBeInTheDocument();
    });

    it('displays the status icon by default', () => {
        render(<StatusChip status="SUCCESS" />);

        expect(screen.getByText('✓')).toBeInTheDocument();
    });

    it('hides the icon when showIcon is false', () => {
        render(<StatusChip status="SUCCESS" showIcon={false} />);

        expect(screen.queryByText('✓')).not.toBeInTheDocument();
        expect(screen.getByText('Success')).toBeInTheDocument();
    });

    it('displays tooltip by default', () => {
        render(<StatusChip status="SUCCESS" />);

        expect(
            screen.getByText(
                'Success: the transaction was completed successfully.'
            )
        ).toBeInTheDocument();
    });

    it('hides tooltip when showTooltip is false', () => {
        render(<StatusChip status="SUCCESS" showTooltip={false} />);

        expect(
            screen.queryByRole('tooltip')
        ).not.toBeInTheDocument();
    });

    it('handles unknown status safely', () => {
        const { container } = render(
            <StatusChip status="PROCESSING" />
        );

        expect(screen.getByText('PROCESSING')).toBeInTheDocument();

        expect(
            container.querySelector('.status-badge--unknown')
        ).toBeInTheDocument();
    });

    it('uses small size when size is sm', () => {
        const { container } = render(
            <StatusChip status="SUCCESS" size="sm" />
        );

        expect(
            container.querySelector('.status-badge--sm')
        ).toBeInTheDocument();
    });

});
