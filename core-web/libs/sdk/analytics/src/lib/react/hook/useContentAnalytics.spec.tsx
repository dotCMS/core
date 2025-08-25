import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { renderHook } from '@testing-library/react';

import { useContentAnalytics } from './useContentAnalytics';

import { getAnalyticsInstance, isInsideUVE } from '../internal';

jest.mock('../internal', () => ({
    isInsideUVE: jest.fn(),
    getAnalyticsInstance: jest.fn()
}));

const mockIsInsideUVE = jest.mocked(isInsideUVE);
const mockGetAnalyticsInstance = jest.mocked(getAnalyticsInstance);

const mockTrack = jest.fn();
const mockPageView = jest.fn();

describe('useContentAnalytics', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockGetAnalyticsInstance.mockReturnValue({
            track: mockTrack as (eventName: string, payload?: Record<string, unknown>) => void,
            pageView: mockPageView as () => void
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
        jest.resetAllMocks();
    });

    afterAll(() => {
        jest.restoreAllMocks();
    });

    it('tracks with timestamp when outside editor', () => {
        mockIsInsideUVE.mockReturnValue(false);

        const mockDate = '2024-01-01T00:00:00.000Z';
        jest.spyOn(Date.prototype, 'toISOString').mockReturnValue(mockDate);

        const { result } = renderHook(() => useContentAnalytics());
        result.current.track('test-event', { data: 'test' });

        expect(mockTrack).toHaveBeenCalledWith('test-event', {
            data: 'test',
            timestamp: mockDate
        });
    });

    it('handles undefined payload', () => {
        mockIsInsideUVE.mockReturnValue(false);

        const mockDate = '2024-01-01T00:00:00.000Z';
        jest.spyOn(Date.prototype, 'toISOString').mockReturnValue(mockDate);

        const { result } = renderHook(() => useContentAnalytics());
        result.current.track('test-event');

        expect(mockTrack).toHaveBeenCalledWith('test-event', {
            timestamp: mockDate
        });
    });

    it('does not track when inside editor', () => {
        mockIsInsideUVE.mockReturnValue(true);

        const { result } = renderHook(() => useContentAnalytics());
        result.current.track('test-event', { data: 'test' });

        expect(mockTrack).not.toHaveBeenCalled();
    });
});
