
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { Select } from './Select';

describe('Select', () => {
    const options = [
        { label: 'Choose wallet', value: '' },
        { label: 'Main Wallet', value: 'main' },
        { label: 'Savings Wallet', value: 'savings' },
    ];

    it('renders the select label', () => {
        render(
            <Select
                id="wallet"
                label="Wallet"
                options={options}
                value="main"
                onChange={vi.fn()}
            />
        );

        expect(screen.getByRole('combobox')).toBeInTheDocument();
    });

    it('renders all options', () => {
        render(
            <Select
                id="wallet"
                label="Wallet"
                options={options}
                value="main"
                onChange={vi.fn()}
            />
        );

        expect(screen.getByRole('option', { name: 'Main Wallet' }))
            .toBeInTheDocument();

        expect(screen.getByRole('option', { name: 'Savings Wallet' }))
            .toBeInTheDocument();
    });

    it('calls onChange when selection changes', () => {
        const handleChange = vi.fn();

        render(
            <Select
                id="wallet"
                label="Wallet"
                options={options}
                value="main"
                onChange={handleChange}
            />
        );

        fireEvent.change(screen.getByRole('combobox'), {
            target: { value: 'savings' },
        });

        expect(handleChange).toHaveBeenCalledWith('savings');
    });

    it('displays an error message', () => {
        render(
            <Select
                id="wallet"
                label="Wallet"
                options={options}
                value=""
                onChange={vi.fn()}
                error="Please select a wallet"
            />
        );

        expect(screen.getByText('Please select a wallet'))
            .toBeInTheDocument();
    });

    it('supports the required attribute', () => {
        render(
            <Select
                id="wallet"
                label="Wallet"
                options={options}
                value=""
                onChange={vi.fn()}
                required
            />
        );

        expect(screen.getByRole('combobox')).toBeRequired();
    });

    it('can be disabled', () => {
        render(
            <Select
                id="wallet"
                label="Wallet"
                options={options}
                value="main"
                onChange={vi.fn()}
                disabled
            />
        );

        expect(screen.getByRole('combobox')).toBeDisabled();
    });
});
