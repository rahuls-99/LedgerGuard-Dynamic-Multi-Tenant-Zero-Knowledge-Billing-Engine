
import React from 'react';
import './InlineError.css';

interface InlineErrorProps {
    message: string;
    className?: string;
}

export const InlineError: React.FC<InlineErrorProps> = ({
    message,
    className = '',
}) => {
    if (!message) return null;

    return (
        <p
            className={`inline-error ${className}`}
            role="alert"
        >
            {message}
        </p>
    );
};

interface FieldErrorProps {
    message?: string;
    id?: string;
}

export const FieldError: React.FC<FieldErrorProps> = ({
    message,
    id,
}) => {
    if (!message) return null;

    return (
        <p
            id={id}
            className="field-error"
            role="alert"
        >
            {message}
        </p>
    );
};
