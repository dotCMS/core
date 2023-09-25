import { Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck, take } from 'rxjs/operators';

import { FeaturedFlags } from '@dotcms/dotcms-models';

@Injectable({
    providedIn: 'root'
})
export class DotPropertiesService {
    constructor(private readonly http: HttpClient) {}
    featureConfig: Record<string, string> | null = null;
    /**
     * Get the value of specific key
     * from the dotmarketing-config.properties
     *
     * @param string key
     * @returns {Observable<string>}
     * @memberof DotPropertiesService
     */
    getKey(key: string): Observable<string> {
        if (this.featureConfig?.[key]) return of(this.featureConfig[key]);

        return this.http
            .get('/api/v1/configuration/config', { params: { keys: key } })
            .pipe(take(1), pluck('entity', key));
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
        if (this.featureConfig) {
            const missingKeys = keys.filter((key) => !(key in (this.featureConfig || {})));
            if (missingKeys.length === 0) {
                return of(this.featureConfig);
            }
        }

        return this.http
            .get('/api/v1/configuration/config', { params: { keys: keys.join() } })
            .pipe(take(1), pluck('entity'));
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
            .pipe(take(1), pluck('entity', key));
    }

    loadConfig() {
        this.getKeys([
            FeaturedFlags.LOAD_FRONTEND_EXPERIMENTS,
            FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE,
            FeaturedFlags.FEATURE_FLAG_TEMPLATE_BUILDER,
            FeaturedFlags.FEATURE_FLAG_SEO_IMPROVEMENTS,
            FeaturedFlags.FEATURE_FLAG_SEO_PAGE_TOOLS,
            FeaturedFlags.FEATURE_FLAG_EDIT_URL_CONTENT_MAP
        ]).subscribe({
            next: (res) => {
                this.featureConfig = res;
            }
        });
    }
}
