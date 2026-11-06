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
    /** Effective authenticated user; null explicitly clears the previous user context. */
    userId?: string | null;
}

export interface LanguageBundleVersionMismatch {
    language: string;
    cachedVersion: string;
    serverVersion: string;
}

const DEFAULT_LANG = 'default';
const BUNDLE_LOCALSTORAGE_KEY = 'dotMessagesBundle';
// Full bundles contain thousands of translations; small maps indicate a partial/bootstrap response.
const MINIMUM_TRANSLATION_KEYS = 100;

@Injectable({
    providedIn: 'root'
})
export class DotMessageService {
    private readonly http = inject(HttpClient);
    private readonly dotLocalstorageService = inject(DotLocalstorageService);

    private messageMap: Record<string, string> = {};
    private readonly installedLanguage = signal<string | null>(null);
    private readonly inFlight = new Map<string, Observable<void>>();
    private latestRequestedIdentity: string | null = null;
    #pendingSession = 0;
    #currentUserId: string | null = null;
    readonly #installationRevision = signal(0);
    private readonly versionMismatch = signal<LanguageBundleVersionMismatch | null>(null);
    private readonly installedVersion = signal<string | null>(null);

    /** Detected version mismatch; cleared after a successful install or a matching cache check. */
    readonly bundleVersionMismatch = this.versionMismatch.asReadonly();
    /** Changes only after a validated bundle is installed, from cache or a successful request. */
    readonly loadedBundleVersion = this.installedVersion.asReadonly();
    /** Locale of the installed messages, including a stale cache fallback. */
    readonly loadedBundleLanguage = this.installedLanguage.asReadonly();

    /** Changes whenever available messages change, including same-version installs and user resets. */
    readonly loadedBundleRevision = this.#installationRevision.asReadonly();

    /**
     * Initializes the DotMessageService.
     * @param {DotMessageServiceParams} params - The parameters for initialization.
     * Without an expected bundle version, always fetch: legacy build dates cannot prove freshness.
     * @return {Observable<void>} Emits and completes when valid messages are available; errors on failure.
     * Superseded requests settle with an error rather than reporting an installation that did not occur.
     */
    init(params?: DotMessageServiceParams): Observable<void> {
        if (params?.userId !== undefined && params.userId !== this.#currentUserId) {
            this.invalidatePendingSession(params.userId);
        }
        const lang = params?.language || DEFAULT_LANG;
        const version = this.isVersion(params?.languageBundleVersion)
            ? params.languageBundleVersion
            : null;
        const load$ = this.getAll(lang, version);

        // Keep eager loading for legacy callers, but let awaiting callers handle errors.
        load$.subscribe({ error: () => undefined });

        return load$;
    }

    /** Prevents eager requests from a previous session from installing or being reused. */
    invalidatePendingSession(userId: string | null = this.#currentUserId): void {
        this.#pendingSession++;
        this.latestRequestedIdentity = null;
        if (userId !== this.#currentUserId) {
            this.#currentUserId = userId;
            this.messageMap = {};
            this.installedLanguage.set(null);
            this.installedVersion.set(null);
            this.versionMismatch.set(null);
            this.#installationRevision.update((revision) => revision + 1);
        }
    }

    /**
     * Retrieves the value associated with the specified key from the message map.
     * Tracks the installed bundle so reactive consumers update after recovery or a locale change.
     *
     * @param {string} key - The key used to retrieve the value from the message map.
     * @param {...string} args - Optional arguments to be passed to the value, if it is a format string.
     * @return {string} - The value associated with the key. If the key is not found in the message map,
     *                    the key itself will be returned.
     */
    get(key: string, ...args: string[]): string {
        this.loadedBundleVersion();
        this.loadedBundleLanguage();
        this.loadedBundleRevision();

        return this.messageMap[key]
            ? args.length
                ? formatMessage(this.messageMap[key], args)
                : this.messageMap[key]
            : key;
    }

