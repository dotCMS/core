import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotLanguage } from '@dotcms/dotcms-models';

/**
 * Provide util methods to get Languages available in the system.
 * @export
 * @class DotLanguagesService
 */
@Injectable()
export class DotLanguagesService {
    private httpClient: HttpClient = inject(HttpClient);

    /**
     * Return languages.
     * @returns Observable<DotLanguage[]>
     * @memberof DotLanguagesService
     */
    get(contentInode?: string): Observable<DotLanguage[]> {
        const url = !contentInode
            ? '/api/v2/languages'
            : `/api/v2/languages?contentInode=${contentInode}`;

        return this.httpClient.get(url).pipe(pluck('entity'));
    }

    /**
     * Retrieves the languages used on a specific page.
     *
     * @param {number} pageIdentifier - The identifier of the page.
     * @return {Observable<DotLanguage[]>} An observable of the languages used on the page.
     */
    getLanguagesUsedPage(pageIdentifier: string): Observable<DotLanguage[]> {
        return this.httpClient
            .get(`/api/v1/page/${pageIdentifier}/languages`)
            .pipe(pluck('entity'));
    }
}
