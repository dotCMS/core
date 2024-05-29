import { Equals } from '@dotcms/lucene-syntax';
import { QueryBuilder } from '@dotcms/query-builder';

import { CONTENT_API_URL } from '../../shared/const';
import { GetCollectionResponse, QueryBuilderCallback, SortByArray } from '../../shared/types';
import { sanitizeQueryForContentType } from '../../shared/utils';

/**
 * 'GetCollection' Is a Typescript class that provides the ability build a query to get a collection of content.
 *
 * @export
 * @class GetCollection
 */
export class GetCollection {
    #page = 1;
    #limit = 10;
    #depth = 0;
    #render = false;
    #sortBy?: SortByArray;
    #contentType: string;
    #defaultQuery: Equals;
    #query?: Equals;
    #rawQuery?: string;

    private serverUrl: string;
    private requestOptions: Omit<RequestInit, 'body' | 'method'>;

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

    /**
     * This method returns the offset of the content to fetch
     *
     * @readonly
     * @private
     * @memberof GetCollection
     */
    private get offset() {
        // This could end in an empty response
        return this.#limit * (this.#page - 1);
    }

    /**
     * This method returns the url to fetch the content
     *
     * @readonly
     * @private
     * @memberof GetCollection
     */
    private get url() {
        return `${this.serverUrl}${CONTENT_API_URL}`;
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

    constructor(
        requestOptions: Omit<RequestInit, 'body' | 'method'>,
        serverUrl: string,
        contentType: string
    ) {
        this.requestOptions = requestOptions;
        this.serverUrl = serverUrl;

        this.#contentType = contentType;

        // We need to build the default query with the contentType field
        this.#defaultQuery = new QueryBuilder().field('contentType').equals(this.#contentType);
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
     * @param {SortByArray} sortBy
     * @return {*}  {this}
     * @memberof GetCollection
     */
    sortBy(sortBy: SortByArray): this {
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

    /**
     * This method allows you to set a lucene query to filter the content using the Lucene Query Builder.
     * All fields will be formatted to: +contentTypeVar.field: value
     *
     * @param {QueryBuilderCallback} queryBuilderCallback
     * @return {*}  {this}
     * @memberof GetCollection
     */
    query(queryBuilderCallback: QueryBuilderCallback): this {
        const queryResult = queryBuilderCallback(this.currentQuery);

        // This can be use in Javascript so we cannot rely on the type checking
        if (queryResult instanceof Equals) {
            this.#query = queryResult;
        } else {
            throw new Error('The query builder callback should return an Equals instance');
        }

        return this;
    }

    /**
     * This method allows you to set a raw lucene query to filter the content
     *
     * @param {string} query
     * @return {*}  {this}
     * @memberof GetCollection
     */
    rawQuery(query: string): this {
        this.#rawQuery = query;

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

    /**
     * This method returns the result of the fetch using the query built.
     *
     * @template T
     * @return {*}  {Promise<GetCollectionResponse<T>>}
     * @memberof GetCollection
     */
    async fetch<T>(): Promise<GetCollectionResponse<T>> {
        const sanitizedQuery = sanitizeQueryForContentType(
            this.currentQuery.build(),
            this.#contentType
        );

        const query = this.#rawQuery ? `${sanitizedQuery} ${this.#rawQuery}` : sanitizedQuery;

        return fetch(this.url, {
            ...this.requestOptions,
            method: 'POST',
            headers: {
                ...this.requestOptions.headers,
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
        }).then(
            async (response) => {
                if (response.ok) {
                    const data = await response.json();

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
                } else {
                    return response.json();
                }
            },
            (error) => error
        );
    }
}
