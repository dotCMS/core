import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

@Injectable()
export class DotPageApiService {
    constructor(private http: HttpClient) {}

    /**
     * Get a page from the Page API
     *
     * @param {{ url: string; language_id: string }} { url, language_id }
     * @return {*}  {Observable<unknown>}
     * @memberof DotEditPageService
     */
    get({ url, language_id }: { url: string; language_id: string }): Observable<{
        page: {
            title: string;
        };
    }> {
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
