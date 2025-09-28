import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotCMSPageAsset } from '@dotcms/types';

@Injectable({
    providedIn: 'root'
})
export class DotPageService {
    private http = inject(HttpClient);

    /**
     * Get a page from the Page API
     *
     * @param {DotPageApiParams} { url, language_id }
     * @return {*}  {Observable<DotCMSPageAsset>}
     * @memberof DotPageApiService
     */
    get(url: string, language_id: string): Observable<DotCMSPageAsset> {
        const pageType = 'json';
        const pageURL = `/api/v1/page/${pageType}/${url}?language_id=${language_id}`;

        return this.http
            .get<{
                entity: DotCMSPageAsset;
            }>(pageURL)
            .pipe(pluck('entity'));
    }
}
