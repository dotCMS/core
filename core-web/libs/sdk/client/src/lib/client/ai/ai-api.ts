import {
    DotCMSClientConfig,
    DotRequestOptions,
    DotHttpClient,
    DotCMSAISearchParams,
    DotCMSBasicContentlet
} from '@dotcms/types';

import { AISearch } from './search/search';

import { BaseApiClient } from '../base/base-api';

/**
 * Client for interacting with the DotCMS AI API.
 * Provides methods to interact with AI features.
 */
export class AIClient extends BaseApiClient {
    /**
     * Creates a new AIClient instance.
     *
     * @param {DotCMSClientConfig} config - Configuration options for the DotCMS client
     * @param {DotRequestOptions} requestOptions - Options for fetch requests including authorization headers
     * @param {DotHttpClient} httpClient - HTTP client for making requests
     * @example
     * ```typescript
     * const aiClient = new AIClient(
     *   {
     *     dotcmsUrl: 'https://demo.dotcms.com',
     *     authToken: 'your-auth-token',
     *     siteId: 'demo.dotcms.com'
     *   },
     *   {
     *     headers: {
     *       Authorization: 'Bearer your-auth-token'
     *     }
     *   },
     *   httpClient
     * );
     * ```
     */
    constructor(
        config: DotCMSClientConfig,
        requestOptions: DotRequestOptions,
        httpClient: DotHttpClient
    ) {
        super(config, requestOptions, httpClient);
    }

    /**
     * Performs an AI-powered search.
     *
     * @param params - Search parameters with query and AI configuration
     * @returns Promise with search results
     * @example
     * ```typescript
     * const results = await client.ai.search('machine learning articles', {
     *   query: {
     *     indexName: 'content_index',
     *     limit: 20,
     *     contentType: 'BlogPost',
     *     languageId: 'en'
     *   },
     *   ai: {
     *     threshold: 0.7
     *   }
     * });
     * ```
     * @example
     * ```typescript
     * client.ai.search('machine learning articles', {
     *   query: {
     *     indexName: 'content_index',
     *     limit: 20,
     *     contentType: 'BlogPost',
     *     languageId: 'en'
     *   },
     *   ai: {
     *     threshold: 0.7,
     *     distanceFunction: DISTANCE_FUNCTIONS.cosine
     *   }
     * }).then((results) => {
     *   console.log(results);
     * }).catch((error) => {
     *   console.error(error);
     * });
     */
    search<T extends DotCMSBasicContentlet>(
        prompt: string,
        params: DotCMSAISearchParams
    ): AISearch<T> {
        return new AISearch<T>(this.config, this.requestOptions, this.httpClient, params, prompt);
    }
}
