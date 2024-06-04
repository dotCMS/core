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
 * Creates a Builder to filter and fetch content from the content API for an specific content type
 *
 * @export
 * @class CollectionBuilder
 * @template T
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

    #serverUrl: string;
    #requestOptions: ClientOptions;

    constructor(requestOptions: ClientOptions, serverUrl: string, contentType: string) {
        this.#requestOptions = requestOptions;
        this.#serverUrl = serverUrl;

        this.#contentType = contentType;

        // We need to build the default query with the contentType field
        this.#defaultQuery = new QueryBuilder().field('contentType').equals(this.#contentType);
    }

    /**
     * This method returns the sort query in this format: field order, field order, ...
     *
     * @readonly
     * @private
     * @memberof CollectionBuilder
     */
    private get sort() {
        return this.#sortBy?.map((sort) => `${sort.field} ${sort.order}`).join(',');
    }

    private get offset() {
        // This could end in an empty response
        return this.#limit * (this.#page - 1);
    }

    private get url() {
        return `${this.#serverUrl}${CONTENT_API_URL}`;
    }

    /**
     * This method returns the current query built
     *
     * @readonly
     * @private
     * @memberof CollectionBuilder
     */
    private get currentQuery() {
        return this.#query ?? this.#defaultQuery;
    }

    /**
     * Takes a language id and filters the content by that language
     *
     *
     * @param {number} languageId The language id to filter the content by
     * @return {CollectionBuilder} CollectionBuilder - A CollectionBuilder instance
     * @memberof CollectionBuilder
     */
    language(languageId: number): this {
        this.#query = this.currentQuery.field('languageId').equals(languageId.toString());

        return this;
    }

    /**
     * Takes a boolean representing if the content should be rendered or not
     *
     * @param {boolean} render A boolean representing if the content should be rendered or not
     * @return {CollectionBuilder} CollectionBuilder - A CollectionBuilder instance
     * @memberof CollectionBuilder
     */
    render(render: boolean): this {
        this.#render = render;

        return this;
    }

    /**
     * Takes an array of constrains to sort the content by field an specific order
     *
     * @example
     * ```javascript
     * // This will sort the content by title in ascending order
     * // and by modDate in descending order
     *  const sortBy = [{ field: 'title', order: 'asc' }, { field: 'modDate', order: 'desc' }]
     *
     *  client.content.getCollection("Blog").sortBy(sortBy)
     *```
     *
     * @param {SortBy[]} sortBy Array of constrains to sort the content by
     * @return {CollectionBuilder} CollectionBuilder - A CollectionBuilder instance
     * @memberof CollectionBuilder
     */
    sortBy(sortBy: SortBy[]): this {
        this.#sortBy = sortBy;

        return this;
    }

    /**
     * Takes a number that represents the max amount of content to fetch
     *
     * Note: The limit is set to 10 by default
     *
     * @param {number} limit The max amount of content to fetch
     * @return {CollectionBuilder} CollectionBuilder - A CollectionBuilder instance
     * @memberof CollectionBuilder
     */
    limit(limit: number): this {
        this.#limit = limit;

        return this;
    }

    /**
     * Takes a number that represents the page to fetch
     *
     * @param {number} page The page to fetch
     * @return {CollectionBuilder} CollectionBuilder - A CollectionBuilder instance
     * @memberof CollectionBuilder
     */
    page(page: number): this {
        this.#page = page;

        return this;
    }

    /**
     * Takes a string that represents a Lucene Query that is used to filter the content to fetch.
     *
     * Note: The string is not validated, so be cautious when using it.
     *
     * @param {string} query A Lucene Query String
     * @return {CollectionBuilder} CollectionBuilder - A CollectionBuilder instance
     * @memberof CollectionBuilder
     */
    query(query: string): this;

    /**
     * Takes a function that recieves a QueryBuilder to buid a query for content filtering.
     * @example
     *```javascript
     * // This will filter the content by title equals 'Hello World' or 'Hello World 2'
     * client.content.getCollection("Activity").query((queryBuilder) =>
     *     queryBuilder.field('title').equals('Hello World').or().equals('Hello World 2')
     * );
     *```
     * @param {BuildQuery} buildQuery A function that receives a QueryBuilder instance and returns a valid query
     * @return {CollectionBuilder} CollectionBuilder - A CollectionBuilder instance
     * @memberof CollectionBuilder
     */
    query(buildQuery: BuildQuery): this;
    query(buildQuery: BuildQuery | string): this {
        if (typeof buildQuery === 'string') {
            this.#rawQuery = buildQuery;

            return this;
        }

        if (typeof buildQuery !== 'function') {
            throw new Error('Parameter for query method should be a function or a string');
        }

        const builtQuery = buildQuery(new QueryBuilder());

        // This can be use in Javascript so we cannot rely on the type checking
        if (builtQuery instanceof Equals) {
            this.#query = builtQuery.raw(this.currentQuery.build());
        } else {
            throw new Error('Provided query is not valid.');
        }

        return this;
    }

    /**
     * Takes a boolean that represents if the content to query should be on draft or not
     *
     * Note: The default value is false to fetch content that is not on draft
     *
     * @param {boolean} draft A boolean that represents the state of the content
     * @return {CollectionBuilder} CollectionBuilder - A CollectionBuilder instance
     * @memberof CollectionBuilder
     */
    draft(draft: boolean): this {
        this.#query = this.currentQuery.field('live').equals((!draft).toString());

        return this;
    }

    /**
     * Takes a string that represents a variant ID of content created with the A/B Testing feature
     *
     * Note: variantId defaults to "DEFAULT" to fetch content that is not part of an A/B test
     *
     * @param {string} variantId A string that represents a variant ID
     * @return {CollectionBuilder} CollectionBuilder - A CollectionBuilder instance
     * @memberof CollectionBuilder
     */
    variant(variantId: string): this {
        this.#query = this.currentQuery.field('variant').equals(variantId);

        return this;
    }

    /**
     * Takes a number that represents the depth of the relationships of a content
     *
     * Note: The depth is set to 0 by default and the max supported value is 3
     *
     * @param {number} depth The depth of the relationships of a content
     * @return {CollectionBuilder} CollectionBuilder - A CollectionBuilder instance
     * @memberof CollectionBuilder
     */
    depth(depth: number): this {
        if (depth < 0 || depth > 3) {
            throw new Error('Depth must be between 0 and 3');
        }

        this.#depth = depth;

        return this;
    }

    /**
     * Executes the fetch and returns a promise that resolves to the content or rejects to an error
     *
     * @param {OnFullfilled} [onfulfilled] A callback that is called when the fetch is successful
     * @param {OnRejected} [onrejected] A callback that is called when the fetch fails
     * @return {Promise<GetCollectionResponse<T> | GetCollectionError>} A promise that resolves to the content or rejects to an error
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
                // Fetch does not reject on server errors, so we only have to bubble up the error as a normal fetch
                return {
                    status: response.status,
                    ...data
                };
            }
        }, onrejected);
    }

    // Formats the response to the desired format
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

    // Calls the content API to fetch the content
    private fetch(): Promise<Response> {
        const sanitizedQuery = sanitizeQueryForContentType(
            this.currentQuery.build(),
            this.#contentType
        );

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
