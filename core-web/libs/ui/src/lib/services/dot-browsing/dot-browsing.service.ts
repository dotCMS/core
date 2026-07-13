import { Observable, forkJoin } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { filter, map } from 'rxjs/operators';

import {
    DEFAULT_FOLDER_SEARCH_PER_PAGE,
    DotFolderService,
    DotSiteService
} from '@dotcms/data-access';
import {
    ContentByFolderParams,
    CustomTreeNode,
    DotFolder,
    DotPagination,
    FolderSearchParams,
    FolderSearchView,
    TreeNodeItem
} from '@dotcms/dotcms-models';

/**
 * Normalizes a persisted host/folder browse path into the plain `hostname/path/` format
 * expected by `buildTreeByPaths`, regardless of how it was serialized when saved.
 *
 * Handles the known persisted formats:
 * - `//hostname/path/` (leading double slash)
 * - `hostname:/path/` (colon-separated, produced by the host/folder field's `pathToSave`)
 *
 * @param {string} path - The raw path value to normalize
 * @returns {string} The normalized `hostname/path/` path
 *
 * @usageNotes
 *
 * ### Example
 *
 * ```ts
 * normalizeHostFolderBrowsePath('//demo.com/level1/'); // 'demo.com/level1/'
 * normalizeHostFolderBrowsePath('demo.com:/level1/'); // 'demo.com/level1/'
 * ```
 */
export function normalizeHostFolderBrowsePath(path: string): string {
    const withoutLeadingSlashes = path.replace(/^\/+/, '');
    const colonIndex = withoutLeadingSlashes.indexOf(':');

    if (colonIndex === -1) {
        return withoutLeadingSlashes;
    }

    const hostname = withoutLeadingSlashes.slice(0, colonIndex);
    const folderPath = withoutLeadingSlashes.slice(colonIndex + 1);

    return `${hostname}${folderPath}`;
}

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
                    folders: folders.map((folder) => this.#createFolderTreeNode(folder))
                };
            })
        );
    }

    /**
     * Creates a TreeNodeItem from a DotFolder
     *
     * @param {DotFolder} folder - The folder to create a TreeNodeItem from
     * @returns {TreeNodeItem} The TreeNodeItem created from the DotFolder
     */
    #createFolderTreeNode(folder: DotFolder): TreeNodeItem {
        return {
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
        };
    }

    /**
     * Searches folders within a site using the new paginated `GET /api/v1/folder/search`
     * endpoint and transforms them into TreeNode format. Used by the Site/Folder selector
     * overlay for lazy-loading a level, paginating ("Load 40 more"), and site-scoped search.
     *
     * The search endpoint does not return the site's hostname, so it must be supplied by
     * the caller (the selected site is already known when browsing/searching).
     *
     * @param {FolderSearchParams} params - Search scope (site, path, recursive), filter, sort and pagination
     * @param {string} hostname - Hostname of the site being searched, used to build each folder's full path/label
     * @returns {Observable<{ folders: TreeNodeItem[]; pagination: DotPagination }>} Observable that emits matching folders as TreeNodeItems and pagination metadata
     */
    searchFolders(
        params: FolderSearchParams,
        hostname: string
    ): Observable<{ folders: TreeNodeItem[]; pagination: DotPagination }> {
        return this.#folderService.searchFolders(params).pipe(
            map(({ folders, pagination }) => ({
                folders: folders.map((folder) =>
                    this.#createFolderSearchTreeNode(folder, hostname, params.recursive === true)
                ),
                pagination
            }))
        );
    }

    /**
     * Creates a TreeNodeItem from a FolderSearchView, using the caller-supplied hostname
     * (not present in the search response) to build the full folder path and label.
     *
     * @param {FolderSearchView} folder - The folder search result to create a TreeNodeItem from
     * @param {string} hostname - Hostname of the site the folder belongs to
     * @param {boolean} flat - When true (recursive name search), nodes are always leaves and cannot expand
     * @returns {TreeNodeItem} The TreeNodeItem created from the FolderSearchView
     */
    #createFolderSearchTreeNode(
        folder: FolderSearchView,
        hostname: string,
        flat = false
    ): TreeNodeItem {
        const parentPath = folder.path.endsWith('/') ? folder.path : `${folder.path}/`;
        const path = `${parentPath}${folder.name}/`;

        return {
            key: folder.id,
            label: `${hostname}${path}`,
            data: {
                id: folder.id,
                hostname,
                path,
                type: 'folder'
            },
            expandedIcon: 'pi pi-folder-open',
            collapsedIcon: 'pi pi-folder',
            leaf: flat ? true : !folder.hasChildren
        };
    }

    /**
     * Builds a hierarchical tree structure for a preselected folder path using the
     * paginated `/folder/search` endpoint so each node gets a correct `leaf` from
     * `hasChildren`. Fetches one level per ancestor (not the target's own children —
     * those load lazily on expand).
     *
     * @param {string} siteId - Site identifier for folder search scoping
     * @param {string} hostname - Hostname used to build folder labels/paths
     * @param {string} folderPath - Folder path within the site (e.g. `/level1/level2/`)
     * @returns {Observable<CustomTreeNode>} Observable that emits the resolved tree and target node
     */
    buildTreeByPaths(
        siteId: string,
        hostname: string,
        folderPath: string
    ): Observable<CustomTreeNode> {
        const segments = folderPath.split('/').filter(Boolean);
        const parentPaths = this.#buildParentPaths(segments);

        const requests = parentPaths.map((path) =>
            this.searchFolders(
                {
                    siteId,
                    path,
                    recursive: false,
                    page: 1,
                    per_page: DEFAULT_FOLDER_SEARCH_PER_PAGE
                },
                hostname
            )
        );

        return forkJoin(requests).pipe(
            map((results) => {
                const rootChildren = results[0]?.folders ?? [];
                const targetPaths = segments.map(
                    (_, index) => `/${segments.slice(0, index + 1).join('/')}/`
                );

                let targetNode: TreeNodeItem | null = null;
                let currentLevel = rootChildren;

                for (let index = 0; index < segments.length; index++) {
                    const folderNode = currentLevel.find(
                        (item) => item.data?.path === targetPaths[index]
                    );

                    if (!folderNode) {
                        break;
                    }

                    targetNode = folderNode;

                    const nextResult = results[index + 1];
                    if (nextResult) {
                        folderNode.children = nextResult.folders;
                        folderNode.expanded = true;
                        currentLevel = nextResult.folders;
                    }
                }

                return {
                    node: targetNode,
                    tree: {
                        path: '/',
                        folders: rootChildren
                    }
                };
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
     * Builds the parent paths needed to resolve each segment of a folder path.
     * For `['level1','level2']` returns `['/', '/level1/']` — root plus each ancestor,
     * excluding the target's own children listing.
     */
    #buildParentPaths(segments: string[]): string[] {
        if (segments.length === 0) {
            return ['/'];
        }

        const parentPaths = ['/'];

        for (let index = 0; index < segments.length - 1; index++) {
            parentPaths.push(`/${segments.slice(0, index + 1).join('/')}/`);
        }

        return parentPaths;
    }
}
