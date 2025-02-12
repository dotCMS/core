import { renderHook } from '@testing-library/react';

import { isInsideEditor } from '@dotcms/client';

import { DotCMSPageContext, RendererMode } from '../../contexts/DotCMSPageContext';
import { useIsDevMode } from '../../hooks/useIsDevMode';

const Wrapper = ({ children, mode = 'production' }: any) => {
    return (
        <DotCMSPageContext.Provider value={{ mode, pageAsset: {} as any, userComponents: {} }}>
            {children}
        </DotCMSPageContext.Provider>
    );
};

jest.mock('@dotcms/client', () => ({
    isInsideEditor: jest.fn()
}));

describe('useIsDevMode', () => {
    const isInsideEditorMock = isInsideEditor as jest.Mock;
    beforeEach(() => isInsideEditorMock.mockReset());

    describe('when outside editor', () => {
        beforeEach(() => {
            isInsideEditorMock.mockReturnValue(false);
        });

        test('should return false when mode is production', () => {
            const { result } = renderHook(() => useIsDevMode(), {
                wrapper: ({ children }) => Wrapper({ children, mode: 'production' })
            });

            expect(result.current).toBe(false);
        });

        test('should return true when mode is development', () => {
            const { result } = renderHook(() => useIsDevMode(), {
                wrapper: ({ children }) => Wrapper({ children, mode: 'development' })
            });

            expect(result.current).toBe(true);
        });
    });

    describe('when inside editor', () => {
        beforeEach(() => {
            isInsideEditorMock.mockReturnValue(true);
        });

        test('should return true when mode is production', () => {
            const { result } = renderHook(() => useIsDevMode(), {
                wrapper: ({ children }) => Wrapper({ children, mode: 'production' })
            });

            expect(result.current).toBe(true);
        });

        test('should return true when mode is development', () => {
            const { result } = renderHook(() => useIsDevMode(), {
                wrapper: ({ children }) => Wrapper({ children, mode: 'development' })
            });

            expect(result.current).toBe(true);
        });
    });

    test('should update when renderMode changes', () => {
        const { result, rerender } = renderHook(({ mode }) => useIsDevMode(mode as RendererMode), {
            wrapper: ({ children }) => Wrapper({ children, mode: 'production' }),
            initialProps: { mode: 'production' }
        });

        expect(result.current).toBe(false);

        rerender({ mode: 'development' });
        expect(result.current).toBe(true);
    });
});
