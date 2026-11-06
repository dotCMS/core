import { defer, firstValueFrom, throwError } from 'rxjs';
import { vi } from 'vitest';

import { HttpClient, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { computed, effect } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotMessageService } from './dot-messages.service';

import { DotLocalstorageService } from '../dot-localstorage/dot-localstorage.service';

const DEFAULT_LANG = 'default';
const LANGUAGE_LOCALSTORAGE_KEY = 'dotMessagesKeys-lang';
const MESSAGES_LOCALSTORAGE_KEY = 'dotMessagesKeys';
const VERSION_LOCALSTORAGE_KEY = 'dotMessagesKeys-version';
const USER_LOCALSTORAGE_KEY = 'dotMessagesKeys-user';
const BUILDDATE_LOCALSTORAGE_KEY = 'buildDate';
const VERSION = 'a'.repeat(64);
const NEXT_VERSION = '1' + 'b'.repeat(63);
const BUNDLE_LOCALSTORAGE_KEY = 'dotMessagesBundle';
const DEFAULT_URL = '/api/v2/languages/default/keys';

const createMessages = (count: number): Record<string, string> =>
    Object.fromEntries(
        Array.from({ length: count }, (_, index) => [`key.${index}`, `Message ${index}`])
    );

const messages = {
    ...createMessages(101),
    'dot.common.cancel': 'Cancel',
    'dot.common.accept': 'Accept {0}'
};

const clearCache = () => {
    [
        LANGUAGE_LOCALSTORAGE_KEY,
        MESSAGES_LOCALSTORAGE_KEY,
        VERSION_LOCALSTORAGE_KEY,
        USER_LOCALSTORAGE_KEY,
        BUILDDATE_LOCALSTORAGE_KEY,
        BUNDLE_LOCALSTORAGE_KEY
    ].forEach((key) => localStorage.removeItem(key));
};

