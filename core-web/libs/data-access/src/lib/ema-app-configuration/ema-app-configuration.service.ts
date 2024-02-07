import { EMPTY, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

import { pluck, map, defaultIfEmpty, catchError } from 'rxjs/operators';

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

    /**
     * Get the EMA app configuration for the current site.
     *
     * @param {string} url
     * @return {*}  {(Observable<EmaAppSecretValue | null>)}
     * @memberof EmaAppConfigurationService
     */
    get(url: string): Observable<EmaAppSecretValue | null> {
        // Remove trailing and leading slashes
        url = url?.replace(/^\/+|\/+$/g, '');

        return this.http.get<{ entity: { config: EmaAppSecretValue[] } }>(`/api/v1/ema`).pipe(
            pluck('entity', 'config'),
            map((config) => {
                for (const secret of config) {
                    try {
                        if (doesPathMatch(secret.pattern, url)) {
                            return secret;
                        }
                    } catch (error) {
                        throw new Error('Error on match URL pattern');
                    }
                }

                throw new Error('Current URL did not match any pattern');
            }),
            catchError(() => {
                return EMPTY;
            }),
            defaultIfEmpty<EmaAppSecretValue | null>(null)
        );
    }
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
