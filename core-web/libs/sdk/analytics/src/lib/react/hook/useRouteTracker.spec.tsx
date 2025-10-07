import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { renderHook } from '@testing-library/react';
import { usePathname, useSearchParams } from 'next/navigation';

import { useRouterTracker } from './useRouterTracker';

import { DotCMSAnalytics } from '../../core/shared/dot-content-analytics.model';

// Mock Next.js hooks
jest.mock('next/navigation', () => ({
    usePathname: jest.fn(),
    useSearchParams: jest.fn(() => null)
}));

describe('useRouterTracker', () => {
    let mockAnalytics: jest.Mocked<DotCMSAnalytics>;
    const mockUsePathname = usePathname as jest.Mock;
    const mockUseSearchParams = useSearchParams as jest.Mock;

    beforeEach(() => {
        jest.clearAllMocks();
        mockAnalytics = {
            pageView: jest.fn()
        } as unknown as jest.Mocked<DotCMSAnalytics>;

        // Reset pathname to initial value
        mockUsePathname.mockReturnValue('/initial-path');
        mockUseSearchParams.mockReturnValue(null);
    });

    it('should not track when analytics is null', () => {
        renderHook(() => useRouterTracker(null));
        expect(mockAnalytics.pageView).not.toHaveBeenCalled();
    });

    it('should track page view when path changes', () => {
        const { rerender } = renderHook(() => useRouterTracker(mockAnalytics));
        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(1);

        // Simulate path change
        mockUsePathname.mockReturnValue('/new-path');
        rerender();

        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(2);
    });

    it('should not track page view when path remains the same', () => {
        const { rerender } = renderHook(() => useRouterTracker(mockAnalytics));
        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(1);

        // Simulate same path
        mockUsePathname.mockReturnValue('/initial-path');
        rerender();

        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(1);
    });

    it('should track when search params change', () => {
        const { rerender } = renderHook(() => useRouterTracker(mockAnalytics));
        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(1);

        // Simulate search params change
        mockUseSearchParams.mockReturnValue(new URLSearchParams('?q=test'));
        rerender();

        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(2);
    });

    it('should cleanup on unmount', () => {
        const { unmount } = renderHook(() => useRouterTracker(mockAnalytics));
        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(1);

        unmount();

        // Simulate path change after unmount
        mockUsePathname.mockReturnValue('/new-path');
        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(1);
    });
});
