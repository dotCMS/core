import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

import { pluck, switchMap, map, defaultIfEmpty, takeWhile } from 'rxjs/operators';

import { Site } from '@dotcms/dotcms-js';
import { DotAppsSite, DotApps, DotAppsSecrets } from '@dotcms/dotcms-models';

import { DotLicenseService } from '../dot-license/dot-license.service';

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
            takeWhile((isEnterprise: boolean) => isEnterprise), // stop if license is not enterprise
            switchMap(() => this.getCurrentSiteIdentifier()),
            switchMap((currentSiteId: string) =>
                this.getEmaAppConfiguration(currentSiteId).pipe(
                    map((appConfiguration) => ({ currentSiteId, appConfiguration }))
                )
            ),
            map(({ currentSiteId, appConfiguration }) => {
                return this.getConfigurationForCurrentSite(appConfiguration, currentSiteId);
            }),
            takeWhile((site: DotAppsSite) => !!site), // stop if site is undefined
            map((site: DotAppsSite) => site.secrets),
            map((secrets: DotAppsSecrets[]) => {
                for (const secret of secrets) {
                    try {
                        const parsedSecrets: SecretValue[] = JSON.parse(secret.value);

                        for (const parsedSecret of parsedSecrets) {
                            if (new RegExp(parsedSecret.pattern).test(url)) {
                                return parsedSecret;
                            }
                        }
                    } catch (error) {
                        console.error('Error parsing JSON:', error);

                        return null;
                    }
                }

                return null;
            }),
            defaultIfEmpty(null)
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
        return appConfiguration.sites.find((site) => site.id === siteId && site.configured);
    }
}
