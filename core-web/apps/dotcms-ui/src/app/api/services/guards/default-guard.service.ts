import { Observable } from 'rxjs';

import { inject, Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';

import { DotMenuService } from '../dot-menu.service';
import { DynamicRouteService } from '../dynamic-route.service';

/**
 * Route Guard for the wildcard (**) route.
 * Waits for the menu to load and dynamic routes to register before deciding
 * whether to redirect. This prevents a race condition where dynamic portlet
 * routes aren't registered yet on page refresh.
 */
@Injectable()
export class DefaultGuardService implements CanActivate {
    private dotRouterService = inject(DotRouterService);
    private router = inject(Router);
    private dotMenuService = inject(DotMenuService);
    private dynamicRouteService = inject(DynamicRouteService);

    canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.dotMenuService.loadMenu().pipe(
            map((menus) => {
                // Ensure dynamic routes are registered (idempotent)
                const allMenuItems = menus.flatMap((menu) => menu.menuItems);
                this.dynamicRouteService.registerRoutesFromMenuItems(allMenuItems);

                // Check if the attempted URL now matches a registered dynamic route
                const url = state.url;
                const path = url.startsWith('/') ? url.substring(1) : url;
                const basePath = path.split('/')[0].split('?')[0];

                if (this.dynamicRouteService.isRouteRegistered(basePath)) {
                    // Route exists now — re-navigate so the router matches it
                    this.router.navigateByUrl(url);

                    return false;
                }

                // No matching dynamic route, redirect to main
                this.dotRouterService.goToMain();

                return true;
            })
        );
    }
}
