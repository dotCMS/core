import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, shareReplay, take } from 'rxjs/operators';

import { DotCMSResponse, FEATURE_FLAG_NOT_FOUND, FeaturedFlags } from '@dotcms/dotcms-models';

@Injectable({
    providedIn: 'root'
})
export class DotPropertiesService {
    private readonly http = inject(HttpClient);
    // Process-lifetime cache so multiple components asking for the same flag during a single
    // page load don't trigger duplicate requests. Admin flag flips take effect after reload.
    private readonly featureFlagCache = new Map<string, Observable<boolean>>();

    /**
     * Get the value of specific key
     * from the dotmarketing-config.properties
     *
     * @param string key
     * @returns {Observable<string | boolean>} Value type depends on the key (e.g. feature flags as booleans).
     * @memberof DotPropertiesService
     */
    getKey(key: string): Observable<string | boolean> {
        const responseKey = this.removePrefix(key);

        return this.http
            .get<
                DotCMSResponse<Record<string, string | boolean>>
            >('/api/v1/configuration/config', { params: { keys: key } })
            .pipe(
                take(1),
                map((response) => response.entity[responseKey] ?? FEATURE_FLAG_NOT_FOUND)
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

    private removePrefix(key: string): string {
        return key.replace(/^(list:|boolean:|number:)/, '');
    }

    /**
     * Get the value of specific feature flag
     * @param {FeaturedFlags} key
     * @return {*}  {Observable<boolean>}
     * @memberof DotPropertiesService
     */
    getFeatureFlag(key: FeaturedFlags): Observable<boolean> {
        const cached = this.featureFlagCache.get(key);
        if (cached) {
            return cached;
        }

        const flag$ = this.getKey(key).pipe(
            map((value) => {
                if (typeof value === 'boolean') {
                    return value;
                }

                return value === FEATURE_FLAG_NOT_FOUND ? true : value.toLowerCase() === 'true';
            }),
            shareReplay(1)
        );

        this.featureFlagCache.set(key, flag$);

        return flag$;
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
                            // Lowercase the comparison so env-var-driven configs ("True", "TRUE")
                            // aren't silently treated as the literal string passthrough.
                            const lower = value.toLowerCase();
                            acc[key] = lower === 'true' ? true : lower === 'false' ? false : value;
                        }

                        return acc;
                    },
                    {} as Record<string, boolean | string>
                );
            })
        );
    }
}
