import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotRouterService, DotSessionStorageService } from '@dotcms/data-access';

import { DotNavigationService } from '../../../view/components/dot-navigation/services/dot-navigation.service';
import { DotMenuService } from '../dot-menu.service';

/**
 * Route Guard that checks if a User have access to the specified Menu portlet.
 */
@Injectable()
export class MenuGuardService implements CanActivate {
    private dotMenuService = inject(DotMenuService);
    private dotRouterService = inject(DotRouterService);
    private dotNavigationService = inject(DotNavigationService);
    private dotSessionStorageService = inject(DotSessionStorageService);

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
