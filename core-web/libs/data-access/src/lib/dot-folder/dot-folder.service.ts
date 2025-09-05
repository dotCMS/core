import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { pluck } from 'rxjs/operators';

export interface DotFolder {
    id: string;
    hostName: string;
    path: string;
    addChildrenAllowed: boolean;
}

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
        // Normalize the path that the backend expects
        const folderPath = path.startsWith('//')
            ? path
            : `//${path.startsWith('/') ? path.slice(1) : path}`;
        return this.#http
            .post<{ entity: DotFolder[] }>(`/api/v1/folder/byPath`, { path: folderPath })
            .pipe(pluck('entity'));
    }
}
