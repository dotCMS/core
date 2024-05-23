import { QueryBuilder } from '../../../../query-builder/sdk-query-builder';
import { Equals } from '../../../../query-builder/utils/lucene-syntax/Equals';
import { CONTENT_API_URL } from '../../const';
import { OrderByMap } from '../../types';

export class GetCollection {
    private _language?: number;
    private _render = false;
    private _sortBy?: OrderByMap;
    private _limit = 10;
    private _page = 1;
    private _query?: string;
    private _contentType: string;
    private _draft = false;
    private _depth = 0;
    private _variantId = 'DEFAULT';

    private _defaultQuery: Equals;
    #requestOptions: Omit<RequestInit, 'body' | 'method'>;
    #serverUrl: string;

    private get sort() {
        const keys = Object.keys(this._sortBy ?? {});

        return keys.map((key) => `${key} ${this._sortBy?.[key]}`).join(',');
    }

    private get offset() {
        return this._limit * (this._page - 1); // Not sure if this is correct
    }

    private get url() {
        return `${this.#serverUrl}${CONTENT_API_URL}`;
    }

    constructor(
        requestOptions: Omit<RequestInit, 'body' | 'method'>,
        serverUrl: string,
        contentType: string
    ) {
        this.#requestOptions = requestOptions;
        this.#serverUrl = serverUrl;

        this._contentType = contentType;

        this._defaultQuery = new QueryBuilder().field('contentType').equals(this._contentType);
    }

    language(language: number): GetCollection {
        this._language = language;

        return this;
    }

    render(render: boolean): GetCollection {
        this._render = render;

        return this;
    }

    sortBy(sortBy: OrderByMap): GetCollection {
        this._sortBy = sortBy;

        return this;
    }

    limit(limit: number): GetCollection {
        this._limit = limit;

        return this;
    }

    page(page: number): GetCollection {
        this._page = page;

        return this;
    }

    query(queryCallback: (qb: Equals) => Equals): GetCollection {
        this._query = queryCallback(this._defaultQuery)
            .build()
            .replace(/\+([^:]+):/g, (match, field) => {
                return field !== 'contentType' && field !== 'structurename' // Legacy field for contentTypes
                    ? `+${this._contentType}.${field}:`
                    : match;
            });

        return this;
    }

    draft(draft: boolean): GetCollection {
        this._draft = draft;

        return this;
    }

    variantId(variantId: string): GetCollection {
        this._variantId = variantId;

        return this;
    }

    depth(depth: number): GetCollection {
        this._depth = depth;

        return this;
    }

    fetch(): Promise<unknown> {
        return fetch(this.url, {
            ...this.#requestOptions,
            method: 'POST',
            headers: {
                ...this.#requestOptions.headers,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                //userId Do we want to support this?
                //allCategoriesInfo Do we want to support this?

                // variantId: Does not exist in the API
                // live: does not exist in the API

                query: this._query ?? this._defaultQuery.build(),
                languageId: this._language,
                render: this._render,
                sort: this.sort,
                limit: this._limit,
                offset: this.offset,
                depth: this._depth
            })
        });
    }
}
