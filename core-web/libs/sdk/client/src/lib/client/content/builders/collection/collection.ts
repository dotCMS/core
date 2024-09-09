import { Equals } from '../../../../query-builder/lucene-syntax';
import { QueryBuilder } from '../../../../query-builder/sdk-query-builder';
import { ClientOptions } from '../../../sdk-js-client';
import { CONTENT_API_URL } from '../../shared/const';
import {
    GetCollectionResponse,
    BuildQuery,
    SortBy,
    GetCollectionRawResponse,
    GetCollectionError,
    OnFullfilled,
    OnRejected
} from '../../shared/types';
import { sanitizeQueryForContentType } from '../../shared/utils';

/**
 * Creates a Builder to filter and fetch content from the content API for a specific content type.
 *
 * @export
 * @class CollectionBuilder
 * @template T Represents the type of the content type to fetch. Defaults to unknown.
 */
export class CollectionBuilder<T = unknown> {
    #page = 1;
    #limit = 10;
    #depth = 0;
    #render = false;
    #sortBy?: SortBy[];
    #contentType: string;
    #defaultQuery: Equals;
    #query?: Equals;
    #rawQuery?: string;
    #languageId: number | string = 1;
    #draft = false;

    #serverUrl: string;
    #requestOptions: ClientOptions;

    /**
     * Creates an instance of CollectionBuilder.
     * @param {ClientOptions} requestOptions Options for the client request.
     * @param {string} serverUrl The server URL.
     * @param {string} contentType The content type to fetch.
     * @memberof CollectionBuilder
     */
    constructor(requestOptions: ClientOptions, serverUrl: string, contentType: string) {
        this.#requestOptions = requestOptions;
        this.#serverUrl = serverUrl;
        this.#contentType = contentType;

        // Build the default query with the contentType field
        this.#defaultQuery = new QueryBuilder().field('contentType').equals(this.#contentType);
    }

    /**
     * Returns the sort query in the format: field order, field order, ...
     *
     * @readonly
     * @private
     * @memberof CollectionBuilder
     */
    private get sort() {
        return this.#sortBy?.map((sort) => `${sort.field} ${sort.order}`).join(',');
    }

    /**
     * Returns the offset for pagination.
     *
     * @readonly
     * @private
     * @memberof CollectionBuilder
     */
    private get offset() {
        return this.#limit * (this.#page - 1);
    }

