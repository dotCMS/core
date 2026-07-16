import {
    DotHttpClient,
    DotRequestOptions,
    DotHttpError,
    DotCMSClientConfig,
    DotErrorContent
} from '@dotcms/types';

import { BaseBuilder } from '../../../base/builder/base-builder';
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
     * Optional language ID for the request body.
     * When provided, it will be sent as `languageId` in the request payload.
     *
     * NOTE: This does NOT modify the raw query string. If you need `+languageId:<id>` inside
     * the Lucene query, include it in the raw query yourself.
     *
     * @private
     */
    #languageId?: number | string;

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
     * This sets the `languageId` request body field (it does not alter the raw Lucene query string).
     *
     * @example
     * ```typescript
     * const response = await client.content
     *     .query('+contentType:Blog +title:Hello')
     *     .language(1)
     *     .limit(10);
     * ```
     *
     * @param {number | string} languageId The language ID to filter the content by.
     * @return {RawQueryBuilder} A RawQueryBuilder instance.
     * @memberof RawQueryBuilder
     */
    language(languageId: number | string): this {
        this.#languageId = languageId;

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
     * @protected
     * @return {number | string | undefined} Optional languageId to send in the request body.
     * @memberof RawQueryBuilder
     */
    protected getLanguageId(): number | string | undefined {
        return this.#languageId;
    }

    /**
     * Builds the final Lucene query string.
     *
     * Raw query is used as provided (only minimal sanitization is applied).
     * No implicit constraints are added.
     *
     * @protected
     * @return {string} The final Lucene query string
     * @memberof RawQueryBuilder
     */
    protected buildFinalQuery(): string {
        return sanitizeQuery(this.#rawQuery);
    }
}
