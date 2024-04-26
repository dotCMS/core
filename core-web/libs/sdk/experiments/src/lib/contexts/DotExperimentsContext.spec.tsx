import { renderHook } from '@testing-library/react-hooks';
import { ReactNode, useContext } from 'react';

import DotExperimentsContext from './DotExperimentsContext';

import { DotExperiments } from '../dot-experiments';

jest.mock('../dot-experiments', () => {
    return jest.fn().mockImplementation(() => {
        return {};
    });
});

describe('useDotExperimentsContext', () => {
    it('returns the context value null', () => {
        const mockContextValue = null;

        const { result } = renderHook(() => useContext(DotExperimentsContext), {
            wrapper: ({ children }: { children: ReactNode }) => (
                <DotExperimentsContext.Provider value={mockContextValue}>
                    {children}
                </DotExperimentsContext.Provider>
            )
        });

        expect(result.current).toEqual(mockContextValue);
    });

    it('returns the context value DotExperiment', () => {
        const mockContextValue = {} as DotExperiments;

        const { result } = renderHook(() => useContext(DotExperimentsContext), {
            wrapper: ({ children }: { children: ReactNode }) => (
                <DotExperimentsContext.Provider value={mockContextValue}>
                    {children}
                </DotExperimentsContext.Provider>
            )
        });

        expect(result.current).toEqual(mockContextValue);
    });
});
