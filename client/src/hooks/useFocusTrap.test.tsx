import { render, screen, fireEvent, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { useRef, useState } from 'react';
import { useFocusTrap } from './useFocusTrap';

function FocusTrapTestComponent() {
    const [active, setActive] = useState(false);
    const containerRef = useRef<HTMLDivElement>(null);

    useFocusTrap(containerRef, active);

    return (
        <>
            <button onClick={() => setActive(true)}>
                Open Modal
            </button>

            {active && (
                <div ref={containerRef} tabIndex={-1}>
                    <button>First Button</button>
                    <button>Last Button</button>
                    <button onClick={() => setActive(false)}>
                        Close Modal
                    </button>
                </div>
            )}
        </>
    );
}

describe('useFocusTrap', () => {
    it('moves focus to the first element when activated', () => {
        render(<FocusTrapTestComponent />);

        fireEvent.click(screen.getByText('Open Modal'));

        expect(screen.getByText('First Button')).toHaveFocus();
    });

    it('keeps keyboard focus inside the container', () => {
        render(<FocusTrapTestComponent />);

        fireEvent.click(screen.getByText('Open Modal'));

        const lastButton = screen.getByText('Close Modal');
        const firstButton = screen.getByText('First Button');

        act(() => {
            lastButton.focus();
        });

        fireEvent.keyDown(lastButton, { key: 'Tab' });

        expect(firstButton).toHaveFocus();
    });

    it('moves focus to the last element with Shift + Tab', () => {
        render(<FocusTrapTestComponent />);

        fireEvent.click(screen.getByText('Open Modal'));

        const firstButton = screen.getByText('First Button');
        const lastButton = screen.getByText('Close Modal');

        fireEvent.keyDown(firstButton, {
            key: 'Tab',
            shiftKey: true,
        });

        expect(lastButton).toHaveFocus();
    });

    it('restores focus to the trigger when the modal closes', () => {
        render(<FocusTrapTestComponent />);

        const trigger = screen.getByText('Open Modal');

        trigger.focus();
        fireEvent.click(trigger);

        fireEvent.click(screen.getByText('Close Modal'));

        expect(trigger).toHaveFocus();
    });
});