import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { Site } from '@dotcms/dotcms-js';

export interface SiteParams {
    archived: boolean;
    live: boolean;
    system: boolean;
}

export const BASE_SITE_URL = '/api/v1/site';

export const DEFAULT_PER_PAGE = 10;

@Injectable({
    providedIn: 'root'
})
export class DotSiteService {
    #defaultParams: SiteParams = {
        archived: false,
        live: true,
        system: true
    };
    readonly #http = inject(HttpClient);

    set searchParam(params: SiteParams) {
        this.#defaultParams = params;
    }

    /**
     * Get sites by filter
     * If no filter is provided, it will return all sites
     *
     * @param {string} [filter='*']
     * @param {number} [perPage]
     * @return {*}  {Observable<Site[]>}
     * @memberof DotSiteService
     */
    getSites(filter = '*', perPage?: number): Observable<Site[]> {
        return this.#http
            .get<{ entity: Site[] }>(this.getSiteURL(filter, perPage))
            .pipe(pluck('entity'));
    }

    private getSiteURL(filter: string, perPage?: number): string {
        const searchParams = new URLSearchParams({
            filter,
            per_page: `${perPage || DEFAULT_PER_PAGE}`,
            archived: `${this.#defaultParams.archived}`,
            live: `${this.#defaultParams.live}`,
            system: `${this.#defaultParams.system}`
        });

        return `${BASE_SITE_URL}?${searchParams.toString()}`;
    }

    /**
     * Get current site
     *
     * @return {*}  {Observable<Site>}
     * @memberof DotSiteService
     */
    getCurrentSite(): Observable<Site> {
        return this.#http
            .get<{ entity: Site }>(`${BASE_SITE_URL}/currentSite`)
            .pipe(pluck('entity'));
    }
}
