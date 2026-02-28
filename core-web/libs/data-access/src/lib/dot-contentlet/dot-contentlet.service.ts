import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck, switchMap } from 'rxjs/operators';

import {
    DotCMSAPIResponse,
    DotCMSContentlet,
    DotContentletCanLock,
    DotLanguage
} from '@dotcms/dotcms-models';

import { DotUploadFileService } from '../dot-upload-file/dot-upload-file.service';

@Injectable({
    providedIn: 'root'
})
export class DotContentletService {
    readonly #http = inject(HttpClient);
    readonly #dotUploadFileService = inject(DotUploadFileService);

    private readonly CONTENTLET_API_URL = '/api/v1/content/';

    /**
     * Get the Contentlet versions by language.
     *
     * @param {string} identifier - The identifier of the contentlet.
     * @param {string} language - The language code to filter the versions.
     * @returns {Observable<DotCMSContentlet[]>} An observable emitting an array of contentlet versions.
     * @memberof DotContentletService
     */
    getContentletVersions(identifier: string, language: string): Observable<DotCMSContentlet[]> {
        return this.#http
            .get<
                DotCMSAPIResponse<DotCMSContentlet[]>
            >(`${this.CONTENTLET_API_URL}versions?identifier=${identifier}&groupByLang=1`)
            .pipe(pluck('entity', 'versions', language));
    }

    /**
     * Get the Contentlet by its inode.
     *
     * @param {string} inode - The inode of the contentlet.
     * @returns {Observable<DotCMSContentlet>} An observable emitting the contentlet.
     * @memberof DotContentletService
     */
    getContentletByInode(inode: string, httpParams?: HttpParams): Observable<DotCMSContentlet> {
        return this.#http
            .get<
                DotCMSAPIResponse<DotCMSContentlet>
            >(`${this.CONTENTLET_API_URL}${inode}`, { params: httpParams })
            .pipe(map((response) => response.entity));
    }

    /**
     * Get the Contentlet by its inode and adds the content if it's a editable as text file.
     *
     * @param {string} inode - The inode of the contentlet.
     * @returns {Observable<DotCMSContentlet>} An observable emitting the contentlet.
     * @memberof DotContentletService
     */
    getContentletByInodeWithContent(
        inode: string,
        httpParams?: HttpParams
    ): Observable<DotCMSContentlet> {
        return this.getContentletByInode(inode, httpParams).pipe(
            switchMap((contentlet) => this.#dotUploadFileService.addContent(contentlet))
        );
    }

    /**
     * Get the languages available for a Contentlet.
     *
     * @param {string} identifier - The identifier of the contentlet.
     * @returns {Observable<DotLanguage[]>} An observable emitting an array of languages.
     * @memberof DotContentletService
     */
    getLanguages(identifier: string): Observable<DotLanguage[]> {
        return this.#http
            .get<
                DotCMSAPIResponse<DotLanguage[]>
            >(`${this.CONTENTLET_API_URL}${identifier}/languages`)
            .pipe(map((response) => response.entity));
    }

    /**
     * Lock a contentlet.
     *
     * @param {string} inode - The inode of the contentlet.
     * @returns {Observable<DotCMSContentlet>} An observable emitting the contentlet.
     * @memberof DotContentletService
     */
    lockContent(inode: string): Observable<DotCMSContentlet> {
        return this.#http
            .put<
                DotCMSAPIResponse<DotCMSContentlet>
            >(`${this.CONTENTLET_API_URL}_lock/${inode}`, {})
            .pipe(map((response) => response.entity));
    }

    /**
     * Unlock a contentlet.
     *
     * @param {string} inode - The inode of the contentlet.
     * @returns {Observable<DotCMSContentlet>} An observable emitting the contentlet.
     * @memberof DotContentletService
     */
    unlockContent(inode: string): Observable<DotCMSContentlet> {
        return this.#http
            .put<
                DotCMSAPIResponse<DotCMSContentlet>
            >(`${this.CONTENTLET_API_URL}_unlock/${inode}`, {})
            .pipe(map((response) => response.entity));
    }

    /**
     * Check if the contentlet can be locked.
     *
     * @param {string} inode - The inode of the contentlet.
     * @returns {Observable<DotContentletCanLock>} An observable emitting the contentlet can lock.
     * @memberof DotContentletService
     */
    canLock(inode: string): Observable<DotContentletCanLock> {
        return this.#http
            .get<
                DotCMSAPIResponse<DotContentletCanLock>
            >(`${this.CONTENTLET_API_URL}_canlock/${inode}`)
            .pipe(map((response) => response.entity));
    }
}
