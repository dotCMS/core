import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

import { pluck, switchMap, map, filter } from 'rxjs/operators';

import { DotLicenseService } from '@dotcms/data-access';
import { Site } from '@dotcms/dotcms-js';
import { DotAppsSite, DotApps, DotAppsSecrets } from '@dotcms/dotcms-models';

interface SecretValue {
    pattern: string;
    url: string;
    options: Options;
}

interface Options {
    authenticationToken: string;
    depth: number;
    'X-CONTENT-APP': string;
}

@Injectable({
    providedIn: 'root'
})
@Injectable()
export class EmaAppConfigurationService {
    http = inject(HttpClient);
    router = inject(Router);
    licenseService = inject(DotLicenseService);

    get(url: string): Observable<SecretValue> {
        return this.licenseService.isEnterprise().pipe(
            filter((isEnterprise: boolean) => isEnterprise),
            switchMap(() => this.getCurrentSiteIdentifier()),
            switchMap((currentSiteId: string) => this.getEmaAppConfiguration(currentSiteId)),
            map((appConfiguration: DotApps) =>
                this.getConfigurationForCurrentSite(appConfiguration, url)
            ),
            map((site: DotAppsSite) => site.secrets),
            map((secrets: DotAppsSecrets[]) => {
                for (const secret of secrets) {
                    try {
                        const parsedSecret: SecretValue = JSON.parse(secret.value);

                        if (new RegExp(parsedSecret.pattern).test(url)) {
                            return parsedSecret;
                        }
                    } catch (error) {
                        console.error('Error parsing JSON:', error);

                        return null;
                    }
                }

                return null;
            })
        );
    }

    private getCurrentSiteIdentifier(): Observable<string> {
        return this.http
            .get<{ entity: Site }>('/api/v1/site/currentSite')
            .pipe(pluck('entity', 'identifier'));
    }

    private getEmaAppConfiguration(id: string): Observable<DotApps> {
        return this.http
            .get<{ entity: DotApps }>(`/api/v1/apps/dotema-config-v2/${id}`)
            .pipe(pluck('entity'));
    }

    private getConfigurationForCurrentSite(appConfiguration: DotApps, siteId: string): DotAppsSite {
        return appConfiguration.sites.find((site) => site.id === siteId);
    }

    // private processSiteConfiguration(
    //     route: ActivatedRouteSnapshot,
    //     currentSiteId: string
    // ): Observable<boolean> {
    //     return this.getEmaAppConfiguration(currentSiteId).pipe(
    //         map((appConfiguration) => {
    //             return this.getConfigurationForCurrentSite(appConfiguration, currentSiteId);
    //         }),
    //         switchMap((site: DotAppsSite) => this.processSite(route, site)),
    //         catchError((error) => {
    //             console.error('Error getting site configuration:', error);

    //             return of(true);
    //         })
    //     );
    // }

    // private processSite(route: ActivatedRouteSnapshot, site: DotAppsSite): Observable<boolean> {
    //     try {
    //         const val = JSON.parse(site.secrets[0].value)[0];
    //         const newQueryParams = getUpdatedQueryParams(route);

    //         if (doesPathMatchPattern(val.pattern, route.queryParams.url)) {
    //             this.router.navigate(['edit-ema'], {
    //                 queryParams: newQueryParams,
    //                 replaceUrl: true,
    //                 state: {
    //                     remoteUrl: 'https://google.com'
    //                 }
    //             });

    //             return of(false);
    //         }

    //         return of(true);
    //     } catch (error) {
    //         console.error('Error processing site:', error);

    //         return of(true);
    //     }
    // }
}
