import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import { formatMessage } from '@dotcms/utils';

import { DotLocalstorageService } from '../dot-localstorage/dot-localstorage.service';

export interface DotMessageServiceParams {
    buildDate?: string;
    language?: string;
}

const DEFAULT_LANG = 'default';
const LANGUAGE_LOCALSTORAGE_KEY = 'dotMessagesKeys-lang';
const MESSAGES_LOCALSTORAGE_KEY = 'dotMessagesKeys';
const BUILDATE_LOCALSTORAGE_KEY = 'buildDate';

@Injectable({
    providedIn: 'root'
})
export class DotMessageService {
    private readonly http = inject(HttpClient);
    private readonly dotLocalstorageService = inject(DotLocalstorageService);

    private messageMap: { [key: string]: string } = {};

    /**
     * Initializes the DotMessageService.
     * @param {DotMessageServiceParams} params - The parameters for initialization.
     * @return {void}
     */
    init(params?: DotMessageServiceParams): void {
        const lang = params?.language || DEFAULT_LANG;
        const buildDate = params?.buildDate || null;

        this.getAll(lang, buildDate);
    }

    /**
     * Retrieves the value associated with the specified key from the message map.
     *
     * @param {string} key - The key used to retrieve the value from the message map.
     * @param {...string} args - Optional arguments to be passed to the value, if it is a format string.
     * @return {string} - The value associated with the key. If the key is not found in the message map,
     *                    the key itself will be returned.
     */
    get(key: string, ...args: string[]): string {
        return this.messageMap[key]
            ? args.length
                ? formatMessage(this.messageMap[key], args)
                : this.messageMap[key]
            : key;
    }

    /**
     * Retrieves all the messages for a specific language.
     *
     * @param {string} lang - The language code for the messages.
     * @param newBuildDate
     * @private
     * @returns {void}
     */
    private getAll(lang: string = DEFAULT_LANG, newBuildDate: string | null = null): void {
        if (this.shouldReloadMessages(lang, newBuildDate)) {
            this.http
                .get(this.geti18nURL(lang))
                .pipe(
                    take(1),
                    map((x) => x?.entity)
                )
                .subscribe((messages) => {
                    this.messageMap = messages as { [key: string]: string };
                    this.dotLocalstorageService.setItem(MESSAGES_LOCALSTORAGE_KEY, this.messageMap);
                    this.dotLocalstorageService.setItem(LANGUAGE_LOCALSTORAGE_KEY, lang);
                    this.dotLocalstorageService.setItem(BUILDATE_LOCALSTORAGE_KEY, newBuildDate);
                });
        } else {
            this.messageMap = this.dotLocalstorageService.getItem(MESSAGES_LOCALSTORAGE_KEY) as {
                [key: string]: string;
            };
        }
    }

    /**
     * Returns the URL for the i18n API endpoint based on the given language.
     * If no language is provided, the default language is used.
     *
     * @param {string} lang - The language code.
     * @returns {string} The URL for the i18n API endpoint.
     * @private
     */
    private geti18nURL(lang: string): string {
        return `/api/v2/languages/${lang ? lang : 'default'}/keys`;
    }

    private shouldReloadMessages(lang: string, newBuildDate: string | null): boolean {
        const currentBuildDate = this.dotLocalstorageService.getItem(BUILDATE_LOCALSTORAGE_KEY);
        const currentLang = this.dotLocalstorageService.getItem(LANGUAGE_LOCALSTORAGE_KEY);
        const storedMessages = this.dotLocalstorageService.getItem(MESSAGES_LOCALSTORAGE_KEY);

        return <boolean>(
            (!storedMessages ||
                lang !== currentLang ||
                (newBuildDate && newBuildDate !== currentBuildDate))
        );
    }
}
