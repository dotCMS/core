import { renderHook } from '@testing-library/react-hooks';
import { ReactNode, useContext } from 'react';

import DotExperimentsContext from './DotExperimentsContext';

jest.mock('../dot-experiments', () => {
    return jest.fn().mockImplementation(() => {
        return {};
    });
});

const mockContextValue = null;

describe('useDotExperimentsContext', () => {
    it('returns the context value null', () => {
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
        //
    });
});
