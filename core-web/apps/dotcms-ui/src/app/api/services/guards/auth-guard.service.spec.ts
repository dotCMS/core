import { firstValueFrom, Observable, of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { DotcmsConfigService, LoginService } from '@dotcms/dotcms-js';

import { AuthGuardService } from './auth-guard.service';

describe('AuthGuardService', () => {
    let guard: AuthGuardService;
    let router: { goToLogin: ReturnType<typeof vi.fn>; storedRedirectUrl: string };
    let login: { isLogin$: Observable<boolean>; currentUserLanguageId: string };
    let config: { getConfig: ReturnType<typeof vi.fn> };
    let messages: { init: ReturnType<typeof vi.fn> };
    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/test' } as RouterStateSnapshot;

    beforeEach(() => {
        router = { goToLogin: vi.fn(), storedRedirectUrl: '' };
        login = { isLogin$: of(true), currentUserLanguageId: 'en_US' };
        config = {
            getConfig: vi.fn().mockReturnValue(of({ languageBundleVersion: 'build-hash' }))
        };
        messages = { init: vi.fn().mockReturnValue(of(void 0)) };
        TestBed.configureTestingModule({
            providers: [
                AuthGuardService,
                { provide: DotRouterService, useValue: router },
                { provide: LoginService, useValue: login },
                { provide: DotcmsConfigService, useValue: config },
                { provide: DotMessageService, useValue: messages }
            ]
        });
        guard = TestBed.inject(AuthGuardService);
    });

    it('should validate the cache with the server build hash after authentication', async () => {
        expect(await firstValueFrom(guard.canActivate(route, state))).toBe(true);
        expect(messages.init).toHaveBeenCalledWith({
            language: 'en_US',
            languageBundleVersion: 'build-hash'
        });
    });

    it('should not load configuration or messages for an unauthenticated user', async () => {
        login.isLogin$ = of(false);
        expect(await firstValueFrom(guard.canActivate(route, state))).toBe(false);
        expect(router.goToLogin).toHaveBeenCalledOnce();
        expect(router.storedRedirectUrl).toBe('/test');
        expect(config.getConfig).not.toHaveBeenCalled();
        expect(messages.init).not.toHaveBeenCalled();
    });

    it('should wait for messages before activating backend components', () => {
        const loaded = new Subject<void>();
        messages.init.mockReturnValue(loaded);
        const activated = vi.fn();
        guard.canActivate(route, state).subscribe(activated);
        expect(activated).not.toHaveBeenCalled();
        loaded.next();
        loaded.complete();
        expect(activated).toHaveBeenCalledWith(true);
    });

    it('should use the default locale when the authenticated user has none', async () => {
        login.currentUserLanguageId = '';
        await firstValueFrom(guard.canActivate(route, state));
        expect(messages.init).toHaveBeenCalledWith({
            language: 'default',
            languageBundleVersion: 'build-hash'
        });
    });

    it('should await a fresh load when build metadata is missing rather than accept null versions', async () => {
        config.getConfig.mockReturnValue(of({}));
        await firstValueFrom(guard.canActivate(route, state));
        expect(messages.init).toHaveBeenCalledWith({
            language: 'en_US',
            languageBundleVersion: undefined
        });
    });

    it('should propagate a failed load without activating and allow a later attempt to retry', async () => {
        const failure = new Error('Language request failed');
        messages.init.mockReturnValueOnce(throwError(() => failure));
        await expect(firstValueFrom(guard.canActivate(route, state))).rejects.toBe(failure);
        expect(await firstValueFrom(guard.canActivate(route, state))).toBe(true);
        expect(messages.init).toHaveBeenCalledTimes(2);
    });
});
