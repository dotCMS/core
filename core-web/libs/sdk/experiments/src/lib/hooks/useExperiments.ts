import { useCallback, useContext } from 'react';

import DotExperimentsContext from './DotExperimentsContext';

import { EXPERIMENT_QUERY_PARAM_KEY } from '../shared/constants';

type QueryParamsHandler = 'merge' | 'replace';

export const useExperiments = () => {
    const dotExperimentsInstance = useContext(DotExperimentsContext);

    // Append the variant name as a query parameter to the URL.
    const getVariantAsQueryParam = useCallback(
        (
            href: string,
            // TODO: find a way to get this queryParams from the router
            currentQueryParams: Record<string, string>,
            handle: QueryParamsHandler = 'merge'
        ) => {
            const variantParams = dotExperimentsInstance
                ? dotExperimentsInstance.getVariantAsQueryParam(href)
                : {};

            if (Object.keys(variantParams).length > 0) {
                // If 'handle' is 'replace', ignore existing queryParams and just return the experiment variant params

                if (handle === 'replace') {
                    return variantParams;
                }

                // Otherwise, merge existing queryParams and experiment variant params
                return { ...currentQueryParams, ...variantParams };
            }

            if (EXPERIMENT_QUERY_PARAM_KEY in currentQueryParams) {
                const cleanedQueryParams = { ...currentQueryParams };
                delete cleanedQueryParams[EXPERIMENT_QUERY_PARAM_KEY];

                return cleanedQueryParams;
            }

            return currentQueryParams;
        },
        [dotExperimentsInstance]
    );

    return { getVariantAsQueryParam };
};
