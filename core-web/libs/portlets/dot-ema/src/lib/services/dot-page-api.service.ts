import { EMPTY, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, map, pluck } from 'rxjs/operators';

import { DotLanguage, DotPersona } from '@dotcms/dotcms-models';

import { SavePagePayload } from '../shared/models';

export interface DotPageApiResponse {
    page: {
        title: string;
        identifier: string;
    };
    viewAs: {
        language: DotLanguage;
        persona?: DotPersona;
    };
}

export interface DotPageApiParams {
    url: string;
    language_id: string;
    persona_id: string;
}

export interface GetPersonasParams {
    pageID: string;
    filter?: string;
    page?: number;
    perPage?: number;
}

export interface GetPersonasResponse {
    data: DotPersona[];
    pagination: PaginationData;
}

export interface PaginationData {
    currentPage: number;
    perPage: number;
    totalEntries: number;
}

@Injectable()
export class DotPageApiService {
    constructor(private http: HttpClient) {}

    /**
     * Get a page from the Page API
     *
     * @param {DotPageApiParams} { url, language_id }
     * @return {*}  {Observable<DotPageApiResponse>}
     * @memberof DotPageApiService
     */
    get({ url, language_id, persona_id }: DotPageApiParams): Observable<DotPageApiResponse> {
        const apiUrl = `/api/v1/page/json/${url}?language_id=${language_id}&com.dotmarketing.persona.id=${persona_id}`;

        return this.http
            .get<{
                entity: DotPageApiResponse;
            }>(apiUrl)
            .pipe(pluck('entity'));
    }

    /**
     * Save a contentlet in a page
     *
     * @param {SavePagePayload} { pageContainers, container, contentletID, pageID }
     * @return {*}
     * @memberof DotPageApiService
     */
    save({ pageContainers, pageID }: SavePagePayload): Observable<unknown> {
        return this.http
            .post(`/api/v1/page/${pageID}/content`, pageContainers)
            .pipe(catchError(() => EMPTY));
    }

    /**
     * Get the personas from the Page API
     *
     * @param null {}
     * @return {*}  {Observable<DotPersona[]>}
     * @memberof DotPageApiService
     */
    getPersonas(personaParams: GetPersonasParams): Observable<GetPersonasResponse> {
        const url = this.getPersonasURL(personaParams);

        return this.http.get<{ entity: DotPersona[]; pagination: PaginationData }>(url).pipe(
            map((res: { entity: DotPersona[]; pagination: PaginationData }) => ({
                data: res.entity,
                pagination: res.pagination
            }))
        );
    }

    private getPersonasURL({ pageID, filter, page, perPage = 10 }: GetPersonasParams): string {
        const apiUrl = `/api/v1/page/${pageID}/personas?`;

        const queryParams = new URLSearchParams({
            perper_page: perPage.toString(),
            respectFrontEndRoles: 'true',
            variantName: 'DEFAULT'
        });

        if (filter) {
            queryParams.set('filter', filter);
        }

        if (page) {
            queryParams.set('page', page.toString());
        }

        return apiUrl + queryParams.toString();
    }
}
