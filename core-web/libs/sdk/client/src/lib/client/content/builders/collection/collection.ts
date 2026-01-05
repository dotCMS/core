import {
    DotHttpClient,
    DotRequestOptions,
    DotHttpError,
    DotCMSClientConfig,
    DotErrorContent
} from '@dotcms/types';

import { BuildQuery } from '../../shared/types';
import { sanitizeQueryForContentType, shouldAddSiteIdConstraint } from '../../shared/utils';
import { BaseBuilder } from '../base/base-builder';
import { Equals } from '../query/lucene-syntax';
import { QueryBuilder } from '../query/query';
import { sanitizeQuery } from '../query/utils';

/**
 * Creates a Builder to filter and fetch content from the content API for a specific content type.
 *
 * @export
 * @class CollectionBuilder
 * @template T Represents the type of the content type to fetch. Defaults to unknown.
 */
export class CollectionBuilder<T = unknown> extends BaseBuilder<T> {
    #contentType: string;
    #defaultQuery: Equals;
    #query?: Equals;
    #rawQuery?: string;
    #languageId: number | string = 1;
    #draft = false;

    /**
     * Creates an instance of CollectionBuilder.
     * @param {object} params - Constructor parameters
     * @param {DotRequestOptions} params.requestOptions - Options for the client request
     * @param {DotCMSClientConfig} params.config - The client configuration
     * @param {string} params.contentType - The content type to fetch
     * @param {DotHttpClient} params.httpClient - HTTP client for making requests
     * @memberof CollectionBuilder
     */
    constructor(params: {
        requestOptions: DotRequestOptions;
        config: DotCMSClientConfig;
        contentType: string;
        httpClient: DotHttpClient;
    }) {
        super(params);
        this.#contentType = params.contentType;

        // Build the default query with the contentType field
        this.#defaultQuery = new QueryBuilder().field('contentType').equals(this.#contentType);
    }

    /**
     * Returns the current query built.
     *
     * @readonly
     * @private
     * @memberof CollectionBuilder
     */
    private get currentQuery() {
        return this.#query ?? this.#defaultQuery;
    }

    /**
     * Filters the content by the specified language ID.
     *
     * @example
     * ```typescript
     * const client = new DotCMSClient(config);
     * const collectionBuilder = client.content.getCollection("Blog");
     * collectionBuilder.language(1);
     * ```
     *
     * @param {number | string} languageId The language ID to filter the content by.
     * @return {CollectionBuilder} A CollectionBuilder instance.
     * @memberof CollectionBuilder
     */
    language(languageId: number | string): this {
        this.#languageId = languageId;

        return this;
    }

    /**
     * Filters the content by a Lucene query string.
     *
     * @param {string} query A Lucene query string.
     * @return {CollectionBuilder} A CollectionBuilder instance.
     * @memberof CollectionBuilder
     */
    query(query: string): this;

    /**
     * Filters the content by building a query using a QueryBuilder function.
     *
     * @example
     * ```typescript
     * const client = new DotCMSClient(config);
     * const collectionBuilder = client.content.getCollection("Blog");
     * collectionBuilder.query((queryBuilder) =>
     *     queryBuilder.field('title').equals('Hello World').or().equals('Hello World 2')
     * );
     * ```
     *
     * @param {BuildQuery} buildQuery A function that receives a QueryBuilder instance and returns a valid query.
     * @return {CollectionBuilder} A CollectionBuilder instance.
     * @memberof CollectionBuilder
     */
    query(buildQuery: BuildQuery): this;
    query(arg: unknown): this {
        if (typeof arg === 'string') {
            this.#rawQuery = arg;

            return this;
        }

        if (typeof arg !== 'function') {
            throw new Error(
                `Parameter for query method should be a buildQuery function or a string.\nExample:\nclient.content.getCollection('Activity').query((queryBuilder) => queryBuilder.field('title').equals('Hello World'))\nor\nclient.content.getCollection('Activity').query('+Activity.title:"Hello World"') \nSee documentation for more information.`
            );
        }

        const builtQuery = arg(new QueryBuilder());

        // This can be use in Javascript so we cannot rely on the type checking
        if (builtQuery instanceof Equals) {
            this.#query = builtQuery.raw(this.currentQuery.build());
        } else {
            throw new Error(
                'Provided query is not valid. A query should end in an equals method call.\nExample:\n(queryBuilder) => queryBuilder.field("title").equals("Hello World")\nSee documentation for more information.'
            );
        }

        return this;
    }

    /**
     * Retrieves draft content.
     * @example
     * ```ts
     * const client = new DotCMSClient(config);
     * const collectionBuilder = client.content.getCollection("Blog");
     * collectionBuilder
     *      .draft() // This will retrieve draft/working content
     *      .then((response) => // Your code here })
     *      .catch((error) => // Your code here })
     * ```
     *
     * @return {CollectionBuilder} A CollectionBuilder instance.
     * @memberof CollectionBuilder
     */
    draft(): this {
        this.#draft = true;

        return this;
    }

