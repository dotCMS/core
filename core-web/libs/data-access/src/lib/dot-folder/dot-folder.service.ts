import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotFolder,
    DotFolderEntity,
    DotCMSAPIResponse,
    FolderSearchParams,
    FolderSearchView,
    DotPagination
} from '@dotcms/dotcms-models';
import { hasValidValue } from '@dotcms/utils';

export const FOLDER_SEARCH_URL = '/api/v1/folder/search';
export const DEFAULT_FOLDER_SEARCH_PAGE = 1;
export const DEFAULT_FOLDER_SEARCH_PER_PAGE = 40;

@Injectable({
    providedIn: 'root'
})
export class DotFolderService {
    readonly #http = inject(HttpClient);

    /**
     * Get folders by path
     *
     * @param {string} path - The path to get folders from
     * @returns {Observable<DotFolder[]>} Observable that emits an array of folders
     */
    getFolders(path: string): Observable<DotFolder[]> {
        const folderPath = this.normalizePath(path);

        return this.#http
            .post<DotCMSAPIResponse<DotFolder[]>>(`/api/v1/folder/byPath`, { path: folderPath })
            .pipe(map((response) => response.entity));
    }

    /**
     * Creates a new folder in the assets system
     *
     * @param {DotFolderEntity} body - The folder data to create
     * @returns {Observable<DotFolder>} Observable that emits the created folder
     */
    createFolder(body: DotFolderEntity): Observable<DotFolder> {
        return this.#http
            .post<DotCMSAPIResponse<DotFolder>>(`/api/v1/assets/folders`, body)
            .pipe(map((response) => response.entity));
    }

    /**
     * Saves a folder in the assets system
     *
     * @param {DotFolderEntity} body - The folder data to save
     * @returns {Observable<DotFolder>} Observable that emits the saved folder
     */
    saveFolder(body: DotFolderEntity): Observable<DotFolder> {
        return this.#http
            .put<DotCMSAPIResponse<DotFolder>>(`/api/v1/assets/folders`, body)
            .pipe(map((response) => response.entity));
    }

    /**
     * Searches folders within a site using the unified, paginated search endpoint.
     * Replaces `getFolders`/`byPath` for the interactive Site/Folder selector, where
     * real server-side pagination and name filtering are required.
     *
     * @param {FolderSearchParams} params - Search scope (site, path, recursive), filter, sort and pagination
     * @returns {Observable<{ folders: FolderSearchView[]; pagination: DotPagination }>} Observable that emits the matching folders and pagination metadata
     */
    searchFolders(
        params: FolderSearchParams
    ): Observable<{ folders: FolderSearchView[]; pagination: DotPagination }> {
        let httpParams = new HttpParams().set('siteId', params.siteId);

        if (hasValidValue(params.path)) {
            httpParams = httpParams.set('path', params.path);
        }

        if (params.recursive !== undefined && params.recursive !== null) {
            httpParams = httpParams.set('recursive', String(params.recursive));
        }

        if (hasValidValue(params.name)) {
            httpParams = httpParams.set('name', params.name);
        }

        if (hasValidValue(params.orderby)) {
            httpParams = httpParams.set('orderby', params.orderby);
        }

        if (hasValidValue(params.direction)) {
            httpParams = httpParams.set('direction', params.direction);
        }

        httpParams = httpParams.set('page', String(params.page ?? DEFAULT_FOLDER_SEARCH_PAGE));
        httpParams = httpParams.set(
            'per_page',
            String(params.per_page ?? DEFAULT_FOLDER_SEARCH_PER_PAGE)
        );

        return this.#http
            .get<{ entity: FolderSearchView[]; pagination: DotPagination }>(FOLDER_SEARCH_URL, {
                params: httpParams
            })
            .pipe(
                map((response) => ({ folders: response.entity, pagination: response.pagination }))
            );
    }

    /**
     * Normalize the path that the backend expects
     * The backend expects a path that starts with //
     *
     * @param {string} path - The path to normalize
     * @returns {string} The normalized path
     */
    private normalizePath(path: string): string {
        if (path.startsWith('//')) {
            return path;
        }

        return `//${path.startsWith('/') ? path.slice(1) : path}`;
    }
}
