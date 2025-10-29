import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentType,
    DotPagination
} from '@dotcms/dotcms-models';

import {
    DEFAULT_PER_PAGE,
    DotCMSAPIResponse,
    DotContentTypeQueryParams,
    DotPageContentTypeQueryParams
} from '../models';

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

    getAllContentTypes(params: DotContentTypeQueryParams): Observable<{
        contenttypes: DotCMSContentType[];
        pagination: DotPagination;
    }> {
        let httpParams = new HttpParams();

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
                        pagination: response.pagination
                    };
                })
            );
    }
}
