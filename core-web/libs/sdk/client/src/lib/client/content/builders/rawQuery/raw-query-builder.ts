import {
    DotHttpClient,
    DotRequestOptions,
    DotHttpError,
    DotCMSClientConfig,
    DotErrorContent
} from '@dotcms/types';

import { shouldAddSiteIdConstraint } from '../../shared/utils';
import { BaseBuilder } from '../base';
import { sanitizeQuery } from '../query/utils';

/**
 * Builder for executing raw Lucene queries against the DotCMS Content API.
 *
 * This builder provides direct access to Lucene query syntax while still
 * offering common functionality like pagination, sorting, and language filtering.
 *
 * @export
 * @class RawQueryBuilder
 * @template T - The type of the content items (defaults to unknown)
 */
export class RawQueryBuilder<T = unknown> extends BaseBuilder<T> {
    /**
     * The raw Lucene query string.
     * @private
     */
    #rawQuery: string;

    /**
     * Language ID for filtering content.
     * @private
     */
    #languageId: number | string = 1;

    /**
     * Whether to retrieve draft content.
     * @private
     */
    #draft = false;

    /**
     * Variant ID for experiments.
     * @private
     */
    #variantId?: string;

    /**
     * Creates an instance of RawQueryBuilder.
     *
     * @param {object} params - Constructor parameters
     * @param {DotRequestOptions} params.requestOptions - Options for the client request
     * @param {DotCMSClientConfig} params.config - The client configuration
     * @param {string} params.rawQuery - The raw Lucene query string
     * @param {DotHttpClient} params.httpClient - HTTP client for making requests
     * @memberof RawQueryBuilder
     */
    constructor(params: {
        requestOptions: DotRequestOptions;
        config: DotCMSClientConfig;
        rawQuery: string;
        httpClient: DotHttpClient;
    }) {
        super(params);
        this.#rawQuery = params.rawQuery;
    }

    /**
     * Filters the content by the specified language ID.
     *
     * @example
     * ```typescript
     * const response = await client.content
     *     .query('+contentType:Blog')
     *     .language(1)
     *     .limit(10);
     * ```
     *
     * @param {number | string} languageId - The language ID to filter the content by
     * @return {this} The builder instance for method chaining
     * @memberof RawQueryBuilder
     */
    language(languageId: number | string): this {
        this.#languageId = languageId;

        return this;
    }

    /**
     * Retrieves draft content instead of live content.
     *
     * @example
     * ```typescript
     * const response = await client.content
     *     .query('+contentType:Blog')
     *     .draft()
     *     .limit(10);
     * ```
     *
     * @return {this} The builder instance for method chaining
     * @memberof RawQueryBuilder
     */
    draft(): this {
        this.#draft = true;

        return this;
    }

    /**
     * Filters the content by a variant ID for Experiments.
     *
     * More information here: {@link https://www.dotcms.com/docs/latest/experiments-and-a-b-testing}
     *
     * @example
     * ```typescript
     * const response = await client.content
     *     .query('+contentType:Blog')
     *     .variant('legends-forceSensitive')
     *     .limit(10);
     * ```
     *
     * @param {string} variantId - A string that represents a variant ID
     * @return {this} The builder instance for method chaining
     * @memberof RawQueryBuilder
     */
    variant(variantId: string): this {
        this.#variantId = variantId;

        return this;
    }

    /**
     * Wraps an error in a DotErrorContent instance with helpful context.
     *
     * @protected
     * @param {unknown} error - The error to wrap
     * @return {DotErrorContent} The wrapped error
     * @memberof RawQueryBuilder
     */
    protected wrapError(error: unknown): DotErrorContent {
        if (error instanceof DotHttpError) {
            return new DotErrorContent(
                `Content API failed for raw query (fetch): ${error.message}`,
                'raw-query',
                'fetch',
                error,
                this.buildFinalQuery()
            );
        }

        const errorMessage = error instanceof Error ? error.message : 'Unknown error';
        return new DotErrorContent(
            `Content API failed for raw query (fetch): ${errorMessage}`,
            'raw-query',
            'fetch',
            undefined,
            this.buildFinalQuery()
        );
    }

    /**
     * Gets the language ID for the query.
     *
     * @protected
     * @return {number | string} The language ID
     * @memberof RawQueryBuilder
     */
    protected getLanguageId(): number | string {
        return this.#languageId;
    }

    /**
     * Builds the final Lucene query string by adding essential system constraints.
     *
     * This method takes the raw query and adds:
     * 1. Language ID constraint
     * 2. Live/draft status constraint
     * 3. Variant constraint (if specified)
     * 4. Site ID constraint (if conditions are met)
     *
     * The raw query is used as-is with minimal sanitization (only trimming spaces).
     * NO field prefixing is applied (unlike CollectionBuilder).
     *
     * @protected
     * @return {string} The final Lucene query string
     * @memberof RawQueryBuilder
     *
     * @example
     * // Input: '+contentType:Blog +title:*'
     * // Output: '+contentType:Blog +title:* +languageId:1 +live:true +conhost:123'
     *
     * @example
     * // With draft flag:
     * // Input: '+contentType:Blog'
     * // Output: '+contentType:Blog +languageId:1 +(live:false AND working:true AND deleted:false) +conhost:123'
     */
    protected buildFinalQuery(): string {
        // Start with the raw query (minimal sanitization only)
        let query = sanitizeQuery(this.#rawQuery);

        // Add language constraint
        query = `${query} +languageId:${this.#languageId}`;

        // Add draft/live constraint
        if (this.#draft) {
            query = `${query} +(live:false AND working:true AND deleted:false)`;
        } else {
            query = `${query} +live:true`;
        }

        // Add variant constraint if specified
        if (this.#variantId) {
            query = `${query} +variant:${this.#variantId}`;
        }

        // Add site constraint if needed (following same logic as CollectionBuilder)
        if (shouldAddSiteIdConstraint(query, this.siteId)) {
            query = `${query} +conhost:${this.siteId}`;
        }

        return sanitizeQuery(query);
    }
}
