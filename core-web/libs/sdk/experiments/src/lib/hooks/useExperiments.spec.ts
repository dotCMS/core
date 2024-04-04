import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import { useExperiments } from './useExperiments';

import { EXPERIMENT_QUERY_PARAM_KEY } from '../shared/constants';

describe('useExperiments', () => {
    it('returns a modified set of query parameters with the experiment variant if it exists', () => {
        // Mock the experiment context have experiment variant
        const mockExperimentContext = {
            getVariantAsQueryParam: jest
                .fn()
                .mockReturnValue({ [EXPERIMENT_QUERY_PARAM_KEY]: 'variant1' })
        };

        jest.spyOn(React, 'useContext').mockReturnValue(mockExperimentContext);
        const { result } = renderHook(() => useExperiments());
        const queryParams = result.current.getVariantAsQueryParamObject('http://localhost', {});

        expect(queryParams).toEqual({ [EXPERIMENT_QUERY_PARAM_KEY]: 'variant1' });
    });

    it('removes the EXPERIMENT_QUERY_PARAM_KEY from the query parameters if no experiment variant for the URL', () => {
        // Mock the experiment context dont have experiment variant
        const mockExperimentContext = {
            getVariantAsQueryParam: jest.fn().mockReturnValue({})
        };

        jest.spyOn(React, 'useContext').mockReturnValue(mockExperimentContext);
        const { result } = renderHook(() => useExperiments());
        const queryParams = result.current.getVariantAsQueryParamObject('http://localhost', {
            [EXPERIMENT_QUERY_PARAM_KEY]: 'variant1'
        });

        expect(queryParams).not.toHaveProperty(EXPERIMENT_QUERY_PARAM_KEY);
    });

    it('returns the same query parameters if no experiment variant for the URL and the EXPERIMENT_QUERY_PARAM_KEY does not exist in the current query parameters', () => {
        // Mock the experiment context dont have experiment variant
        const mockExperimentContext = {
            getVariantAsQueryParam: jest.fn().mockReturnValue({})
        };

        jest.spyOn(React, 'useContext').mockReturnValue(mockExperimentContext);
        const { result } = renderHook(() => useExperiments());
        const queryParams = result.current.getVariantAsQueryParamObject('http://localhost', {
            param1: 'value1'
        });

        expect(queryParams).toEqual({ param1: 'value1' });
    });
});
