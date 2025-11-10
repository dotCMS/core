import {
    DotCMSAIConfig,
    DotCMSAISearchParams,
    DotCMSAISearchQuery,
    DotCMSBasicContentlet,
    DotCMSClientConfig,
    DotErrorAISearch,
    DotHttpClient,
    DotHttpError,
    DotRequestOptions
} from '@dotcms/types';

import { BaseApiClient } from '../../base/base-api';
import { DEFAULT_AI_CONFIG, DEFAULT_QUERY } from '../shared/const';
import { DotCMSAISearchResponse, OnFullfilled, OnRejected } from '../shared/types';

export class AISearch<T extends DotCMSBasicContentlet> extends BaseApiClient {
    #params: DotCMSAISearchParams;
    #prompt: string;

    constructor(
        config: DotCMSClientConfig,
        requestOptions: DotRequestOptions,
        httpClient: DotHttpClient,
        params: DotCMSAISearchParams,
        prompt: string
    ) {
        super(config, requestOptions, httpClient);
        this.#params = params;
        this.#prompt = prompt;
    }

    then(
        onfulfilled?: OnFullfilled<T>,
        onrejected?: OnRejected
    ): Promise<DotCMSAISearchResponse<T> | DotErrorAISearch> {
        return this.fetch<T>().then(
            (data) => {
                if (typeof onfulfilled === 'function') {
                    const result = onfulfilled(data);
                    // Ensure we always return a value, fallback to formattedResponse if callback returns undefined
                    return result ?? data;
                }

                return data;
            },
            (error: unknown) => {
                // Wrap error in DotCMSContentError
                let contentError: DotErrorAISearch;

                if (error instanceof DotHttpError) {
                    contentError = new DotErrorAISearch({
                        message: `AI Search failed for '${this.#prompt}' (fetch): ${error.message}`,
                        httpError: error,
                        params: this.#params,
                        prompt: this.#prompt
                    });
                } else {
                    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
                    contentError = new DotErrorAISearch({
                        message: `AI Search failed for '${this.#prompt}' (fetch): ${errorMessage}`,
                        httpError: undefined,
                        params: this.#params,
                        prompt: this.#prompt
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

    //
    async stream(callback: (data: { done: boolean; value: unknown }) => void): Promise<void> {
        const stream = await this.fetchStream<T>();

        const reader = stream.getReader();

        try {
            // eslint-disable-next-line no-constant-condition
            while (true) {
                const { done, value } = await reader.read();

                callback({ done, value });
                if (done) break;
            }
        } catch (error) {
            throw new Error(
                `AI Search failed for '${this.#prompt}' (stream): ${error instanceof Error ? error.message : 'Unknown error'}`
            );
        } finally {
            reader.releaseLock();
        }
    }

    private fetch<T extends DotCMSBasicContentlet>(): Promise<DotCMSAISearchResponse<T>> {
        const searchParams = this.buildSearchParams(this.#prompt, this.#params);
        const url = new URL('/api/v1/ai/search', this.dotcmsUrl);
        url.search = searchParams.toString();

        return this.httpClient.request<DotCMSAISearchResponse<T>>(url.toString(), {
            ...this.requestOptions,
            headers: {
                ...this.requestOptions.headers
            },
            method: 'GET'
        });
    }

    private fetchStream<T extends DotCMSBasicContentlet>(): Promise<ReadableStream<T>> {
        const searchParams = this.buildSearchParams(this.#prompt, this.#params);
        const url = new URL('/api/v1/ai/search', this.dotcmsUrl);
        url.search = searchParams.toString();

        return this.httpClient.request<ReadableStream<T>>(url.toString(), {
            ...this.requestOptions,
            headers: {
                ...this.requestOptions.headers,
                'Content-Type': 'application/octet-stream'
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
        const { query = {}, ai = {} } = params;

        const combinedQuery: DotCMSAISearchQuery = {
            ...DEFAULT_QUERY,
            siteId: this.siteId,
            ...query
        };
        const combinedAI: DotCMSAIConfig = {
            ...DEFAULT_AI_CONFIG,
            ...ai
        };

        // Map SDK query parameters to backend parameter names
        if (combinedQuery.limit !== undefined) {
            searchParams.append('searchLimit', String(combinedQuery.limit));
        }
        if (combinedQuery.offset !== undefined) {
            searchParams.append('searchOffset', String(combinedQuery.offset));
        }
        if (combinedQuery.siteId !== undefined) {
            searchParams.append('site', combinedQuery.siteId);
        } else if (this.siteId) {
            searchParams.append('site', this.siteId);
        }

        if (combinedQuery.contentType !== undefined) {
            searchParams.append('contentType', combinedQuery.contentType);
        }

        if (combinedQuery.indexName !== undefined) {
            searchParams.append('indexName', combinedQuery.indexName);
        }

        if (combinedQuery.languageId !== undefined) {
            searchParams.append('language', combinedQuery.languageId);
        }

        // Map SDK AI config parameters to backend parameter names
        if (combinedAI?.threshold !== undefined) {
            searchParams.append('threshold', String(combinedAI.threshold));
        }
        if (combinedAI?.distanceFunction !== undefined) {
            searchParams.append('operator', combinedAI.distanceFunction);
        }
        if (ai?.responseLength !== undefined) {
            searchParams.append('responseLength', String(combinedAI.responseLength));
        }

        searchParams.append('query', prompt);

        return searchParams;
    }
}
