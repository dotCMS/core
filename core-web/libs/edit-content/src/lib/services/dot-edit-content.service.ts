import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotSiteService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentlet, DotContentletDepth } from '@dotcms/dotcms-models';

import { DotFolder } from '../fields/dot-edit-content-host-folder-field/models/tree-item.model';

@Injectable()
export class DotEditContentService {
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #siteService = inject(DotSiteService);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    readonly #http = inject(HttpClient);

    /**
     * Retrieves the content by its ID.
     *
     * @param {string} id - The ID of the content to retrieve.
     * @param {number} [languageId] - Optional language ID to filter the content.
     * @param {DotContentletDepth} [depth] - Optional depth to filter the content.
     * @returns {Observable<DotCMSContentlet>} An observable of the DotCMSContentlet object.
     */
    getContentById(params: {
        id: string;
        languageId?: number;
        depth?: DotContentletDepth;
    }): Observable<DotCMSContentlet> {
        const { id, languageId, depth } = params;
        let httpParams = new HttpParams();

        if (languageId) {
            httpParams = httpParams.set('language', languageId.toString());
        }

        if (depth) {
            httpParams = httpParams.set('depth', depth);
        }

        return this.#http
            .get(`/api/v1/content/${id}`, { params: httpParams })
            .pipe(pluck('entity'));
    }

    /**
     * Retrieves the content type by its ID or variable name.
     *
     * @param {string} idOrVar - The identifier or variable name of the content type to retrieve form data for.
     * @return {*}  {Observable<DotCMSContentType>}
     * @memberof DotEditContentService
     */
    getContentType(idOrVar: string): Observable<DotCMSContentType> {
        return this.#dotContentTypeService.getContentType(idOrVar);
    }

    /**
     * Retrieves tags based on the provided name.
     * @param name - The name of the tags to retrieve.
     * @returns An Observable that emits an array of tag labels.
     */
    getTags(name: string): Observable<string[]> {
        const params = new HttpParams().set('name', name);

        return this.#http.get<string[]>('/api/v2/tags', { params }).pipe(
            pluck('entity'),
            map((res) => Object.values(res).map((obj) => obj.label))
        );
    }
    /**
     * Saves a contentlet with the provided data.
     * @param data An object containing key-value pairs of data to be saved.
     * @returns An observable that emits the saved contentlet.
     * The type of the emitted contentlet is determined by the generic type parameter.
     */
    saveContentlet<T>(data: { [key: string]: string }): Observable<T> {
        return this.#dotWorkflowActionsFireService.saveContentlet(data);
    }

    /**
     *
     *
     * @param {string} path
     * @return {*}  {Observable<DotFolder[]>}
     * @memberof DotEditContentService
     */
    getFolders(path: string): Observable<DotFolder[]> {
        return this.#http.post<DotFolder>('/api/v1/folder/byPath', { path }).pipe(pluck('entity'));
    }

    /**
     * Get the number of reference pages for a contentlet
     * @param identifier - The identifier of the contentlet
     * @returns An observable that emits the number of reference pages
     */
    getReferencePages(identifier: string): Observable<number> {
        return this.#http
            .get<{ entity: { count: number } }>(`/api/v1/content/${identifier}/references/count`)
            .pipe(map((response) => response.entity.count));
    }

    /**
     * Get content by folder
     *
     * @param {{ folderId: string; mimeTypes?: string[] }} { folderId, mimeTypes }
     * @return {*}
     * @memberof DotEditContentService
     */
    getContentByFolder({ folderId, mimeTypes }: { folderId: string; mimeTypes?: string[] }) {
        const params = {
            hostFolderId: folderId,
            showLinks: false,
            showDotAssets: true,
            showPages: false,
            showFiles: true,
            showFolders: false,
            showWorking: true,
            showArchived: false,
            sortByDesc: true,
            mimeTypes: mimeTypes || []
        };

        return this.#siteService.getContentByFolder(params);
    }
}
