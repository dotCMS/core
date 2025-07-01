import { jest } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import { ReactNode } from 'react';

import { UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

import { useContentAnalytics } from './useContentAnalytics';

import DotContentAnalyticsContext from '../contexts/DotContentAnalyticsContext';

jest.mock('@dotcms/uve', () => ({
    getUVEState: jest.fn()
}));

const mockGetUVEState = jest.mocked(getUVEState);

const mockTrack = jest.fn();
const mockPageView = jest.fn();

interface WrapperProps {
    children: ReactNode;
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
        mockGetUVEState.mockReturnValue(undefined);

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
        mockGetUVEState.mockReturnValue(undefined);

        const mockDate = '2024-01-01T00:00:00.000Z';
        jest.spyOn(Date.prototype, 'toISOString').mockReturnValue(mockDate);

        const { result } = renderHook(() => useContentAnalytics(), { wrapper });
        result.current.track('test-event');

        expect(mockTrack).toHaveBeenCalledWith('test-event', {
            timestamp: mockDate
        });
    });

    it('should not track when inside editor', () => {
        mockGetUVEState.mockReturnValue({
            mode: UVE_MODE.EDIT,
            persona: null,
            variantName: null,
            experimentId: null,
            publishDate: null,
            languageId: null,
            dotCMSHost: null
        });

        const { result } = renderHook(() => useContentAnalytics(), { wrapper });
        result.current.track('test-event', { data: 'test' });

        expect(mockTrack).not.toHaveBeenCalled();
    });
});
