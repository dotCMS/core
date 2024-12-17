import { jest } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React, { ReactNode } from 'react';

import { useContentAnalytics } from './useContentAnalytics';

import DotContentAnalyticsContext from '../contexts/DotContentAnalyticsContext';

interface WrapperProps {
    children: ReactNode;
}

const mockTrack = jest.fn();

const wrapper = ({ children }: WrapperProps) => (
    <DotContentAnalyticsContext.Provider
        value={{
            track: mockTrack
        }}>
        {children}
    </DotContentAnalyticsContext.Provider>
);

const mockIsInsideEditor = jest.fn();

jest.mock('../../dotAnalytics/shared/dot-content-analytics.utils', () => ({
    isInsideEditor: mockIsInsideEditor
}));

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
        mockIsInsideEditor.mockImplementation(() => false);

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
        mockIsInsideEditor.mockImplementation(() => false);

        const mockDate = '2024-01-01T00:00:00.000Z';
        jest.spyOn(Date.prototype, 'toISOString').mockReturnValue(mockDate);

        const { result } = renderHook(() => useContentAnalytics(), { wrapper });
        result.current.track('test-event');

        expect(mockTrack).toHaveBeenCalledWith('test-event', {
            timestamp: mockDate
        });
    });
});
