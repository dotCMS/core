import { defer, firstValueFrom, throwError } from 'rxjs';
import { vi } from 'vitest';

import { HttpClient, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { effect } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotMessageService } from './dot-messages.service';

import { DotLocalstorageService } from '../dot-localstorage/dot-localstorage.service';

const DEFAULT_LANG = 'default';
const LANGUAGE_LOCALSTORAGE_KEY = 'dotMessagesKeys-lang';
const MESSAGES_LOCALSTORAGE_KEY = 'dotMessagesKeys';
const VERSION_LOCALSTORAGE_KEY = 'dotMessagesKeys-version';
const BUILDDATE_LOCALSTORAGE_KEY = 'buildDate';
const VERSION = 'a'.repeat(64);
const NEXT_VERSION = '1' + 'b'.repeat(63);
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
        BUILDDATE_LOCALSTORAGE_KEY
    ].forEach((key) => localStorage.removeItem(key));
};

describe('DotMessageService', () => {
    let service: DotMessageService;
    let http: HttpTestingController;
    let storage: DotLocalstorageService;

    const seedCache = (cachedMessages: unknown = messages, language = DEFAULT_LANG) => {
        storage.setItem(MESSAGES_LOCALSTORAGE_KEY, cachedMessages);
        storage.setItem(LANGUAGE_LOCALSTORAGE_KEY, language);
        storage.setItem(VERSION_LOCALSTORAGE_KEY, JSON.stringify(VERSION));
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

    describe('init', () => {
        it('should eagerly fetch default messages for callers that do not subscribe', () => {
            service.init();

            const request = http.expectOne(DEFAULT_URL);
            expect(request.request.method).toBe('GET');
            request.flush({ entity: messages, languageBundleVersion: VERSION });

            expect(service.get('dot.common.cancel')).toBe('Cancel');
            expect(storage.getItem(MESSAGES_LOCALSTORAGE_KEY)).toEqual(messages);
            expect(storage.getItem(LANGUAGE_LOCALSTORAGE_KEY)).toBe(DEFAULT_LANG);
            expect(storage.getItem(VERSION_LOCALSTORAGE_KEY)).toBe(VERSION);
            expect(storage.getItem(MESSAGES_LOCALSTORAGE_KEY)).not.toHaveProperty('version');
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
            expect(storage.getItem(LANGUAGE_LOCALSTORAGE_KEY)).toBe('en_US');
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
            expect(storage.getItem(VERSION_LOCALSTORAGE_KEY)).toBe(NEXT_VERSION);
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
                expect(storage.getItem(VERSION_LOCALSTORAGE_KEY)).toBe(VERSION);
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
            expect(storage.getItem(VERSION_LOCALSTORAGE_KEY)).toBeNull();
            expect(service.loadedBundleVersion()).toBeNull();
        });

        it('should accept 101 translations and persist the version last', async () => {
            const persist = vi.spyOn(storage, 'setItem');
            const loaded = firstValueFrom(service.init({ languageBundleVersion: VERSION }));
            http.expectOne(DEFAULT_URL).flush({
                entity: createMessages(101),
                languageBundleVersion: VERSION
            });
            await loaded;
            expect(persist).toHaveBeenLastCalledWith(
                VERSION_LOCALSTORAGE_KEY,
                JSON.stringify(VERSION)
            );
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
                if (version === undefined) {
                    storage.removeItem(VERSION_LOCALSTORAGE_KEY);
                } else {
                    storage.setItem(VERSION_LOCALSTORAGE_KEY, JSON.stringify(version));
                }
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
            storage.removeItem(MESSAGES_LOCALSTORAGE_KEY);
            const loaded = firstValueFrom(service.init({ languageBundleVersion: VERSION }));

            http.expectOne(DEFAULT_URL).flush({
                entity: messages,
                languageBundleVersion: VERSION
            });
            await loaded;
        });

        it('should not treat missing legacy build dates as a valid version match', async () => {
            seedCache();
            storage.removeItem(VERSION_LOCALSTORAGE_KEY);
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

            expect(storage.getItem(VERSION_LOCALSTORAGE_KEY)).toBe(NEXT_VERSION);
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
            await Promise.all([first$, second$, third$].map((load$) => firstValueFrom(load$)));
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
                expect(storage.getItem(VERSION_LOCALSTORAGE_KEY)).toBe(VERSION);

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
            expect(storage.getItem(VERSION_LOCALSTORAGE_KEY)).toBe(VERSION);
            expect(storage.getItem(MESSAGES_LOCALSTORAGE_KEY)).toEqual(messages);

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

        it.each([MESSAGES_LOCALSTORAGE_KEY, LANGUAGE_LOCALSTORAGE_KEY, VERSION_LOCALSTORAGE_KEY])(
            'should install a valid bundle even if persisting %s fails',
            async (failedKey) => {
                seedCache();
                const persist = storage.setItem.bind(storage);
                vi.spyOn(storage, 'setItem').mockImplementation((key, value) => {
                    if (key === failedKey) {
                        throw new DOMException('Storage quota exceeded', 'QuotaExceededError');
                    }
                    persist(key, value);
                });
                const loaded = firstValueFrom(
                    service.init({ languageBundleVersion: NEXT_VERSION })
                );
                http.expectOne(DEFAULT_URL).flush({
                    entity: { ...messages, 'dot.common.cancel': 'Updated cancel' },
                    languageBundleVersion: NEXT_VERSION
                });
                await loaded;
                expect(service.get('dot.common.cancel')).toBe('Updated cancel');
                expect(service.loadedBundleVersion()).toBe(NEXT_VERSION);
                expect(storage.getItem(VERSION_LOCALSTORAGE_KEY)).toBeNull();
            }
        );

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
                expect(storage.getItem(MESSAGES_LOCALSTORAGE_KEY)).toEqual(messages);
                expect(storage.getItem(VERSION_LOCALSTORAGE_KEY)).toBe(VERSION);

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
            storage.removeItem(VERSION_LOCALSTORAGE_KEY);
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