    private getAll(lang: string, expectedVersion: string | null): Observable<void> {
        const userId = this.#currentUserId;
        const requestKey = JSON.stringify([lang, expectedVersion, this.#pendingSession]);
        // Every selection counts, including memory/cache hits and deduplicated pending requests.
        this.latestRequestedIdentity = requestKey;
        const hasInstalledMessages =
            this.installedLanguage() === lang && this.isMessageMap(this.messageMap);
        if (
            expectedVersion &&
            hasInstalledMessages &&
            this.installedVersion() === expectedVersion
        ) {
            this.versionMismatch.set(null);

            return of(void 0);
        }

        const pending = this.inFlight.get(requestKey);
        if (pending) {
            return pending;
        }

        let messages: unknown = null;
        let cachedLang: unknown = null;
        let cachedVersion: unknown = null;
        let cachedUserId: unknown = null;
        try {
            const bundle = this.dotLocalstorageService.getItem<{
                messages?: unknown;
                language?: unknown;
                version?: unknown;
                userId?: unknown;
            }>(BUNDLE_LOCALSTORAGE_KEY);
            if (
                bundle &&
                typeof bundle === 'object' &&
                !Array.isArray(bundle) &&
                (bundle.userId === null || this.isVersion(bundle.userId))
            ) {
                messages = bundle.messages;
                cachedLang = bundle.language;
                cachedVersion = bundle.version;
                cachedUserId = bundle.userId;
            }
        } catch {
            // Blocked storage must not prevent an authenticated, uncached load.
            messages = null;
            cachedLang = null;
            cachedVersion = null;
            cachedUserId = null;
        }
        const previousVersion = hasInstalledMessages
            ? this.installedVersion()
            : lang === cachedLang && userId === cachedUserId
              ? cachedVersion
              : null;
        const mismatch =
            expectedVersion &&
            this.isVersion(previousVersion) &&
            expectedVersion !== previousVersion
                ? { language: lang, cachedVersion: previousVersion, serverVersion: expectedVersion }
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
            userId === cachedUserId &&
            lang === cachedLang &&
            this.isMessageMap(messages)
        ) {
            this.messageMap = messages;
            this.installedLanguage.set(lang);
            this.installedVersion.set(expectedVersion);
            this.#installationRevision.update((revision) => revision + 1);

            return of(void 0);
        }

        const load$ = this.http
            .get<{ entity: unknown; languageBundleVersion: unknown }>(this.geti18nURL(lang))
            .pipe(
                take(1),
                tap((response) => {
                    if (this.latestRequestedIdentity !== requestKey) {
                        throw new Error('Language bundle request superseded by a newer selection.');
                    }

                    if (
                        !this.isMessageMap(response?.entity) ||
                        !this.isVersion(response?.languageBundleVersion)
                    ) {
                        throw new Error(
                            'Invalid language bundle: more than 100 translation keys and a version are required.'
                        );
                    }

                    this.messageMap = response.entity;
                    this.installedLanguage.set(lang);
                    this.cacheMessages(
                        lang,
                        response.entity,
                        response.languageBundleVersion,
                        userId
                    );
                    this.installedVersion.set(response.languageBundleVersion);
                    this.#installationRevision.update((revision) => revision + 1);
                    this.versionMismatch.set(null);
                }),
                map(() => void 0),
                catchError((error) => {
                    // A stale, complete bundle is a fallback, not proof the requested version loaded.
                    if (
                        this.latestRequestedIdentity === requestKey &&
                        (this.installedLanguage() !== lang ||
                            !this.isMessageMap(this.messageMap)) &&
                        lang === cachedLang &&
                        userId === cachedUserId &&
                        this.isMessageMap(messages)
                    ) {
                        this.messageMap = messages;
                        this.installedLanguage.set(lang);
                        this.installedVersion.set(
                            this.isVersion(cachedVersion) ? cachedVersion : null
                        );
                        this.#installationRevision.update((revision) => revision + 1);
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

    private cacheMessages(
        lang: string,
        messages: Record<string, string>,
        version: string,
        userId: string | null
    ): void {
        try {
            // One atomic write keeps a tab's messages and ownership metadata inseparable.
            this.dotLocalstorageService.setItem(BUNDLE_LOCALSTORAGE_KEY, {
                messages,
                language: lang,
                version,
                userId
            });
        } catch {
            // Keep the previous complete record; valid in-memory messages do not require storage.
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
