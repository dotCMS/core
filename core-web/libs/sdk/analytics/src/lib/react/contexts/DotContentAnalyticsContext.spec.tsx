import { jest } from '@jest/globals';
import '@testing-library/jest-dom';
import { renderHook } from '@testing-library/react-hooks';
import { ReactNode, useContext } from 'react';

import DotContentAnalyticsContext from './DotContentAnalyticsContext';

import { DotAnalytics } from '../../dotAnalytics/shared/dot-content-analytics.model';

jest.mock('../../dotAnalytics/dot-content-analytics', () => ({
    DotContentAnalytics: {
        getInstance: jest.fn().mockImplementation(() => ({
            track: jest.fn(),
            ready: jest.fn(),
            logger: console,
            initialized: false
        }))
    }
}));

describe('useDotContentAnalyticsContext', () => {
    it('returns the context value null', () => {
        const mockContextValue = null;

        const { result } = renderHook(() => useContext(DotContentAnalyticsContext), {
            wrapper: ({ children }: { children: ReactNode }) => (
                <DotContentAnalyticsContext.Provider value={mockContextValue}>
                    {children}
                </DotContentAnalyticsContext.Provider>
            )
        });

        expect(result.current).toEqual(mockContextValue);
    });

    it('returns the context value DotContentAnalytics', () => {
        const mockContextValue = {} as DotAnalytics;

        const { result } = renderHook(() => useContext(DotContentAnalyticsContext), {
            wrapper: ({ children }: { children: ReactNode }) => (
                <DotContentAnalyticsContext.Provider value={mockContextValue}>
                    {children}
                </DotContentAnalyticsContext.Provider>
            )
        });

        expect(result.current).toEqual(mockContextValue);
    });
});
