import { EMPTY, Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { expand, filter, map, reduce, switchMap } from 'rxjs/operators';

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
    TreeLevelPagination,
    TreeNodeItem
} from '@dotcms/dotcms-models';

/**
 * Pagination key for the site-root folder level. Shared with the host/folder field
 * store so `loadFolders` / `loadMore` and `buildTreeByPaths` use the same map key.
 */
export const TREE_ROOT_NODE_KEY = 'root';
export const SITE_PAGE_LIMIT = 40;

type LevelLoadResult = {
    folders: TreeNodeItem[];
    page: number;
    hasMore: boolean;
};

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
        return this.getSitesPage(data).pipe(map(({ sites }) => sites));
    }

    /**
     * Retrieves a paginated page of sites as TreeNodeItems plus pagination metadata.
     *
     * @param {Object} data - The parameters for fetching sites
     * @param {string} data.filter - Filter string to search sites
     * @param {number} [data.perPage] - Number of items per page
     * @param {number} [data.page] - Page number to fetch
     * @returns {Observable<{ sites: TreeNodeItem[]; pagination: DotPagination }>}
     */
    getSitesPage(data: {
        filter: string;
        perPage?: number;
        page?: number;
    }): Observable<{ sites: TreeNodeItem[]; pagination: DotPagination }> {
        const { filter, perPage, page } = data;

        return this.#siteService.getSites({ filter, per_page: perPage, page }).pipe(
            map(({ sites, pagination }) => ({
                sites: sites.map((site) => this.#mapSiteToTreeNodeItem(site)),
                pagination
            }))
        );
    }

    /**
     * Resolves a single site by exact hostname match using the paginated sites API.
     * Returns null when no exact match is found.
     *
     * @param {string} hostname - Site hostname to resolve
     * @param {number} [perPage] - Page size for the lookup request
     * @returns {Observable<TreeNodeItem | null>}
     */
    resolveSiteByHostname(
        hostname: string,
        perPage: number = SITE_PAGE_LIMIT
    ): Observable<TreeNodeItem | null> {
        return this.getSitesPage({ filter: hostname, perPage, page: 1 }).pipe(
            map(
                ({ sites }) =>
                    sites.find(
                        (site) => site.label === hostname || site.data?.hostname === hostname
                    ) ?? null
            )
        );
    }

    #mapSiteToTreeNodeItem(site: { identifier: string; hostname: string }): TreeNodeItem {
        return {
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
        };
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
     * When the target segment is not on page 1 of a level, successive pages are
     * fetched until it appears (or pages are exhausted). Per-level pagination
     * metadata is returned so callers can seed "Load more" state correctly.
     *
     * @param {string} siteId - Site identifier for folder search scoping
     * @param {string} hostname - Hostname used to build folder labels/paths
     * @param {string} folderPath - Folder path within the site (e.g. `/level1/level2/`)
     * @returns {Observable<CustomTreeNode>} Observable that emits the resolved tree, target node, and pagination
     */
    buildTreeByPaths(
        siteId: string,
        hostname: string,
        folderPath: string
    ): Observable<CustomTreeNode> {
        const segments = folderPath.split('/').filter(Boolean);
        const targetPaths = segments.map(
            (_, index) => `/${segments.slice(0, index + 1).join('/')}/`
        );
        const pagination: Record<string, TreeLevelPagination> = {};

        const resolveLevel = (
            segmentIndex: number,
            parentPath: string,
            levelKey: string,
            ancestorNode: TreeNodeItem | null
        ): Observable<{
            node: TreeNodeItem | null;
            folders: TreeNodeItem[];
        }> => {
            const targetPath = targetPaths[segmentIndex];

            return this.#loadLevelUntilTarget(siteId, hostname, parentPath, targetPath).pipe(
                switchMap(({ folders, page, hasMore }) => {
                    pagination[levelKey] = { page, hasMore };

                    const folderNode = folders.find((item) => item.data?.path === targetPath);

                    if (!folderNode?.data) {
                        return of({
                            node: ancestorNode,
                            folders
                        });
                    }

                    const isLastSegment = segmentIndex >= segments.length - 1;

                    if (isLastSegment) {
                        return of({
                            node: folderNode,
                            folders
                        });
                    }

                    return resolveLevel(
                        segmentIndex + 1,
                        targetPath,
                        folderNode.key ?? folderNode.data.id,
                        folderNode
                    ).pipe(
                        map(({ node, folders: childFolders }) => {
                            folderNode.children = childFolders;
                            folderNode.expanded = true;

                            return {
                                node,
                                folders
                            };
                        })
                    );
                })
            );
        };

        if (segments.length === 0) {
            return this.#loadLevelUntilTarget(siteId, hostname, '/', null).pipe(
                map(({ folders, page, hasMore }) => ({
                    node: null,
                    tree: {
                        path: '/',
                        folders
                    },
                    pagination: {
                        [TREE_ROOT_NODE_KEY]: { page, hasMore }
                    }
                }))
            );
        }

        return resolveLevel(0, '/', TREE_ROOT_NODE_KEY, null).pipe(
            map(({ node, folders }) => ({
                node,
                tree: {
                    path: '/',
                    folders
                },
                pagination
            }))
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
     * Loads a single folder level, fetching successive pages until `targetPath` is
     * present among the accumulated children (or no more pages remain). When
     * `targetPath` is null, only page 1 is fetched.
     */
    #loadLevelUntilTarget(
        siteId: string,
        hostname: string,
        path: string,
        targetPath: string | null
    ): Observable<LevelLoadResult> {
        const fetchPage = (page: number) =>
            this.searchFolders(
                {
                    siteId,
                    path,
                    recursive: false,
                    page,
                    per_page: DEFAULT_FOLDER_SEARCH_PER_PAGE
                },
                hostname
            ).pipe(
                map(({ folders, pagination }) => ({
                    folders,
                    page,
                    hasMore: pagination.currentPage * pagination.perPage < pagination.totalEntries
                }))
            );

        return fetchPage(1).pipe(
            expand((result) => {
                if (
                    !targetPath ||
                    !result.hasMore ||
                    result.folders.some((folder) => folder.data?.path === targetPath)
                ) {
                    return EMPTY;
                }

                return fetchPage(result.page + 1);
            }),
            reduce(
                (acc: LevelLoadResult, result: LevelLoadResult) => ({
                    folders: [...acc.folders, ...result.folders],
                    page: result.page,
                    hasMore: result.hasMore
                }),
                { folders: [], page: 1, hasMore: false }
            )
        );
    }
}
