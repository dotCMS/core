import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

import { pluck, switchMap, map, defaultIfEmpty, takeWhile } from 'rxjs/operators';

import { Site } from '@dotcms/dotcms-js';
import { DotAppsSite, DotApps } from '@dotcms/dotcms-models';

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

@Injectable()
export class EmaAppConfigurationService {
    http = inject(HttpClient);
    router = inject(Router);
    licenseService = inject(DotLicenseService);

    get(url: string): Observable<SecretValue | null> {
        return this.licenseService.isEnterprise().pipe(
            takeWhile((isEnterprise: boolean) => isEnterprise), // stop if license is not enterprise
            switchMap(() => this.getCurrentSiteIdentifier()),
            switchMap((currentSiteId: string) =>
                this.getEmaAppConfiguration(currentSiteId).pipe(
                    map((appConfiguration) => ({ currentSiteId, appConfiguration }))
                )
            ),
            map(({ currentSiteId, appConfiguration }) =>
                this.getConfigurationForCurrentSite(appConfiguration, currentSiteId)
            ),
            takeWhile((site: DotAppsSite | null) => !!site), // stop if site is undefined or null
            map((site: DotAppsSite | null) => {
                if (!site) {
                    return null; // Explicitly handle the null case
                }

                const secrets = site.secrets || []; // Provide a default empty array if secrets is undefined

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
            defaultIfEmpty<SecretValue | null>(null)
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

    private getConfigurationForCurrentSite(
        appConfiguration: DotApps,
        siteId: string
    ): DotAppsSite | null {
        return (
            appConfiguration?.sites?.find((site) => site.id === siteId && site.configured) || null
        );
    }
}
