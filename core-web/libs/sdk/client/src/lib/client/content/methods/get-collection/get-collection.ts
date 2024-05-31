import { Equals } from '../../../../query-builder/lucene-syntax';
import { QueryBuilder } from '../../../../query-builder/sdk-query-builder';
import { ClientOptions } from '../../../sdk-js-client';
import { CONTENT_API_URL } from '../../shared/const';
import {
    GetCollectionResponse,
    BuildQuery,
    SortBy,
    GetCollectionRawResponse
} from '../../shared/types';
import { sanitizeQueryForContentType } from '../../shared/utils';

/**
 * 'GetCollection' Is a Typescript class that provides the ability build a query to get a collection of content.
 *
 * @export
 * @class GetCollection
 */
export class GetCollection<T = unknown> {
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
     * @memberof GetCollection
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
     * @memberof GetCollection
     */
    private get currentQuery() {
        return this.#query ?? this.#defaultQuery;
    }

    /**
     * This method allows to filter the content by language.
     *
     * @param {number} language
     * @return {*}  {this}
     * @memberof GetCollection
     */
    language(language: number): this {
        this.#query = this.currentQuery.field('languageId').equals(language.toString());

        return this;
    }

    /**
     * This method allows you to fetch the render version of the content
     *
     * @param {boolean} render
     * @return {*}  {this}
     * @memberof GetCollection
     */
    render(render: boolean): this {
        this.#render = render;

        return this;
    }

    /**
     * This method allows you to sort the content by field
     *
     * @param {SortBy[]} sortBy
     * @return {*}  {this}
     * @memberof GetCollection
     */
    sortBy(sortBy: SortBy[]): this {
        this.#sortBy = sortBy;

        return this;
    }

    /**
     * This method allows you to limit the number of content to fetch
     *
     * @param {number} limit
     * @return {*}  {this}
     * @memberof GetCollection
     */
    limit(limit: number): this {
        this.#limit = limit;

        return this;
    }

    /**
     * This method allows you to set the page of the content to fetch
     *
     * @param {number} page
     * @return {*}  {this}
     * @memberof GetCollection
     */
    page(page: number): this {
        this.#page = page;

        return this;
    }

    // Docs here
    query(buildQuery: string): this;
    // Docs here
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
            throw new Error('The query builder callback should return a DotQuery instance');
        }

        return this;
    }

    /**
     * This method allows you to fetch draft content
     *
     * @param {boolean} draft
     * @return {*}  {this}
     * @memberof GetCollection
     */
    draft(draft: boolean): this {
        this.#query = this.currentQuery.field('live').equals((!draft).toString());

        return this;
    }

    /**
     * This method allows you to filter content by experiment variant
     *
     * @param {string} variant
     * @return {*}  {this}
     * @memberof GetCollection
     */
    variant(variant: string): this {
        this.#query = this.currentQuery.field('variant').equals(variant);

        return this;
    }

    /**
     * This method allows you to set the depth of the relationships in the content
     *
     * @param {number} depth
     * @return {*}  {this}
     * @memberof GetCollection
     */
    depth(depth: number): this {
        this.#depth = depth;

        return this;
    }

    // DOCS MISSING.
    then(
        onfulfilled?:
            | ((
                  value: GetCollectionResponse<T>
              ) => GetCollectionResponse<T> | PromiseLike<GetCollectionResponse<T>> | void)
            | undefined
            | null,
        onrejected?: ((error: unknown) => unknown | PromiseLike<unknown> | void) | undefined | null
    ): Promise<GetCollectionResponse<T> | unknown> {
        return this.fetchContentApi().then(async (response) => {
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
    private fetchContentApi(): Promise<Response> {
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