    /**
     * Returns the full URL for the content API.
     *
     * @readonly
     * @private
     * @memberof CollectionBuilder
     */
    private get url() {
        return `${this.#serverUrl}${CONTENT_API_URL}`;
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
     * Setting this to true will server side render (using velocity) any widgets that are returned by the content query.
     *
     * More information here: {@link https://www.dotcms.com/docs/latest/content-api-retrieval-and-querying#ParamsOptional}
     *
     * @return {CollectionBuilder} A CollectionBuilder instance.
     * @memberof CollectionBuilder
     */
    render(): this {
        this.#render = true;

        return this;
    }

    /**
     * Sorts the content by the specified fields and orders.
     *
     * @example
     * ```typescript
     * const client = new DotCMSClient(config);
     * const collectionBuilder = client.content.getCollection("Blog");
     * const sortBy = [{ field: 'title', order: 'asc' }, { field: 'modDate', order: 'desc' }];
     * collectionBuilder("Blog").sortBy(sortBy);
     * ```
     *
     * @param {SortBy[]} sortBy Array of constraints to sort the content by.
     * @return {CollectionBuilder} A CollectionBuilder instance.
     * @memberof CollectionBuilder
     */
    sortBy(sortBy: SortBy[]): this {
        this.#sortBy = sortBy;

        return this;
    }

    /**
     * Sets the maximum amount of content to fetch.
     *
     * @param {number} limit The maximum amount of content to fetch.
     * @return {CollectionBuilder} A CollectionBuilder instance.
     * @memberof CollectionBuilder
     */
    limit(limit: number): this {
        this.#limit = limit;

        return this;
    }

    /**
     * Sets the page number to fetch.
     *
     * @param {number} page The page number to fetch.
     * @return {CollectionBuilder} A CollectionBuilder instance.
     * @memberof CollectionBuilder
     */
    page(page: number): this {
        this.#page = page;

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
     * Sets the depth of the relationships of the content.
     * Specifies the depth of related content to return in the results.
     *
     * More information here: {@link https://www.dotcms.com/docs/latest/content-api-retrieval-and-querying#ParamsOptional}
     *
     * @example
     * ```ts
     * const client = new DotCMSClient(config);
     * const collectionBuilder = client.content.getCollection("Blog");
     * collectionBuilder
     *      .depth(1)
     *      .then((response) => // Your code here })
     *      .catch((error) => // Your code here })
     * ```
     *
     * @param {number} depth The depth of the relationships of the content.
     * @return {CollectionBuilder} A CollectionBuilder instance.
     * @memberof CollectionBuilder
     */
    depth(depth: number): this {
        if (depth < 0 || depth > 3) {
            throw new Error('Depth value must be between 0 and 3');
        }

        this.#depth = depth;

        return this;
    }

    /**
     * Executes the fetch and returns a promise that resolves to the content or rejects with an error.
     *
     * @example
     * ```ts
     * const client = new DotCMSClient(config);
     * const collectionBuilder = client.content.getCollection("Blog");
     * collectionBuilder
     *      .limit(10)
     *      .then((response) => // Your code here })
     *      .catch((error) => // Your code here })
     * ```
     *
     * @param {OnFullfilled} [onfulfilled] A callback that is called when the fetch is successful.
     * @param {OnRejected} [onrejected] A callback that is called when the fetch fails.
     * @return {Promise<GetCollectionResponse<T> | GetCollectionError>} A promise that resolves to the content or rejects with an error.
     * @memberof CollectionBuilder
     */
    then(
        onfulfilled?: OnFullfilled<T>,
        onrejected?: OnRejected
    ): Promise<GetCollectionResponse<T> | GetCollectionError> {
        return this.fetch().then(async (response) => {
            const data = await response.json();
            if (response.ok) {
                const formattedResponse = this.formatResponse<T>(data);

                const finalResponse =
                    typeof onfulfilled === 'function'
                        ? onfulfilled(formattedResponse)
                        : formattedResponse;

                return finalResponse;
            } else {
                return {
                    status: response.status,
                    ...data
                };
            }
        }, onrejected);
    }

    /**
     * Formats the response to the desired format.
     *
     * @private
     * @param {GetCollectionRawResponse<T>} data The raw response data.
     * @return {GetCollectionResponse<T>} The formatted response.
     * @memberof CollectionBuilder
     */
    private formatResponse<T>(data: GetCollectionRawResponse<T>): GetCollectionResponse<T> {
        const contentlets = data.entity.jsonObjectView.contentlets;
        const total = data.entity.resultsSize;

        const mappedResponse: GetCollectionResponse<T> = {
            contentlets,
            total,
            page: this.#page,
            size: contentlets.length
        };

        return this.#sortBy
            ? {
                  ...mappedResponse,
                  sortedBy: this.#sortBy
              }
            : mappedResponse;
    }

    /**
     * Calls the content API to fetch the content.
     *
     * @private
     * @return {Promise<Response>} The fetch response.
     * @memberof CollectionBuilder
     */
    private fetch(): Promise<Response> {
        const finalQuery = this.currentQuery
            .field('languageId')
            .equals(this.#languageId.toString())
            .field('live')
            .equals((!this.#draft).toString())
            .build();

        const sanitizedQuery = sanitizeQueryForContentType(finalQuery, this.#contentType);

        const query = this.#rawQuery ? `${sanitizedQuery} ${this.#rawQuery}` : sanitizedQuery;

        return fetch(this.url, {
            ...this.#requestOptions,
            method: 'POST',
            headers: {
                ...this.#requestOptions.headers,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                query,
                render: this.#render,
                sort: this.sort,
                limit: this.#limit,
                offset: this.offset,
                depth: this.#depth
                //userId: This exist but we currently don't use it
                //allCategoriesInfo: This exist but we currently don't use it
            })
        });
    }
}
