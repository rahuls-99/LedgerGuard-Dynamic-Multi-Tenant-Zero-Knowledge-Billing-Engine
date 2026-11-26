import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { SegmentedControl } from './SegmentedControl';

describe('SegmentedControl', () => {
    const options = [
        { label: 'All', value: 'all' },
        { label: 'Success', value: 'success' },
        { label: 'Failed', value: 'failed' },
    ];

    it('renders all options', () => {
        render(
            <SegmentedControl
                options={options}
                value="all"
                onChange={vi.fn()}
            />
        );

        expect(screen.getByText('All')).toBeInTheDocument();
        expect(screen.getByText('Success')).toBeInTheDocument();
        expect(screen.getByText('Failed')).toBeInTheDocument();
    });

    it('marks the selected option', () => {
        render(
            <SegmentedControl
                options={options}
                value="success"
                onChange={vi.fn()}
            />
        );

        expect(screen.getByRole('radio', { name: 'Success' }))
            .toHaveAttribute('aria-checked', 'true');
    });

    it('calls onChange when an option is clicked', () => {
        const handleChange = vi.fn();

        render(
            <SegmentedControl
                options={options}
                value="all"
                onChange={handleChange}
            />
        );

        fireEvent.click(screen.getByRole('radio', { name: 'Failed' }));

        expect(handleChange).toHaveBeenCalledWith('failed');
    });

    it('supports arrow-key navigation', () => {
        const handleChange = vi.fn();

        render(
            <SegmentedControl
                options={options}
                value="all"
                onChange={handleChange}
            />
        );

        fireEvent.keyDown(
            screen.getByRole('radio', { name: 'All' }),
            { key: 'ArrowRight' }
        );

        expect(handleChange).toHaveBeenCalledWith('success');
    });

    it('disables all options when disabled', () => {
        render(
            <SegmentedControl
                options={options}
                value="all"
                onChange={vi.fn()}
                disabled
            />
        );

        expect(screen.getByRole('radio', { name: 'All' }))
            .toBeDisabled();
    });
});