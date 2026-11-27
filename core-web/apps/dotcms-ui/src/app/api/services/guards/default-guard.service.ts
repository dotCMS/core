import { Observable } from 'rxjs';

import { inject, Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';

import { DynamicRouteService } from '../dynamic-route.service';

/**
 * Route Guard for the wildcard (**) route.
 * Waits for the menu to load before deciding whether to redirect. Loading the menu registers the
 * dynamic portlet routes it declares (before it reports `ready`), so this guard never decides
 * before they exist. That prevents a race where dynamic routes aren't registered yet on refresh.
 */
@Injectable()
export class DefaultGuardService implements CanActivate {
    private dotRouterService = inject(DotRouterService);
    private router = inject(Router);
    private globalStore = inject(GlobalStore);
    private dynamicRouteService = inject(DynamicRouteService);

    canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.globalStore.ensureLoaded().pipe(
            map(() => {
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
