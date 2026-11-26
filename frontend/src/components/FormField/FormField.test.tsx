import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { FormField } from './FormField';

describe('FormField', () => {
    it('renders the label and input control', () => {
        render(
            <FormField id="email" label="Email">
                <input id="email" type="email" />
            </FormField>
        );

        expect(screen.getByLabelText('Email')).toBeInTheDocument();
    });

    it('shows the required marker', () => {
        render(
            <FormField id="name" label="Name" required>
                <input id="name" />
            </FormField>
        );

        expect(screen.getByLabelText('required')).toBeInTheDocument();
    });

    it('displays hint text and connects it to the input', () => {
        render(
            <FormField
                id="email"
                label="Email"
                hint="Enter your email address"
            >
                <input id="email" type="email" />
            </FormField>
        );

        const input = screen.getByLabelText('Email');

        expect(
            screen.getByText('Enter your email address')
        ).toBeInTheDocument();

        expect(input).toHaveAttribute(
            'aria-describedby',
            'email-hint'
        );
    });

    it('displays an error and connects it to the input', () => {
        render(
            <FormField
                id="email"
                label="Email"
                error="Invalid email address"
            >
                <input id="email" type="email" />
            </FormField>
        );

        const input = screen.getByLabelText('Email');

        expect(
            screen.getByRole('alert')
        ).toHaveTextContent('Invalid email address');

        expect(input).toHaveAttribute('aria-invalid', 'true');

        expect(input).toHaveAttribute(
            'aria-describedby',
            'email-error'
        );
    });

    it('connects both hint and error to the input', () => {
        render(
            <FormField
                id="email"
                label="Email"
                hint="Enter your email"
                error="Email is required"
            >
                <input id="email" type="email" />
            </FormField>
        );

        expect(screen.getByLabelText('Email')).toHaveAttribute(
            'aria-describedby',
            'email-hint email-error'
        );
    });
});