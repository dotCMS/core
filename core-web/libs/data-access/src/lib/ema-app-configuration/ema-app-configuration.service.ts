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
        // Remove trailing and leading slashes
        url = url.replace(/^\/+|\/+$/g, '');

        return this.licenseService
            .isEnterprise()
            .pipe(
                map((isEnterprise) => {
                    if (!isEnterprise) {
                        throw new Error('Not enterprise');
                    }

                    return isEnterprise;
                })
            )
            .pipe(
                switchMap(() => this.getCurrentSiteIdentifier()),
                switchMap((currentSiteId: string) => {
                    return this.getEmaAppConfiguration(currentSiteId).pipe(
                        map(getConfigurationForCurrentSite(currentSiteId))
                    );
                }),
                map(getSecretByUrlMatch(url)),
                catchError(() => {
                    return EMPTY;
                }),
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

function getSecretByUrlMatch(url: string): (site: DotAppsSite) => EmaAppSecretValue | null {
    return (site: DotAppsSite) => {
        const secrets = site.secrets || [];

        for (const secret of secrets) {
            try {
                const parsedSecrets: EmaAppSecretValue[] = JSON.parse(secret.value);

                for (const parsedSecret of parsedSecrets) {
                    if (doesPathMatch(parsedSecret.pattern, url)) {
                        return parsedSecret;
                    }
                }
            } catch (error) {
                console.error(error);

                throw new Error('Error parsing JSON');
            }
        }

        throw new Error('Current URL did not match any pattern');
    };
}

function doesPathMatch(pattern: string, path: string): boolean {
    // Remove leading and trailing slashes from the pattern and the path
    const normalizedPattern = pattern.replace(/^\/|\/$/g, '');
    const normalizedPath = path.replace(/^\/|\/$/g, '');

    // Create a RegExp object using the normalized pattern
    const regex = new RegExp(normalizedPattern);

    // Test the normalized path against the regex pattern
    return regex.test(normalizedPath);
}
