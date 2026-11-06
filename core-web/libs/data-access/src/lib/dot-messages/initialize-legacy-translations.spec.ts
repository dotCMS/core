import { firstValueFrom, of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { Auth, LoginService, DotcmsConfigService, LoggerService } from '@dotcms/dotcms-js';

import { initializeLegacyTranslations } from './initialize-legacy-translations';

import type { DotMessageService } from './dot-messages.service';
import type { DotLocalstorageService } from '../dot-localstorage/dot-localstorage.service';

type Config =
    ReturnType<DotcmsConfigService['getConfig']> extends import('rxjs').Observable<infer T>
        ? T
        : never;

// Only the version is consumed by this initializer.
const config = (languageBundleVersion?: string): Config => ({ languageBundleVersion }) as Config;

describe('initializeLegacyTranslations', () => {
    let messages: Pick<DotMessageService, 'init'>;
    let configuration: Pick<DotcmsConfigService, 'getConfig'>;
    let storage: Pick<DotLocalstorageService, 'getItem'>;
    let logger: Pick<LoggerService, 'warn'>;

    let login: Pick<LoginService, 'loadAuth' | 'currentUserLanguageId'>;
    const initialize = () =>
        initializeLegacyTranslations(messages, configuration, storage, logger, login);

    beforeEach(() => {
        login = {
            loadAuth: vi.fn().mockReturnValue(of({ sessionLanguageId: 'es_ES' } as Auth)),
            currentUserLanguageId: 'en_US'
        };
        messages = { init: vi.fn().mockReturnValue(of(void 0)) };
        configuration = { getConfig: vi.fn().mockReturnValue(of(config('server-hash'))) };
        storage = { getItem: vi.fn().mockReturnValue('es_ES') };
        logger = { warn: vi.fn() };
    });

    it.each(['es_ES', 'en-US', 'default', 'a1_2-3'])(
        'uses the selected locale %s and server bundle version',
        async (language) => {
            vi.mocked(login.loadAuth).mockReturnValue(of({ sessionLanguageId: language } as Auth));

            await expect(firstValueFrom(initialize())).resolves.toBeUndefined();

            expect(storage.getItem).not.toHaveBeenCalled();
            expect(messages.init).toHaveBeenCalledExactlyOnceWith({
                language,
                languageBundleVersion: 'server-hash',
                userId: null
            });
            expect(logger.warn).not.toHaveBeenCalled();
        }
    );

    it.each([null, undefined, '', '../es', 'en US', '1en', '_en', 'en/US', 42, {}])(
        'ignores invalid or missing shared storage value %j',
        async (language) => {
            vi.mocked(storage.getItem).mockReturnValue(language);

            await firstValueFrom(initialize());

            expect(messages.init).toHaveBeenCalledExactlyOnceWith({
                language: 'es_ES',
                languageBundleVersion: 'server-hash',
                userId: null
            });
        }
    );

    it('does not read storage even when it is blocked', async () => {
        const error = new Error('Storage blocked');
        vi.mocked(storage.getItem).mockImplementation(() => {
            throw error;
        });

        await expect(firstValueFrom(initialize())).resolves.toBeUndefined();

        expect(messages.init).toHaveBeenCalledWith({
            language: 'es_ES',
            languageBundleVersion: 'server-hash',
            userId: null
        });
        expect(storage.getItem).not.toHaveBeenCalled();
        expect(logger.warn).not.toHaveBeenCalled();
    });

    it.each([false, true])(
        'scopes legacy editor translations to the effective user (login-as: %s)',
        async (impersonating) => {
            vi.mocked(login.loadAuth).mockReturnValue(
                of({
                    user: { userId: 'principal' },
                    loginAsUser: impersonating ? { userId: 'impersonated' } : null
                } as Auth)
            );
            await firstValueFrom(initialize());
            expect(messages.init).toHaveBeenCalledWith(
                expect.objectContaining({
                    userId: impersonating ? 'impersonated' : 'principal'
                })
            );
        }
    );

    it('uses the fresh session locale even when another tab cached a different valid locale', async () => {
        vi.mocked(storage.getItem).mockReturnValue('fr_FR');
        await firstValueFrom(initialize());
        expect(messages.init).toHaveBeenCalledExactlyOnceWith({
            language: 'es_ES',
            languageBundleVersion: 'server-hash',
            userId: null
        });
        expect(storage.getItem).not.toHaveBeenCalled();
    });

    it('prefers the impersonated user locale over the original session locale', async () => {
        vi.mocked(login.loadAuth).mockReturnValue(
            of({
                sessionLanguageId: 'es_ES',
                loginAsUser: { languageId: 'de_DE' }
            } as Auth)
        );

        await firstValueFrom(initialize());

        expect(messages.init).toHaveBeenCalledExactlyOnceWith({
            language: 'de_DE',
            languageBundleVersion: 'server-hash',
            userId: null
        });
    });

    it('continues bootstrap without reusing cached translations when auth fails', async () => {
        vi.mocked(login.loadAuth).mockReturnValue(throwError(() => new Error('Auth failed')));
        await expect(firstValueFrom(initialize())).resolves.toBeUndefined();
        expect(messages.init).not.toHaveBeenCalled();
        expect(logger.warn).toHaveBeenCalledOnce();
    });

    it('waits for fresh auth before initializing translations', () => {
        const auth$ = new Subject<Auth>();
        vi.mocked(login.loadAuth).mockReturnValue(auth$);
        initialize().subscribe();
        expect(messages.init).not.toHaveBeenCalled();
        auth$.next({ sessionLanguageId: 'de_DE' } as Auth);
        expect(messages.init).toHaveBeenCalledExactlyOnceWith({
            language: 'de_DE',
            languageBundleVersion: 'server-hash',
            userId: null
        });
    });

    it('waits for delayed configuration and translation completion', () => {
        const config$ = new Subject<Config>();
        const translations$ = new Subject<void>();
        vi.mocked(configuration.getConfig).mockReturnValue(config$);
        vi.mocked(messages.init).mockReturnValue(translations$);
        const next = vi.fn();
        const complete = vi.fn();
        initialize().subscribe({ next, complete });

        expect(messages.init).not.toHaveBeenCalled();
        expect(next).not.toHaveBeenCalled();
        expect(complete).not.toHaveBeenCalled();

        config$.next(config('delayed-hash'));
        expect(messages.init).toHaveBeenCalledWith({
            language: 'es_ES',
            languageBundleVersion: 'delayed-hash',
            userId: null
        });
        expect(next).not.toHaveBeenCalled();
        expect(complete).not.toHaveBeenCalled();

        translations$.next();
        expect(next).toHaveBeenCalledExactlyOnceWith(undefined);
        expect(complete).not.toHaveBeenCalled();
        translations$.complete();
        expect(complete).toHaveBeenCalledOnce();
    });

    it('does not invent a version when configuration has no bundle hash', async () => {
        vi.mocked(configuration.getConfig).mockReturnValue(of(config()));

        await firstValueFrom(initialize());

        expect(messages.init).toHaveBeenCalledWith({
            language: 'es_ES',
            languageBundleVersion: undefined,
            userId: null
        });
    });

    it('fetches translations without an expected hash when configuration fails', async () => {
        const error = new Error('Configuration failed');
        vi.mocked(configuration.getConfig).mockReturnValue(throwError(() => error));

        await expect(firstValueFrom(initialize())).resolves.toBeUndefined();

        expect(messages.init).toHaveBeenCalledWith({
            language: 'es_ES',
            languageBundleVersion: undefined,
            userId: null
        });
        expect(logger.warn).toHaveBeenCalledExactlyOnceWith(
            expect.stringContaining('configuration'),
            error
        );
    });

    it('logs translation failure and emits void before completing gracefully', () => {
        const error = new Error('Translations failed');
        vi.mocked(messages.init).mockReturnValue(throwError(() => error));
        const next = vi.fn();
        const complete = vi.fn();
        const onError = vi.fn();

        initialize().subscribe({ next, complete, error: onError });

        expect(next).toHaveBeenCalledExactlyOnceWith(undefined);
        expect(complete).toHaveBeenCalledOnce();
        expect(onError).not.toHaveBeenCalled();
        expect(logger.warn).toHaveBeenCalledExactlyOnceWith(
            expect.stringContaining('translations'),
            error
        );
    });

    it('completes gracefully when both configuration and translations fail', async () => {
        const configError = new Error('Configuration failed');
        const translationError = new Error('Translations failed');
        vi.mocked(configuration.getConfig).mockReturnValue(throwError(() => configError));
        vi.mocked(messages.init).mockReturnValue(throwError(() => translationError));

        await expect(firstValueFrom(initialize())).resolves.toBeUndefined();

        expect(messages.init).toHaveBeenCalledExactlyOnceWith({
            language: 'es_ES',
            languageBundleVersion: undefined,
            userId: null
        });
        expect(logger.warn).toHaveBeenCalledTimes(2);
        expect(logger.warn).toHaveBeenNthCalledWith(1, expect.any(String), configError);
        expect(logger.warn).toHaveBeenNthCalledWith(2, expect.any(String), translationError);
    });

    it('takes only the first configuration without cancelling pending translations', () => {
        const config$ = new Subject<Config>();
        const translations$ = new Subject<void>();
        vi.mocked(configuration.getConfig).mockReturnValue(config$);
        vi.mocked(messages.init).mockReturnValue(translations$);
        const complete = vi.fn();
        initialize().subscribe({ complete });

        config$.next(config('first-hash'));
        config$.next(config('second-hash'));
        config$.error(new Error('Ignored after the first configuration'));

        expect(messages.init).toHaveBeenCalledExactlyOnceWith({
            language: 'es_ES',
            languageBundleVersion: 'first-hash',
            userId: null
        });
        expect(complete).not.toHaveBeenCalled();
        expect(logger.warn).not.toHaveBeenCalled();
        translations$.complete();
        expect(complete).toHaveBeenCalledOnce();
    });
});
