import { Observable, forkJoin } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { filter, map } from 'rxjs/operators';

import { DotSiteService, DotFolderService } from '@dotcms/data-access';
import {
    DotFolder,
    TreeNodeItem,
    CustomTreeNode,
    ContentByFolderParams
} from '@dotcms/dotcms-models';

/**
 * Provide util methods to get Tags available in the system.
 * @export
 * @class DotBrowsingService
 */
@Injectable({
    providedIn: 'root'
})
export class DotBrowsingService {
    readonly #siteService = inject(DotSiteService);
    readonly #folderService = inject(DotFolderService);
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
    getSitesTreePath(data: {
        filter: string;
        perPage?: number;
        page?: number;
    }): Observable<TreeNodeItem[]> {
        const { filter, perPage, page } = data;

        return this.#siteService.getSites({ filter, per_page: perPage, page }).pipe(
            map((data) => {
                return data.sites.map((site) => ({
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
     *
     *
     * @param {string} path
     * @return {*}  {Observable<DotFolder[]>}
     * @memberof DotEditContentService
     */
    getFolders(path: string): Observable<DotFolder[]> {
        return this.#folderService.getFolders(path);
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
            filter((folders) => folders.length > 0),
            map((folders) => {
                const parent = folders.shift() as DotFolder;

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
        const paths = this.#createPaths(path);

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

                        rta.tree = { path: node.parent.path, folders: node.folders };

                        return rta;
                    },
                    { tree: null, node: null } as CustomTreeNode
                );
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
     * Get content by folder
     *
     * @param {Object} options - The parameters for fetching content by folder
     * @param {string} options.folderId - The folder ID
     * @return {*}
     * @memberof DotEditContentService
     */
    getContentByFolder(params: ContentByFolderParams) {
        return this.#siteService.getContentByFolder(params);
    }

    /**
     * Converts a JSON string into a JavaScript object.
     * Create all paths based in a Path
     *
     * @param {string} path - the path
     * @return {string[]} - An array with all posibles paths
     *
     * @usageNotes
     *
     * ### Example
     *
     * ```ts
     * const path = 'demo.com/level1/level2';
     * const paths = createPaths(path);
     * console.log(paths); // ['demo.com/', 'demo.com/level1/', 'demo.com/level1/level2/']
     * ```
     */
    #createPaths(path: string): string[] {
        const split = path.split('/').filter((item) => item !== '');

        return split.reduce((array, item, index) => {
            const prev = array[index - 1];
            let path = `${item}/`;
            if (prev) {
                path = `${prev}${path}`;
            }

            array.push(path);

            return array;
        }, [] as string[]);
    }
}
