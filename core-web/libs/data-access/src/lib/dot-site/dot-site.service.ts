import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import { DotCMSContentlet, DotPagination, DotSite } from '@dotcms/dotcms-models';
import { hasValidValue } from '@dotcms/utils';

/**
 * Base interface for site entity fields returned by both list and detail endpoints.
 * The only difference between these endpoint responses is the field 'name' vs 'siteName' for the site display name.
 */
interface SiteBase {
    identifier: string;
    aliases: string | null;
    archived: boolean;
    // The display name property varies between endpoints and is overridden in extending interfaces.
}

/**
 * Minimal interface for site entity from list endpoints (/api/v1/site?per_page=...).
 * The list endpoint returns "name" as the display name property.
 *
 * Note: This interface exists only due to the inconsistency in the DotCMS API, where the list endpoint uses "name"
 * while the single-site endpoint uses "siteName".
 */
interface SiteEntity extends SiteBase {
    name: string;
}

/**
 * Minimal interface for site entity from single site endpoint (/api/v1/site/{siteId}).
 * The detail endpoint returns "siteName" as the display name property.
 *
 * Note: This interface exists only due to the inconsistency in the DotCMS API, where the single-site endpoint uses "siteName"
 * while the list endpoint uses "name".
 */
interface SiteDetailEntity extends SiteBase {
    siteName: string;
}

export interface SiteParams {
    archived: boolean;
    live: boolean;
    system: boolean;
}

export interface DotSiteOptions {
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
     * Get sites from the endpoint with pagination
     *
     * @param options Optional parameters for filtering and pagination
     * @return {Observable<{sites: DotSite[]; pagination: DotPagination;}>}
     * Observable containing sites and pagination info
     * @memberof DotSiteService
     */
    getSites(options: DotSiteOptions = {}): Observable<{
        sites: DotSite[];
        pagination: DotPagination;
    }> {
        return this.#http
            .get<{
                entity: SiteEntity[];
                pagination: DotPagination;
            }>(BASE_SITE_URL, { params: this.getSitePaginationParams(options) })
            .pipe(
                map((data) => ({
                    sites: data.entity.map((site) => this.normalizeSiteEntity(site)),
                    pagination: data.pagination
                }))
            );
    }

    /**
     * Get current site
     *
     * @return {*}  {Observable<Site>}
     * @memberof DotSiteService
     */
    getCurrentSite(): Observable<DotSite> {
        return this.#http.get<{ entity: SiteEntity }>(`${BASE_SITE_URL}/currentSite`).pipe(
            pluck('entity'),
            map((site) => this.normalizeSiteEntity(site))
        );
    }

    /**
     * Get site by identifier
     *
     * @param {string} siteId The site identifier
     * @return {*}  {Observable<Site>}
     * @memberof DotSiteService
     */
    getSiteById(siteId: string): Observable<DotSite> {
        return this.#http.get<{ entity: SiteDetailEntity }>(`${BASE_SITE_URL}/${siteId}`).pipe(
            pluck('entity'),
            map((site) => this.normalizeSiteDetailEntity(site))
        );
    }

    // TODO: This method doesn't belong in the site service. Consider moving it to a more appropriate location.
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

    /**
     * Switches the current working site to the provided site.
     *
     * If a specific site is provided, this method will perform a site switch to that site by its identifier.
     * If `null` is provided, it will switch to the default site.
     *
     * @param site The site to switch to. If null, defaults to switching to the default site.
     * @returns An observable emitting the new current site as a `DotSite` object.
     */
    switchSite(identifier: string | null): Observable<DotSite> {
        const url = identifier
            ? `${BASE_SITE_URL}/switch/${identifier}`
            : `${BASE_SITE_URL}/switch`;
        return this.#http.put<{ entity: DotSite }>(url, null).pipe(pluck('entity'));
    }

    /**
     * Creates HttpParams for retrieving sites with optional parameters
     * Only includes parameters that have meaningful values (not empty, null, or undefined)
     */
    private getSitePaginationParams(options: DotSiteOptions = {}): HttpParams {
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
     * Normalizes a SiteEntity to the Site type
     */
    private normalizeSiteEntity(site: SiteEntity): DotSite {
        return {
            identifier: site.identifier,
            hostname: site.name,
            aliases: site.aliases,
            archived: site.archived
        };
    }

    /**
     * Normalizes a SiteDetailEntity to the Site type
     */
    private normalizeSiteDetailEntity(site: SiteDetailEntity): DotSite {
        return {
            identifier: site.identifier,
            hostname: site.siteName,
            aliases: site.aliases,
            archived: site.archived
        };
    }
}
