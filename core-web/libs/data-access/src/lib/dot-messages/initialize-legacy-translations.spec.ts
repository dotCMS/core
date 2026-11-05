import { firstValueFrom, of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { DotcmsConfigService, LoggerService } from '@dotcms/dotcms-js';

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

    const initialize = () => initializeLegacyTranslations(messages, configuration, storage, logger);

    beforeEach(() => {
        messages = { init: vi.fn().mockReturnValue(of(void 0)) };
        configuration = { getConfig: vi.fn().mockReturnValue(of(config('server-hash'))) };
        storage = { getItem: vi.fn().mockReturnValue('es_ES') };
        logger = { warn: vi.fn() };
    });

    it.each(['es_ES', 'en-US', 'default', 'a1_2-3'])(
        'uses the selected locale %s and server bundle version',
        async (language) => {
            vi.mocked(storage.getItem).mockReturnValue(language);

            await expect(firstValueFrom(initialize())).resolves.toBeUndefined();

            expect(storage.getItem).toHaveBeenCalledExactlyOnceWith('dotMessagesKeys-lang');
            expect(messages.init).toHaveBeenCalledExactlyOnceWith({
                language,
                languageBundleVersion: 'server-hash'
            });
            expect(logger.warn).not.toHaveBeenCalled();
        }
    );

    it.each([null, undefined, '', '../es', 'en US', '1en', '_en', 'en/US', 42, {}])(
        'uses the default locale for invalid or missing storage value %j',
        async (language) => {
            vi.mocked(storage.getItem).mockReturnValue(language);

            await firstValueFrom(initialize());

            expect(messages.init).toHaveBeenCalledExactlyOnceWith({
                language: 'default',
                languageBundleVersion: 'server-hash'
            });
        }
    );

    it('continues with the default locale when storage is blocked', async () => {
        const error = new Error('Storage blocked');
        vi.mocked(storage.getItem).mockImplementation(() => {
            throw error;
        });

        await expect(firstValueFrom(initialize())).resolves.toBeUndefined();

        expect(messages.init).toHaveBeenCalledWith({
            language: 'default',
            languageBundleVersion: 'server-hash'
        });
        expect(logger.warn).toHaveBeenCalledWith(expect.stringContaining('locale'), error);
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
            languageBundleVersion: 'delayed-hash'
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
            languageBundleVersion: undefined
        });
    });

    it('fetches translations without an expected hash when configuration fails', async () => {
        const error = new Error('Configuration failed');
        vi.mocked(configuration.getConfig).mockReturnValue(throwError(() => error));

        await expect(firstValueFrom(initialize())).resolves.toBeUndefined();

        expect(messages.init).toHaveBeenCalledWith({
            language: 'es_ES',
            languageBundleVersion: undefined
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
            languageBundleVersion: undefined
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
            languageBundleVersion: 'first-hash'
        });
        expect(complete).not.toHaveBeenCalled();
        expect(logger.warn).not.toHaveBeenCalled();
        translations$.complete();
        expect(complete).toHaveBeenCalledOnce();
    });
});
