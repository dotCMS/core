import { jest } from '@jest/globals';
import '@testing-library/jest-dom';
import { renderHook } from '@testing-library/react-hooks';
import * as React from 'react';
import { ReactNode, useContext } from 'react';

import DotContentAnalyticsContext from './DotContentAnalyticsContext';

import { DotContentAnalytics } from '../../dotAnalytics/dot-content-analytics';

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
        const mockContextValue = {} as DotContentAnalytics;

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
