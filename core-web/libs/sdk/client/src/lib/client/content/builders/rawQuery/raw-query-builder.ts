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
     * Raw queries do not automatically inject any system constraints.
     * If you need constraints like language, live/draft, site, or variant, include them in the raw query string.
     *
     * @protected
     * @return {undefined} No languageId is sent unless provided in the raw query string itself.
     * @memberof RawQueryBuilder
     */
    protected getLanguageId(): undefined {
        return undefined;
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
