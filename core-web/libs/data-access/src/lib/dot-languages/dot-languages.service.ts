import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { filter, map, pluck } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-js';
import { DotAddLanguage, DotLanguage, DotLanguagesISO } from '@dotcms/dotcms-models';

export const LANGUAGE_API_URL = '/api/v2/languages';
export const LANGUAGE_API_URL_WITH_VARS = '/api/v2/languages?countLangVars=true';
export interface DotLanguageVariables {
    total: number;
    variables: Record<string, DotLanguageVariableEntry>;
}
export interface DotLanguageVariableEntry {
    [languageCode: string]: {
        identifier: string;
        value: string;
    };
}
/**
 * Provide util methods to get Languages available in the system.
 * @export
 * @class DotLanguagesService
 */
@Injectable({
    providedIn: 'root'
})
export class DotLanguagesService {
    private httpClient: HttpClient = inject(HttpClient);

    /**
     * Return languages.
     *
     * This method fetches the available languages from the server. If a content inode is provided,
     * it includes the content inode in the request URL to filter the languages accordingly.
     *
     * @param {string} [contentInode] - Optional content inode to filter the languages.
     * @returns {Observable<DotLanguage[]>} An observable emitting the list of languages.
     * @memberof DotLanguagesService
     */
    get(contentInode?: string): Observable<DotLanguage[]> {
        const url = !contentInode
            ? LANGUAGE_API_URL_WITH_VARS
            : `${LANGUAGE_API_URL_WITH_VARS}&contentInode=${contentInode}`;

        return this.httpClient
            .get<DotCMSResponse<DotLanguage[]>>(url)
            .pipe(map((res) => res.entity as DotLanguage[]));
    }

    /**
     * Retrieves the languages used on a specific page.
     *
     * @param {number} pageIdentifier - The identifier of the page.
     * @return {Observable<DotLanguage[]>} An observable of the languages used on the page.
     */
    getLanguagesUsedPage(pageIdentifier: string): Observable<DotLanguage[]> {
        return this.httpClient
            .get<DotCMSResponse<DotLanguage[]>>(`/api/v1/page/${pageIdentifier}/languages`)
            .pipe(map((res) => res.entity as DotLanguage[]));
    }

    /**
     * Add a new language to the system.
     *
     * @param {DotAddLanguage} language - The language to be added.
     * @return {Observable<DotLanguage>} An observable of the language added.
     */
    add(language: DotAddLanguage): Observable<DotLanguage> {
        return this.httpClient
            .post<DotCMSResponse<DotLanguage>>(LANGUAGE_API_URL, language)
            .pipe(map((res) => res.entity as DotLanguage));
    }

    /**
     * Get Language by id.
     *
     * @param {number} id
     * @return {Observable<DotLanguage>}
     */
    getById(id: number): Observable<DotLanguage> {
        return this.httpClient
            .get<DotCMSResponse<DotLanguage>>(`${LANGUAGE_API_URL}/id/${id}`)
            .pipe(map((res) => res.entity as DotLanguage));
    }

    /**
     * Get Language by ISO code.
     *
     * @param {string} isoCode
     * @return {Observable<DotLanguage>}
     */
    getByISOCode(isoCode: string): Observable<DotLanguage> {
        return this.httpClient
            .get<DotCMSResponse<DotLanguage>>(`${LANGUAGE_API_URL}/${isoCode}`)
            .pipe(map((res) => res.entity as DotLanguage));
    }

    /**
     * Update a language.
     *
     * @param {DotLanguage} locale - The language to be updated.
     * @return {Observable<DotLanguage>} An observable of the updated language.
     */
    update(locale: DotLanguage): Observable<DotLanguage> {
        const { id, languageCode, language, countryCode, country } = locale;

        return this.httpClient
            .put<
                DotCMSResponse<DotLanguage>
            >(`${LANGUAGE_API_URL}/${id}`, { languageCode, language, countryCode, country })
            .pipe(map((res) => res.entity as DotLanguage));
    }

    /**
     * Delete a language.
     *
     * @param {string} id -
     * @return {Observable<void>}
     */
    delete(id: number): Observable<void> {
        return this.httpClient
            .delete<DotCMSResponse<void>>(`${LANGUAGE_API_URL}/${id}`)
            .pipe(map((res) => res.entity));
    }

    /**
     * Make a language the default language.
     *
     * @param {number} id - The identifier of the language to be made the default.
     * @return {Observable<void>}
     */
    makeDefault(id: number): Observable<void> {
        return this.httpClient
            .put<DotCMSResponse<void>>(`${LANGUAGE_API_URL}/${id}/_makedefault`, {})
            .pipe(map((res) => res.entity));
    }

    /**
     * Get the default language.
     *
     * @returns {Observable<DotLanguage>} An observable emitting the default language.
     */
    getDefault(): Observable<DotLanguage> {
        return this.httpClient
            .get<DotCMSResponse<DotLanguage>>(`${LANGUAGE_API_URL}/_getdefault`)
            .pipe(map((res) => res.entity as DotLanguage));
    }

    /**
     * Get the ISO language codes.
     *
     * @returns {Observable<DotLanguagesISO>} An observable emitting the ISO language codes.
     */
    getISO(): Observable<DotLanguagesISO> {
        return this.httpClient
            .get<DotCMSResponse<DotLanguagesISO>>(`${LANGUAGE_API_URL}/iso`)
            .pipe(map((res) => res.entity as DotLanguagesISO));
    }

    /**
     * Get language variables.
     *
     * @returns {Observable<Record<string, DotLanguageVariableEntry>>} An observable of the language variables.
     */
    getLanguageVariables(): Observable<DotLanguageVariables['variables']> {
        return this.httpClient
            .get<DotCMSResponse<DotLanguageVariables>>(`${LANGUAGE_API_URL}/variables`)
            .pipe(map((res) => res.entity?.variables || {}));
    }
}
