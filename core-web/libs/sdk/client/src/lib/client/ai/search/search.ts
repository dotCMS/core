import {
    DotCMSAIConfig,
    DotCMSAISearchParams,
    DotCMSAISearchQuery,
    DotCMSBasicContentlet,
    DotCMSClientConfig,
    DotErrorAISearch,
    DotHttpClient,
    DotHttpError,
    DotRequestOptions,
    DotCMSAISearchResponse
} from '@dotcms/types';
import { DotCMSAISearchRawResponse } from '@dotcms/types/internal';

import { appendMappedParams } from '../../../utils/params/utils';
import { BaseApiClient } from '../../base/base-api';
import { DEFAULT_AI_CONFIG, DEFAULT_QUERY } from '../shared/const';
import { OnFullfilled, OnRejected } from '../shared/types';

/**
 * Class for executing AI searches.
 *
 * @template T - The type of the contentlet.
 * @param config - The configuration for the client.
 * @param requestOptions - The request options for the client.
 * @param httpClient - The HTTP client for the client.
 * @param params - The parameters for the search.
 * @param prompt - The prompt for the search.
 */
export class AISearch<T extends DotCMSBasicContentlet> extends BaseApiClient {
    #params: DotCMSAISearchParams;
    #prompt: string;
    #indexName: string;
    constructor(
        config: DotCMSClientConfig,
        requestOptions: DotRequestOptions,
        httpClient: DotHttpClient,
        prompt: string,
        indexName: string,
        params: DotCMSAISearchParams = {}
    ) {
        super(config, requestOptions, httpClient);
        this.#params = params;
        this.#prompt = prompt;
        this.#indexName = indexName;
    }

    /**
     * Executes the AI search and returns a promise with the search results.
     *
     * @param onfulfilled - Callback function to handle the search results.
     * @param onrejected - Callback function to handle the search error.
     * @returns Promise with the search results or the error.
     * @example
     * ```typescript
     * const results = await client.ai.search('machine learning articles', 'content_index', {
     *   query: {
     *     limit: 20,
     *     contentType: 'BlogPost',
     *     languageId: '1' // or 1
     *   },
     *   config: {
     *     threshold: 0.7
     *   }
     * });
     * ```
     * @example
     * ```typescript
     * client.ai.search('machine learning articles', 'content_index', {
     *   query: {
     *     limit: 20,
     *     contentType: 'BlogPost',
     *     languageId: '1' // or 1
     *   },
     *   config: {
     *     threshold: 0.7
     *   }
     * }).then((results) => {
     *   console.log(results);
     * }).catch((error) => {
     *   console.error(error);
     * });
     * ```
     */
    then(
        onfulfilled?: OnFullfilled<T>,
        onrejected?: OnRejected
    ): Promise<DotCMSAISearchResponse<T> | DotErrorAISearch> {
        return this.fetch<T>().then(
            (data) => {
                const response: DotCMSAISearchResponse<T> = {
                    ...data,
                    results: data.dotCMSResults
                };
                if (typeof onfulfilled === 'function') {
                    const result = onfulfilled(response);
                    // Ensure we always return a value, fallback to data if callback returns undefined

                    return result ?? response;
                }

                return response;
            },
            (error: unknown) => {
                // Wrap error in DotCMSContentError
                let contentError: DotErrorAISearch;

                if (error instanceof DotHttpError) {
                    contentError = new DotErrorAISearch({
                        message: `AI Search failed (fetch): ${error.message}`,
                        httpError: error,
                        params: this.#params,
                        prompt: this.#prompt,
                        indexName: this.#indexName
                    });
                } else {
                    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
                    contentError = new DotErrorAISearch({
                        message: `AI Search failed (fetch): ${errorMessage}`,
                        httpError: undefined,
                        params: this.#params,
                        prompt: this.#prompt,
                        indexName: this.#indexName
                    });
                }

                if (typeof onrejected === 'function') {
                    const result = onrejected(contentError);
                    // Ensure we always return a value, fallback to original error if callback returns undefined
                    return result ?? contentError;
                }

                // Throw the wrapped error to trigger .catch()
                throw contentError;
            }
        );
    }

    private fetch<T extends DotCMSBasicContentlet>(): Promise<DotCMSAISearchRawResponse<T>> {
        const searchParams = this.buildSearchParams(this.#prompt, this.#params);
        const url = new URL('/api/v1/ai/search', this.dotcmsUrl);
        url.search = searchParams.toString();

        return this.httpClient.request<DotCMSAISearchRawResponse<T>>(url.toString(), {
            ...this.requestOptions,
            headers: {
                ...this.requestOptions.headers
            },
            method: 'GET'
        });
    }

    /**
     * Builds URLSearchParams from the SDK interface, mapping to backend parameter names.
     * Only includes parameters that have values.
     *
     * @param params - Search parameters with SDK naming
     * @returns URLSearchParams with backend parameter names
     * @private
     */
    private buildSearchParams(prompt: string, params: DotCMSAISearchParams = {}): URLSearchParams {
        const searchParams = new URLSearchParams();
        const { query = {}, config = {} } = params;

        const combinedQuery: DotCMSAISearchQuery = {
            ...DEFAULT_QUERY,
            siteId: this.siteId,
            ...query
        };
        const combinedConfig: DotCMSAIConfig = {
            ...DEFAULT_AI_CONFIG,
            ...config
        };

        const entriesQueryParameters: Array<Array<string>> = [
            ['searchLimit', 'limit'],
            ['searchOffset', 'offset'],
            ['site', 'siteId'],
            ['language', 'languageId'],
            ['contentType']
        ];

        // Map SDK query parameters to backend parameter names
        appendMappedParams(searchParams, combinedQuery, entriesQueryParameters);

        // Map config parameters using the same key names
        appendMappedParams(
            searchParams,
            combinedConfig,
            Object.keys(combinedConfig).map((key) => [key])
        );

        // Add search-specific parameters
        searchParams.append('indexName', this.#indexName);
        searchParams.append('query', prompt);
        return searchParams;
    }
}
