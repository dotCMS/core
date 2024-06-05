import { Observable, forkJoin } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotWorkflowActionsFireService,
    DotSiteService
} from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotFolder, TreeNodeItem } from '../models/dot-edit-content-host-folder-field.interface';

@Injectable()
export class DotEditContentService {
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #siteService = inject(DotSiteService);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    readonly #http = inject(HttpClient);

    /**
     * Retrieves the content by its ID.
     * @param id - The ID of the content to retrieve.
     * @returns An observable of the DotCMSContentType object.
     */
    getContentById(id: string): Observable<DotCMSContentlet> {
        return this.#http.get(`/api/v1/content/${id}`).pipe(pluck('entity'));
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
    getTags(name: string) {
        const params = new HttpParams().set('name', name);

        return this.#http.get('/api/v2/tags', { params }).pipe(
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
     * Get data for site/folder field and tranform into TreeNode
     * @returns files
     */
    getSitesTreePath(): Observable<TreeNodeItem[]> {
        return this.#siteService.getSites().pipe(
            map((sites) => {
                return sites.map((site) => ({
                    key: site.hostname,
                    label: `${site.hostname}`,
                    data: { ...site, path: site.hostname, type: 'site' },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false
                }));
            })
        );
    }

    getFolders(path: string): Observable<DotFolder[]> {
        return this.#http.post<DotFolder>('/api/v1/folder/byPath', { path }).pipe(pluck('entity'));
    }

    getFoldersTreeNode(fullPath: string): Observable<TreeNodeItem[]> {
        let path = fullPath.split('/').splice(1).join('/');
        path = path === '' ? '/' : `/${path}`;

        return this.getFolders(`//${fullPath}`).pipe(
            map((folders) => {
                return folders
                    .filter((folder) => folder.path !== path)
                    .map((folder) => ({
                        key: `${folder.hostName}${folder.path}`,
                        label: `${folder.hostName}${folder.path}`,
                        data: { ...folder, path: folder.path, type: 'folder' },
                        expandedIcon: 'pi pi-folder-open',
                        collapsedIcon: 'pi pi-folder',
                        leaf: false
                    }));
            })
        );
    }

    buildTreeByPaths(paths: string[]) {
        const requests = paths
            .reverse()
            .map((path) =>
                this.getFoldersTreeNode(path).pipe(map((folders) => ({ path, folders })))
            );

        return forkJoin(requests).pipe(
            map((response) => {
                const [mainNode] = response;

                return response.reduce(
                    (rta, node, index, array) => {
                        const next = array[index + 1];
                        if (next) {
                            const folder = next.folders.find((item) => item.key === node.path);
                            if (folder) {
                                folder.children = node.folders;
                                if (mainNode.path === folder.key) {
                                    rta.node = folder;
                                }
                            }
                        }

                        rta.tree = node;

                        return rta;
                    },
                    { tree: null, node: null }
                );
            })
        );
    }
}
