import { useEffect, useRef } from 'react';

export function useFocusTrap(
    containerRef: React.RefObject<HTMLElement | null>,
    active: boolean,
    initialFocusRef?: React.RefObject<HTMLElement | null>
) {
    const previousFocusRef = useRef<HTMLElement | null>(null);

    useEffect(() => {
        if (!active) {
            return;
        }

        previousFocusRef.current =
            document.activeElement instanceof HTMLElement
                ? document.activeElement
                : null;

        const container = containerRef.current;

        if (!container) {
            return;
        }

        const getFocusableElements = () => {
            return Array.from(
                container.querySelectorAll<HTMLElement>(
                    'button:not(:disabled), [href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'
                )
            ).filter((element) => {
                return element.getAttribute('aria-hidden') !== 'true';
            });
        };

        const initialElement =
            initialFocusRef?.current ?? getFocusableElements()[0];

        initialElement?.focus();

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key !== 'Tab') {
                return;
            }

            const focusableElements = getFocusableElements();

            if (focusableElements.length === 0) {
                event.preventDefault();
                container.focus();
                return;
            }

            const firstElement = focusableElements[0];
            const lastElement =
                focusableElements[focusableElements.length - 1];

            if (event.shiftKey) {
                if (
                    document.activeElement === firstElement ||
                    !container.contains(document.activeElement)
                ) {
                    event.preventDefault();
                    lastElement.focus();
                }
            } else if (
                document.activeElement === lastElement ||
                !container.contains(document.activeElement)
            ) {
                event.preventDefault();
                firstElement.focus();
            }
        };

        container.addEventListener('keydown', handleKeyDown);

        return () => {
            container.removeEventListener('keydown', handleKeyDown);
            previousFocusRef.current?.focus();
        };
    }, [active, containerRef, initialFocusRef]);
}