import { Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import {
    ActivatedRouteSnapshot,
    CanActivate,
    CanActivateChild,
    RouterStateSnapshot
} from '@angular/router';

import { map, switchMap, take } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';

/**
 * Route Guard that checks if a User is logged in.
 */
@Injectable()
export class AuthGuardService implements CanActivate, CanActivateChild {
    private dotRouterService = inject(DotRouterService);
    private loginService = inject(LoginService);
    private globalStore = inject(GlobalStore);

    canActivateChild(
        route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): Observable<boolean> {
        return this.canActivate(route, state);
    }

    canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.loginService.isLogin$.pipe(
            take(1),
            switchMap((isLogin) => {
                if (!isLogin) {
                    this.dotRouterService.goToLogin();
                    this.dotRouterService.storedRedirectUrl = state.url;

                    return of(false);
                }

                return this.globalStore.waitForSessionReady().pipe(map(() => true));
            })
        );
    }
}
