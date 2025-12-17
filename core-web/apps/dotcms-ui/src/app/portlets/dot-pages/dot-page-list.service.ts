import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSAPIResponse, ESContent } from '@dotcms/dotcms-models';

import { FAVORITE_PAGE_LIMIT } from './dot-pages-store/dot-pages.store';

export interface ListPagesParams {
    search: string;
    sort: string;
    limit: number;
    offset: number;
    languageId: number;
    host: string;
    archived: boolean;
    userId?: string;
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

    getFavoritePages(params: ListPagesParams): Observable<ESContent> {
        return this.#http
            .post<DotCMSAPIResponse<ESContent>>(this.#url, {
                query: this.#buildFavoritePagesQuery(params),
                sort: 'title ASC',
                limit: FAVORITE_PAGE_LIMIT,
                offset: 0
            })
            .pipe(map((response) => response.entity));
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
        const langQuery = languageId ? `+languageId:${languageId}` : '';
        const archivedQuery = archived ? `+deleted:true` : '+deleted:false';
        const hostQuery = host ? `+conhost:${host}` : '';

        return `+working:true +(urlmap:* OR basetype:5) ${searchQuery} ${langQuery} ${archivedQuery} ${hostQuery}`;
    }

    /**
     * Build the query for the favorite pages API
     * @param params - The parameters for the request
     * @returns The query for the favorite pages API
     */
    #buildFavoritePagesQuery(params: ListPagesParams): string {
        const { host, userId } = params;
        const hostQuery = host ? `+conhost:${host}` : '';

        return `+contentType:dotFavoritePage +deleted:false +working:true ${hostQuery} +owner:${userId}`;
    }
}
