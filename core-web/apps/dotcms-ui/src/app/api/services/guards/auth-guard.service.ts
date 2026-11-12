import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot } from '@angular/router';

import { map, switchMap } from 'rxjs/operators';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';

/**
 * Route Guard that checks if a User is logged in and, once logged in, waits for the
 * translated labels to be available before activating the route.
 */
@Injectable()
export class AuthGuardService implements CanActivate {
    constructor(
        private dotRouterService: DotRouterService,
        private loginService: LoginService,
        private dotMessageService: DotMessageService
    ) {}

    canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.loginService.isLogin$.pipe(
            switchMap((isLogin) => {
                if (!isLogin) {
                    this.dotRouterService.goToLogin();
                    this.dotRouterService.storedRedirectUrl = state.url;

                    return of(false);
                }

                // The labels are only downloadable by an authenticated user, so make sure
                // they are loaded before the backend screens render.
                return this.dotMessageService.ensureLoaded().pipe(map(() => true));
            })
        );
    }
}
