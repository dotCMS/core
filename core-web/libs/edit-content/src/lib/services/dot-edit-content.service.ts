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

import {
    CustomTreeNode,
    DotFolder,
    TreeNodeItem
} from '../models/dot-edit-content-host-folder-field.interface';
import { createPaths } from '../utils/functions.util';

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
     * Get data for site/folder field and tranform into TreeNode
     *
     * @return {*}  {Observable<TreeNodeItem[]>}
     * @memberof DotEditContentService
     */
    getSitesTreePath(data: { filter: string; perPage: number }): Observable<TreeNodeItem[]> {
        const { filter, perPage } = data;

        return this.#siteService.getSites(filter, perPage).pipe(
            map((sites) => {
                return sites.map((site) => ({
                    key: site.hostname,
                    label: `//${site.hostname}`,
                    data: {
                        hostname: `//${site.hostname}`,
                        path: '',
                        type: 'site'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false
                }));
            })
        );
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
     *
     *
     * @param {string} hostName
     * @param {string} path
     * @return {*}  {Observable<TreeNodeItem[]>}
     * @memberof DotEditContentService
     */
    getFoldersTreeNode(hostName: string, path: string): Observable<TreeNodeItem[]> {
        return this.getFolders(`${hostName}${path}`).pipe(
            map((folders) => {
                return folders
                    .filter((folder) => {
                        const checkPath = path === '' ? '/' : path;

                        return folder.path !== checkPath;
                    })
                    .map((folder) => ({
                        key: `${folder.hostName}${folder.path}`.replace(/[/]/g, ''),
                        label: `//${folder.hostName}${folder.path}`,
                        data: {
                            hostname: `//${folder.hostName}`,
                            path: folder.path,
                            type: 'folder'
                        },
                        expandedIcon: 'pi pi-folder-open',
                        collapsedIcon: 'pi pi-folder',
                        leaf: false
                    }));
            })
        );
    }

    /**
     *
     *
     * @param {string} fullPath
     * @return {*}  {Observable<CustomTreeNode>}
     * @memberof DotEditContentService
     */
    buildTreeByPaths(fullPath: string): Observable<CustomTreeNode> {
        const paths = createPaths(fullPath);
        const requests = paths.reverse().map((path) => {
            const split = path.split('/');
            const [hostName] = split;
            const subPath = split.slice(1).join('/');

            return this.getFoldersTreeNode(`//${hostName}`, `/${subPath}`).pipe(
                map((folders) => ({ path: path.replace(/[/]/g, ''), folders }))
            );
        });

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

    /**
     *
     *
     * @return {*}  {Observable<TreeNodeItem>}
     * @memberof DotEditContentService
     */
    getCurrentSiteAsTreeNodeItem(): Observable<TreeNodeItem> {
        return this.#siteService.getCurrentSite().pipe(
            map((site) => ({
                key: site.hostname,
                label: `//${site.hostname}`,
                data: {
                    hostname: `//${site.hostname}`,
                    path: '',
                    type: 'site'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                leaf: false
            }))
        );
    }
}
