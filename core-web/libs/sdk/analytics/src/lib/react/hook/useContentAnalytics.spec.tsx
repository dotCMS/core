import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { renderHook } from '@testing-library/react';

import { useContentAnalytics } from './useContentAnalytics';

import DotContentAnalyticsContext from '../contexts/DotContentAnalyticsContext';
import { isInsideUVE } from '../internal';

jest.mock('../internal', () => ({
    isInsideUVE: jest.fn()
}));

const mockIsInsideUVE = jest.mocked(isInsideUVE);

const mockTrack = jest.fn();
const mockPageView = jest.fn();

interface WrapperProps {
    children: React.ReactNode;
}

const wrapper = ({ children }: WrapperProps) => (
    <DotContentAnalyticsContext.Provider
        value={{
            track: mockTrack,
            pageView: mockPageView
        }}>
        {children}
    </DotContentAnalyticsContext.Provider>
);

describe('useContentAnalytics', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    afterEach(() => {
        jest.clearAllMocks();
        jest.resetAllMocks();
    });

    afterAll(() => {
        jest.restoreAllMocks();
    });

    it('should track with timestamp when outside editor', () => {
        mockIsInsideUVE.mockReturnValue(false);

        const mockDate = '2024-01-01T00:00:00.000Z';
        jest.spyOn(Date.prototype, 'toISOString').mockReturnValue(mockDate);

        const { result } = renderHook(() => useContentAnalytics(), { wrapper });
        result.current.track('test-event', { data: 'test' });

        expect(mockTrack).toHaveBeenCalledWith('test-event', {
            data: 'test',
            timestamp: mockDate
        });
    });

    it('should handle undefined payload', () => {
        mockIsInsideUVE.mockReturnValue(false);

        const mockDate = '2024-01-01T00:00:00.000Z';
        jest.spyOn(Date.prototype, 'toISOString').mockReturnValue(mockDate);

        const { result } = renderHook(() => useContentAnalytics(), { wrapper });
        result.current.track('test-event');

        expect(mockTrack).toHaveBeenCalledWith('test-event', {
            timestamp: mockDate
        });
    });

    it('should not track when inside editor', () => {
        mockIsInsideUVE.mockReturnValue(true);

        const { result } = renderHook(() => useContentAnalytics(), { wrapper });
        result.current.track('test-event', { data: 'test' });

        expect(mockTrack).not.toHaveBeenCalled();
    });
});
