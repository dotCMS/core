import { EMPTY, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, pluck } from 'rxjs/operators';

import { DotLanguage } from '@dotcms/dotcms-models';

import { SavePagePayload } from '../shared/models';

export interface DotPageApiResponse {
    page: {
        title: string;
        identifier: string;
    };
    viewAs: {
        language: DotLanguage;
    };
}

export interface DotPageApiParams {
    url: string;
    language_id: string;
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
    get({ url, language_id }: DotPageApiParams): Observable<DotPageApiResponse> {
        const apiUrl = `/api/v1/page/json/${url}?language_id=${language_id}`;

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
}
