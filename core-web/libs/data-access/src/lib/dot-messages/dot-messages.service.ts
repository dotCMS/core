import { Observable, of, ReplaySubject, throwError } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';

import { catchError, finalize, map, share, take, tap } from 'rxjs/operators';

import { formatMessage } from '@dotcms/utils';

import { DotLocalstorageService } from '../dot-localstorage/dot-localstorage.service';

export interface DotMessageServiceParams {
    /** @deprecated Cache freshness requires languageBundleVersion, not a build date. */
    buildDate?: string;
    language?: string;
    languageBundleVersion?: string;
}

export interface LanguageBundleVersionMismatch {
    language: string;
    cachedVersion: string;
    serverVersion: string;
}

const DEFAULT_LANG = 'default';
const LANGUAGE_LOCALSTORAGE_KEY = 'dotMessagesKeys-lang';
const MESSAGES_LOCALSTORAGE_KEY = 'dotMessagesKeys';
const VERSION_LOCALSTORAGE_KEY = 'dotMessagesKeys-version';
// Full bundles contain thousands of translations; small maps indicate a partial/bootstrap response.
const MINIMUM_TRANSLATION_KEYS = 100;

@Injectable({
    providedIn: 'root'
})
export class DotMessageService {
    private readonly http = inject(HttpClient);
    private readonly dotLocalstorageService = inject(DotLocalstorageService);

    private messageMap: Record<string, string> = {};
    private readonly inFlight = new Map<string, Observable<void>>();
    private readonly versionMismatch = signal<LanguageBundleVersionMismatch | null>(null);
    private readonly installedVersion = signal<string | null>(null);

    /** Detected version mismatch; cleared after a successful install or a matching cache check. */
    readonly bundleVersionMismatch = this.versionMismatch.asReadonly();
    /** Changes only after a validated bundle is installed, from cache or a successful request. */
    readonly loadedBundleVersion = this.installedVersion.asReadonly();

    /**
     * Initializes the DotMessageService.
     * @param {DotMessageServiceParams} params - The parameters for initialization.
     * Without an expected bundle version, always fetch: legacy build dates cannot prove freshness.
     * @return {Observable<void>} Emits and completes when valid messages are available; errors on failure.
     */
    init(params?: DotMessageServiceParams): Observable<void> {
        const lang = params?.language || DEFAULT_LANG;
        const version = this.isVersion(params?.languageBundleVersion)
            ? params.languageBundleVersion
            : null;
        const load$ = this.getAll(lang, version);

        // Keep eager loading for legacy callers, but let awaiting callers handle errors.
        load$.subscribe({ error: () => undefined });

        return load$;
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

    private getAll(lang: string, expectedVersion: string | null): Observable<void> {
        const requestKey = JSON.stringify([lang, expectedVersion]);
        const pending = this.inFlight.get(requestKey);
        if (pending) {
            return pending;
        }

        let messages: unknown = null;
        let cachedLang: unknown = null;
        let cachedVersion: unknown = null;
        try {
            messages = this.dotLocalstorageService.getItem<unknown>(MESSAGES_LOCALSTORAGE_KEY);
            cachedLang = this.dotLocalstorageService.getItem<unknown>(LANGUAGE_LOCALSTORAGE_KEY);
            cachedVersion = this.dotLocalstorageService.getItem<unknown>(VERSION_LOCALSTORAGE_KEY);
        } catch {
            // Blocked storage must not prevent an authenticated, uncached load.
            messages = null;
            cachedLang = null;
            cachedVersion = null;
        }
        const mismatch =
            expectedVersion && this.isVersion(cachedVersion) && expectedVersion !== cachedVersion
                ? { language: lang, cachedVersion, serverVersion: expectedVersion }
                : null;
        const previousMismatch = this.versionMismatch();
        if (
            previousMismatch?.language !== mismatch?.language ||
            previousMismatch?.cachedVersion !== mismatch?.cachedVersion ||
            previousMismatch?.serverVersion !== mismatch?.serverVersion
        ) {
            this.versionMismatch.set(mismatch);
        }
        if (
            expectedVersion &&
            expectedVersion === cachedVersion &&
            lang === cachedLang &&
            this.isMessageMap(messages)
        ) {
            this.messageMap = messages;
            this.installedVersion.set(expectedVersion);

            return of(void 0);
        }

        const load$ = this.http
            .get<{ entity: unknown; languageBundleVersion: unknown }>(this.geti18nURL(lang))
            .pipe(
                take(1),
                tap((response) => {
                    if (
                        !this.isMessageMap(response?.entity) ||
                        !this.isVersion(response?.languageBundleVersion)
                    ) {
                        throw new Error(
                            'Invalid language bundle: more than 100 translation keys and a version are required.'
                        );
                    }

                    this.messageMap = response.entity;
                    this.cacheMessages(lang, response.entity, response.languageBundleVersion);
                    this.installedVersion.set(response.languageBundleVersion);
                    this.versionMismatch.set(null);
                }),
                map(() => void 0),
                catchError((error) => {
                    // A stale, complete bundle is a fallback, not proof the requested version loaded.
                    if (
                        !this.isMessageMap(this.messageMap) &&
                        lang === cachedLang &&
                        this.isMessageMap(messages)
                    ) {
                        this.messageMap = messages;
                        if (this.isVersion(cachedVersion)) {
                            this.installedVersion.set(cachedVersion);
                        }
                    }

                    return throwError(() => error);
                }),
                finalize(() => this.inFlight.delete(requestKey)),
                // Retain errors as well as success for late subscribers to the eager request.
                // A new init() retries; subscribing to the old result must not start another GET.
                share({
                    connector: () => new ReplaySubject<void>(1),
                    resetOnError: false,
                    resetOnComplete: false,
                    resetOnRefCountZero: false
                })
            );
        this.inFlight.set(requestKey, load$);

        return load$;
    }

    private cacheMessages(lang: string, messages: Record<string, string>, version: string): void {
        try {
            // Invalidate first: a partial write must never retain a previously valid version.
            this.dotLocalstorageService.removeItem(VERSION_LOCALSTORAGE_KEY);
            this.dotLocalstorageService.setItem(MESSAGES_LOCALSTORAGE_KEY, messages);
            this.dotLocalstorageService.setItem(LANGUAGE_LOCALSTORAGE_KEY, lang);
            // Quote the hash to avoid DotLocalstorageService coercing digit-leading hashes to numbers.
            this.dotLocalstorageService.setItem(VERSION_LOCALSTORAGE_KEY, JSON.stringify(version));
        } catch {
            // Persistence is optional; a valid in-memory bundle still allows route activation.
            try {
                this.dotLocalstorageService.removeItem(VERSION_LOCALSTORAGE_KEY);
            } catch {
                // Storage can be entirely unavailable in restricted browser contexts.
            }
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

    private isVersion(value: unknown): value is string {
        return typeof value === 'string' && value.trim().length > 0;
    }

    private isMessageMap(value: unknown): value is Record<string, string> {
        if (!value || typeof value !== 'object' || Array.isArray(value)) {
            return false;
        }

        const entries = Object.entries(value);

        return (
            entries.filter(([key]) => key !== 'version' && key !== 'languageBundleVersion').length >
                MINIMUM_TRANSLATION_KEYS &&
            entries.every(([, translation]) => typeof translation === 'string')
        );
    }
}
