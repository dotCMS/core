import { renderHook } from '@testing-library/react';

import { UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';
import { DEVELOPMENT_MODE, PRODUCTION_MODE } from '@dotcms/uve/internal';

import { DotCMSPageContext } from '../../contexts/DotCMSPageContext';
import { useIsDevMode } from '../../hooks/useIsDevMode';

const Wrapper = ({ children, mode = 'production' }: any) => {
    return (
        <DotCMSPageContext.Provider value={{ mode, pageAsset: {} as any, userComponents: {} }}>
            {children}
        </DotCMSPageContext.Provider>
    );
};

jest.mock('@dotcms/uve', () => ({
    getUVEState: jest.fn()
}));

describe('useIsDevMode', () => {
    const getUVEStateMock = getUVEState as jest.Mock;
    beforeEach(() => getUVEStateMock.mockReset());

    describe('when outside editor', () => {
        beforeEach(() => {
            getUVEStateMock.mockReturnValue(null);
        });

        test('should return false when mode is production', () => {
            const { result } = renderHook(() => useIsDevMode(), {
                wrapper: ({ children }) => Wrapper({ children, mode: PRODUCTION_MODE })
            });

            expect(result.current).toBe(false);
        });

        test('should return true when mode is development', () => {
            const { result } = renderHook(() => useIsDevMode(), {
                wrapper: ({ children }) => Wrapper({ children, mode: DEVELOPMENT_MODE })
            });

            expect(result.current).toBe(true);
        });
    });

    describe('when inside UVE', () => {
        describe('when UVE is in edit mode', () => {
            beforeEach(() => {
                getUVEStateMock.mockReturnValue({ mode: UVE_MODE.EDIT });
            });

            test('should return true when mode is production', () => {
                const { result } = renderHook(() => useIsDevMode(), {
                    wrapper: ({ children }) => Wrapper({ children, mode: PRODUCTION_MODE })
                });

                expect(result.current).toBe(true);
            });

            test('should return true when mode is development', () => {
                const { result } = renderHook(() => useIsDevMode(), {
                    wrapper: ({ children }) => Wrapper({ children, mode: DEVELOPMENT_MODE })
                });

                expect(result.current).toBe(true);
            });
        });

        describe('when UVE is in live or preview mode', () => {
            beforeEach(() => {
                getUVEStateMock.mockReturnValue({ mode: UVE_MODE.LIVE });
            });

            test('should return false when mode is production', () => {
                const { result } = renderHook(() => useIsDevMode(), {
                    wrapper: ({ children }) => Wrapper({ children, mode: PRODUCTION_MODE })
                });

                expect(result.current).toBe(false);
            });

            test('should return false even when mode is development', () => {
                const { result } = renderHook(() => useIsDevMode(), {
                    wrapper: ({ children }) => Wrapper({ children, mode: DEVELOPMENT_MODE })
                });

                expect(result.current).toBe(false);
            });
        });
    });
});
