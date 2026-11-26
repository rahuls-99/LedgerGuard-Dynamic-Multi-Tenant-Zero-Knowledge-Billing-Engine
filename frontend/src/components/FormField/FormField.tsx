import { cloneElement, isValidElement } from 'react';
import type { ReactElement, ReactNode } from 'react';
import './FormField.css';

interface FormFieldProps {
    id: string;
    label: string;
    children: ReactNode;
    hint?: string;
    error?: string;
    required?: boolean;
}

interface AccessibleControlProps {
    'aria-describedby'?: string;
    'aria-invalid'?: boolean;
}

export function FormField({
    id,
    label,
    children,
    hint,
    error,
    required = false,
}: FormFieldProps) {
    const hintId = `${id}-hint`;
    const errorId = `${id}-error`;

    const describedBy = [
        hint ? hintId : null,
        error ? errorId : null,
    ]
        .filter(Boolean)
        .join(' ') || undefined;

    const control = isValidElement(children)
        ? cloneElement(
            children as ReactElement<AccessibleControlProps>,
            {
                'aria-describedby': describedBy,
                'aria-invalid': error ? true : undefined,
            }
        )
        : children;

    return (
        <div className="form-field">
            <label className="form-field__label" htmlFor={id}>
                {label}
                {required && (
                    <span
                        className="form-field__required"
                        aria-label="required"
                    >
                        {' '}*
                    </span>
                )}
            </label>

            <div className="form-field__control">
                {control}
            </div>

            {hint && (
                <p className="form-field__hint" id={hintId}>
                    {hint}
                </p>
            )}

            {error && (
                <p
                    className="form-field__error"
                    id={errorId}
                    role="alert"
                >
                    {error}
                </p>
            )}
        </div>
    );
}