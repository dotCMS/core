import { forkJoin, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import { DotSiteService } from '@dotcms/data-access';
import { createPaths } from '@dotcms/edit-content/utils/functions.util';

import { CustomTreeNode, DotFolder, TreeNodeItem } from '../models/tree-item.model';

export const SYSTEM_HOST_NAME = 'System Host';

@Injectable({
    providedIn: 'root'
})
export class HostFieldService {
    readonly #siteService = inject(DotSiteService);
    readonly #http = inject(HttpClient);

    /**
     * Retrieves and transforms site data into TreeNode format for the site/folder field.
     * Optionally filters out the System Host based on the isRequired parameter.
     *
     * @param {Object} data - The parameters for fetching sites
     * @param {string} data.filter - Filter string to search sites
     * @param {number} [data.perPage] - Number of items per page
     * @param {number} [data.page] - Page number to fetch
     * @param {boolean} data.isRequired - If true, excludes System Host from results
     * @returns {Observable<TreeNodeItem[]>} Observable that emits an array of TreeNodeItems
     */
    getSites(data: {
        filter: string;
        perPage?: number;
        page?: number;
        isRequired: boolean;
    }): Observable<TreeNodeItem[]> {
        const { filter, perPage, page, isRequired } = data;

        return this.#siteService.getSites(filter, perPage, page).pipe(
            map((sites) => {
                if (isRequired) {
                    return sites.filter((site) => site.hostname !== SYSTEM_HOST_NAME);
                }

                return sites;
            }),
            map((sites) => {
                return sites.map((site) => ({
                    key: site.identifier,
                    label: site.hostname,
                    data: {
                        id: site.identifier,
                        hostname: site.hostname,
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
     * Retrieves the current site and transforms it into a TreeNodeItem format.
     * Useful for initializing the site/folder field with the current context.
     *
     * @returns {Observable<TreeNodeItem>} Observable that emits the current site as a TreeNodeItem
     */
    getCurrentSiteAsTreeNodeItem(): Observable<TreeNodeItem> {
        return this.#siteService.getCurrentSite().pipe(
            map((site) => ({
                key: site.identifier,
                label: site.hostname,
                data: {
                    id: site.identifier,
                    hostname: site.hostname,
                    path: '',
                    type: 'site'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                leaf: false
            }))
        );
    }

    /**
     * Fetches folders based on the provided path.
     *
     * @param {string} path - The path to fetch folders from
     * @returns {Observable<DotFolder[]>} Observable that emits an array of DotFolder objects
     */
    getFolders(path: string): Observable<DotFolder[]> {
        return this.#http.post<DotFolder>('/api/v1/folder/byPath', { path }).pipe(pluck('entity'));
    }

    /**
     * Retrieves folders and transforms them into a tree node structure.
     * The first folder in the response is considered the parent folder.
     *
     * @param {string} path - The path to fetch folders from
     * @returns {Observable<{ parent: DotFolder; folders: TreeNodeItem[] }>} Observable that emits an object containing the parent folder and child folders as TreeNodeItems
     */
    getFoldersTreeNode(path: string): Observable<{ parent: DotFolder; folders: TreeNodeItem[] }> {
        return this.getFolders(`//${path}`).pipe(
            map((folders) => {
                const parent = folders.shift();

                return {
                    parent,
                    folders: folders.map((folder) => ({
                        key: folder.id,
                        label: `${folder.hostName}${folder.path}`,
                        data: {
                            id: folder.id,
                            hostname: folder.hostName,
                            path: folder.path,
                            type: 'folder'
                        },
                        expandedIcon: 'pi pi-folder-open',
                        collapsedIcon: 'pi pi-folder',
                        leaf: false
                    }))
                };
            })
        );
    }

    /**
     * Builds a hierarchical tree structure based on the provided path.
     * Splits the path into segments and creates a nested tree structure
     * by making multiple API calls for each path segment.
     *
     * @param {string} path - The full path to build the tree from (e.g., 'hostname/folder1/folder2')
     * @returns {Observable<CustomTreeNode>} Observable that emits a CustomTreeNode containing the complete tree structure and the target node
     */
    buildTreeByPaths(path: string): Observable<CustomTreeNode> {
        const paths = createPaths(path);

        const requests = paths.reverse().map((path) => {
            const split = path.split('/');
            const [hostName] = split;
            const subPath = split.slice(1).join('/');

            const fullPath = `${hostName}/${subPath}`;

            return this.getFoldersTreeNode(fullPath);
        });

        return forkJoin(requests).pipe(
            map((response) => {
                const [mainNode] = response;

                return response.reduce(
                    (rta, node, index, array) => {
                        const next = array[index + 1];
                        if (next) {
                            const folder = next.folders.find((item) => item.key === node.parent.id);
                            if (folder) {
                                folder.children = node.folders;
                                if (mainNode.parent.id === folder.key) {
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
