import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import { DotCMSBaseTypesContentTypes, DotCMSContentType } from '@dotcms/dotcms-models';

/**
 * Query parameters for fetching page content types.
 *
 * @export
 * @interface DotPageContentTypeParams
 */
export interface DotPageContentTypeParams {
    /** The URL of the page to filter content types for the palette */
    pagePathOrId: string;
    /** Language ID for content type analysis (default: "-1") */
    language?: string;
    /** Filter content types by name or description */
    filter?: string;
    /** Page number for pagination (default: 1) */
    page?: number;
    /** Items per page - max: 100 (default: 20) */
    per_page?: number;
    /** Sort field - "name", "usage", "modified" (default: "usage") */
    orderby?: 'name' | 'usage';
    /** Sort direction - ASC|DESC (default: "ASC") */
    direction?: 'ASC' | 'DESC';
    types?: DotCMSBaseTypesContentTypes[];
}

/**
 * TOD: Move this to app.models.ts
 * Generic response structure for dotCMS API endpoints.
 *
 * @export
 * @interface DotCMSAPIResponse
 * @template T
 */
export interface DotCMSAPIResponse<T = unknown> {
    entity: T;
    errors: string[];
    messages: string[];
    permissions: string[];
    i18nMessagesMap: { [key: string]: string };
    pagination?: DotPagination;
}

/**
 * Pagination information for the response.
 *
 * @export
 * @interface DotPageContentTypePagination
 */
export interface DotPagination {
    /** Current page number */
    currentPage: number;
    /** Items per page */
    perPage: number;
    /** Total number of entries */
    totalEntries: number;
}

/**
 * Service to manage page content types for the Universal Visual Editor palette.
 *
 * @export
 * @class DotPageContentTypeService
 */
@Injectable()
export class DotPageContentTypeService {
    private http = inject(HttpClient);

    private readonly CONTENTTYPE_PAGE_API_URL = '/api/v1/contenttype/page';

    /**
     * Get available content types for Universal Visual Editor palette by analyzing page structure.
     *
     * @param {DotPageContentTypeParams} params - The query parameters for fetching content types.
     * @returns {Observable<DotPageContentTypeResponse>} An observable emitting the content types response with pagination.
     * @memberof DotPageContentTypeService
     */
    get(params: DotPageContentTypeParams): Observable<{
        contenttypes: DotCMSContentType[];
        pagination: DotPagination;
    }> {
        let httpParams = new HttpParams().set('pagePathOrId', params.pagePathOrId);

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
            >(this.CONTENTTYPE_PAGE_API_URL, { params: httpParams })
            .pipe(
                take(1),
                map(({ entity, pagination }) => {
                    return {
                        contenttypes: entity,
                        pagination: pagination
                    };
                })
            );
    }
}
