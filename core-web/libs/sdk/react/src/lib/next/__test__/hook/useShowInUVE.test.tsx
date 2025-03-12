import { renderHook } from '@testing-library/react-hooks';

import { getUVEState } from '@dotcms/uve';
import { UVE_MODE } from '@dotcms/uve/types';

import { useShowInUVE } from '../../hooks/useShowInUVE';

jest.mock('@dotcms/uve', () => ({
    getUVEState: jest.fn()
}));

describe('useShowInUVE', () => {
    const getUVEStateMock = getUVEState as jest.Mock;

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should return true when UVE mode matches the specified mode', () => {
        getUVEStateMock.mockReturnValue({ mode: UVE_MODE.EDIT });

        const { result } = renderHook(() => useShowInUVE(UVE_MODE.EDIT));

        expect(result.current).toBe(true);
    });

    test('should return false when UVE mode does not match the specified mode', () => {
        getUVEStateMock.mockReturnValue({ mode: UVE_MODE.LIVE });

        const { result } = renderHook(() => useShowInUVE(UVE_MODE.EDIT));

        expect(result.current).toBe(false);
    });

    test('should return false when UVE state is undefined', () => {
        getUVEStateMock.mockReturnValue(undefined);

        const { result } = renderHook(() => useShowInUVE(UVE_MODE.EDIT));

        expect(result.current).toBe(false);
    });

    test('should update when the specified mode changes', () => {
        getUVEStateMock.mockReturnValue({ mode: UVE_MODE.PREVIEW });

        const { result, rerender } = renderHook(({ mode }) => useShowInUVE(mode), {
            initialProps: { mode: UVE_MODE.EDIT }
        });

        expect(result.current).toBe(false);

        rerender({ mode: UVE_MODE.PREVIEW });
        expect(result.current).toBe(true);
    });
});
