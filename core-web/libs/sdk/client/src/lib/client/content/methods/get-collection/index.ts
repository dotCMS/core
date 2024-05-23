import { QueryBuilder } from '../../../../query-builder/sdk-query-builder';
import { Equals } from '../../../../query-builder/utils/lucene-syntax/Equals';
import { CONTENT_API_URL, CONTENT_TYPE_MAIN_FIELDS } from '../../const';
import { GetCollectionResponse, SortByArray } from '../../types';

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

    private get sort() {
        return this._sortBy?.map((sort) => `${sort.field} ${sort.order}`).join(',');
    }

    private get offset() {
        return this._limit * (this._page - 1); // Not sure if this is correct
    }

    private get url() {
        return `${this.serverUrl}${CONTENT_API_URL}`;
    }

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

        this._defaultQuery = new QueryBuilder().field('contentType').equals(this._contentType);
    }

    language(language: number): this {
        this._query = this.currentQuery.field('languageId').equals(language.toString());

        return this;
    }

    render(render: boolean): this {
        this._render = render;

        return this;
    }

    sortBy(sortBy: SortByArray): this {
        this._sortBy = sortBy;

        return this;
    }

    limit(limit: number): this {
        this._limit = limit;

        return this;
    }

    page(page: number): this {
        this._page = page;

        return this;
    }

    query(queryCallback: (qb: Equals) => Equals): this {
        this._query = queryCallback(this._defaultQuery);

        return this;
    }

    draft(draft: boolean): this {
        this._query = this.currentQuery.field('live').equals((!draft).toString());

        return this;
    }

    variant(variant: string): this {
        this._query = this.currentQuery.field('variant').equals(variant);

        return this;
    }

    depth(depth: number): this {
        this._depth = depth;

        return this;
    }

    async fetch(): Promise<GetCollectionResponse> {
        const query = this.currentQuery.build().replace(/\+([^+:]*?):/g, (match, field) => {
            return !CONTENT_TYPE_MAIN_FIELDS.includes(field) // Fields that are not contentType fields
                ? `+${this._contentType}.${field}:` // Should have this fromat: +contentType.field:
                : match;
        });

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

                        return {
                            contentlets,
                            page: this._page,
                            size: contentlets.length,
                            total: 'Not yet implemented',
                            sortedBy: this._sortBy
                        };
                    });
                } else return response.json();
            },

            (error) => error
        );
    }
}
