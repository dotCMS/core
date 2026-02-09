import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import {
    DotCMSAPIResponse,
    DotCMSBaseTypesContentTypes,
    DotCMSContentType,
    DotPagination
} from '@dotcms/dotcms-models';

const DEFAULT_PER_PAGE = 30;

/**
 * Base query parameters for fetching content types.
 * Used by the content type service for API requests.
 */
export interface DotContentTypeQueryParams {
    /** Site identifier for context-aware filtering */
    host?: string;
    /** Language ID for content type filtering (default: 1) */
    language?: number;
    /** Filter content types by name or description */
    filter?: string;
    /** Page number for pagination (default: 1) */
    page?: number;
    /** Items per page - max: 100 (default: 30) */
    per_page?: number;
    /** Sort field - "name" or "usage" (default: "usage") */
    orderby?: 'name' | 'usage';
    /** Sort direction - ASC or DESC (default: "ASC") */
    direction?: 'ASC' | 'DESC';
    /** Content type base types to filter by */
    types?: DotCMSBaseTypesContentTypes[];
}

/**
 * Extended query parameters for fetching page-specific content types.
 * Adds page context to the base content type parameters.
 * Used when filtering content types based on page context.
 */
export interface DotPageContentTypeQueryParams extends DotContentTypeQueryParams {
    /** The URL path or identifier of the page to filter content types */
    pagePathOrId: string;
}

/**
 * Service to manage page content types for the Universal Visual Editor palette.
 *
 * @export
 * @class DotPageContentTypeService
 */
@Injectable({
    providedIn: 'root'
})
export class DotPageContentTypeService {
    private http = inject(HttpClient);

    private readonly CONTENTTYPE_PAGE_API_URL = '/api/v1/contenttype/page';
    private readonly CONTENTTYPE_API_URL = '/api/v1/contenttype';

    /**
     * Get available content types for Universal Visual Editor palette by analyzing page structure.
     *
     * @param {DotPageContentTypeQueryParams} params - The query parameters for fetching content types.
     * @returns {Observable<DotPageContentTypeResponse>} An observable emitting the content types response with pagination.
     * @memberof DotPageContentTypeService
     */
    get(params: DotPageContentTypeQueryParams): Observable<{
        contenttypes: DotCMSContentType[];
        pagination: DotPagination;
    }> {
        let httpParams = new HttpParams()
            .set('pagePathOrId', params.pagePathOrId)
            .set('per_page', DEFAULT_PER_PAGE)
            .set('page', 1);

        // Add optional parameters if provided
        if (params.language) {
            httpParams = httpParams.set('language', params.language);
        }

        if (params.filter) {
            httpParams = httpParams.set('filter', params.filter);
        }

        if (params.page) {
            httpParams = httpParams.set('page', params.page.toString());
        }

        if (params.per_page) {
            httpParams = httpParams.set('per_page', params.per_page.toString());
        }

        if (params.orderby) {
            httpParams = httpParams.set('orderby', params.orderby);
        }

        if (params.direction) {
            httpParams = httpParams.set('direction', params.direction);
        }

        if (params.types && params.types.length > 0) {
            params.types.forEach((type: DotCMSBaseTypesContentTypes) => {
                httpParams = httpParams.append('type', type);
            });
        }

        if (params.host) {
            httpParams = httpParams.set('host', params.host);
        }

        return this.http
            .get<
                DotCMSAPIResponse<DotCMSContentType[]>
            >(this.CONTENTTYPE_PAGE_API_URL, { params: httpParams })
            .pipe(
                take(1),
                map(({ entity, pagination }) => {
                    return {
                        contenttypes: entity,
                        pagination: pagination as DotPagination
                    };
                })
            );
    }

    getAllContentTypes(params: DotContentTypeQueryParams): Observable<{
        contenttypes: DotCMSContentType[];
        pagination: DotPagination;
    }> {
        let httpParams = new HttpParams()
            .set('per_page', DEFAULT_PER_PAGE.toString())
            .set('page', 1);

        // Add optional parameters if provided
        if (params.language) {
            httpParams = httpParams.set('language', params.language);
        }

        if (params.filter) {
            httpParams = httpParams.set('filter', params.filter);
        }

        if (params.page) {
            httpParams = httpParams.set('page', params.page.toString());
        }

        if (params.per_page) {
            httpParams = httpParams.set('per_page', params.per_page.toString());
        }

        if (params.orderby) {
            httpParams = httpParams.set('orderby', params.orderby);
        }

        if (params.direction) {
            httpParams = httpParams.set('direction', params.direction);
        }

        if (params.types) {
            params.types.forEach((type: DotCMSBaseTypesContentTypes) => {
                httpParams = httpParams.append('type', type);
            });
        }

        return this.http
            .get<
                DotCMSAPIResponse<DotCMSContentType[]>
            >(this.CONTENTTYPE_API_URL, { params: httpParams })
            .pipe(
                take(1),
                map((response) => {
                    return {
                        contenttypes: response.entity,
                        pagination: response.pagination as DotPagination
                    };
                })
            );
    }
}
