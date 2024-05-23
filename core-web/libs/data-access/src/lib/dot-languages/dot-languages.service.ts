import { Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotLanguage, DotLanguagesISO } from '@dotcms/dotcms-models';

export const LANGUAGE_API_URL = '/api/v2/languages';

export const LANGUAGE_API_URL_WITH_VARS = '/api/v2/languages?countLangVars=true';

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
            ? LANGUAGE_API_URL_WITH_VARS
            : `${LANGUAGE_API_URL_WITH_VARS}&contentInode=${contentInode}`;

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

    /**
     * Add a new language to the system.
     *
     * @param {{
     *         languageCode: string;
     *         countryCode: string;
     *         language: string;
     *         country: string;
     *     }} language - The language to be added.
     * @return {Observable<DotLanguage>} An observable of the language added.
     */
    add(language: {
        languageCode: string;
        countryCode: string;
        language: string;
        country: string;
    }): Observable<DotLanguage> {
        return this.httpClient.post(LANGUAGE_API_URL, language).pipe(pluck('entity'));
    }

    /**
     * Get Language by id.
     *
     * @param {number} id
     * @return {Observable<DotLanguage>}
     */
    getById(id: number): Observable<DotLanguage> {
        return this.httpClient.get(`${LANGUAGE_API_URL}/id/${id}`).pipe(pluck('entity'));
    }

    /**
     * Update a language.
     *
     * @param {DotLanguage} language - The language to be updated.
     * @return {Observable<DotLanguage>} An observable of the updated language.
     */
    update(language: DotLanguage): Observable<DotLanguage> {
        return this.httpClient
            .put(`${LANGUAGE_API_URL}/${language.id}`, language)
            .pipe(pluck('entity'));
    }

    /**
     * Delete a language.
     *
     * @param {string} id -
     * @return {Observable<void>}
     */
    delete(id: number): Observable<void> {
        return this.httpClient.delete(`${LANGUAGE_API_URL}/${id}`).pipe(pluck('entity'));
    }

    /**
     * Make a language the default language.
     *
     * @param {number} id - The identifier of the language to be made the default.
     * @return {Observable<void>}
     */
    makeDefault(id: number): Observable<void> {
        return this.httpClient
            .put(`${LANGUAGE_API_URL}/${id}/_makedefault`, {})
            .pipe(pluck('entity'));
    }

    getISO(): Observable<DotLanguagesISO> {
        //return this.httpClient.get(`${LANGUAGE_API_URL}/iso`).pipe(pluck('entity'));
        //placeholder
        return of({
            countries: [
                { code: 'US', name: 'United States' },
                { code: 'CA', name: 'Canada' },
                { code: 'MX', name: 'Mexico' },
                { code: 'BR', name: 'Brazil' },
                { code: 'AR', name: 'Argentina' },
                { code: 'GB', name: 'United Kingdom' },
                { code: 'FR', name: 'France' },
                { code: 'DE', name: 'Germany' },
                { code: 'IT', name: 'Italy' },
                { code: 'ES', name: 'Spain' },
                { code: 'CN', name: 'China' },
                { code: 'JP', name: 'Japan' },
                { code: 'IN', name: 'India' },
                { code: 'RU', name: 'Russia' },
                { code: 'AU', name: 'Australia' },
                { code: 'ZA', name: 'South Africa' },
                { code: 'NG', name: 'Nigeria' },
                { code: 'EG', name: 'Egypt' },
                { code: 'KE', name: 'Kenya' },
                { code: 'KR', name: 'South Korea' },
                { code: 'SA', name: 'Saudi Arabia' },
                { code: 'TR', name: 'Turkey' },
                { code: 'SE', name: 'Sweden' },
                { code: 'NO', name: 'Norway' },
                { code: 'CH', name: 'Switzerland' }
            ],
            languages: [
                { code: 'en', name: 'English' },
                { code: 'es', name: 'Spanish' },
                { code: 'fr', name: 'French' },
                { code: 'de', name: 'German' },
                { code: 'zh', name: 'Chinese' },
                { code: 'ja', name: 'Japanese' },
                { code: 'ru', name: 'Russian' },
                { code: 'hi', name: 'Hindi' },
                { code: 'ar', name: 'Arabic' },
                { code: 'pt', name: 'Portuguese' },
                { code: 'bn', name: 'Bengali' },
                { code: 'ko', name: 'Korean' },
                { code: 'it', name: 'Italian' },
                { code: 'tr', name: 'Turkish' },
                { code: 'vi', name: 'Vietnamese' },
                { code: 'pl', name: 'Polish' },
                { code: 'nl', name: 'Dutch' },
                { code: 'th', name: 'Thai' },
                { code: 'sv', name: 'Swedish' },
                { code: 'no', name: 'Norwegian' }
            ]
        });
    }
}
