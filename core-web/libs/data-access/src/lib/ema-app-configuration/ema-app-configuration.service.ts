import { EMPTY, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

import { pluck, switchMap, map, defaultIfEmpty, catchError } from 'rxjs/operators';

import { SiteService } from '@dotcms/dotcms-js';
import { DotAppsSite, DotApp } from '@dotcms/dotcms-models';

import { DotLicenseService } from '../dot-license/dot-license.service';

interface EmaAppSecretValue {
    pattern: string;
    url: string;
    options: EmaAppOptions;
}

interface EmaAppOptions {
    authenticationToken: string;
    [key: string]: string;
}

@Injectable()
export class EmaAppConfigurationService {
    http = inject(HttpClient);
    router = inject(Router);
    licenseService = inject(DotLicenseService);
    siteService = inject(SiteService);

    /**
     * Get the EMA app configuration for the current site.
     *
     * @param {string} url
     * @return {*}  {(Observable<EmaAppSecretValue | null>)}
     * @memberof EmaAppConfigurationService
     */
    get(url: string): Observable<EmaAppSecretValue | null> {
        return this.licenseService
            .isEnterprise()
            .pipe(catchError(() => EMPTY))
            .pipe(
                switchMap(() => this.getCurrentSiteIdentifier()),
                switchMap((currentSiteId: string) =>
                    this.getEmaAppConfiguration(currentSiteId).pipe(
                        map(getConfigurationForCurrentSite(currentSiteId))
                    )
                ),
                map(getSecretByUrlMatch(url)),
                catchError(() => EMPTY),
                defaultIfEmpty<EmaAppSecretValue | null>(null)
            );
    }

    private getCurrentSiteIdentifier(): Observable<string> {
        return this.siteService.getCurrentSite().pipe(pluck('identifier'));
    }

    private getEmaAppConfiguration(id: string): Observable<DotApp> {
        return this.http
            .get<{ entity: DotApp }>(`/api/v1/apps/dotema-config-v2/${id}`)
            .pipe(pluck('entity'));
    }
}

function getConfigurationForCurrentSite(currentSiteId: string): (app: DotApp) => DotAppsSite {
    return (app) => {
        const site =
            app?.sites?.find((site) => site.id === currentSiteId && site.configured) || null;

        if (!site) {
            throw new Error('No site configuration found');
        }

        return site;
    };
}

function getSecretByUrlMatch(url: string): (value: DotAppsSite | null) => EmaAppSecretValue | null {
    return (site: DotAppsSite | null) => {
        if (!site) {
            return null;
        }

        const secrets = site.secrets || [];

        for (const secret of secrets) {
            try {
                const parsedSecrets: EmaAppSecretValue[] = JSON.parse(secret.value);

                for (const parsedSecret of parsedSecrets) {
                    if (new RegExp(parsedSecret.pattern).test(url)) {
                        return parsedSecret;
                    }
                }
            } catch (error) {
                console.error(error);

                throw new Error('Error parsing JSON');
            }
        }

        return null;
    };
}
