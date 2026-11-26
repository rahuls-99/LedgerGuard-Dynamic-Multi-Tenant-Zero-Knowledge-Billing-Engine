import './Select.css';

export interface SelectOption {
    label: string;
    value: string;
}

interface SelectProps {
    id: string;
    label: string;
    options: SelectOption[];
    value: string;
    onChange: (value: string) => void;
    error?: string;
    disabled?: boolean;
    required?: boolean;
    placeholder?: string;
}

export function Select({
    id,
    label,
    options,
    value,
    onChange,
    error,
    disabled = false,
    required = false,
    placeholder = 'Select an option',
}: SelectProps) {
    const errorId = `${id}-error`;

    return (
        <div className="custom-select">
            <label className="custom-select__label" htmlFor={id}>
                {label}
                {required && (
                    <span className="custom-select__required" aria-hidden="true">
                        {' '}*
                    </span>
                )}
            </label>

            <select
                id={id}
                className={`custom-select__control ${
                    error ? 'has-error' : ''
                }`}
                value={value}
                onChange={(event) => onChange(event.target.value)}
                disabled={disabled}
                required={required}
                aria-invalid={Boolean(error)}
                aria-describedby={error ? errorId : undefined}
            >
                <option value="" disabled>
                    {placeholder}
                </option>

                {options.map((option) => (
                    <option key={option.value} value={option.value}>
                        {option.label}
                    </option>
                ))}
            </select>

            {error && (
                <p
                    id={errorId}
                    className="custom-select__error"
                    role="alert"
                >
                    {error}
                </p>
            )}
        </div>
    );
}