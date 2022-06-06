import { map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { LoginService } from '@dotcms/dotcms-js';
import { DotRouterService } from '../dot-router/dot-router.service';

/**
 * Route Guard that handle the public pages, where the User don't need to be logged in.
 */
@Injectable()
export class PublicAuthGuardService implements CanActivate {
    constructor(private router: DotRouterService, private loginService: LoginService) {}

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
