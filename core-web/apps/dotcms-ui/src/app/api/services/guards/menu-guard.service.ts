import { Observable } from 'rxjs';

import { map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { DotMenuService } from '../dot-menu.service';
import { DotRouterService } from '../dot-router/dot-router.service';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';

/**
 * Route Guard that checks if a User have access to the specified Menu portlet.
 */
@Injectable()
export class MenuGuardService implements CanActivate {
    constructor(
        private dotMenuService: DotMenuService,
        private dotRouterService: DotRouterService,
        private dotNavigationService: DotNavigationService
    ) {}

    canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.canAccessPortlet(this.dotRouterService.getPortletId(state.url));
    }

    canActivateChild(
        _route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): Observable<boolean> {
        return this.canAccessPortlet(this.dotRouterService.getPortletId(state.url));
    }

    /**
     * Check if User has access to the requested route (url) based on the Menu, otherwise go to the 'First Portlet' and return false.
     *
     * @param string url
     * @returns boolean
     */
    private canAccessPortlet(url: string): Observable<boolean> {
        return this.dotMenuService.isPortletInMenu(url).pipe(
            map((isValidPortlet) => {
                if (!isValidPortlet) {
                    this.dotNavigationService.goToFirstPortlet();
                }
                return isValidPortlet;
            })
        );
    }
}
