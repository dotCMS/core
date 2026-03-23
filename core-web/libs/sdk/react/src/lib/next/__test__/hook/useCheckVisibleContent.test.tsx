import { act, render, screen } from '@testing-library/react';
import { useRef } from 'react';

import { useCheckVisibleContent } from '../../hooks/useCheckVisibleContent';

type MockResizeObserverCallback = (entries: Partial<ResizeObserverEntry>[]) => void;

let resizeObserverCallback: MockResizeObserverCallback;
const mockObserve = jest.fn();
const mockDisconnect = jest.fn();

const TestComponent = () => {
    const ref = useRef<HTMLDivElement>(null);
    const haveContent = useCheckVisibleContent(ref);

    return (
        <div data-testid="container" ref={ref}>
            <span data-testid="result">{haveContent ? 'true' : 'false'}</span>
        </div>
    );
};

describe('useCheckVisibleContent hook', () => {
    beforeEach(() => {
        global.ResizeObserver = jest.fn((callback: MockResizeObserverCallback) => {
            resizeObserverCallback = callback;

            return {
                observe: mockObserve,
                disconnect: mockDisconnect,
                unobserve: jest.fn()
            };
        }) as unknown as typeof ResizeObserver;
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    test('should return false if ref is null (container not rendered)', () => {
        const TestComponentWithNullRef = () => {
            const haveContent = useCheckVisibleContent({ current: null });

            return <span data-testid="result">{haveContent ? 'true' : 'false'}</span>;
        };

        render(<TestComponentWithNullRef />);

        const result = screen.getByTestId('result');
        expect(result.textContent).toBe('false');
        // ResizeObserver should not be set up when ref is null
        expect(mockObserve).not.toHaveBeenCalled();
    });

    test('should return false if height is 0', () => {
        render(<TestComponent />);

        act(() => {
            resizeObserverCallback([{ contentRect: { height: 0 } as DOMRectReadOnly }]);
        });

        const result = screen.getByTestId('result');
        expect(result.textContent).toBe('false');
    });

    test('should return true if height is greater than 0', () => {
        render(<TestComponent />);

        act(() => {
            resizeObserverCallback([{ contentRect: { height: 10 } as DOMRectReadOnly }]);
        });

        const result = screen.getByTestId('result');
        expect(result.textContent).toBe('true');
    });

    test('should disconnect ResizeObserver on unmount', () => {
        const { unmount } = render(<TestComponent />);
        unmount();
        expect(mockDisconnect).toHaveBeenCalled();
    });
});
