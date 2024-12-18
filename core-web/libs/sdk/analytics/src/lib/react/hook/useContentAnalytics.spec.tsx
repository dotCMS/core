import { jest } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import { ReactNode } from 'react';

import { useContentAnalytics } from './useContentAnalytics';

import { isInsideEditor } from '../../dotAnalytics/shared/dot-content-analytics.utils';
import DotContentAnalyticsContext from '../contexts/DotContentAnalyticsContext';
jest.mock('../../dotAnalytics/shared/dot-content-analytics.utils', () => {
    return {
        isInsideEditor: jest.fn()
    };
});

const mockIsInsideEditor = jest.mocked(isInsideEditor);

const mockTrack = jest.fn();

interface WrapperProps {
    children: ReactNode;
}

const wrapper = ({ children }: WrapperProps) => (
    <DotContentAnalyticsContext.Provider
        value={{
            track: mockTrack
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
        mockIsInsideEditor.mockReturnValue(false);

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
        mockIsInsideEditor.mockReturnValue(false);

        const mockDate = '2024-01-01T00:00:00.000Z';
        jest.spyOn(Date.prototype, 'toISOString').mockReturnValue(mockDate);

        const { result } = renderHook(() => useContentAnalytics(), { wrapper });
        result.current.track('test-event');

        expect(mockTrack).toHaveBeenCalledWith('test-event', {
            timestamp: mockDate
        });
    });
});
