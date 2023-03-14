import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck, shareReplay } from 'rxjs/operators';

import { DotCopyContent, DotCMSContentlet } from '@dotcms/dotcms-models';

export const DEFAULT_PERSONALIZATION = 'dot:default';

const API_ENDPOINT = `/api/v1/page/copyContent`;

@Injectable({
    providedIn: 'root'
})
export class DotCopyContentService {
    constructor(private readonly http: HttpClient) {}

    /**
     *
     * Create a copy of a content in a page.
     * @param {DotCopyContent} data
     * @return {*}  {Observable<DotCMSContentlet>}
     * @memberof DotCopyContentService
     */
    copyContentInPage(data: DotCopyContent): Observable<DotCMSContentlet> {
        const body = {
            ...data,
            personalization: data?.personalization || DEFAULT_PERSONALIZATION
        };

        return this.http.put(API_ENDPOINT, body).pipe(shareReplay(), pluck('entity'));
    }
}
