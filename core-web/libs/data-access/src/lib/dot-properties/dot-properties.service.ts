import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import { DotCMSResponse, FEATURE_FLAG_NOT_FOUND, FeaturedFlags } from '@dotcms/dotcms-models';

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
     * @returns {Observable<string | boolean>} Value type depends on the key (e.g. feature flags as booleans).
     * @memberof DotPropertiesService
     */
    getKey(key: string): Observable<string | boolean> {
        return this.http
            .get<
                DotCMSResponse<Record<string, string | boolean>>
            >('/api/v1/configuration/config', { params: { keys: key } })
            .pipe(
                take(1),
                map((response) => response.entity[key] ?? FEATURE_FLAG_NOT_FOUND)
            );
    }

    /**
     * Get the values of specific keys
     * from the dotmarketing-config.properties
     *
     * @param string[] keys
     * @returns {Observable<Record<string, string | boolean>>}
     * @memberof DotPropertiesService
     */
    getKeys(keys: string[]): Observable<Record<string, string | boolean>> {
        return this.http
            .get<DotCMSResponse<Record<string, string | boolean>>>('/api/v1/configuration/config', {
                params: { keys: keys.join() }
            })
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
            .get<DotCMSResponse<Record<string, string[]>>>('/api/v1/configuration/config', {
                params: { keys: `list:${key}` }
            })
            .pipe(
                take(1),
                map((x) => x?.entity?.[key])
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
            map((value) => {
                // /api/v1/configuration/config returns JSON booleans for FEATURE_FLAG_* keys
                // (see ConfigurationResource) but other keys may still come through as "true"/"false" strings.
                if (typeof value === 'boolean') {
                    return value;
                }

                return value === FEATURE_FLAG_NOT_FOUND ? true : value === 'true';
            })
        );
    }

    /**
     * Retrieves feature flags for given keys.
     *
     * Value resolution (mirrors {@link getFeatureFlag}):
     * - Native booleans pass through as-is (FEATURE_FLAG_* keys return JSON booleans
     *   from /api/v1/configuration/config — see ConfigurationResource).
     * - String `"true"` / `"false"` is coerced to the matching boolean.
     * - `FEATURE_FLAG_NOT_FOUND` ("NOT_FOUND") is treated as an implicit `true`:
     *   when a feature flag is not defined on the server, the feature is considered enabled by default.
     * - Any other string value passes through unchanged.
     *
     * @param {string[]} keys - An array of keys to retrieve feature flags for.
     * @returns {Observable<Record<string, boolean | string>>} - An Observable that emits a record containing key-value pairs of feature flags.
     */
    getFeatureFlags(keys: FeaturedFlags[]): Observable<Record<string, boolean | string>> {
        return this.getKeys(keys).pipe(
            map((flags) => {
                return Object.entries(flags).reduce(
                    (acc, [key, value]) => {
                        if (typeof value === 'boolean') {
                            acc[key] = value;
                        } else if (value === FEATURE_FLAG_NOT_FOUND) {
                            acc[key] = true;
                        } else {
                            acc[key] = value === 'true' ? true : value === 'false' ? false : value;
                        }

                        return acc;
                    },
                    {} as Record<string, boolean | string>
                );
            })
        );
    }
}
