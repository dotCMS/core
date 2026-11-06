import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { firstValueFrom, Observable, of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';

import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { DotRouterService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';

import { AuthGuardService } from './auth-guard.service';

describe('AuthGuardService', () => {
    let spectator: SpectatorService<AuthGuardService>;
    let router: { goToLogin: ReturnType<typeof vi.fn>; storedRedirectUrl: string };
    let login: { isLogin$: Observable<boolean> };
    let store: { waitForSessionReady: ReturnType<typeof vi.fn> };
    const route = {} as ActivatedRouteSnapshot;
    const state = { url: '/test' } as RouterStateSnapshot;
    const createService = createServiceFactory({ service: AuthGuardService });

    beforeEach(() => {
        router = { goToLogin: vi.fn(), storedRedirectUrl: '' };
        login = { isLogin$: of(true) };
        store = { waitForSessionReady: vi.fn().mockReturnValue(of(void 0)) };
        spectator = createService({
            providers: [
                { provide: DotRouterService, useValue: router },
                { provide: LoginService, useValue: login },
                { provide: GlobalStore, useValue: store }
            ]
        });
    });

    it('allows authenticated navigation after the store is ready', async () => {
        expect(await firstValueFrom(spectator.service.canActivate(route, state))).toBe(true);
        expect(store.waitForSessionReady).toHaveBeenCalledOnce();
    });

    it('redirects unauthenticated users without waiting for protected data', async () => {
        login.isLogin$ = of(false);
        expect(await firstValueFrom(spectator.service.canActivate(route, state))).toBe(false);
        expect(router.goToLogin).toHaveBeenCalledOnce();
        expect(router.storedRedirectUrl).toBe('/test');
        expect(store.waitForSessionReady).not.toHaveBeenCalled();
    });

    it('waits for store readiness before activating protected components', () => {
        const loaded = new Subject<void>();
        store.waitForSessionReady.mockReturnValue(loaded);
        const activated = vi.fn();
        spectator.service.canActivate(route, state).subscribe(activated);
        expect(activated).not.toHaveBeenCalled();
        loaded.next();
        loaded.complete();
        expect(activated).toHaveBeenCalledWith(true);
    });

    it('uses the same readiness check for child navigation', async () => {
        expect(await firstValueFrom(spectator.service.canActivateChild(route, state))).toBe(true);
        expect(store.waitForSessionReady).toHaveBeenCalledOnce();
    });

    it('rejects unauthenticated child navigation', async () => {
        login.isLogin$ = of(false);
        expect(await firstValueFrom(spectator.service.canActivateChild(route, state))).toBe(false);
        expect(store.waitForSessionReady).not.toHaveBeenCalled();
    });

    it('does not downgrade authentication failures into successful activation', async () => {
        const failure = new Error('Authentication failed');
        login.isLogin$ = throwError(() => failure);
        await expect(firstValueFrom(spectator.service.canActivate(route, state))).rejects.toBe(
            failure
        );
        expect(store.waitForSessionReady).not.toHaveBeenCalled();
    });
});
