import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSAPIResponse, DotCMSContentlet, ESContent } from '@dotcms/dotcms-models';

import { FAVORITE_PAGE_LIMIT } from '../dot-pages-store/dot-pages.store';

export interface ListPagesParams {
    search: string;
    sort: string;
    limit: number;
    offset: number;
    languageId: number | null;
    host: string;
    archived: boolean;
}

@Injectable()
export class DotPageListService {
    readonly #http = inject(HttpClient);
    readonly #url = '/api/content/_search';

    /**
     * Get pages from the API
     * @param params - The parameters for the request
     * @returns An observable of the pages
     */
    getPages(params: ListPagesParams): Observable<ESContent> {
        const query = this.#buildQuery(params);
        const { sort, limit, offset } = params;
        return this.#http
            .post<DotCMSAPIResponse<ESContent>>(this.#url, {
                query,
                sort,
                limit,
                offset
            })
            .pipe(map((response) => response.entity));
    }

    getFavoritePages(params: ListPagesParams, userId: string): Observable<ESContent> {
        return this.#http
            .post<DotCMSAPIResponse<ESContent>>(this.#url, {
                query: this.#buildFavoritePagesQuery(params, userId),
                sort: 'title ASC',
                limit: FAVORITE_PAGE_LIMIT,
                offset: 0
            })
            .pipe(map((response) => response.entity));
    }

    getFavoritePageByURL(url: string): Observable<DotCMSContentlet> {
        return this.#http
            .post<DotCMSAPIResponse<ESContent>>(this.#url, {
                query: `+DotFavoritePage.url_dotraw:${url}`,
                sort: 'title ASC',
                limit: 1,
                offset: 0
            })
            .pipe(map((response) => response.entity.jsonObjectView.contentlets[0]));
    }

    getSinglePage(identifier: string): Observable<DotCMSContentlet> {
        return this.#http
            .post<DotCMSAPIResponse<ESContent>>(this.#url, {
                query: `+identifier:${identifier}`,
                sort: 'title ASC',
                limit: 1,
                offset: 0
            })
            .pipe(map((response) => response.entity.jsonObjectView.contentlets[0]));
    }

    /**
     * Build the query for the API
     * @param params - The parameters for the request
     * @returns The query for the API
     */
    #buildQuery(params: ListPagesParams): string {
        const { search, languageId, archived, host } = params;
        const searchQuery = search
            ? `+(title:${search}* OR path:*${search}* OR urlmap:*${search}*)`
            : '';
        const langQuery = `+languageId:${languageId ?? '*'}`;
        const archivedQuery = archived ? `+deleted:true` : '+deleted:false';
        const hostQuery = host ? `+conhost:${host}` : '';

        return `+working:true +(urlmap:* OR basetype:5) ${searchQuery} ${langQuery} ${archivedQuery} ${hostQuery}`;
    }

    /**
     * Build the query for the favorite pages API
     * @param params - The parameters for the request
     * @returns The query for the favorite pages API
     */
    #buildFavoritePagesQuery(_params: ListPagesParams, userId: string): string {
        // NOTE: Host scoping (+conhost) is currently not supported/working for dotFavoritePage queries.
        // If you need host-scoped favorites, please create a ticket (suggested title: "Favorite pages API ignores host (+conhost) filter")
        // and include an example request/query plus expected vs actual results.

        return `+contentType:dotFavoritePage +deleted:false +working:true +owner:${userId}`;
    }
}
