import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Observable, of } from 'rxjs';

import { catchError, finalize, map, pluck, shareReplay, take, tap } from 'rxjs/operators';

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
    private messageMap: { [key: string]: string } = {};
    private lastParams: { lang: string; buildDate: string | null } = {
        lang: DEFAULT_LANG,
        buildDate: null
    };
    private inFlight$: Observable<boolean> | null = null;

    constructor(
        private readonly http: HttpClient,
        private readonly dotLocalstorageService: DotLocalstorageService
    ) {}

    /**
     * Initializes the DotMessageService.
     * @param {DotMessageServiceParams} params - The parameters for initialization.
     * @return {void}
     */
    init(params?: DotMessageServiceParams): void {
        const lang = params?.language || DEFAULT_LANG;
        const buildDate = params?.buildDate || null;

        this.lastParams = { lang, buildDate };
        this.getAll(lang, buildDate);
    }

    /**
     * Makes sure the message bundle is available for the current session.
     *
     * The bundle is requested when the application starts, which happens before the
     * user logs in; because the translation endpoint requires an authenticated caller
     * that first request can be rejected, and nothing else would ever retry it. This
     * method is called once the user is authenticated: it resolves right away when the
     * messages are already loaded and otherwise fetches them (sharing a single request
     * between concurrent callers). It never errors, so a failed download cannot block
     * the user from entering the application; the keys are then shown untranslated.
     *
     * @returns {Observable<boolean>} emits `true` when messages are available, `false` otherwise
     */
    ensureLoaded(): Observable<boolean> {
        if (Object.keys(this.messageMap).length) {
            return of(true);
        }

        if (!this.inFlight$) {
            const { lang, buildDate } = this.lastParams;
            this.inFlight$ = this.fetchMessages(lang, buildDate).pipe(
                finalize(() => (this.inFlight$ = null)),
                shareReplay({ bufferSize: 1, refCount: false })
            );
        }

        return this.inFlight$;
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
            this.fetchMessages(lang, newBuildDate).subscribe();
        } else {
            this.messageMap = this.dotLocalstorageService.getItem(MESSAGES_LOCALSTORAGE_KEY) as {
                [key: string]: string;
            };
        }
    }

    /**
     * Downloads the messages, installs them and caches them in the local storage.
     * Errors are swallowed and reported as `false`.
     */
    private fetchMessages(lang: string, buildDate: string | null): Observable<boolean> {
        return this.http.get(this.geti18nURL(lang)).pipe(
            take(1),
            pluck('entity'),
            tap((messages) => {
                this.messageMap = messages as { [key: string]: string };
                this.dotLocalstorageService.setItem(MESSAGES_LOCALSTORAGE_KEY, this.messageMap);
                this.dotLocalstorageService.setItem(LANGUAGE_LOCALSTORAGE_KEY, lang);
                this.dotLocalstorageService.setItem(BUILDATE_LOCALSTORAGE_KEY, buildDate);
            }),
            map(() => true),
            catchError(() => of(false))
        );
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
