import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';

/**
 * Route Guard that handle the public pages, where the User don't need to be logged in.
 */
@Injectable()
export class PublicAuthGuardService implements CanActivate {
    private router = inject(DotRouterService);
    private loginService = inject(LoginService);

    /**
     * Guard checks is the User is logged in to redirect to the First Portlet otherwise approve the route request.
     * @param ActivatedRouteSnapshot route
     * @param RouterStateSnapshot state
     * @returns Observable<boolean>
     */
    canActivate(_route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<boolean> {
        return this.loginService.isLogin$.pipe(
            map((isLogin) => {
                if (isLogin) {
                    this.router.goToMain();
                }

                return !isLogin;
            })
        );
    }
}