describe('DotMessageService', () => {
    let service: DotMessageService;
    let http: HttpTestingController;
    let storage: DotLocalstorageService;

    const cachedBundle = () =>
        storage.getItem<{
            messages?: Record<string, string>;
            language?: unknown;
            version?: unknown;
            userId?: unknown;
        }>(BUNDLE_LOCALSTORAGE_KEY);
    const seedCache = (cachedMessages: unknown = messages, language = DEFAULT_LANG) => {
        storage.setItem(BUNDLE_LOCALSTORAGE_KEY, {
            messages: cachedMessages,
            language,
            version: VERSION,
            userId: null
        });
    };

    beforeEach(() => {
        clearCache();
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()]
        });
        service = TestBed.inject(DotMessageService);
        http = TestBed.inject(HttpTestingController);
        storage = TestBed.inject(DotLocalstorageService);
    });

    afterEach(() => {
        http.verify();
        clearCache();
        vi.restoreAllMocks();
    });

    describe('user-scoped bundles', () => {
        it('never pairs a failed write for one user with another user cache metadata', () => {
            service.init({ userId: 'principal', languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
            const persist = vi.spyOn(storage, 'setItem').mockImplementation(() => {
                throw new DOMException('Storage quota exceeded', 'QuotaExceededError');
            });
            service.init({ userId: 'other-user', languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({
                entity: { ...messages, 'dot.common.cancel': 'Other user' },
                languageBundleVersion: VERSION
            });
            expect(cachedBundle()?.userId).toBe('principal');
            expect(cachedBundle()?.messages?.['dot.common.cancel']).toBe('Cancel');
            expect(service.get('dot.common.cancel')).toBe('Other user');
            persist.mockRestore();
            const restored = TestBed.runInInjectionContext(() => new DotMessageService());
            restored.init({ userId: 'other-user', languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush(null, { status: 500, statusText: 'Server Error' });
            expect(restored.get('dot.common.cancel')).toBe('dot.common.cancel');
        });

        it.each([
            null,
            [],
            'invalid',
            {},
            { userId: 42 },
            { messages, language: DEFAULT_LANG, version: VERSION }
        ])('does not trust a malformed atomic cache record (case %#)', (record) => {
            storage.setItem(BUNDLE_LOCALSTORAGE_KEY, record);
            service.init({ userId: 'principal', languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush(null, { status: 500, statusText: 'Server Error' });
            expect(service.get('dot.common.cancel')).toBe('dot.common.cancel');
        });

        it('does not reuse an installed same-locale/version bundle for an impersonated user', () => {
            service.init({ userId: 'principal', languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
            service.init({ userId: 'principal', languageBundleVersion: VERSION });
            http.expectNone(DEFAULT_URL);
            service.invalidatePendingSession('impersonated');
            expect(service.get('dot.common.cancel')).toBe('dot.common.cancel');
            service.init({ userId: 'impersonated', languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({
                entity: { ...messages, 'dot.common.cancel': 'Scoped cancel' },
                languageBundleVersion: VERSION
            });
            expect(service.get('dot.common.cancel')).toBe('Scoped cancel');
            expect(cachedBundle()?.userId ?? null).toBe('impersonated');
        });

        it.each(['other-user', null])(
            'does not restore another user cache on failure for %s',
            (userId) => {
                service.init({ userId: 'principal', languageBundleVersion: VERSION });
                http.expectOne(DEFAULT_URL).flush({
                    entity: messages,
                    languageBundleVersion: VERSION
                });
                service.init({ userId, languageBundleVersion: VERSION });
                http.expectOne(DEFAULT_URL).flush(null, {
                    status: 500,
                    statusText: 'Server Error'
                });
                expect(service.get('dot.common.cancel')).toBe('dot.common.cancel');
                expect(service.loadedBundleVersion()).toBeNull();
            }
        );

        it('reuses a persistent cache only for the same effective user, including numeric IDs', () => {
            service.init({ userId: '123', languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
            const restored = TestBed.runInInjectionContext(() => new DotMessageService());
            restored.init({ userId: '123', languageBundleVersion: VERSION });
            http.expectNone(DEFAULT_URL);
            expect(restored.get('dot.common.cancel')).toBe('Cancel');
            restored.init({ userId: '456', languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({
                entity: createMessages(101),
                languageBundleVersion: VERSION
            });
            expect(restored.get('dot.common.cancel')).toBe('dot.common.cancel');
        });

        it('does not treat legacy split-field caches as an authenticated bundle', () => {
            storage.setItem(MESSAGES_LOCALSTORAGE_KEY, messages);
            storage.setItem(LANGUAGE_LOCALSTORAGE_KEY, DEFAULT_LANG);
            storage.setItem(VERSION_LOCALSTORAGE_KEY, JSON.stringify(VERSION));
            service.init({ userId: 'principal', languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush(null, { status: 500, statusText: 'Server Error' });
            expect(service.get('dot.common.cancel')).toBe('dot.common.cancel');
        });

        it('clears principal messages on logout before another request starts', () => {
            service.init({ userId: 'principal', languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
            service.invalidatePendingSession(null);
            expect(service.get('dot.common.cancel')).toBe('dot.common.cancel');
            expect(service.loadedBundleLanguage()).toBeNull();
            expect(service.loadedBundleVersion()).toBeNull();
        });
    });

    describe('init', () => {
        it('rejects a previous session response before the new session starts its bundle request', () => {
            const failed = vi.fn();
            service.init({ languageBundleVersion: VERSION }).subscribe({ error: failed });
            const older = http.expectOne(DEFAULT_URL);
            service.invalidatePendingSession();
            older.flush({ entity: messages, languageBundleVersion: VERSION });
            expect(failed).toHaveBeenCalledExactlyOnceWith(
                expect.objectContaining({ message: expect.stringContaining('superseded') })
            );
            expect(service.loadedBundleVersion()).toBeNull();
            expect(cachedBundle()?.messages ?? null).toBeNull();
        });

        it.each(['older-first', 'newer-first'])(
            'does not share or install a previous session request for the same locale and version (%s)',
            (order) => {
                const params = { languageBundleVersion: VERSION };
                const failed = vi.fn();
                const older$ = service.init(params);
                older$.subscribe({ error: failed });
                const older = http.expectOne(DEFAULT_URL);
                service.invalidatePendingSession();
                const newer$ = service.init(params);
                const newer = http.expectOne(DEFAULT_URL);
                expect(newer$).not.toBe(older$);
                const finishOlder = () =>
                    older.flush({ entity: messages, languageBundleVersion: VERSION });
                const finishNewer = () =>
                    newer.flush({
                        entity: { ...messages, 'dot.common.cancel': 'Current session' },
                        languageBundleVersion: VERSION
                    });
                if (order === 'older-first') {
                    finishOlder();
                    expect(service.init(params)).toBe(newer$);
                    finishNewer();
                } else {
                    finishNewer();
                    finishOlder();
                }
                expect(failed).toHaveBeenCalledOnce();
                expect(service.get('dot.common.cancel')).toBe('Current session');
                expect(cachedBundle()?.messages?.['dot.common.cancel']).toBe('Current session');
            }
        );

        it.each(['locale', 'version'])(
            'should reject an older %s response without installing or caching it',
            (identity) => {
                const failed = vi.fn();
                const next = vi.fn();
                const complete = vi.fn();
                const older$ = service.init({ languageBundleVersion: VERSION });
                older$.subscribe({ next, complete, error: failed });
                const older = http.expectOne(DEFAULT_URL);
                const language = identity === 'locale' ? 'es' : DEFAULT_LANG;
                service.init({ language, languageBundleVersion: NEXT_VERSION });
                http.expectOne(`/api/v2/languages/${language}/keys`).flush({
                    entity: { ...messages, 'dot.common.cancel': 'Newest' },
                    languageBundleVersion: NEXT_VERSION
                });
                const persist = vi.spyOn(storage, 'setItem');

                older.flush({ entity: messages, languageBundleVersion: VERSION });

                expect(failed).toHaveBeenCalledExactlyOnceWith(
                    expect.objectContaining({ message: expect.stringContaining('superseded') })
                );
                expect(next).not.toHaveBeenCalled();
                expect(complete).not.toHaveBeenCalled();
                expect(service.get('dot.common.cancel')).toBe('Newest');
                expect(service.loadedBundleLanguage()).toBe(language);
                expect(service.loadedBundleVersion()).toBe(NEXT_VERSION);
                expect(service.bundleVersionMismatch()).toBeNull();
                expect(persist).not.toHaveBeenCalled();
                const lateError = vi.fn();
                older$.subscribe({ error: lateError });
                expect(lateError).toHaveBeenCalledOnce();
                http.expectNone(DEFAULT_URL);
            }
        );

        it.each(['older-first', 'newer-first'])(
            'should honor A -> B -> A pending selection (%s)',
            (order) => {
                const params = { languageBundleVersion: VERSION };
                const a$ = service.init(params);
                const a = http.expectOne(DEFAULT_URL);
                const bError = vi.fn();
                service
                    .init({ language: 'es', languageBundleVersion: NEXT_VERSION })
                    .subscribe({ error: bError });
                const b = http.expectOne('/api/v2/languages/es/keys');
                const selected$ = service.init(params);
                expect(selected$).toBe(a$);
                const next = vi.fn();
                const complete = vi.fn();
                selected$.subscribe({ next, complete });
                const finishA = () => a.flush({ entity: messages, languageBundleVersion: VERSION });
                const finishB = () =>
                    b.flush({
                        entity: { ...messages, 'dot.common.cancel': 'Old B' },
                        languageBundleVersion: NEXT_VERSION
                    });
                if (order === 'older-first') {
                    finishA();
                    finishB();
                } else {
                    finishB();
                    finishA();
                }
                expect(next).toHaveBeenCalledExactlyOnceWith(undefined);
                expect(complete).toHaveBeenCalledOnce();
                expect(bError).toHaveBeenCalledOnce();
                expect(service.loadedBundleLanguage()).toBe(DEFAULT_LANG);
                expect(service.loadedBundleVersion()).toBe(VERSION);
                expect(service.get('dot.common.cancel')).toBe('Cancel');
                expect(cachedBundle()?.language ?? null).toBe(DEFAULT_LANG);
                expect(cachedBundle()?.version ?? null).toBe(VERSION);
            }
        );

        it.each(['memory', 'cache'])(
            'should let a %s selection supersede pending success and fallback',
            (source) => {
                seedCache(messages, 'es');
                const successError = vi.fn();
                service
                    .init({ language: 'es', languageBundleVersion: NEXT_VERSION })
                    .subscribe({ error: successError });
                const pendingSuccess = http.expectOne('/api/v2/languages/es/keys');
                const failureError = vi.fn();
                service
                    .init({ language: 'es', languageBundleVersion: 'third-version' })
                    .subscribe({ error: failureError });
                const pendingFailure = http.expectOne('/api/v2/languages/es/keys');
                seedCache();
                if (source === 'memory') {
                    service.init({ languageBundleVersion: VERSION });
                    seedCache(messages, 'es');
                }
                service.init({ languageBundleVersion: VERSION });
                const persist = vi.spyOn(storage, 'setItem');
                pendingSuccess.flush({
                    entity: { ...messages, 'dot.common.cancel': 'Old' },
                    languageBundleVersion: NEXT_VERSION
                });
                pendingFailure.flush('failed', { status: 500, statusText: 'Server Error' });

                expect(successError).toHaveBeenCalledOnce();
                expect(failureError).toHaveBeenCalledExactlyOnceWith(
                    expect.objectContaining({ status: 500 })
                );
                expect(service.loadedBundleLanguage()).toBe(DEFAULT_LANG);
                expect(service.loadedBundleVersion()).toBe(VERSION);
                expect(service.get('dot.common.cancel')).toBe('Cancel');
                expect(service.bundleVersionMismatch()).toBeNull();
                expect(persist).not.toHaveBeenCalled();
                http.expectNone(DEFAULT_URL);
            }
        );
        it('should eagerly fetch default messages for callers that do not subscribe', () => {
            service.init();

            const request = http.expectOne(DEFAULT_URL);
            expect(request.request.method).toBe('GET');
            request.flush({ entity: messages, languageBundleVersion: VERSION });

            expect(service.get('dot.common.cancel')).toBe('Cancel');
            expect(cachedBundle()?.messages ?? null).toEqual(messages);
            expect(cachedBundle()?.language ?? null).toBe(DEFAULT_LANG);
            expect(cachedBundle()?.version ?? null).toBe(VERSION);
            expect(cachedBundle()?.messages ?? null).not.toHaveProperty('version');
        });

        it('should emit and complete only after valid messages are loaded', () => {
            const next = vi.fn();
            const complete = vi.fn();
            const load$ = service.init({ language: 'en_US', languageBundleVersion: VERSION });
            load$.subscribe({ next, complete });

            expect(next).not.toHaveBeenCalled();
            expect(complete).not.toHaveBeenCalled();
            http.expectOne('/api/v2/languages/en_US/keys').flush({
                entity: messages,
                languageBundleVersion: VERSION
            });

            expect(next).toHaveBeenCalledExactlyOnceWith(undefined);
            expect(complete).toHaveBeenCalledOnce();
            expect(cachedBundle()?.language ?? null).toBe('en_US');
        });

        it('should reuse a valid cache only for the same locale and nonempty expected hash', async () => {
            seedCache();

            await firstValueFrom(service.init({ languageBundleVersion: VERSION }));

            http.expectNone(DEFAULT_URL);
            expect(service.get('dot.common.cancel')).toBe('Cancel');
        });

        it('should fetch when the expected hash changes, even with the same build date', async () => {
            seedCache();
            storage.setItem(BUILDDATE_LOCALSTORAGE_KEY, '2020-01-01');
            const loaded = firstValueFrom(
                service.init({ buildDate: '2020-01-01', languageBundleVersion: NEXT_VERSION })
            );

            http.expectOne(DEFAULT_URL).flush({
                entity: messages,
                languageBundleVersion: NEXT_VERSION
            });
            await loaded;
            expect(cachedBundle()?.version ?? null).toBe(NEXT_VERSION);
        });

        it('should reuse a matching cache containing exactly 101 translations', async () => {
            seedCache(createMessages(101));
            await firstValueFrom(service.init({ languageBundleVersion: VERSION }));
            http.expectNone(DEFAULT_URL);
            expect(service.get('key.100')).toBe('Message 100');
        });

        it.each([1, 100])(
            'should reject a %i-key response without advancing the version',
            async (count) => {
                seedCache();
                await firstValueFrom(service.init({ languageBundleVersion: VERSION }));
                const loaded = firstValueFrom(
                    service.init({ languageBundleVersion: NEXT_VERSION })
                );
                const rejected = expect(loaded).rejects.toThrow('more than 100 translation keys');
                http.expectOne(DEFAULT_URL).flush({
                    entity: createMessages(count),
                    languageBundleVersion: NEXT_VERSION
                });
                await rejected;
                expect(cachedBundle()?.version ?? null).toBe(VERSION);
                expect(service.loadedBundleVersion()).toBe(VERSION);
                expect(service.get('dot.common.cancel')).toBe('Cancel');
            }
        );

        it('should not count version metadata toward the minimum number of translations', async () => {
            const loaded = firstValueFrom(service.init({ languageBundleVersion: VERSION }));
            const rejected = expect(loaded).rejects.toThrow('more than 100 translation keys');
            http.expectOne(DEFAULT_URL).flush({
                entity: {
                    ...createMessages(100),
                    version: VERSION,
                    languageBundleVersion: VERSION
                },
                languageBundleVersion: VERSION
            });
            await rejected;
            expect(cachedBundle()?.version ?? null).toBeNull();
            expect(service.loadedBundleVersion()).toBeNull();
        });

        it('clears a stale in-memory mismatch when another tab has cached the matching version', () => {
            service.init({ languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
            storage.setItem(BUNDLE_LOCALSTORAGE_KEY, {
                messages: { ...messages, 'dot.common.cancel': 'Cached update' },
                language: DEFAULT_LANG,
                userId: null,
                version: NEXT_VERSION
            });
            service.init({ languageBundleVersion: NEXT_VERSION });
            http.expectNone(DEFAULT_URL);
            expect(service.get('dot.common.cancel')).toBe('Cached update');
            expect(service.loadedBundleVersion()).toBe(NEXT_VERSION);
            expect(service.bundleVersionMismatch()).toBeNull();
        });

        it('should accept 101 translations and persist one complete atomic record', async () => {
            const persist = vi.spyOn(storage, 'setItem');
            const loaded = firstValueFrom(service.init({ languageBundleVersion: VERSION }));
            http.expectOne(DEFAULT_URL).flush({
                entity: createMessages(101),
                languageBundleVersion: VERSION
            });
            await loaded;
            expect(persist).toHaveBeenCalledExactlyOnceWith(BUNDLE_LOCALSTORAGE_KEY, {
                messages: createMessages(101),
                language: DEFAULT_LANG,
                version: VERSION,
                userId: null
            });
            expect(service.loadedBundleVersion()).toBe(VERSION);
            expect(service.get('key.100')).toBe('Message 100');
        });

        it('should fetch when the locale changes despite an equal hash', async () => {
            seedCache();
            const loaded = firstValueFrom(
                service.init({ language: 'es_ES', languageBundleVersion: VERSION })
            );

            http.expectOne('/api/v2/languages/es_ES/keys').flush({
                entity: { ...messages, 'dot.common.cancel': 'Cancelar' },
                languageBundleVersion: VERSION
            });
            await loaded;
            expect(service.get('dot.common.cancel')).toBe('Cancelar');
        });

        it.each([undefined, null, '', '   ', 123])(
            'should fetch when the expected version is missing or invalid: %s',
            async (version) => {
                seedCache();
                const loaded = firstValueFrom(
                    service.init({ languageBundleVersion: version as string })
                );

                http.expectOne(DEFAULT_URL).flush({
                    entity: messages,
                    languageBundleVersion: VERSION
                });
                await loaded;
            }
        );

        it.each([undefined, null, '', '   ', 123])(
            'should fetch when the cached version is missing or invalid: %s',
            async (version) => {
                seedCache();
                storage.setItem(BUNDLE_LOCALSTORAGE_KEY, { ...cachedBundle(), version });
                const loaded = firstValueFrom(service.init({ languageBundleVersion: VERSION }));

                http.expectOne(DEFAULT_URL).flush({
                    entity: messages,
                    languageBundleVersion: VERSION
                });
                await loaded;
            }
        );

        it.each([
            null,
            {},
            [],
            [messages],
            '{invalid-json',
            42,
            { 'dot.common.cancel': null },
            { ...messages, invalid: 42 },
            createMessages(1),
            createMessages(100),
            { version: VERSION },
            { languageBundleVersion: VERSION },
            { version: VERSION, languageBundleVersion: VERSION }
        ])(
            'should fetch for an undersized, malformed or metadata-only cache (case %#)',
            async (cached) => {
                seedCache(cached);
                const loaded = firstValueFrom(service.init({ languageBundleVersion: VERSION }));

                http.expectOne(DEFAULT_URL).flush({
                    entity: messages,
                    languageBundleVersion: VERSION
                });
                await loaded;
                expect(service.get('dot.common.cancel')).toBe('Cancel');
            }
        );

        it('should fetch when the cached message map is missing', async () => {
            seedCache();
            storage.setItem(BUNDLE_LOCALSTORAGE_KEY, { ...cachedBundle(), messages: null });
            const loaded = firstValueFrom(service.init({ languageBundleVersion: VERSION }));

            http.expectOne(DEFAULT_URL).flush({
                entity: messages,
                languageBundleVersion: VERSION
            });
            await loaded;
        });

        it('should not treat missing legacy build dates as a valid version match', async () => {
            seedCache();
            storage.setItem(BUNDLE_LOCALSTORAGE_KEY, { ...cachedBundle(), version: null });
            storage.setItem(BUILDDATE_LOCALSTORAGE_KEY, null);
            const loaded = firstValueFrom(service.init());

            http.expectOne(DEFAULT_URL).flush({
                entity: messages,
                languageBundleVersion: VERSION
            });
            await loaded;
        });

        it('should fetch again for unversioned callers rather than claim cache freshness', async () => {
            service.init();
            http.expectOne(DEFAULT_URL).flush({
                entity: messages,
                languageBundleVersion: VERSION
            });
            const loaded = firstValueFrom(service.init());

            http.expectOne(DEFAULT_URL).flush({
                entity: messages,
                languageBundleVersion: VERSION
            });
            await loaded;
        });

        it('should store the response hash, not the expected hash, and round-trip digit-leading hashes', async () => {
            const loaded = firstValueFrom(service.init({ languageBundleVersion: VERSION }));
            http.expectOne(DEFAULT_URL).flush({
                entity: messages,
                languageBundleVersion: NEXT_VERSION
            });
            await loaded;

            expect(cachedBundle()?.version ?? null).toBe(NEXT_VERSION);
            await firstValueFrom(service.init({ languageBundleVersion: NEXT_VERSION }));
            http.expectNone(DEFAULT_URL);

            const reloaded = firstValueFrom(service.init({ languageBundleVersion: VERSION }));
            http.expectOne(DEFAULT_URL).flush({
                entity: messages,
                languageBundleVersion: VERSION
            });
            await reloaded;
        });

        it('should share a single in-flight request for the same locale and version', async () => {
            const params = { language: 'en_US', languageBundleVersion: VERSION };
            const first$ = service.init(params);
            const second$ = service.init(params);
            expect(second$).toBe(first$);
            const first = firstValueFrom(first$);
            const second = firstValueFrom(second$);

            http.expectOne('/api/v2/languages/en_US/keys').flush({
                entity: messages,
                languageBundleVersion: VERSION
            });
            await Promise.all([first, second]);

            // The completed request must be gone: a different version forces a new GET.
            const third$ = service.init({ ...params, languageBundleVersion: NEXT_VERSION });
            expect(third$).not.toBe(first$);
            http.expectOne('/api/v2/languages/en_US/keys').flush({
                entity: messages,
                languageBundleVersion: VERSION
            });
            await firstValueFrom(third$);
        });

        it('should share in-flight requests even when no expected version was supplied', async () => {
            const first$ = service.init();
            expect(service.init()).toBe(first$);

            http.expectOne(DEFAULT_URL).flush({
                entity: messages,
                languageBundleVersion: VERSION
            });
            await firstValueFrom(first$);
        });

        it('should not deduplicate different locales or expected versions', async () => {
            const first$ = service.init({ languageBundleVersion: VERSION });
            const second$ = service.init({ languageBundleVersion: NEXT_VERSION });
            const third$ = service.init({ language: 'es_ES', languageBundleVersion: VERSION });
            expect(second$).not.toBe(first$);
            expect(third$).not.toBe(first$);

            const requests = http.match(DEFAULT_URL);
            expect(requests).toHaveLength(2);
            requests.forEach((request) =>
                request.flush({ entity: messages, languageBundleVersion: VERSION })
            );
            http.expectOne('/api/v2/languages/es_ES/keys').flush({
                entity: messages,
                languageBundleVersion: VERSION
            });
            await expect(firstValueFrom(first$)).rejects.toThrow('superseded');
            await expect(firstValueFrom(second$)).rejects.toThrow('superseded');
            await expect(firstValueFrom(third$)).resolves.toBeUndefined();
        });

        it('should replay success to late subscribers without another GET', async () => {
            const load$ = service.init({ languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({
                entity: messages,
                languageBundleVersion: VERSION
            });

            await firstValueFrom(load$);
            await firstValueFrom(load$);
            http.expectNone(DEFAULT_URL);
        });

        it.each([NEXT_VERSION, undefined])(
            'should restore a same-language warm cache on fetch failure (expected %s)',
            async (expectedVersion) => {
                seedCache();
                const loaded = firstValueFrom(
                    service.init({ languageBundleVersion: expectedVersion })
                );
                const failed = expect(loaded).rejects.toMatchObject({ status: 500 });
                http.expectOne(DEFAULT_URL).flush(null, {
                    status: 500,
                    statusText: 'Server Error'
                });
                await failed;
                expect(service.get('dot.common.cancel')).toBe('Cancel');
                expect(service.loadedBundleVersion()).toBe(VERSION);
                expect(cachedBundle()?.version ?? null).toBe(VERSION);

                const retry = firstValueFrom(
                    service.init({ languageBundleVersion: expectedVersion })
                );
                http.expectOne(DEFAULT_URL).flush({
                    entity: messages,
                    languageBundleVersion: NEXT_VERSION
                });
                await retry;
                expect(service.loadedBundleVersion()).toBe(NEXT_VERSION);
            }
        );

        it.each([createMessages(100), null])(
            'should not restore an incomplete warm cache (case %#)',
            async (cached) => {
                seedCache(cached);
                const failed = expect(
                    firstValueFrom(service.init({ languageBundleVersion: NEXT_VERSION }))
                ).rejects.toMatchObject({ status: 500 });
                http.expectOne(DEFAULT_URL).flush(null, {
                    status: 500,
                    statusText: 'Server Error'
                });
                await failed;
                expect(service.get('dot.common.cancel')).toBe('dot.common.cancel');
                expect(service.loadedBundleVersion()).toBeNull();
            }
        );

        it('should not restore a warm cache from a different language', async () => {
            seedCache(messages, 'es_ES');
            const failed = expect(
                firstValueFrom(service.init({ languageBundleVersion: NEXT_VERSION }))
            ).rejects.toMatchObject({ status: 500 });
            http.expectOne(DEFAULT_URL).flush(null, { status: 500, statusText: 'Server Error' });
            await failed;
            expect(service.get('dot.common.cancel')).toBe('dot.common.cancel');
            expect(service.loadedBundleVersion()).toBeNull();
        });

        it('should preserve valid messages on network failure and retry on a new init', async () => {
            seedCache();
            await firstValueFrom(service.init({ languageBundleVersion: VERSION }));
            const setItem = vi.spyOn(storage, 'setItem');
            const load$ = service.init({ languageBundleVersion: NEXT_VERSION });
            const failed = expect(firstValueFrom(load$)).rejects.toMatchObject({ status: 500 });

            http.expectOne(DEFAULT_URL).flush(null, { status: 500, statusText: 'Server Error' });
            await failed;
            expect(setItem).not.toHaveBeenCalled();
            expect(service.get('dot.common.cancel')).toBe('Cancel');
            expect(cachedBundle()?.version ?? null).toBe(VERSION);
            expect(cachedBundle()?.messages ?? null).toEqual(messages);

            await expect(firstValueFrom(load$)).rejects.toMatchObject({ status: 500 });
            http.expectNone(DEFAULT_URL);

            const retry$ = service.init({ languageBundleVersion: NEXT_VERSION });
            expect(retry$).not.toBe(load$);
            http.expectOne(DEFAULT_URL).flush({
                entity: { ...messages, 'dot.common.cancel': 'Updated cancel' },
                languageBundleVersion: NEXT_VERSION
            });
            await firstValueFrom(retry$);
            expect(service.get('dot.common.cancel')).toBe('Updated cancel');
        });

        it('keeps a complete previous record when an atomic cache write fails', async () => {
            seedCache();
            vi.spyOn(storage, 'setItem').mockImplementation(() => {
                throw new DOMException('Storage quota exceeded', 'QuotaExceededError');
            });
            const loaded = firstValueFrom(service.init({ languageBundleVersion: NEXT_VERSION }));
            http.expectOne(DEFAULT_URL).flush({
                entity: { ...messages, 'dot.common.cancel': 'Updated cancel' },
                languageBundleVersion: NEXT_VERSION
            });
            await loaded;
            expect(service.get('dot.common.cancel')).toBe('Updated cancel');
            expect(service.loadedBundleVersion()).toBe(NEXT_VERSION);
            expect(cachedBundle()).toEqual({
                messages,
                language: DEFAULT_LANG,
                version: VERSION,
                userId: null
            });
        });

        it('should load without persistence when storage is completely unavailable', async () => {
            vi.spyOn(storage, 'getItem').mockImplementation(() => {
                throw new DOMException('Storage blocked', 'SecurityError');
            });
            vi.spyOn(storage, 'removeItem').mockImplementation(() => {
                throw new DOMException('Storage blocked', 'SecurityError');
            });
            const loaded = firstValueFrom(service.init({ languageBundleVersion: VERSION }));
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
            await loaded;
            expect(service.get('dot.common.cancel')).toBe('Cancel');
            expect(service.loadedBundleVersion()).toBe(VERSION);
        });

        it.each(['blocked', 'quota'])(
            'should reuse installed messages when storage is %s',
            async (failure) => {
                const read = vi.spyOn(storage, 'getItem');
                if (failure === 'blocked') {
                    read.mockImplementation(() => {
                        throw new DOMException('Storage blocked', 'SecurityError');
                    });
                }
                vi.spyOn(storage, 'setItem').mockImplementation(() => {
                    throw new DOMException('Storage unavailable', 'QuotaExceededError');
                });
                service.init({ languageBundleVersion: VERSION });
                http.expectOne(DEFAULT_URL).flush({
                    entity: messages,
                    languageBundleVersion: VERSION
                });
                read.mockClear();

                await firstValueFrom(service.init({ languageBundleVersion: VERSION }));
                await firstValueFrom(service.init({ languageBundleVersion: VERSION }));

                http.expectNone(DEFAULT_URL);
                expect(read).not.toHaveBeenCalled();
                expect(service.get('dot.common.cancel')).toBe('Cancel');
                expect(service.loadedBundleVersion()).toBe(VERSION);
                expect(service.bundleVersionMismatch()).toBeNull();
            }
        );

        it('should reuse a storage-installed bundle before accessing storage again', async () => {
            seedCache();
            await firstValueFrom(service.init({ languageBundleVersion: VERSION }));
            const read = vi.spyOn(storage, 'getItem').mockImplementation(() => {
                throw new Error('Storage must not be read');
            });

            await firstValueFrom(service.init({ languageBundleVersion: VERSION }));

            expect(read).not.toHaveBeenCalled();
            http.expectNone(DEFAULT_URL);
        });

        it('should retain the installed version on blocked-storage failure and clear mismatch on a memory match', async () => {
            vi.spyOn(storage, 'getItem').mockImplementation(() => {
                throw new Error('Storage blocked');
            });
            service.init({ languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });

            const failed = expect(
                firstValueFrom(service.init({ languageBundleVersion: NEXT_VERSION }))
            ).rejects.toMatchObject({ status: 500 });
            expect(service.bundleVersionMismatch()).toEqual({
                language: DEFAULT_LANG,
                cachedVersion: VERSION,
                serverVersion: NEXT_VERSION
            });
            http.expectOne(DEFAULT_URL).flush(null, { status: 500, statusText: 'Server Error' });
            await failed;
            expect(service.loadedBundleVersion()).toBe(VERSION);
            expect(service.get('dot.common.cancel')).toBe('Cancel');

            await firstValueFrom(service.init({ languageBundleVersion: VERSION }));
            http.expectNone(DEFAULT_URL);
            expect(service.bundleVersionMismatch()).toBeNull();
        });

        it('should not reuse another locale with the same installed version', async () => {
            service.init({ languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
            service.init({ language: 'es_ES', languageBundleVersion: VERSION });
            expect(service.bundleVersionMismatch()).toBeNull();
            http.expectOne('/api/v2/languages/es_ES/keys').flush({
                entity: { ...messages, 'dot.common.cancel': 'Cancelar' },
                languageBundleVersion: VERSION
            });
            await firstValueFrom(
                service.init({ language: 'es_ES', languageBundleVersion: VERSION })
            );
            http.expectNone('/api/v2/languages/es_ES/keys');
            expect(service.get('dot.common.cancel')).toBe('Cancelar');

            service.init({ languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
            expect(service.get('dot.common.cancel')).toBe('Cancel');
        });

        it('should track the actual version and locale of a stale fallback', async () => {
            seedCache(messages, 'es_ES');
            const failed = expect(
                firstValueFrom(
                    service.init({
                        language: 'es_ES',
                        languageBundleVersion: NEXT_VERSION
                    })
                )
            ).rejects.toMatchObject({ status: 500 });
            http.expectOne('/api/v2/languages/es_ES/keys').flush(null, {
                status: 500,
                statusText: 'Server Error'
            });
            await failed;
            expect(service.loadedBundleVersion()).toBe(VERSION);
            expect(service.bundleVersionMismatch()?.serverVersion).toBe(NEXT_VERSION);
            clearCache();
            await firstValueFrom(
                service.init({ language: 'es_ES', languageBundleVersion: VERSION })
            );
            http.expectNone('/api/v2/languages/es_ES/keys');
            expect(service.bundleVersionMismatch()).toBeNull();
            service.init({ languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
        });

        it('should not resubscribe to a synchronous failure after the eager subscription', async () => {
            const error = new Error('Network failure');
            const subscribe = vi.fn();
            const get = vi.spyOn(TestBed.inject(HttpClient), 'get').mockReturnValue(
                defer(() => {
                    subscribe();

                    return throwError(() => error);
                })
            );
            const load$ = service.init({ languageBundleVersion: VERSION });

            await expect(firstValueFrom(load$)).rejects.toBe(error);
            await expect(firstValueFrom(load$)).rejects.toBe(error);
            expect(get).toHaveBeenCalledOnce();
            expect(subscribe).toHaveBeenCalledOnce();

            await expect(
                firstValueFrom(service.init({ languageBundleVersion: VERSION }))
            ).rejects.toBe(error);
            expect(get).toHaveBeenCalledTimes(2);
            expect(subscribe).toHaveBeenCalledTimes(2);
        });

        it.each([
            null,
            {},
            { entity: null, languageBundleVersion: NEXT_VERSION },
            { entity: {}, languageBundleVersion: NEXT_VERSION },
            { entity: [], languageBundleVersion: NEXT_VERSION },
            { entity: 'not a map', languageBundleVersion: NEXT_VERSION },
            { entity: { ...messages, invalid: 123 }, languageBundleVersion: NEXT_VERSION },
            { entity: { version: NEXT_VERSION }, languageBundleVersion: NEXT_VERSION },
            { entity: messages },
            { entity: messages, languageBundleVersion: null },
            { entity: messages, languageBundleVersion: '' },
            { entity: messages, languageBundleVersion: '   ' },
            { entity: messages, languageBundleVersion: 123 }
        ])(
            'should reject an invalid response without replacing a valid cache (case %#)',
            async (response) => {
                seedCache();
                await firstValueFrom(service.init({ languageBundleVersion: VERSION }));
                const setItem = vi.spyOn(storage, 'setItem');
                const load$ = service.init({ languageBundleVersion: NEXT_VERSION });
                const failed = expect(firstValueFrom(load$)).rejects.toThrow(
                    'Invalid language bundle'
                );

                http.expectOne(DEFAULT_URL).flush(response);
                await failed;
                expect(setItem).not.toHaveBeenCalled();
                expect(service.get('dot.common.cancel')).toBe('Cancel');
                expect(cachedBundle()?.messages ?? null).toEqual(messages);
                expect(cachedBundle()?.version ?? null).toBe(VERSION);

                const retry$ = service.init({ languageBundleVersion: NEXT_VERSION });
                http.expectOne(DEFAULT_URL).flush({
                    entity: messages,
                    languageBundleVersion: NEXT_VERSION
                });
                await firstValueFrom(retry$);
            }
        );
    });

    describe('bundle version signals', () => {
        it.each(['network', 'cache', 'fallback'])(
            'should publish the installed locale on a same-version switch via %s',
            (source) => {
                expect(service.loadedBundleLanguage()).toBeNull();
                seedCache();
                service.init({ languageBundleVersion: VERSION });
                expect(service.loadedBundleLanguage()).toBe(DEFAULT_LANG);
                const spanish = { ...messages, 'dot.common.cancel': 'Cancelar' };
                if (source !== 'network') {
                    seedCache(spanish, 'es_ES');
                }
                service.init({
                    language: 'es_ES',
                    languageBundleVersion: source === 'fallback' ? NEXT_VERSION : VERSION
                });
                if (source !== 'cache') {
                    expect(service.loadedBundleLanguage()).toBe(DEFAULT_LANG);
                    const request = http.expectOne('/api/v2/languages/es_ES/keys');
                    if (source === 'fallback') {
                        request.flush(null, { status: 500, statusText: 'Server Error' });
                    } else {
                        request.flush({ entity: spanish, languageBundleVersion: VERSION });
                    }
                }
                expect(service.loadedBundleVersion()).toBe(VERSION);
                expect(service.loadedBundleLanguage()).toBe('es_ES');
                expect(service.get('dot.common.cancel')).toBe('Cancelar');
            }
        );

        it('should retain the installed locale on failure without a usable fallback', () => {
            seedCache();
            service.init({ languageBundleVersion: VERSION });
            service.init({ language: 'es_ES', languageBundleVersion: VERSION });
            http.expectOne('/api/v2/languages/es_ES/keys').flush(null, {
                status: 500,
                statusText: 'Server Error'
            });
            expect(service.loadedBundleLanguage()).toBe(DEFAULT_LANG);
            expect(service.loadedBundleVersion()).toBe(VERSION);
        });

        it('should publish the locale of an unversioned stale fallback', () => {
            seedCache(messages, 'es_ES');
            storage.setItem(BUNDLE_LOCALSTORAGE_KEY, { ...cachedBundle(), version: null });
            service.init({ language: 'es_ES', languageBundleVersion: VERSION });
            http.expectOne('/api/v2/languages/es_ES/keys').flush(null, {
                status: 500,
                statusText: 'Server Error'
            });
            expect(service.loadedBundleLanguage()).toBe('es_ES');
            expect(service.loadedBundleVersion()).toBeNull();
        });
        it('should distinguish initial loading from a version mismatch', () => {
            expect(service.bundleVersionMismatch()).toBeNull();
            expect(service.loadedBundleVersion()).toBeNull();
            service.init({ languageBundleVersion: VERSION });
            expect(service.bundleVersionMismatch()).toBeNull();
            expect(service.loadedBundleVersion()).toBeNull();
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
            expect(service.loadedBundleVersion()).toBe(VERSION);
        });

        it('should announce a mismatch before fetching, and install the version only after success', () => {
            seedCache();
            service.init({ languageBundleVersion: VERSION });
            service.init({ languageBundleVersion: NEXT_VERSION });
            expect(service.bundleVersionMismatch()).toEqual({
                language: DEFAULT_LANG,
                cachedVersion: VERSION,
                serverVersion: NEXT_VERSION
            });
            expect(service.loadedBundleVersion()).toBe(VERSION);
            http.expectOne(DEFAULT_URL).flush({
                entity: { ...messages, 'dot.common.cancel': 'Updated cancel' },
                languageBundleVersion: NEXT_VERSION
            });
            expect(service.loadedBundleVersion()).toBe(NEXT_VERSION);
            expect(service.bundleVersionMismatch()).toBeNull();
            expect(service.get('dot.common.cancel')).toBe('Updated cancel');
        });

        it('should let effects read updated translations when the installed version changes', () => {
            const installed = vi.fn();
            TestBed.runInInjectionContext(() =>
                effect(() => {
                    const version = service.loadedBundleVersion();
                    if (version) {
                        installed(version, service.get('dot.common.cancel'));
                    }
                })
            );
            TestBed.tick();
            service.init({ languageBundleVersion: VERSION });
            TestBed.tick();
            expect(installed).not.toHaveBeenCalled();
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
            TestBed.tick();
            expect(installed).toHaveBeenCalledExactlyOnceWith(VERSION, 'Cancel');
            service.init({ languageBundleVersion: VERSION });
            TestBed.tick();
            http.expectNone(DEFAULT_URL);
            expect(installed).toHaveBeenCalledOnce();
        });

        it('should not announce an installation when a mismatched request fails', () => {
            seedCache();
            service.init({ languageBundleVersion: VERSION });
            service.init({ languageBundleVersion: NEXT_VERSION });
            http.expectOne(DEFAULT_URL).flush(null, { status: 500, statusText: 'Server Error' });
            expect(service.loadedBundleVersion()).toBe(VERSION);
            expect(service.get('dot.common.cancel')).toBe('Cancel');
            expect(service.bundleVersionMismatch()?.serverVersion).toBe(NEXT_VERSION);
        });

        it('should clear the mismatch after installation and keep matching checks free of refetches', () => {
            seedCache();
            service.init({ languageBundleVersion: NEXT_VERSION });
            http.expectOne(DEFAULT_URL).flush({
                entity: messages,
                languageBundleVersion: NEXT_VERSION
            });
            service.init({ languageBundleVersion: NEXT_VERSION });
            expect(service.bundleVersionMismatch()).toBeNull();
            expect(service.loadedBundleVersion()).toBe(NEXT_VERSION);
            http.expectNone(DEFAULT_URL);
        });
    });

    describe('reactive get', () => {
        it('invalidates computed labels when a fallback is replaced at the same locale and version', () => {
            seedCache();
            service.init();
            http.expectOne(DEFAULT_URL).flush(null, { status: 500, statusText: 'Server Error' });
            const revision = service.loadedBundleRevision();
            const label = computed(() => service.get('dot.common.cancel'));
            expect(label()).toBe('Cancel');
            service.init();
            http.expectOne(DEFAULT_URL).flush({
                entity: { ...messages, 'dot.common.cancel': 'Refreshed cancel' },
                languageBundleVersion: VERSION
            });
            expect(service.loadedBundleVersion()).toBe(VERSION);
            expect(service.loadedBundleRevision()).toBe(revision + 1);
            expect(label()).toBe('Refreshed cancel');
            service.init({ languageBundleVersion: VERSION });
            expect(service.loadedBundleRevision()).toBe(revision + 1);
            http.expectNone(DEFAULT_URL);
        });

        it('should invalidate a computed after a cold failure recovers, including formatted messages', () => {
            const translate = vi.fn(() =>
                service.get('dot.common.accept', service.get('dot.common.cancel'))
            );
            const label = computed(translate);
            expect(label()).toBe('dot.common.accept');

            service.init({ languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush(null, { status: 500, statusText: 'Server Error' });
            expect(label()).toBe('dot.common.accept');
            expect(translate).toHaveBeenCalledOnce();

            service.init({ languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
            expect(label()).toBe('Accept Cancel');
            expect(translate).toHaveBeenCalledTimes(2);
        });

        it('should invalidate a computed when a stale bundle recovers with a new version', () => {
            seedCache();
            service.init({ languageBundleVersion: NEXT_VERSION });
            http.expectOne(DEFAULT_URL).flush(null, { status: 500, statusText: 'Server Error' });
            const label = computed(() => service.get('dot.common.cancel'));
            expect(label()).toBe('Cancel');

            service.init({ languageBundleVersion: NEXT_VERSION });
            http.expectOne(DEFAULT_URL).flush({
                entity: { ...messages, 'dot.common.cancel': 'Updated cancel' },
                languageBundleVersion: NEXT_VERSION
            });
            expect(label()).toBe('Updated cancel');
        });

        it.each(['network', 'cache', 'fallback'])(
            'should invalidate a computed on a same-version locale switch via %s',
            (source) => {
                seedCache();
                service.init({ languageBundleVersion: VERSION });
                const label = computed(() => service.get('dot.common.cancel'));
                expect(label()).toBe('Cancel');
                const spanish = { ...messages, 'dot.common.cancel': 'Cancelar' };
                if (source !== 'network') {
                    seedCache(spanish, 'es_ES');
                }

                service.init({
                    language: 'es_ES',
                    languageBundleVersion: source === 'fallback' ? NEXT_VERSION : VERSION
                });
                if (source === 'network') {
                    http.expectOne('/api/v2/languages/es_ES/keys').flush({
                        entity: spanish,
                        languageBundleVersion: VERSION
                    });
                } else if (source === 'fallback') {
                    http.expectOne('/api/v2/languages/es_ES/keys').flush(null, {
                        status: 500,
                        statusText: 'Server Error'
                    });
                }

                expect(service.loadedBundleVersion()).toBe(VERSION);
                expect(label()).toBe('Cancelar');
            }
        );

        it('should rerun a get-only effect once per install without refetching or cycling', () => {
            const translated = vi.fn();
            TestBed.runInInjectionContext(() =>
                effect(() => translated(service.get('dot.common.cancel')))
            );
            TestBed.tick();
            expect(translated).toHaveBeenCalledExactlyOnceWith('dot.common.cancel');

            service.init({ languageBundleVersion: VERSION });
            http.expectOne(DEFAULT_URL).flush({ entity: messages, languageBundleVersion: VERSION });
            TestBed.tick();
            expect(translated).toHaveBeenLastCalledWith('Cancel');
            expect(translated).toHaveBeenCalledTimes(2);

            service.init({ languageBundleVersion: VERSION });
            TestBed.tick();
            TestBed.tick();
            http.expectNone(DEFAULT_URL);
            expect(translated).toHaveBeenCalledTimes(2);
        });
    });

    describe('get', () => {
        beforeEach(async () => {
            seedCache();
            await firstValueFrom(service.init({ languageBundleVersion: VERSION }));
        });

        it('should return a message', () => {
            expect(service.get('dot.common.cancel')).toBe('Cancel');
        });

        it('should return the key if the message is not found', () => {
            expect(service.get('dot.unknown')).toBe('dot.unknown');
        });

        it('should replace text in a message', () => {
            expect(service.get('dot.common.accept', 'data')).toBe('Accept data');
        });
    });
});
