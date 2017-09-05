import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs/Rx';
import { LoginService } from 'dotcms-js/dotcms-js';
import { DotRouterService } from './dot-router-service';

@Injectable()
export class RoutingPublicAuthService implements CanActivate {
    constructor(private router: DotRouterService, private loginService: LoginService) {}

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.loginService.isLogin$
            .map(isLogin => {
                if (isLogin) {
                    this.router.goToMain();
                    return false;
                } else {
                    return true;
                }
            })
            .take(1);
    }
}
