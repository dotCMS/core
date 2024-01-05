import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
    ActivatedRouteSnapshot,
    CanActivate,
    Router,
    RouterStateSnapshot,
    UrlTree
} from '@angular/router';

import { pluck, switchMap, map } from 'rxjs/operators';

import { Site } from '@dotcms/dotcms-js';
import { DotAppsSite } from '@dotcms/dotcms-models';

import { DotAppsService } from '../../dot-apps/dot-apps.service';

function doesPathMatchPattern(regexPattern: string, pathname: string) {
    return new RegExp(regexPattern).test(pathname);
}

@Injectable({
    providedIn: 'root'
})
export class EmaAppGuard implements CanActivate {
    http = inject(HttpClient);
    router = inject(Router);
    appService = inject(DotAppsService);

    canActivate(
        route: ActivatedRouteSnapshot,
        _state: RouterStateSnapshot
    ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
        const currentSite$ = this.http
            .get<{ entity: Site }>('/api/v1/site/currentSite')
            .pipe(pluck('entity', 'identifier'));

        return currentSite$.pipe(
            switchMap((siteId) => {
                return this.appService.getConfiguration('dotema-config-v2', siteId).pipe(
                    map((appConfiguration) =>
                        appConfiguration.sites.find((site) => site.id === siteId)
                    ),
                    map((site: DotAppsSite) => {
                        try {
                            const val = JSON.parse(site.secrets[0].value)[0];

                            const newQueryParams = {
                                ...route.queryParams,
                                url: route.queryParams.url.substring(1)
                            };

                            delete newQueryParams['device_inode'];

                            if (doesPathMatchPattern(val.pattern, route.queryParams.url)) {
                                return this.router.createUrlTree(['edit-ema'], {
                                    queryParams: newQueryParams
                                });
                            }

                            return true;
                        } catch (error) {
                            return true;
                        }
                    })
                );
            })
        );
    }
}
