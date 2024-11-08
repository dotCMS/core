import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { Site } from '@dotcms/dotcms-js';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

export interface SiteParams {
    archived: boolean;
    live: boolean;
    system: boolean;
}

export interface ContentByFolderParams {
    hostFolderId: string;
    showLinks?: boolean;
    showDotAssets?: boolean;
    showArchived?: boolean;
    sortByDesc?: boolean;
    showPages?: boolean;
    showFiles?: boolean;
    showFolders?: boolean;
    showWorking?: boolean;
    extensions?: string[];
    mimeTypes?: string[];
}

export const BASE_SITE_URL = '/api/v1/site';

export const DEFAULT_PER_PAGE = 10;

export const DEFAULT_PAGE = 1;

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
    getSites(filter = '*', perPage?: number, page?: number): Observable<Site[]> {
        return this.#http
            .get<{ entity: Site[] }>(this.getSiteURL(filter, perPage, page))
            .pipe(pluck('entity'));
    }

    private getSiteURL(filter: string, perPage?: number, page?: number): string {
        const searchParams = new URLSearchParams({
            filter,
            per_page: `${perPage || DEFAULT_PER_PAGE}`,
            page: `${page || DEFAULT_PAGE}`,
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

    getContentByFolder(params: ContentByFolderParams) {
        return this.#http
            .post<{ entity: { list: DotCMSContentlet[] } }>('/api/v1/browser', params)
            .pipe(pluck('entity', 'list'));
    }
}
