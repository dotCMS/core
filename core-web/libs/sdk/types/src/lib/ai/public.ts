import { DotCMSAISearchRawResponse } from './internal';

import { DotHttpError } from '../client/public';
import { DotCMSBasicContentlet } from '../page/public';

/**
 * Query parameters for AI search - defines what and where to search
 * @public
 * @interface DotCMSAISearchQuery
 */
export interface DotCMSAISearchQuery {
    /**
     * The limit of the search results.
     * @property {number} limit - The limit of the search results.
     * @default 1000
     */
    limit?: number;
    /**
     * The offset of the search results.
     * @property {number} offset - The offset of the search results.
     * @default 0
     */
    offset?: number;
    /**
     * The site identifier.
     * @property {string} siteId - The site identifier.
     */
    siteId?: string;
    /**
     * The content type you want to search for.
     * @property {string} contentType - The content type you want to search for.
     */
    contentType?: string;
    /**
     * The language id to search in.
     * @property {number | string} languageId - The language id to search in.
     */
    languageId?: number | string;
}

/**
 * AI configuration parameters - controls how AI processes the results
 * @public
 * @interface DotCMSAIConfig
 */
export interface DotCMSAIConfig {
    /**
     * The threshold for the search results.
     * @property {number} threshold - The threshold for the search results.
     * @default 0.5
     */
    threshold?: number;
    /**
     * The distance function for the search results.
     * Possible values:
     *
     * - <-> - L2 distance
     * - <#> - (negative) inner product
     * - <=> - cosine distance
     * - <+> - L1 distance
     * - <~> - Hamming distance (binary vectors)
     * - <%> - Jaccard distance (binary vectors)
     * @default DISTANCE_FUNCTIONS.cosine
     * @see {@link https://platform.openai.com/docs/guides/embeddings/which-distance-function-should-i-use#which-distance-function-should-i-use} - OpenAI documentation for the distance functions
     * @property {string} distanceFunction - The distance function for the search results.
     */
    distanceFunction?: (typeof DISTANCE_FUNCTIONS)[keyof typeof DISTANCE_FUNCTIONS];
    /**
     * The length of the response.
     * @property {number} responseLength - The length of the response.
     * @default 1024
     */
    responseLength?: number;
}

/**
 * Parameters for making an AI search request to DotCMS.
 * @public
 * @interface DotCMSAISearchParams
 */
export interface DotCMSAISearchParams {
    /**
     * Query parameters defining what and where to search.
     * @property {DotCMSAISearchQuery} query - The search query parameters.
     */
    query?: DotCMSAISearchQuery;
    /**
     * AI configuration parameters controlling how results are processed.
     * @property {DotCMSAIConfig} config - The AI configuration parameters.
     */
    config?: DotCMSAIConfig;
}

/**
 * The distance functions for the search results.
 * @public
 * @constant DISTANCE_FUNCTIONS
 */
export const DISTANCE_FUNCTIONS = {
    /**
     * The L2 distance function.
     * @constant L2 - The L2 distance function.
     */
    L2: '<->',
    /**
     * The inner product distance function.
     * @constant innerProduct - The inner product distance function.
     */
    innerProduct: '<#>',
    cosine: '<=>',
    /**
     * The L1 distance function.
     * @constant L1 - The L1 distance function.
     */
    L1: '<+>',
    /**
     * The hamming distance function.
     * @constant hamming - The hamming distance function.
     */
    hamming: '<~>',
    /**
     * The jaccard distance function.
     * @constant jaccard - The jaccard distance function.
     */
    jaccard: '<%>'
} as const;

/**
 * AI Search API specific error class
 * Wraps HTTP errors and adds AI search-specific context including query information
 */
export class DotErrorAISearch extends Error {
    public readonly httpError?: DotHttpError;

    public readonly prompt?: string;
    public readonly params?: DotCMSAISearchParams;
    public readonly indexName?: string;
    constructor({
        message,
        httpError,
        prompt,
        params,
        indexName
    }: {
        message: string;
        httpError?: DotHttpError;
        prompt?: string;
        params?: DotCMSAISearchParams;
        indexName?: string;
    }) {
        super(message);
        this.name = 'DotCMAISearchError';
        this.httpError = httpError;
        this.prompt = prompt;
        this.params = params;
        this.indexName = indexName;
        // Ensure proper prototype chain for instanceof checks
        Object.setPrototypeOf(this, DotErrorAISearch.prototype);
    }

    /**
     * Serializes the error to a plain object for logging or transmission
     */
    toJSON() {
        return {
            name: this.name,
            message: this.message,
            httpError: this.httpError?.toJSON(),
            prompt: this.prompt,
            params: this.params,
            indexName: this.indexName,
            stack: this.stack
        };
    }
}

/**
 * The response from the AI search.
 * @public
 * @interface DotCMSAISearchResponse
 */
export interface DotCMSAISearchResponse<T extends DotCMSBasicContentlet>
    extends Omit<DotCMSAISearchRawResponse<T>, 'dotCMSResults'> {
    /**
     * The results from the AI search.
     * @property {DotCMSAISearchContentletData<T>[]} results - The results from the AI search.
     */
    results: DotCMSAISearchContentletData<T>[];
}

/**
 * The match from the AI search.
 * @public
 * @interface DotCMSAISearchMatch
 */
export interface DotCMSAISearchMatch {
    /**
     * The distance from the AI search.
     * @property {number} distance - The distance from the AI search.
     */
    distance: number;
    /**
     * The extracted text from the AI search.
     * @property {string} extractedText - The extracted text from the AI search.
     */
    extractedText: string;
}

/**
 * The contentlet data from the AI search.
 * @public
 * @interface DotCMSAISearchContentletData
 */
export type DotCMSAISearchContentletData<T extends DotCMSBasicContentlet> = T & {
    /**
     * The matches from the AI search.
     * @property {DotCMSAISearchMatch[]} matches - The matches from the AI search.
     */
    matches?: DotCMSAISearchMatch[];
};
