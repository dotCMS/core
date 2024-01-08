import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotRouterService, DotSessionStorageService } from '@dotcms/data-access';

import { DotMenuService } from '../dot-menu.service';

/**
 * Route Guard that checks if a User have access to the specified Menu portlet.
 */
@Injectable()
export class MenuGuardService implements CanActivate {
    constructor(
        private dotMenuService: DotMenuService,
        private dotRouterService: DotRouterService,
        private dotNavigationService: DotNavigationService,
        private dotSessionStorageService: DotSessionStorageService
    ) {}

    canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.canAccessPortlet(state.url);
    }

    canActivateChild(
        _route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): Observable<boolean> {
        return this.canAccessPortlet(state.url);
    }

    /**
     * Check if User has access to the requested route (url) based on the Menu, otherwise go to the 'First Portlet' and return false.
     *
     * @param string url
     * @returns boolean
     */
    private canAccessPortlet(url: string): Observable<boolean> {
        const id = this.dotRouterService.getPortletId(url);
        const checkJSPPortlet = this.dotRouterService.isJSPPortletURL(url);

        return this.dotMenuService.isPortletInMenu(id, checkJSPPortlet).pipe(
            map((isValidPortlet) => {
                if (!isValidPortlet) {
                    this.dotSessionStorageService.removeVariantId();
                    this.dotNavigationService.goToFirstPortlet();
                }

                return isValidPortlet;
            })
        );
    }
}
