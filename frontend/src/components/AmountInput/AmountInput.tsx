
import { useMemo } from 'react';
import type { ChangeEvent, KeyboardEvent, WheelEvent } from 'react';
import './AmountInput.css';

interface AmountInputProps {
    value: string;
    onChange: (value: string) => void;
    currency: string;
    maxDecimals?: number;
    maxIntegerDigits?: number;
    error?: string;
    disabled?: boolean;
}

function sanitizeAmount(
    input: string,
    maxDecimals: number,
    maxIntegerDigits: number
): string {
    let cleaned = input.replace(/,/g, '');

    cleaned = cleaned.replace(/[^\d.]/g, '');

    const decimalIndex = cleaned.indexOf('.');

    if (decimalIndex !== -1) {
        cleaned =
            cleaned.slice(0, decimalIndex + 1) +
            cleaned.slice(decimalIndex + 1).replace(/\./g, '');
    }

    const parts = cleaned.split('.');
    const integerPart = parts[0].slice(0, maxIntegerDigits);

    if (parts.length === 1) {
        return integerPart;
    }

    const decimalPart = parts[1].slice(0, maxDecimals);

    return `${integerPart}.${decimalPart}`;
}

function getCurrencySymbol(currency: string): string {
    try {
        const parts = new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency,
        }).formatToParts(0);

        return parts.find((part) => part.type === 'currency')?.value ?? currency;
    } catch {
        return currency;
    }
}

function formatPreview(value: string, currency: string): string {
    if (!value || value === '.') {
        return '';
    }

    try {
        const [integerPart, decimalPart] = value.split('.');
        const wholeAmount = BigInt(integerPart || '0');

        const formatter = new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency,
            maximumFractionDigits: 0,
        });

        const formattedInteger = formatter
            .formatToParts(wholeAmount)
            .map((part) => part.value)
            .join('');

        if (value.includes('.')) {
            return `${formattedInteger}.${decimalPart ?? ''}`;
        }

        return formattedInteger;
    } catch {
        return '';
    }
}

export function AmountInput({
    value,
    onChange,
    currency,
    maxDecimals = 2,
    maxIntegerDigits = 13,
    error,
    disabled = false,
}: AmountInputProps) {
    const currencySymbol = useMemo(
        () => getCurrencySymbol(currency),
        [currency]
    );

    const preview = useMemo(
        () => formatPreview(value, currency),
        [value, currency]
    );

    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
        const sanitized = sanitizeAmount(
            event.target.value,
            maxDecimals,
            maxIntegerDigits
        );

        onChange(sanitized);
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
            event.preventDefault();
        }
    };

    const handleWheel = (event: WheelEvent<HTMLInputElement>) => {
        event.preventDefault();
    };

    return (
        <div className="amount-input">
            <div className="amount-input__wrapper">
                <span
                    className="amount-input__currency"
                    aria-hidden="true"
                >
                    {currencySymbol}
                </span>

                <input
                    type="text"
                    inputMode="decimal"
                    value={value}
                    onChange={handleChange}
                    onKeyDown={handleKeyDown}
                    onWheel={handleWheel}
                    disabled={disabled}
                    aria-invalid={error ? true : undefined}
                    aria-describedby={error ? 'amount-input-error' : undefined}
                    className="amount-input__field"
                    placeholder="0.00"
                />
            </div>

            {preview && (
                <p className="amount-input__preview">
                    Preview: {preview}
                </p>
            )}

            {error && (
                <p
                    className="amount-input__error"
                    id="amount-input-error"
                    role="alert"
                >
                    {error}
                </p>
            )}
        </div>
    );
}
