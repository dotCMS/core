import { useCallback, useContext } from 'react';

import DotExperimentsContext from './DotExperimentsContext';

import { EXPERIMENT_QUERY_PARAM_KEY } from '../shared/constants';

/**
 * A custom hook to interact with DotCMS Experiments.
 * Currently, this hook provides a method to get an experiment variant
 * as a query parameter and add it to a URL string.
 *
 * @returns {Object} The API for this hook.
 * @returns {Function} getVariantAsQueryParam - A function to get experiment variant as a query param and append it to a URL string.
 *
 * @example
 * ```javascript
 * const { getVariantAsQueryParam } = useExperiments();
 * const urlWithVariant = getVariantAsQueryParam(url, currentQueryParams);
 * ```
 *
 */
export const useExperiments = () => {
    const experimentContext = useContext(DotExperimentsContext);

    /**
     * This function gets an experiment variant as a query parameter based on the provided URL and current URL query parameters.
     * If there's an experiment variant for the URL, it appends that variant as a query parameter.
     * If there's no experiment variant for the URL but the EXPERIMENT_QUERY_PARAM_KEY exists in the current query parameters, this method removes the EXPERIMENT_QUERY_PARAM_KEY.
     * If there's no experiment variant for the URL and the EXPERIMENT_QUERY_PARAM_KEY doesn't exist in the current query parameters, the URL is returned unchanged.
     *
     * @param {string} href - The URL that might contain an experiment.
     * @param {Record<string, string>} currentQueryParams - The current set of query parameters.
     * @returns {Record<string, string>} The updated set of query parameters.
     */
    const getVariantAsQueryParamObject = useCallback(
        (href: string, currentQueryParams: Record<string, string>) => {
            const variantParams = experimentContext
                ? experimentContext.getVariantAsQueryParam(href)
                : {};

            // If variantParams is not empty, append variantParams to currentQueryParams
            if (Object.keys(variantParams).length > 0) {
                currentQueryParams = { ...currentQueryParams, ...variantParams };
            }

            // If the EXPERIMENT_QUERY_PARAM_KEY is already in current params and variantParams is empty, delete EXPERIMENT_QUERY_PARAM_KEY
            if (
                EXPERIMENT_QUERY_PARAM_KEY in currentQueryParams &&
                Object.keys(variantParams).length === 0
            ) {
                delete currentQueryParams[EXPERIMENT_QUERY_PARAM_KEY];
            }

            return currentQueryParams;
        },
        [experimentContext]
    );

    return { getVariantAsQueryParamObject };
};
