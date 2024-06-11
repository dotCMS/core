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

@Injectable({
    providedIn: 'root'
})
export class DotSiteService {
    #BASE_SITE_URL = '/api/v1/site';
    #params: SiteParams = {
        archived: false,
        live: true,
        system: true
    };
    readonly #http = inject(HttpClient);

    readonly #defaultPerpage = 10;

    set searchParam(params: SiteParams) {
        this.#params = params;
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
        const paramPerPage = perPage || this.#defaultPerpage;
        const searchParam = `filter=${filter}&per_page=${paramPerPage}&${this.getQueryParams()}`;

        return `${this.#BASE_SITE_URL}?${searchParam}`;
    }

    private getQueryParams(): string {
        return Object.entries(this.#params)
            .map(([key, value]) => `${key}=${value}`)
            .join('&');
    }
}
