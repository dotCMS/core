import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

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
    private BASE_SITE_URL = '/api/v1/site';
    private params: SiteParams = {
        archived: false,
        live: true,
        system: true
    };

    private readonly defaultPerpage = 10;

    set searchParam(params: SiteParams) {
        this.params = params;
    }

    constructor(private http: HttpClient) {}

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
        return this.http
            .get<{ entity: Site[] }>(this.siteURL(filter, perPage))
            .pipe(pluck('entity'));
    }

    private siteURL(filter: string, perPage?: number): string {
        const paramPerPage = perPage || this.defaultPerpage;
        const searchParam = `filter=${filter}&perPage=${paramPerPage}&${this.getQueryParams()}`;

        return `${this.BASE_SITE_URL}?${searchParam}`;
    }

    private getQueryParams(): string {
        return Object.keys(this.params)
            .map((key: string) => `${key}=${this.params[key as keyof SiteParams]}`)
            .join('&');
    }
}
