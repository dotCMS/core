import { Equals } from '../../../../query-builder/lucene-syntax/Equals';
import { QueryBuilder } from '../../../../query-builder/sdk-query-builder';
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
    private _render = false;
    private _sortBy?: SortByArray;
    private _limit = 10;
    private _page = 1;
    private _depth = 0;
    private _query?: Equals;
    private _contentType: string;

    private _defaultQuery: Equals;
    private requestOptions: Omit<RequestInit, 'body' | 'method'>;
    private serverUrl: string;

    /**
     * This method returns the sort query in this format: field order, field order, ...
     *
     * @readonly
     * @private
     * @memberof GetCollection
     */
    private get sort() {
        return this._sortBy?.map((sort) => `${sort.field} ${sort.order}`).join(',');
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
        return this._limit * (this._page - 1);
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
        return this._query ?? this._defaultQuery;
    }

    constructor(
        requestOptions: Omit<RequestInit, 'body' | 'method'>,
        serverUrl: string,
        contentType: string
    ) {
        this.requestOptions = requestOptions;
        this.serverUrl = serverUrl;

        this._contentType = contentType;

        // We need to build the default query with the contentType field
        this._defaultQuery = new QueryBuilder().field('contentType').equals(this._contentType);
    }

    /**
     * This method allows to filter the content by language.
     *
     * @param {number} language
     * @return {*}  {this}
     * @memberof GetCollection
     */
    language(language: number): this {
        this._query = this.currentQuery.field('languageId').equals(language.toString());

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
        this._render = render;

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
        this._sortBy = sortBy;

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
        this._limit = limit;

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
        this._page = page;

        return this;
    }

    /**
     * This method allows you to set a lucene query to filter the content using the Lucene Query Builder.
     *
     * @param {QueryBuilderCallback} queryBuilderCallback
     * @return {*}  {this}
     * @memberof GetCollection
     */
    query(queryBuilderCallback: QueryBuilderCallback): this {
        const queryResult = queryBuilderCallback(this.currentQuery);

        // This can be use in Javascript so we cannot rely on the type checking
        if (queryResult instanceof Equals) {
            this._query = queryResult;
        } else {
            throw new Error('The query builder callback should return an Equals instance');
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
        this._query = this.currentQuery.field('live').equals((!draft).toString());

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
        this._query = this.currentQuery.field('variant').equals(variant);

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
        this._depth = depth;

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
        const query = sanitizeQueryForContentType(this.currentQuery.build(), this._contentType);

        return fetch(this.url, {
            ...this.requestOptions,
            method: 'POST',
            headers: {
                ...this.requestOptions.headers,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                query,
                render: this._render,
                sort: this.sort,
                limit: this._limit,
                offset: this.offset,
                depth: this._depth
                //userId: This exist but we currently don't use it
                //allCategoriesInfo: This exist but we currently don't use it
            })
        }).then(
            async (response) => {
                if (response.ok) {
                    return response.json().then((data) => {
                        const contentlets = data.entity.jsonObjectView.contentlets;

                        const mappedResponse: GetCollectionResponse<T> = {
                            contentlets,
                            page: this._page,
                            size: contentlets.length,
                            total: 0 // There's no way to know this in the response
                        };

                        return this._sortBy
                            ? {
                                  ...mappedResponse,
                                  sortedBy: this._sortBy
                              }
                            : mappedResponse;
                    });
                } else return response.json();
            },

            (error) => error
        );
    }
}
