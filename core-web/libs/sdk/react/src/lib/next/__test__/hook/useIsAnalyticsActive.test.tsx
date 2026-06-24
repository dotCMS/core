import { act, renderHook } from '@testing-library/react';

import { ANALYTICS_READY_EVENT, isDotAnalyticsActive } from '@dotcms/uve/internal';

import { useIsAnalyticsActive } from '../../hooks/useIsAnalyticsActive';

jest.mock('@dotcms/uve/internal', () => ({
    isDotAnalyticsActive: jest.fn(() => false),
    ANALYTICS_READY_EVENT: 'dotcms:analytics:ready'
}));

describe('useIsAnalyticsActive', () => {
    const isDotAnalyticsActiveMock = isDotAnalyticsActive as jest.Mock;

    beforeEach(() => {
        isDotAnalyticsActiveMock.mockReturnValue(false);
    });

    test('should return false when analytics is not active on mount', () => {
        const { result } = renderHook(() => useIsAnalyticsActive());

        expect(result.current).toBe(false);
    });

    test('should return true when analytics is already active on mount', () => {
        isDotAnalyticsActiveMock.mockReturnValue(true);

        const { result } = renderHook(() => useIsAnalyticsActive());

        expect(result.current).toBe(true);
    });

    test('should update to true when the analytics ready event fires', () => {
        const { result } = renderHook(() => useIsAnalyticsActive());

        expect(result.current).toBe(false);

        act(() => {
            isDotAnalyticsActiveMock.mockReturnValue(true);
            window.dispatchEvent(new CustomEvent(ANALYTICS_READY_EVENT));
        });

        expect(result.current).toBe(true);
    });

    test('should stop listening after unmount', () => {
        const removeSpy = jest.spyOn(window, 'removeEventListener');

        const { unmount } = renderHook(() => useIsAnalyticsActive());
        unmount();

        expect(removeSpy).toHaveBeenCalledWith(ANALYTICS_READY_EVENT, expect.any(Function));

        removeSpy.mockRestore();
    });
});
