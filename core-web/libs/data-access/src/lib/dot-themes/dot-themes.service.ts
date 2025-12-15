import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import { DotTheme, DotPagination } from '@dotcms/dotcms-models';
import { hasValidValue } from '@dotcms/utils';

const THEMES_API_URL = '/api/v1/themes';
const DEFAULT_PER_PAGE = 10;
const DEFAULT_PAGE = 1;
const HARDCODED_HOST_ID = '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d';

export interface DotThemeOptions {
    hostId?: string;
    page?: number;
    per_page?: number;
    direction?: 'ASC' | 'DESC';
    searchParam?: string;
}

/**
 * Provide util methods to get themes information.
 * @export
 * @class DotThemesService
 */
@Injectable({
    providedIn: 'root'
})
export class DotThemesService {
    private readonly http = inject(HttpClient);

    /**
     * Get Theme information based on the inode.
     *
     * @param string inode
     * @returns Observable<DotTheme>
     * @memberof DotThemesService
     */
    get(inode: string): Observable<DotTheme> {
        return this.http
            .get<{ entity: DotTheme }>(`${THEMES_API_URL}/id/${inode}`)
            .pipe(pluck('entity'));
    }

    /**
     * Get themes from the endpoint with pagination
     *
     * @param options Optional parameters for filtering and pagination
     * @return {Observable<{themes: DotTheme[]; pagination: DotPagination;}>}
     * Observable containing themes and pagination info
     * @memberof DotThemesService
     */
    getThemes(options: DotThemeOptions = {}): Observable<{
        themes: DotTheme[];
        pagination: DotPagination;
    }> {
        return this.http
            .get<{
                entity: DotTheme[];
                pagination: DotPagination;
            }>(THEMES_API_URL, { params: this.getThemePaginationParams(options) })
            .pipe(
                map((data) => ({
                    themes: data.entity,
                    pagination: data.pagination
                }))
            );
    }

    /**
     * Creates HttpParams for retrieving themes with optional parameters
     * Only includes parameters that have meaningful values (not empty, null, or undefined)
     */
    private getThemePaginationParams(options: DotThemeOptions = {}): HttpParams {
        let params = new HttpParams();

        // Default parameters
        params = params.set('hostId', options.hostId ?? HARDCODED_HOST_ID);
        params = params.set('per_page', (options.per_page ?? DEFAULT_PER_PAGE).toString());
        params = params.set('page', (options.page ?? DEFAULT_PAGE).toString());

        // Add optional parameters if they have meaningful values
        if (hasValidValue(options.direction)) {
            params = params.set('direction', options.direction);
        } else {
            params = params.set('direction', 'ASC');
        }

        if (hasValidValue(options.searchParam)) {
            params = params.set('searchParam', options.searchParam);
        }

        return params;
    }
}
