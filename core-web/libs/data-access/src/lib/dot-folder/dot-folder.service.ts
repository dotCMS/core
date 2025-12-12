import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotFolder, DotFolderEntity } from '@dotcms/dotcms-models';
@Injectable()
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
            .post<{ entity: DotFolder[] }>(`/api/v1/folder/byPath`, { path: folderPath })
            .pipe(map((response: { entity: DotFolder[] }) => response.entity));
    }

    /**
     * Creates a new folder in the assets system
     *
     * @param {DotFolderEntity} body - The folder data to create
     * @returns {Observable<DotFolder>} Observable that emits the created folder
     */
    createFolder(body: DotFolderEntity): Observable<DotFolder> {
        return this.#http
            .post<{ entity: DotFolder }>(`/api/v1/assets/folders`, body)
            .pipe(map((response: { entity: DotFolder }) => response.entity));
    }

    /**
     * Saves a folder in the assets system
     *
     * @param {DotFolderEntity} body - The folder data to save
     * @returns {Observable<DotFolder>} Observable that emits the saved folder
     */
    saveFolder(body: DotFolderEntity): Observable<DotFolder> {
        return this.#http
            .put<{ entity: DotFolder }>(`/api/v1/assets/folders`, body)
            .pipe(map((response: { entity: DotFolder }) => response.entity));
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
