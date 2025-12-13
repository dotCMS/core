import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import { Site } from '@dotcms/dotcms-js';
import { DotCMSContentlet, DotPagination, SiteEntity } from '@dotcms/dotcms-models';
import { hasValidValue } from '@dotcms/utils';

export interface SiteParams {
    archived: boolean;
    live: boolean;
    system: boolean;
}

export interface SitePaginationOptions {
    filter?: string;
    archive?: boolean;
    live?: boolean;
    system?: boolean;
    page?: number;
    per_page?: number;
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
     * Creates HttpParams for retrieving sites with optional parameters
     * Only includes parameters that have meaningful values (not empty, null, or undefined)
     */
    private getSitePaginationParams(options: SitePaginationOptions = {}): HttpParams {
        let params = new HttpParams();

        // Default parameters
        params = params.set('per_page', (options.per_page ?? DEFAULT_PER_PAGE).toString());
        params = params.set('page', (options.page ?? DEFAULT_PAGE).toString());

        // Add optional parameters if they have meaningful values
        if (hasValidValue(options.filter)) {
            params = params.set('filter', options.filter);
        } else {
            params = params.set('filter', '*');
        }

        if (options.archive !== undefined && options.archive !== null) {
            params = params.set('archive', options.archive.toString());
        } else {
            params = params.set('archive', this.#defaultParams.archived.toString());
        }

        if (options.live !== undefined && options.live !== null) {
            params = params.set('live', options.live.toString());
        } else {
            params = params.set('live', this.#defaultParams.live.toString());
        }

        if (options.system !== undefined && options.system !== null) {
            params = params.set('system', options.system.toString());
        } else {
            params = params.set('system', this.#defaultParams.system.toString());
        }

        return params;
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
            .get<{ entity: Site[] }>(BASE_SITE_URL, {
                params: this.getSitePaginationParams({ filter, per_page: perPage, page })
            })
            .pipe(pluck('entity'));
    }

    /**
     * Get sites from the endpoint with pagination
     *
     * @param options Optional parameters for filtering and pagination
     * @return {Observable<{sites: Site[]; pagination: DotPagination;}>}
     * Observable containing sites and pagination info
     * @memberof DotSiteService
     */
    getSitesWithPagination(options: SitePaginationOptions = {}): Observable<{
        sites: Site[];
        pagination: DotPagination;
    }> {
        return this.#http
            .get<{
                entity: Site[];
                pagination: DotPagination;
            }>(BASE_SITE_URL, { params: this.getSitePaginationParams(options) })
            .pipe(map((data) => ({ sites: data.entity, pagination: data.pagination })));
    }

    /**
     * Get current site
     *
     * @return {*}  {Observable<Site>}
     * @memberof DotSiteService
     */
    getCurrentSite(): Observable<SiteEntity> {
        return this.#http
            .get<{ entity: SiteEntity }>(`${BASE_SITE_URL}/currentSite`)
            .pipe(pluck('entity'));
    }

    /**
     * Retrieves contentlets from a specified folder.
     *
     * @param params - Parameters defining the folder and retrieval options.
     * @returns An observable emitting an array of `DotCMSContentlet` items.
     */
    getContentByFolder(params: ContentByFolderParams) {
        return this.#http
            .post<{ entity: { list: DotCMSContentlet[] } }>('/api/v1/browser', params)
            .pipe(pluck('entity', 'list'));
    }
}