    /**
     * Filters the content by a variant ID for [Experiments](https://www.dotcms.com/docs/latest/experiments-and-a-b-testing)
     *
     * More information here: {@link https://www.dotcms.com/docs/latest/content-api-retrieval-and-querying#ParamsOptional}
     *
     * @example
     * ```ts
     * const client = new DotCMSClient(config);
     * const collectionBuilder = client.content.getCollection("Blog");
     * collectionBuilder
     *      .variant("YOUR_VARIANT_ID")
     *      .then((response) => // Your code here })
     *      .catch((error) => // Your code here })
     * ```
     *
     * @param {string} variantId A string that represents a variant ID.
     * @return {CollectionBuilder} A CollectionBuilder instance.
     * @memberof CollectionBuilder
     */
    variant(variantId: string): this {
        this.#query = this.currentQuery.field('variant').equals(variantId);

        return this;
    }

    /**
     * Wraps an error in a DotErrorContent instance with helpful context.
     *
     * @protected
     * @param {unknown} error - The error to wrap
     * @return {DotErrorContent} The wrapped error
     * @memberof CollectionBuilder
     */
    protected wrapError(error: unknown): DotErrorContent {
        if (error instanceof DotHttpError) {
            return new DotErrorContent(
                `Content API failed for '${this.#contentType}' (fetch): ${error.message}`,
                this.#contentType,
                'fetch',
                error,
                this.buildFinalQuery()
            );
        }

        const errorMessage = error instanceof Error ? error.message : 'Unknown error';
        return new DotErrorContent(
            `Content API failed for '${this.#contentType}' (fetch): ${errorMessage}`,
            this.#contentType,
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
     * @memberof CollectionBuilder
     */
    protected getLanguageId(): number | string {
        return this.#languageId;
    }

    /**
     * Builds the final Lucene query string by combining the base query with required system constraints.
     *
     * This method constructs the complete query by:
     * 1. Adding language ID filter to ensure content matches the specified language
     * 2. Adding live/draft status filter based on the draft flag
     * 3. Applying content type specific query sanitization
     * 4. Optionally adding site ID constraint if conditions are met
     *
     * Site ID constraint is added only when:
     * - Query doesn't already contain a positive site constraint (+conhost)
     * - Query doesn't explicitly exclude the current site ID (-conhost:currentSiteId)
     * - Site ID is configured in the system
     *
     * @protected
     * @returns {string} The complete Lucene query string ready for the Content API
     * @memberof CollectionBuilder
     *
     * @example
     * // For live content in language 1 with site ID 123:
     * // Returns: "+contentType:Blog +languageId:1 +live:true +conhost:123"
     *
     * @example
     * // For draft content without site constraint:
     * // Returns: "+contentType:Blog +languageId:1 +(live:false AND working:true AND deleted:false)"
     *
     * @example
     * // For content with explicit exclusion of current site (site ID 123):
     * // Query: "+contentType:Blog -conhost:123"
     * // Returns: "+contentType:Blog -conhost:123 +languageId:1 +live:true" (no site ID added)
     *
     * @example
     * // For content with exclusion of different site (site ID 456, current is 123):
     * // Query: "+contentType:Blog -conhost:456"
     * // Returns: "+contentType:Blog -conhost:456 +languageId:1 +live:true +conhost:123" (site ID still added)
     */
    protected buildFinalQuery(): string {
        // Build base query with language and live/draft constraints.
        // NOTE: languageId is intentionally sent in BOTH places:
        // - in the Lucene query (backend requires this)
        // - as a top-level request body field (backend also requires this)
        let baseQuery = this.currentQuery.field('languageId').equals(this.#languageId.toString());

        if (this.#draft) {
            baseQuery = baseQuery.raw('+(live:false AND working:true AND deleted:false)');
        } else {
            baseQuery = baseQuery.field('live').equals('true');
        }

        const builtQuery = baseQuery.build();

        // Check if site ID constraint should be added using utility function
        const shouldAddSiteId = shouldAddSiteIdConstraint(builtQuery, this.siteId);

        // Add site ID constraint if needed
        let finalQuery: string;
        if (shouldAddSiteId) {
            finalQuery = `${builtQuery} +conhost:${this.siteId}`;
        } else {
            finalQuery = builtQuery;
        }

        // Apply content-type specific sanitization (adds contentType. prefix to fields)
        const sanitizedQuery = sanitizeQueryForContentType(finalQuery, this.#contentType);

        // Append raw query if provided (raw query is NOT sanitized for content type)
        const query = this.#rawQuery ? `${sanitizedQuery} ${this.#rawQuery}` : sanitizedQuery;

        return sanitizeQuery(query);
    }
}
