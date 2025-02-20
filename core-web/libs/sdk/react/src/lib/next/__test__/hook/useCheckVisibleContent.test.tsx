import { render, screen, waitFor } from '@testing-library/react';
import { useRef } from 'react';

import { useCheckVisibleContent } from '../../hooks/useCheckVisibleContent';

const MOCK_DOM_RECT = {
    height: 0,
    width: 0,
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    x: 0,
    y: 0,
    toJSON: jest.fn()
};

const TestComponent = ({ height }: { height: number }) => {
    const ref = useRef<HTMLDivElement>(null);
    const haveContent = useCheckVisibleContent(ref);

    jest.spyOn(Element.prototype, 'getBoundingClientRect').mockReturnValue({
        ...MOCK_DOM_RECT,
        height
    });

    return (
        <div data-testid="container" ref={ref}>
            <span data-testid="result">{haveContent ? 'true' : 'false'}</span>
        </div>
    );
};

describe('useCheckVisibleContent hook', () => {
    test('should return false if ref is null (container not rendered)', async () => {
        const TestComponentWithNullRef = () => {
            const haveContent = useCheckVisibleContent({ current: null });

            return <span data-testid="result">{haveContent ? 'true' : 'false'}</span>;
        };

        render(<TestComponentWithNullRef />);

        const result = screen.getByTestId('result');
        expect(result.textContent).toBe('false');
    });

    test('should return false if height is 0', async () => {
        render(<TestComponent height={0} />);
        const result = await screen.findByTestId('result');
        await waitFor(() => expect(result.textContent).toBe('false'));
    });

    test('should return true if height is greater than 0', async () => {
        render(<TestComponent height={10} />);
        const result = await screen.findByTestId('result');
        await waitFor(() => expect(result.textContent).toBe('true'));
    });
});
