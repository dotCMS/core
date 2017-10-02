import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { DotMenuService } from './dot-menu.service';
import { Observable } from 'rxjs/Rx';
import { LoginService } from 'dotcms-js/dotcms-js';
import { DotRouterService } from './dot-router-service';
import { DotNavigationService } from '../../view/components/dot-navigation/dot-navigation.service';

@Injectable()
export class RoutingPrivateAuthService implements CanActivate {
    constructor(
        private dotRouterService: DotRouterService,
        private dotMenuService: DotMenuService,
        private dotNavigationService: DotNavigationService,
        private loginService: LoginService
    ) {}

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.loginService.isLogin$.do(isLogin => {
            if (!isLogin) {
                this.dotRouterService.goToLogin();
            } else if (state.url === '/') {
                this.dotNavigationService.goToFirstPortlet();
            }
            return isLogin;
        });
    }

    canActivateChild(
        route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): Observable<boolean> {
        return this.dotMenuService
            .isPortletInMenu(this.dotRouterService.getPortletId(state.url))
            .map((isPortletInMenu: boolean) => {
                if (!isPortletInMenu) {
                    this.dotNavigationService.goToFirstPortlet();
                }
                return isPortletInMenu;
            });
    }
}
