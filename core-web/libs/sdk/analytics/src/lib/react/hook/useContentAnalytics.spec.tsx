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

    it('tracks with timestamp when outside editor', () => {
        const mockDate = '2024-01-01T00:00:00.000Z';
        jest.spyOn(Date.prototype, 'toISOString').mockReturnValue(mockDate);

        const { result } = renderHook(() => useContentAnalytics(mockConfig));
        result.current.track('test-event', { data: 'test' });

        expect(mockTrack).toHaveBeenCalledWith('test-event', {
            data: 'test',
            timestamp: mockDate
        });
    });

    it('handles undefined payload', () => {
        const mockDate = '2024-01-01T00:00:00.000Z';
        jest.spyOn(Date.prototype, 'toISOString').mockReturnValue(mockDate);

        const { result } = renderHook(() => useContentAnalytics(mockConfig));
        result.current.track('test-event');

        expect(mockTrack).toHaveBeenCalledWith('test-event', {
            timestamp: mockDate
        });
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
});
