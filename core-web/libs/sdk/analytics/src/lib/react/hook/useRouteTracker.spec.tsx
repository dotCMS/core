import { renderHook } from '@testing-library/react';
import { usePathname, useSearchParams } from 'next/navigation';
import { Mock, Mocked, beforeEach, describe, expect, it, vi } from 'vitest';

import { useRouterTracker } from './useRouterTracker';

import { DotCMSAnalytics } from '../../core/shared/models';

// Mock Next.js hooks
vi.mock('next/navigation', () => ({
    usePathname: vi.fn(),
    useSearchParams: vi.fn(() => null)
}));

describe('useRouterTracker', () => {
    let mockAnalytics: Mocked<DotCMSAnalytics>;
    const mockUsePathname = usePathname as Mock;
    const mockUseSearchParams = useSearchParams as Mock;

    beforeEach(() => {
        vi.clearAllMocks();
        mockAnalytics = {
            pageView: vi.fn()
        } as unknown as Mocked<DotCMSAnalytics>;

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

    it('should not track when enabled is false, even after a route change', () => {
        const { rerender } = renderHook(() => useRouterTracker(mockAnalytics, false, false));
        expect(mockAnalytics.pageView).not.toHaveBeenCalled();

        // Simulate path change
        mockUsePathname.mockReturnValue('/new-path');
        rerender();

        expect(mockAnalytics.pageView).not.toHaveBeenCalled();
    });

    it('should track on render and unique route changes when enabled is explicitly true', () => {
        const { rerender } = renderHook(() => useRouterTracker(mockAnalytics, false, true));
        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(1);

        // Unique route change
        mockUsePathname.mockReturnValue('/new-path');
        rerender();
        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(2);

        // Same route, no additional pageView
        mockUsePathname.mockReturnValue('/new-path');
        rerender();
        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(2);
    });

    it('should not crash and not track when enabled is false and analytics is null', () => {
        const { rerender } = renderHook(() => useRouterTracker(null, false, false));
        expect(mockAnalytics.pageView).not.toHaveBeenCalled();

        mockUsePathname.mockReturnValue('/new-path');
        rerender();

        expect(mockAnalytics.pageView).not.toHaveBeenCalled();
    });
});
