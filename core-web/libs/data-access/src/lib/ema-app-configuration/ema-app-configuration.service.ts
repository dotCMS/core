import { EMPTY, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

import { pluck, switchMap, map, defaultIfEmpty, takeWhile, catchError } from 'rxjs/operators';

import { Site } from '@dotcms/dotcms-js';
import { DotAppsSite, DotApps } from '@dotcms/dotcms-models';

import { DotLicenseService } from '../dot-license/dot-license.service';

interface EmaAppSecretValue {
    pattern: string;
    url: string;
    options: EmaAppOptions;
}

interface EmaAppOptions {
    authenticationToken: string;
    depth: number;
    'X-CONTENT-APP': string;
}

@Injectable()
export class EmaAppConfigurationService {
    http = inject(HttpClient);
    router = inject(Router);
    licenseService = inject(DotLicenseService);

    get(url: string): Observable<EmaAppSecretValue | null> {
        return this.licenseService
            .isEnterprise()
            .pipe(catchError(() => EMPTY))
            .pipe(
                takeWhile((isEnterprise: boolean) => isEnterprise), // stop if license is not enterprise
                switchMap(() => this.getCurrentSiteIdentifier().pipe(catchError(() => EMPTY))),
                switchMap((currentSiteId: string) =>
                    this.getEmaAppConfiguration(currentSiteId).pipe(
                        map((appConfiguration) => ({ currentSiteId, appConfiguration })),
                        catchError(() => EMPTY)
                    )
                ),
                map(({ currentSiteId, appConfiguration }) =>
                    this.getConfigurationForCurrentSite(appConfiguration, currentSiteId)
                ),
                takeWhile((site: DotAppsSite | null) => !!site), // stop if site is undefined or null
                map(getSecretByUrlMatch(url)),
                defaultIfEmpty<EmaAppSecretValue | null>(null)
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

function getSecretByUrlMatch(url: string): (value: DotAppsSite | null) => EmaAppSecretValue | null {
    return (site: DotAppsSite | null) => {
        if (!site) {
            return null; // Explicitly handle the null case
        }

        const secrets = site.secrets || []; // Provide a default empty array if secrets is undefined

        for (const secret of secrets) {
            try {
                const parsedSecrets: EmaAppSecretValue[] = JSON.parse(secret.value);

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
    };
}
