import { jest } from '@jest/globals';

import React, { renderHook } from '@testing-library/react-hooks';
import { ReactNode, useContext } from 'react';

import DotContentAnalyticsContext from './DotContentAnalyticsContext';

import { DotContentAnalytics } from '../../dot-content-analytics';

jest.mock('../dot-content-analytics', () => {
    return jest.fn().mockImplementation(() => {
        return {};
    });
});

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
