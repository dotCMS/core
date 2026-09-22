import { renderHook } from '@testing-library/react';
import { Mock, vi } from 'vitest';

import { UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';
import { DEVELOPMENT_MODE, PRODUCTION_MODE } from '@dotcms/uve/internal';

import { DotCMSPageContext } from '../../contexts/DotCMSPageContext';
import { resolveDevMode, useIsDevMode, useResolvedDevMode } from '../../hooks/useIsDevMode';

vi.mock('@dotcms/uve', () => ({
    getUVEState: vi.fn()
}));

describe('useIsDevMode', () => {
    const getUVEStateMock = getUVEState as Mock;
    beforeEach(() => getUVEStateMock.mockReset());

    // The UVE-state-vs-renderer-mode resolution now lives in useResolvedDevMode, which
    // DotCMSPageProvider calls once per layout tree.
    describe('useResolvedDevMode', () => {
        describe('when outside editor', () => {
            beforeEach(() => {
                getUVEStateMock.mockReturnValue(null);
            });

            test('should return false when mode is production', () => {
                const { result } = renderHook(() => useResolvedDevMode(PRODUCTION_MODE));

                expect(result.current).toBe(false);
            });

            test('should return true when mode is development', () => {
                const { result } = renderHook(() => useResolvedDevMode(DEVELOPMENT_MODE));

                expect(result.current).toBe(true);
            });
        });

        describe('when inside UVE', () => {
            describe('when UVE is in edit mode', () => {
                beforeEach(() => {
                    getUVEStateMock.mockReturnValue({ mode: UVE_MODE.EDIT });
                });

                test('should return true when mode is production', () => {
                    const { result } = renderHook(() => useResolvedDevMode(PRODUCTION_MODE));

                    expect(result.current).toBe(true);
                });

                test('should return true when mode is development', () => {
                    const { result } = renderHook(() => useResolvedDevMode(DEVELOPMENT_MODE));

                    expect(result.current).toBe(true);
                });
            });

            describe('when UVE is in live or preview mode', () => {
                beforeEach(() => {
                    getUVEStateMock.mockReturnValue({ mode: UVE_MODE.LIVE });
                });

                test('should return false when mode is production', () => {
                    const { result } = renderHook(() => useResolvedDevMode(PRODUCTION_MODE));

                    expect(result.current).toBe(false);
                });

                test('should return false even when mode is development', () => {
                    const { result } = renderHook(() => useResolvedDevMode(DEVELOPMENT_MODE));

                    expect(result.current).toBe(false);
                });
            });
        });

        test('should resolve the UVE state once, not once per call', () => {
            getUVEStateMock.mockReturnValue({ mode: UVE_MODE.EDIT });

            renderHook(() => useResolvedDevMode(PRODUCTION_MODE));

            expect(getUVEStateMock).toHaveBeenCalledTimes(1);
        });
    });

    describe('resolveDevMode', () => {
        test('should follow the UVE mode when inside the editor', () => {
            getUVEStateMock.mockReturnValue({ mode: UVE_MODE.EDIT });

            expect(resolveDevMode(PRODUCTION_MODE)).toBe(true);
        });

        test('should fall back to the renderer mode when outside the editor', () => {
            getUVEStateMock.mockReturnValue(null);

            expect(resolveDevMode(DEVELOPMENT_MODE)).toBe(true);
            expect(resolveDevMode(PRODUCTION_MODE)).toBe(false);
        });
    });

    // The public hook is now a plain context read: the value is resolved once at the layout
    // root and shared, instead of every container and contentlet resolving its own.
    describe('useIsDevMode', () => {
        const wrapperWith = (isDevMode: boolean) =>
            function ContextWrapper({ children }: { children: React.ReactNode }) {
                return (
                    <DotCMSPageContext.Provider
                        value={
                            {
                                mode: PRODUCTION_MODE,
                                pageAsset: {},
                                userComponents: {},
                                isDevMode,
                                isAnalyticsActive: false
                            } as any
                        }>
                        {children}
                    </DotCMSPageContext.Provider>
                );
            };

        test('should read the shared value from the page context', () => {
            const { result } = renderHook(() => useIsDevMode(), {
                wrapper: wrapperWith(true)
            });

            expect(result.current).toBe(true);
        });

        test('should not resolve the UVE state itself', () => {
            renderHook(() => useIsDevMode(), { wrapper: wrapperWith(false) });

            expect(getUVEStateMock).not.toHaveBeenCalled();
        });
    });
});
