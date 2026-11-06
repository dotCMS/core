import { signalStore } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { firstValueFrom, of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';

import { TestBed } from '@angular/core/testing';

import { DotMessageService, DotSystemConfigService } from '@dotcms/data-access';
import { Auth, LoginService } from '@dotcms/dotcms-js';
import { DotSystemConfig } from '@dotcms/dotcms-models';

import { withSystem } from './with-system.feature';

const SystemStore = signalStore(withSystem());
const config = {
    languageBundleVersion: 'build-hash',
    emailRegex: '^[^@]+@dotcms\\.com$',
    systemTimezone: { id: 'America/New_York', label: 'Eastern Time', offset: -18000000 }
} as DotSystemConfig;
const authFor = (languageId = 'en_US'): Auth =>
    ({ user: { userId: 'principal', languageId }, loginAsUser: null }) as Auth;

describe('withSystem session loading', () => {
    let spectator: SpectatorService<InstanceType<typeof SystemStore>>;
    let auth$: Subject<Auth>;
    let login: { auth: Auth | null; auth$: Subject<Auth>; currentUserLanguageId: string };
    const createService = createServiceFactory({
        service: SystemStore,
        providers: [
            mockProvider(DotSystemConfigService, {
                getSystemConfig: vi.fn().mockReturnValue(of(config))
            }),
            mockProvider(DotMessageService, { init: vi.fn().mockReturnValue(of(void 0)) })
        ]
    });

    const emitAuth = (auth: Auth) => {
        login.auth = auth;
        login.currentUserLanguageId = auth.sessionLanguageId || auth.user?.languageId || '';
        auth$.next(auth);
    };
    const ready = () => {
        const result = firstValueFrom(spectator.service.waitForSessionReady());
        spectator.flushEffects();

        return result;
    };

    beforeEach(() => {
        vi.spyOn(console, 'warn').mockImplementation(() => undefined);
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        auth$ = new Subject<Auth>();
        login = { auth: null, auth$, currentUserLanguageId: 'en_US' };
        spectator = createService({ providers: [{ provide: LoginService, useValue: login }] });
        spectator
            .inject(DotSystemConfigService)
            .getSystemConfig.mockReset()
            .mockReturnValue(of(config));
        spectator
            .inject(DotMessageService)
            .init.mockReset()
            .mockReturnValue(of(void 0));
        vi.clearAllMocks();
    });

    afterEach(() => vi.restoreAllMocks());

    it('does not request protected configuration or translations before login', () => {
        expect(spectator.inject(DotSystemConfigService).getSystemConfig).not.toHaveBeenCalled();
        expect(spectator.inject(DotMessageService).init).not.toHaveBeenCalled();
    });

    it('invalidates old bundle requests immediately on auth changes, before configuration resolves', () => {
        spectator
            .inject(DotSystemConfigService)
            .getSystemConfig.mockReturnValue(new Subject<DotSystemConfig>());
        emitAuth(authFor());
        expect(spectator.inject(DotMessageService).invalidatePendingSession).toHaveBeenCalledOnce();
        expect(spectator.inject(DotMessageService).init).not.toHaveBeenCalled();
        emitAuth({ ...authFor(), loginAsUser: { userId: 'other', languageId: 'en_US' } } as Auth);
        expect(spectator.inject(DotMessageService).invalidatePendingSession).toHaveBeenCalledTimes(
            2
        );
        emitAuth({ user: null, loginAsUser: null } as unknown as Auth);
        expect(spectator.inject(DotMessageService).invalidatePendingSession).toHaveBeenCalledTimes(
            3
        );
    });

    it('loads configuration and the matching versioned bundle after login', async () => {
        emitAuth(authFor());
        await ready();
        expect(spectator.service.systemConfig()).toEqual(config);
        expect(spectator.inject(DotMessageService).init).toHaveBeenCalledWith({
            language: 'en_US',
            languageBundleVersion: 'build-hash',
            userId: 'principal'
        });
    });

    it('initializes an already-authenticated session', async () => {
        login.auth = authFor();
        const store = TestBed.runInInjectionContext(() => new SystemStore());
        const initialized = firstValueFrom(store.waitForSessionReady());
        spectator.flushEffects();
        await initialized;
        expect(spectator.inject(DotSystemConfigService).getSystemConfig).toHaveBeenCalledOnce();
    });

    it('waits for translations without duplicating the configuration request on navigation', async () => {
        const loaded$ = new Subject<void>();
        spectator.inject(DotMessageService).init.mockReturnValue(loaded$);
        emitAuth(authFor());
        const activated = vi.fn();
        spectator.service.waitForSessionReady().subscribe(activated);
        spectator.flushEffects();
        expect(activated).not.toHaveBeenCalled();
        loaded$.next();
        loaded$.complete();
        spectator.flushEffects();
        expect(activated).toHaveBeenCalledOnce();
        await ready();
        expect(spectator.inject(DotSystemConfigService).getSystemConfig).toHaveBeenCalledOnce();
        expect(spectator.inject(DotMessageService).init).toHaveBeenCalledOnce();
    });

    it('preserves the explicit login selection ahead of the saved profile locale', async () => {
        login.auth = authFor('en_US');
        login.currentUserLanguageId = 'fr_FR';
        auth$.next(login.auth);
        await ready();
        expect(spectator.inject(DotMessageService).init).toHaveBeenCalledWith({
            language: 'fr_FR',
            languageBundleVersion: 'build-hash',
            userId: 'principal'
        });
    });

    it('refreshes a changed session locale for the same principal', async () => {
        emitAuth(authFor());
        await ready();
        emitAuth({ ...authFor(), sessionLanguageId: 'fr_FR' });
        await ready();
        expect(spectator.inject(DotMessageService).init).toHaveBeenLastCalledWith({
            language: 'fr_FR',
            languageBundleVersion: 'build-hash',
            userId: 'principal'
        });
    });

    it('refreshes login-as and logout-as without changing the principal ID', async () => {
        emitAuth(authFor());
        await ready();
        emitAuth({ ...authFor(), loginAsUser: { userId: 'other', languageId: 'es_ES' } } as Auth);
        await ready();
        expect(spectator.inject(DotMessageService).init).toHaveBeenLastCalledWith({
            language: 'es_ES',
            languageBundleVersion: 'build-hash',
            userId: 'other'
        });
        emitAuth(authFor());
        await ready();
        expect(spectator.inject(DotMessageService).init).toHaveBeenLastCalledWith({
            language: 'en_US',
            languageBundleVersion: 'build-hash',
            userId: 'principal'
        });
    });

    it('does not accept replayed readiness from the previous session', async () => {
        emitAuth(authFor());
        await ready();
        spectator.inject(DotMessageService).init.mockReturnValue(new Subject<void>());
        emitAuth({ ...authFor(), sessionLanguageId: 'es_ES' });
        const activated = vi.fn();
        spectator.service.waitForSessionReady().subscribe(activated);
        expect(activated).not.toHaveBeenCalled();
        spectator.flushEffects();
        expect(activated).not.toHaveBeenCalled();
    });

    it('discards an in-flight configuration response when the session changes', async () => {
        const oldConfig$ = new Subject<DotSystemConfig>();
        spectator.inject(DotSystemConfigService).getSystemConfig.mockReturnValueOnce(oldConfig$);
        emitAuth(authFor());
        emitAuth({ ...authFor(), sessionLanguageId: 'es_ES' });
        await ready();
        oldConfig$.next({ languageBundleVersion: 'old-build' } as DotSystemConfig);
        expect(spectator.service.systemConfig()).toEqual(config);
        expect(spectator.inject(DotMessageService).init).toHaveBeenCalledOnce();
    });

    it('cancels pending configuration and clears it on logout', () => {
        const pending$ = new Subject<DotSystemConfig>();
        spectator.inject(DotSystemConfigService).getSystemConfig.mockReturnValue(pending$);
        emitAuth(authFor());
        emitAuth({ user: null, loginAsUser: null } as unknown as Auth);
        pending$.next(config);
        expect(spectator.service.systemConfig()).toBeNull();
        expect(spectator.inject(DotMessageService).init).not.toHaveBeenCalled();
    });

    it('uses the default language when no session locale is available', async () => {
        login.currentUserLanguageId = '';
        emitAuth(authFor(''));
        await ready();
        expect(spectator.inject(DotMessageService).init).toHaveBeenCalledWith({
            language: 'default',
            languageBundleVersion: 'build-hash',
            userId: 'principal'
        });
    });

    it('loads uncached translations when bundle metadata is missing', async () => {
        spectator
            .inject(DotSystemConfigService)
            .getSystemConfig.mockReturnValue(of({} as DotSystemConfig));
        emitAuth(authFor());
        await ready();
        expect(spectator.inject(DotMessageService).init).toHaveBeenCalledWith({
            language: 'en_US',
            languageBundleVersion: undefined,
            userId: 'principal'
        });
    });

    it('falls back to an uncached bundle on configuration failure and retries later', async () => {
        const failure = new Error('Configuration failed');
        spectator
            .inject(DotSystemConfigService)
            .getSystemConfig.mockReturnValueOnce(throwError(() => failure));
        emitAuth(authFor());
        expect(spectator.inject(DotMessageService).init).toHaveBeenCalledWith({
            language: 'en_US',
            languageBundleVersion: undefined,
            userId: 'principal'
        });
        expect(console.warn).toHaveBeenCalledWith(
            'Unable to load configuration; fetching uncached translations.',
            failure
        );
        await ready();
        expect(spectator.service.systemConfig()).toEqual(config);
        expect(spectator.service.systemConfig()?.emailRegex).toBe(config.emailRegex);
    });

    it('allows degraded readiness when translations fail and retries on later navigation', async () => {
        const loaded$ = new Subject<void>();
        spectator.inject(DotMessageService).init.mockReturnValueOnce(loaded$);
        emitAuth(authFor());
        const waiting = ready();
        loaded$.error(new Error('Bundle failed'));
        spectator.flushEffects();
        await waiting;
        expect(console.error).toHaveBeenCalledOnce();
        await ready();
        expect(spectator.inject(DotMessageService).init).toHaveBeenCalledTimes(2);
    });

    it('retains loaded configuration and timezone when a refresh fails, without reusing its bundle version', async () => {
        emitAuth(authFor());
        await ready();
        const refresh$ = new Subject<DotSystemConfig>();
        spectator.inject(DotSystemConfigService).getSystemConfig.mockReturnValueOnce(refresh$);
        spectator.service.loadSystemConfig();
        const settled = ready();
        refresh$.error(new Error('Refresh failed'));
        spectator.flushEffects();
        await settled;
        expect(spectator.service.systemConfig()).toEqual(config);
        expect(spectator.service.systemTimezone()).toEqual(config.systemTimezone);
        expect(spectator.inject(DotMessageService).init).toHaveBeenLastCalledWith({
            language: 'en_US',
            languageBundleVersion: undefined,
            userId: 'principal'
        });
    });

    it('settles without blocking authenticated navigation when both requests fail', async () => {
        spectator
            .inject(DotSystemConfigService)
            .getSystemConfig.mockReturnValue(throwError(() => new Error('Configuration failed')));
        spectator
            .inject(DotMessageService)
            .init.mockReturnValue(throwError(() => new Error('Bundle failed')));
        emitAuth(authFor());
        await ready();
        expect(spectator.service.systemConfig()).toBeNull();
        expect(console.warn).toHaveBeenCalled();
        expect(console.error).toHaveBeenCalled();
    });

    it('reloads configuration after logout and re-login as the same principal', async () => {
        emitAuth(authFor());
        await ready();
        emitAuth({ user: null, loginAsUser: null } as unknown as Auth);
        emitAuth(authFor());
        await ready();
        expect(spectator.inject(DotSystemConfigService).getSystemConfig).toHaveBeenCalledTimes(2);
    });

    it('supports explicit configuration refreshes used by the configuration portlet', async () => {
        emitAuth(authFor());
        await ready();
        spectator
            .inject(DotSystemConfigService)
            .getSystemConfig.mockReturnValue(
                of({ languageBundleVersion: 'new-build' } as DotSystemConfig)
            );
        spectator.service.loadSystemConfig();
        await ready();
        expect(spectator.inject(DotMessageService).init).toHaveBeenLastCalledWith({
            language: 'en_US',
            languageBundleVersion: 'new-build',
            userId: 'principal'
        });
    });
});
