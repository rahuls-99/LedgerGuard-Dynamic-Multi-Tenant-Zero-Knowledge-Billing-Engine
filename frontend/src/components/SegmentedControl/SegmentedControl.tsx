import type { KeyboardEvent } from 'react';
import './SegmentedControl.css';

export interface SegmentedControlOption {
    label: string;
    value: string;
}

interface SegmentedControlProps {
    options: SegmentedControlOption[];
    value: string;
    onChange: (value: string) => void;
    disabled?: boolean;
}

export function SegmentedControl({
    options,
    value,
    onChange,
    disabled = false,
}: SegmentedControlProps) {
    const handleKeyDown = (event: KeyboardEvent<HTMLButtonElement>) => {
        if (
            event.key !== 'ArrowRight' &&
            event.key !== 'ArrowLeft'
        ) {
            return;
        }

        event.preventDefault();

        const currentIndex = options.findIndex(
            (option) => option.value === value
        );

        if (currentIndex === -1 || options.length === 0) {
            return;
        }

        const direction = event.key === 'ArrowRight' ? 1 : -1;

        const nextIndex =
            (currentIndex + direction + options.length) % options.length;

        onChange(options[nextIndex].value);
    };

    return (
        <div
            className="segmented-control"
            role="radiogroup"
            aria-label="Select an option"
            aria-disabled={disabled}
        >
            {options.map((option) => (
                <button
                    key={option.value}
                    type="button"
                    className={`segmented-control__option ${
                        value === option.value ? 'is-selected' : ''
                    }`}
                    role="radio"
                    aria-checked={value === option.value}
                    tabIndex={value === option.value ? 0 : -1}
                    disabled={disabled}
                    onClick={() => onChange(option.value)}
                    onKeyDown={handleKeyDown}
                >
                    {option.label}
                </button>
            ))}
        </div>
    );
}