import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { act, renderHook } from '@testing-library/react';

import { useRouterTracker } from './useRouterTracker';

import { DotContentAnalytics } from '../../dotAnalytics/dot-content-analytics';

describe('useRouterTracker', () => {
    let mockAnalytics: jest.Mocked<DotContentAnalytics>;

    beforeEach(() => {
        jest.clearAllMocks();
        mockAnalytics = {
            pageView: jest.fn()
        } as unknown as jest.Mocked<DotContentAnalytics>;

        Object.defineProperty(window, 'location', {
            value: {
                pathname: '/initial-path',
                search: ''
            },
            writable: true,
            configurable: true
        });
    });

    it('should not track when analytics is null', () => {
        renderHook(() => useRouterTracker(null));
        expect(mockAnalytics.pageView).not.toHaveBeenCalled();
    });

    it('should track page view when path changes', () => {
        renderHook(() => useRouterTracker(mockAnalytics));

        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(1);

        act(() => {
            Object.defineProperty(window, 'location', {
                value: {
                    pathname: '/new-path',
                    search: ''
                },
                writable: true,
                configurable: true
            });

            window.dispatchEvent(new Event('popstate'));
        });

        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(2);
    });

    it('should not track page view when path remains the same', () => {
        renderHook(() => useRouterTracker(mockAnalytics));

        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(1);

        act(() => {
            Object.defineProperty(window, 'location', {
                value: {
                    pathname: '/initial-path',
                    search: ''
                },
                writable: true,
                configurable: true
            });

            window.dispatchEvent(new Event('popstate'));
        });

        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(1);
    });

    it('should cleanup event listener on unmount', () => {
        const { unmount } = renderHook(() => useRouterTracker(mockAnalytics));

        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(1);

        unmount();

        act(() => {
            Object.defineProperty(window, 'location', {
                value: {
                    pathname: '/new-path',
                    search: ''
                },
                writable: true,
                configurable: true
            });

            window.dispatchEvent(new Event('popstate'));
        });

        expect(mockAnalytics.pageView).toHaveBeenCalledTimes(1);
    });
});
