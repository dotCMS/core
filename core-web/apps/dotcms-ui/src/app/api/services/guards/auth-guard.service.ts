import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';

/**
 * Route Guard that checks if a User is logged in.
 */
@Injectable()
export class AuthGuardService implements CanActivate {
    constructor(
        private dotRouterService: DotRouterService,
        private loginService: LoginService
    ) {}

    canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.loginService.isLogin$.pipe(
            map((isLogin) => {
                if (!isLogin) {
                    this.dotRouterService.goToLogin();
                    this.dotRouterService.storedRedirectUrl = state.url;
                }

                return isLogin;
            })
        );
    }
}
