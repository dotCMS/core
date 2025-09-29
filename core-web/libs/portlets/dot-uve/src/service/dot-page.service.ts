import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import { DotPagination, DotPersona } from '@dotcms/dotcms-models';
import { DotCMSPageAsset } from '@dotcms/types';

export interface PersonasParams {
    perPage: number;
    filter?: string;
    page?: number;
}

@Injectable({
    providedIn: 'root'
})
export class DotPageService {
    private http = inject(HttpClient);
    private BASE_URL = '/api/v1/page';

    /**
     * Get a page from the Page API
     *
     * @param {DotPageApiParams} { url, language_id }
     * @return {*}  {Observable<DotCMSPageAsset>}
     * @memberof DotPageApiService
     */
    get(url: string, language_id: string): Observable<DotCMSPageAsset> {
        const pageType = 'json';
        const pageURL = `${this.BASE_URL}/${pageType}/${url}?language_id=${language_id}`;

        return this.http
            .get<{
                entity: DotCMSPageAsset;
            }>(pageURL)
            .pipe(pluck('entity'));
    }

    /**
     * Get the personas from a page
     *
     * @param {string} pageId
     * @param {PersonasParams} personaParams
     * @return {*}  {Observable<{ personas: DotPersona[]; pagination: DotPagination }>}
     * @memberof DotPageService
     */
    getPagePersonas(
        pageId: string,
        personaParams?: PersonasParams
    ): Observable<{ personas: DotPersona[]; pagination: DotPagination }> {
        const { perPage = 10, page = 1 } = personaParams || {};
        const url = `${this.BASE_URL}/${pageId}/personas?perPage=${perPage}&page=${page}`;

        return this.http.get<{ entity: DotPersona[]; pagination: DotPagination }>(url).pipe(
            map(({ entity, pagination }) => {
                return {
                    personas: entity,
                    pagination
                };
            })
        );
    }
}
