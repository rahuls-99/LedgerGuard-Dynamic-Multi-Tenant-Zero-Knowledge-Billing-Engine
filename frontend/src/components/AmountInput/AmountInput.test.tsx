
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { AmountInput } from './AmountInput';

describe('AmountInput', () => {
    it('renders the input with the currency symbol', () => {
        render(
            <AmountInput
                value=""
                onChange={vi.fn()}
                currency="INR"
            />
        );

        expect(screen.getByRole('textbox')).toBeInTheDocument();
        expect(screen.getByText('₹')).toBeInTheDocument();
    });

    it('removes alphabetic characters', () => {
        const onChange = vi.fn();

        render(
            <AmountInput value="" onChange={onChange} currency="INR" />
        );

        fireEvent.change(screen.getByRole('textbox'), {
            target: { value: 'abc' },
        });

        expect(onChange).toHaveBeenCalledWith('');
    });

    it('removes the letter e from the amount', () => {
        const onChange = vi.fn();

        render(
            <AmountInput value="" onChange={onChange} currency="INR" />
        );

        fireEvent.change(screen.getByRole('textbox'), {
            target: { value: '1e5' },
        });

        expect(onChange).toHaveBeenCalledWith('15');
    });

    it('removes negative signs', () => {
        const onChange = vi.fn();

        render(
            <AmountInput value="" onChange={onChange} currency="INR" />
        );

        fireEvent.change(screen.getByRole('textbox'), {
            target: { value: '-5' },
        });

        expect(onChange).toHaveBeenCalledWith('5');
    });

    it('limits decimal places to two', () => {
        const onChange = vi.fn();

        render(
            <AmountInput value="" onChange={onChange} currency="INR" />
        );

        fireEvent.change(screen.getByRole('textbox'), {
            target: { value: '12.345' },
        });

        expect(onChange).toHaveBeenCalledWith('12.34');
    });

    it('removes extra decimal points', () => {
        const onChange = vi.fn();

        render(
            <AmountInput value="" onChange={onChange} currency="INR" />
        );

        fireEvent.change(screen.getByRole('textbox'), {
            target: { value: '1..2' },
        });

        expect(onChange).toHaveBeenCalledWith('1.2');
    });

    it('removes commas from pasted amounts', () => {
        const onChange = vi.fn();

        render(
            <AmountInput value="" onChange={onChange} currency="INR" />
        );

        fireEvent.change(screen.getByRole('textbox'), {
            target: { value: '1,234.50' },
        });

        expect(onChange).toHaveBeenCalledWith('1234.50');
    });

    it('displays the error message', () => {
        render(
            <AmountInput
                value=""
                onChange={vi.fn()}
                currency="INR"
                error="Please enter an amount"
            />
        );

        expect(
            screen.getByRole('alert')
        ).toHaveTextContent('Please enter an amount');
    });

    it('disables the input when disabled is true', () => {
        render(
            <AmountInput
                value=""
                onChange={vi.fn()}
                currency="INR"
                disabled
            />
        );

        expect(screen.getByRole('textbox')).toBeDisabled();
    });
});
