import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { renderHook } from '@testing-library/react';

import { UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

import { useContentAnalytics } from './useContentAnalytics';

import { initializeAnalytics } from '../internal';

// Mock dependencies
jest.mock('@dotcms/uve', () => ({
    getUVEState: jest.fn()
}));

jest.mock('../internal', () => ({
    initializeAnalytics: jest.fn()
}));

// Setup mocks
const mockGetUVEState = jest.mocked(getUVEState);
const mockInitializeAnalytics = jest.mocked(initializeAnalytics);
const mockTrack = jest.fn();
const mockPageView = jest.fn();
const mockConversion = jest.fn();

const mockConfig = {
    server: 'https://example.com',
    siteAuth: 'test-site-key',
    debug: false
};

describe('useContentAnalytics', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockInitializeAnalytics.mockReturnValue({
            track: mockTrack,
            pageView: mockPageView,
            conversion: mockConversion
        });
        mockGetUVEState.mockReturnValue(undefined);
    });

    it('tracks event with payload when outside editor', () => {
        const { result } = renderHook(() => useContentAnalytics(mockConfig));
        result.current.track('test-event', { data: 'test' });

        expect(mockTrack).toHaveBeenCalledWith('test-event', {
            data: 'test'
        });
    });

    it('tracks event without payload when outside editor', () => {
        const { result } = renderHook(() => useContentAnalytics(mockConfig));
        result.current.track('test-event');

        expect(mockTrack).toHaveBeenCalledWith('test-event', {});
    });

    it('returns no-op functions and warns when inside UVE editor', () => {
        const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
        mockInitializeAnalytics.mockReturnValue(null);
        mockGetUVEState.mockReturnValue({
            mode: UVE_MODE.EDIT,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: '1',
            dotCMSHost: 'https://demo.dotcms.com'
        });

        const { result } = renderHook(() => useContentAnalytics(mockConfig));

        // Should not throw, returns no-op functions
        expect(() => result.current.track('test-event', { data: 'test' })).not.toThrow();
        expect(() => result.current.pageView()).not.toThrow();
        expect(() => result.current.conversion('purchase')).not.toThrow();

        // Should warn about UVE
        expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('inside the UVE editor'));

        // No tracking calls should be made
        expect(mockTrack).not.toHaveBeenCalled();
        expect(mockPageView).not.toHaveBeenCalled();
        expect(mockConversion).not.toHaveBeenCalled();

        consoleSpy.mockRestore();
    });

    it('logs error when analytics fails to initialize outside UVE', () => {
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
        mockInitializeAnalytics.mockReturnValue(null);
        mockGetUVEState.mockReturnValue(undefined);

        const { result } = renderHook(() => useContentAnalytics(mockConfig));

        // Should not throw, returns no-op functions
        expect(() => result.current.track('test-event')).not.toThrow();

        // Should log config error
        expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('Failed to initialize'));

        consoleSpy.mockRestore();
    });

    it('memoizes instance when config does not change', () => {
        const { rerender } = renderHook(() => useContentAnalytics(mockConfig));

        // First render initializes
        expect(mockInitializeAnalytics).toHaveBeenCalledTimes(1);

        // Re-render with same config should not re-initialize
        rerender();
        expect(mockInitializeAnalytics).toHaveBeenCalledTimes(1);
    });

    it('re-initializes when server changes', () => {
        const { rerender } = renderHook((config) => useContentAnalytics(config), {
            initialProps: mockConfig
        });

        expect(mockInitializeAnalytics).toHaveBeenCalledTimes(1);

        // Change server
        rerender({ ...mockConfig, server: 'https://new-server.com' });
        expect(mockInitializeAnalytics).toHaveBeenCalledTimes(2);
    });

    it('re-initializes when siteAuth changes', () => {
        const { rerender } = renderHook((config) => useContentAnalytics(config), {
            initialProps: mockConfig
        });

        expect(mockInitializeAnalytics).toHaveBeenCalledTimes(1);

        // Change siteKey
        rerender({ ...mockConfig, siteAuth: 'new-site-key' });
        expect(mockInitializeAnalytics).toHaveBeenCalledTimes(2);
    });

    it('does not re-initialize when debug changes', () => {
        const { rerender } = renderHook((config) => useContentAnalytics(config), {
            initialProps: mockConfig
        });

        expect(mockInitializeAnalytics).toHaveBeenCalledTimes(1);

        // Change debug (should not trigger re-initialization in useMemo)
        rerender({ ...mockConfig, debug: true });
        expect(mockInitializeAnalytics).toHaveBeenCalledTimes(1);
    });

    describe('conversion', () => {
        it('tracks conversion with options when outside editor', () => {
            const { result } = renderHook(() => useContentAnalytics(mockConfig));
            result.current.conversion('purchase', { productId: '123', price: 99.99 });

            expect(mockConversion).toHaveBeenCalledWith('purchase', {
                productId: '123',
                price: 99.99
            });
        });

        it('tracks conversion without options when outside editor', () => {
            const { result } = renderHook(() => useContentAnalytics(mockConfig));
            result.current.conversion('signup');

            expect(mockConversion).toHaveBeenCalledWith('signup', {});
        });
    });
});
