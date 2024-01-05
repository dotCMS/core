import { Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
    ActivatedRouteSnapshot,
    CanActivate,
    Router,
    RouterStateSnapshot,
    UrlTree
} from '@angular/router';

import { pluck, switchMap, map, catchError } from 'rxjs/operators';

import { Site } from '@dotcms/dotcms-js';
import { DotApps, DotAppsSite } from '@dotcms/dotcms-models';

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
        return this.getCurrentSiteIdentifier().pipe(
            switchMap((currentSiteId) => this.processSiteConfiguration(route, currentSiteId)),
            catchError((error) => {
                console.error('Error with currentSite$ stream:', error);

                return of(true);
            })
        );
    }

    private getCurrentSiteIdentifier(): Observable<string> {
        return this.http
            .get<{ entity: Site }>('/api/v1/site/currentSite')
            .pipe(pluck('entity', 'identifier'));
    }

    private processSiteConfiguration(
        route: ActivatedRouteSnapshot,
        currentSiteId: string
    ): Observable<boolean> {
        return this.appService.getConfiguration('dotema-config-v2', currentSiteId).pipe(
            map((appConfiguration) =>
                this.getConfigurationForCurrentSite(appConfiguration, currentSiteId)
            ),
            switchMap((site: DotAppsSite) => this.processSite(route, site)),
            catchError((error) => {
                console.error('Error getting site configuration:', error);

                return of(true);
            })
        );
    }

    private getConfigurationForCurrentSite(appConfiguration: DotApps, siteId: string): DotAppsSite {
        return appConfiguration.sites.find((site) => site.id === siteId);
    }

    private processSite(route: ActivatedRouteSnapshot, site: DotAppsSite): Observable<boolean> {
        try {
            const val = JSON.parse(site.secrets[0].value)[0];
            const newQueryParams = getUpdatedQueryParams(route);

            if (doesPathMatchPattern(val.pattern, route.queryParams.url)) {
                this.router.navigate(['edit-ema'], {
                    queryParams: newQueryParams,
                    state: {
                        remoteUrl: 'https://google.com'
                    }
                });

                return of(false);
            }

            return of(true);
        } catch (error) {
            console.error('Error processing site:', error);

            return of(true);
        }
    }
}

function getUpdatedQueryParams(route: ActivatedRouteSnapshot) {
    const newQueryParams = {
        ...route.queryParams,
        url: route.queryParams.url.substring(1)
    };

    delete newQueryParams['device_inode'];

    return newQueryParams;
}
