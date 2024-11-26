import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck, take } from 'rxjs/operators';

import { DotCMSContentlet, DotLanguage } from '@dotcms/dotcms-models';

@Injectable()
export class DotContentletService {
    private readonly CONTENTLET_API_URL = '/api/v1/content/';

    constructor(private http: HttpClient) {}

    /**
     * Get the Contentlet versions by language.
     *
     * @param {string} identifier - The identifier of the contentlet.
     * @param {string} language - The language code to filter the versions.
     * @returns {Observable<DotCMSContentlet[]>} An observable emitting an array of contentlet versions.
     * @memberof DotContentletService
     */
    getContentletVersions(identifier: string, language: string): Observable<DotCMSContentlet[]> {
        return this.http
            .get(`${this.CONTENTLET_API_URL}versions?identifier=${identifier}&groupByLang=1`)
            .pipe(take(1), pluck('entity', 'versions', language));
    }

    /**
     * Get the Contentlet by its inode.
     *
     * @param {string} inode - The inode of the contentlet.
     * @returns {Observable<DotCMSContentlet>} An observable emitting the contentlet.
     * @memberof DotContentletService
     */
    getContentletByInode(inode: string): Observable<DotCMSContentlet> {
        return this.http.get(`${this.CONTENTLET_API_URL}${inode}`).pipe(take(1), pluck('entity'));
    }

    /**
     * Get the languages available for a Contentlet.
     *
     * @param {string} identifier - The identifier of the contentlet.
     * @returns {Observable<DotLanguage[]>} An observable emitting an array of languages.
     * @memberof DotContentletService
     */
    getLanguages(identifier: string): Observable<DotLanguage[]> {
        return this.http
            .get(`${this.CONTENTLET_API_URL}${identifier}/languages`)
            .pipe(take(1), pluck('entity'));
    }
}
