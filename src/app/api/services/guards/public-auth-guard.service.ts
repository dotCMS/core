import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs/Rx';
import { LoginService } from 'dotcms-js/dotcms-js';
import { DotRouterService } from './../dot-router-service';

/**
 * Route Guard that handle the public pages, where the User don't need to be logged in.
 */
@Injectable()
export class PublicAuthGuardService implements CanActivate {
    constructor(private router: DotRouterService, private loginService: LoginService) {}

    /**
     * Guard checks is the User is logged in to redirect to the First Portlet otherwise approve the route request.
     * @param {ActivatedRouteSnapshot} route
     * @param {RouterStateSnapshot} state
     * @returns {Observable<boolean>}
     */
    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.loginService.isLogin$.map(isLogin => {
            if (isLogin) {
                this.router.goToMain();
            }
            return !isLogin;
        });
    }
}
