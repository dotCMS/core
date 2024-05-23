import { QueryBuilder } from '../../../../query-builder/sdk-query-builder';
import { Equals } from '../../../../query-builder/utils/lucene-syntax/Equals';
import { CONTENT_API_URL } from '../../const';
import { OrderByMap } from '../../types';

export class GetCollection {
    private _render = false;
    private _sortBy?: OrderByMap;
    private _limit = 10;
    private _page = 1;
    private _depth = 0;
    private _query?: Equals;
    private _contentType: string;

    private _defaultQuery: Equals;
    private requestOptions: Omit<RequestInit, 'body' | 'method'>;
    private serverUrl: string;

    private excludedFields: string[] = [
        'contentType',
        'structurename',
        'variantId',
        'live',
        'languageId'
    ];

    private get sort() {
        const keys = Object.keys(this._sortBy ?? {});

        return keys.map((key) => `${key} ${this._sortBy?.[key]}`).join(',');
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

    language(language: number): GetCollection {
        this._query = this.currentQuery.field('languageId').equals(language.toString());

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
        this._query = queryCallback(this._defaultQuery);

        return this;
    }

    draft(draft: boolean): GetCollection {
        this._query = this.currentQuery.field('live').equals((!draft).toString());

        return this;
    }

    variantId(variantId: string): GetCollection {
        this._query = this.currentQuery.field('variantId').equals(variantId);

        return this;
    }

    depth(depth: number): GetCollection {
        this._depth = depth;

        return this;
    }

    fetch(): Promise<unknown> {
        const query = this.currentQuery.build().replace(/\+([^+:]*?):/g, (match, field) => {
            return !this.excludedFields.includes(field) // Fields that are not contentType fields
                ? `+${this._contentType}.${field}:`
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
                // I have to test the variantId

                query,
                render: this._render,
                sort: this.sort,
                limit: this._limit,
                offset: this.offset,
                depth: this._depth
                //userId: Do we want to support this?
                //allCategoriesInfo: Do we want to support this?
            })
        });
    }
}
