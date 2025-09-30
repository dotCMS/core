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
            pageView: mockPageView
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

    it('does not track when inside editor', () => {
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
        result.current.track('test-event', { data: 'test' });

        expect(mockTrack).not.toHaveBeenCalled();
    });

    it('throws error when analytics fails to initialize', () => {
        const originalError = console.error;
        console.error = jest.fn();

        mockInitializeAnalytics.mockReturnValue(null);

        expect(() => {
            renderHook(() => useContentAnalytics(mockConfig));
        }).toThrow('Failed to initialize DotContentAnalytics');

        console.error = originalError;
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

    it('re-initializes when siteKey changes', () => {
        const { rerender } = renderHook((config) => useContentAnalytics(config), {
            initialProps: mockConfig
        });

        expect(mockInitializeAnalytics).toHaveBeenCalledTimes(1);

        // Change siteKey
        rerender({ ...mockConfig, siteKey: 'new-site-key' });
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
});
