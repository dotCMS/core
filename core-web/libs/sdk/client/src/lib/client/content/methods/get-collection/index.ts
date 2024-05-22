import { QueryBuilder } from '../../../../query-builder/sdk-query-builder';
import { Equals } from '../../../../query-builder/utils/lucene-syntax/Equals';
import { ClientConfig } from '../../../sdk-js-client';
import { OrderByMap } from '../../types';

export class GetCollection {
    protected _language = 1;
    protected _render = false;
    protected _sortBy: Array<OrderByMap> = [];
    protected _limit = 10;
    protected _page = 1;
    protected _query?: Equals;
    protected _contentType: string;
    protected _draft = false;
    protected _variantId = 'DEFAULT';
    #clientConfig: ClientConfig;

    constructor(clientConfig: ClientConfig, contentType: string) {
        this.#clientConfig = clientConfig;
        this._contentType = contentType;
    }

    language(language: number): GetCollection {
        this._language = language;

        return this;
    }

    render(render: boolean): GetCollection {
        this._render = render;

        return this;
    }

    sortBy(sortBy: Array<OrderByMap>): GetCollection {
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

    query(queryCallback: (qb: QueryBuilder) => Equals): GetCollection {
        this._query = queryCallback(new QueryBuilder());

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

    fetch(): Promise<unknown> {
        // console.log('Fetching collection for content type:', this._contentType);
        // console.log('using client config:', this.#clientConfig);

        return Promise.resolve();
    }
}
