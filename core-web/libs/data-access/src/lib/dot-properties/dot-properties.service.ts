import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import { FEATURE_FLAG_NOT_FOUND, FeaturedFlags } from '@dotcms/dotcms-models';

@Injectable({
    providedIn: 'root'
})
export class DotPropertiesService {
    private readonly http = inject(HttpClient);

    /**
     * Get the value of specific key
     * from the dotmarketing-config.properties
     *
     * @param string key
     * @returns {Observable<string>}
     * @memberof DotPropertiesService
     */
    getKey(key: string): Observable<string> {
        return this.http.get('/api/v1/configuration/config', { params: { keys: key } }).pipe(
            take(1),
            map((x) => (x as any)?.['entity']?.[key])
        );
    }

    /**
     * Get the values of specific keys
     * from the dotmarketing-config.properties
     *
     * @param string[] keys
     * @returns {Observable<Record<string, string>>}
     * @memberof DotPropertiesService
     */
    getKeys(keys: string[]): Observable<Record<string, string>> {
        return this.http
            .get('/api/v1/configuration/config', { params: { keys: keys.join() } })
            .pipe(
                take(1),
                map((x) => x?.entity)
            );
    }

    /**
     * Get the value of specific key as a list
     * from the dotmarketing-config.properties
     *
     * @param string key
     * @returns {Observable<string[]>}
     * @memberof DotPropertiesService
     */
    getKeyAsList(key: string): Observable<string[]> {
        return this.http
            .get('/api/v1/configuration/config', { params: { keys: `list:${key}` } })
            .pipe(
                take(1),
                map((x) => (x as any)?.['entity']?.[key])
            );
    }

    /**
     * Get the value of specific feature flag
     * @param {FeaturedFlags} key
     * @return {*}  {Observable<boolean>}
     * @memberof DotPropertiesService
     */
    getFeatureFlag(key: FeaturedFlags): Observable<boolean> {
        return this.getKey(key).pipe(
            map((value) => (value === FEATURE_FLAG_NOT_FOUND ? true : value === 'true'))
        );
    }

    /**
     * Retrieves feature flags for given keys.
     *
     * @param {string[]} keys - An array of keys to retrieve feature flags for.
     * @returns {Observable<Record<string, boolean | string>>} - An Observable that emits a record containing key-value pairs of feature flags.
     */
    getFeatureFlags(keys: FeaturedFlags[]): Observable<Record<string, boolean | string>> {
        return this.getKeys(keys).pipe(
            map((flags) => {
                return Object.entries(flags).reduce(
                    (acc, [key, value]) => {
                        acc[key] = value === 'true' ? true : value === 'false' ? false : value;

                        return acc;
                    },
                    {} as Record<string, boolean | string>
                );
            })
        );
    }
}
