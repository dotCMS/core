import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

export interface DotPageApiResponse {
    page: {
        title: string;
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
                entity: {
                    page: {
                        title: string;
                    };
                };
            }>(apiUrl)
            .pipe(pluck('entity'));
    }
}
