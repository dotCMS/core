import { forkJoin, Observable } from 'rxjs';

import { map } from 'rxjs/operators';

import {
    createLoadMoreTreeNode,
    DotFolder,
    DotSite,
    DOT_FOLDER_TREE_PAGE_SIZE,
    FolderSearchView,
    LOAD_MORE_NODE_TYPE,
    TreeNodeItem
} from '@dotcms/dotcms-models';

import { DotFolderService } from './dot-folder.service';
import { createTreeNode, generateAllParentPaths } from './folder-tree.utils';

/**
 * Page size for interactive folder-tree expand and load-more.
 * Re-exports the shared limit used by Host Folder Field so both stay in sync.
 */
export const FOLDER_TREE_PAGE_SIZE = DOT_FOLDER_TREE_PAGE_SIZE;

/**
 * Page size for the deep-link / initial hierarchy fetch only.
 * One request per ancestor level (parallel); large enough that path segments past
 * the interactive page of 40 still appear so `buildTreeFolderNodes` can select them.
 * Expand and load-more keep using {@link FOLDER_TREE_PAGE_SIZE}.
 */
export const FOLDER_TREE_HIERARCHY_PAGE_SIZE = 10000;

/**
 * Adapts a folder search API view into a {@link DotFolder}.
 *
 * @param {FolderSearchView} view - The folder search result item
 * @param {string} hostName - Hostname of the site being browsed
 * @returns {DotFolder} The adapted folder
 */
export function folderSearchViewToDotFolder(view: FolderSearchView, hostName: string): DotFolder {
    // Normalize the parent path to a trailing slash before composing the folder's own path, so the
    // result is always `.../<name>/`. `buildTreeFolderNodes` compares this against
    // `generateAllParentPaths` (always trailing-slashed); a missing slash would break target-path
    // matching. Mirrors the guard in dot-browsing.service.ts.
    const parentPath = view.path.endsWith('/') ? view.path : `${view.path}/`;

    return {
        id: view.id,
        inode: view.inode,
        hostName,
        path: `${parentPath}${view.name}/`,
        addChildrenAllowed: view.addChildrenAllowed,
        hasChildren: view.hasChildren,
        defaultBaseType: view.defaultBaseType
    };
}

/**
 * One level of the folder hierarchy returned by {@link getFolderHierarchyByPath}.
 * `path` is the parent path that was queried; `folders` are its direct children (first page).
 */
export type FolderTreeHierarchyLevel = {
    path: string;
    folders: DotFolder[];
    totalEntries: number;
};

/**
 * Fetches the folders for every level of a target path using parallel search calls, so the sidebar
 * tree can be rendered expanded down to that path (deep-link restore).
 *
 * One `GET /api/v1/folder/search` (non-recursive) call is made per level, starting at the site root
 * (`'/'`) and descending through each parent path. Uses {@link FOLDER_TREE_HIERARCHY_PAGE_SIZE}
 * (large, page 1 only) so ancestors past the interactive page of 40 still resolve without a
 * sequential page-until-found waterfall. Interactive expand/load-more use
 * {@link getFolderNodesByPath} with {@link FOLDER_TREE_PAGE_SIZE}. Callers should append load-more
 * via {@link applyLoadMoreToHierarchy} when `totalEntries` exceeds the returned page.
 *
 * @param {string} folderPath - The folder path (without hostname) to expand to, e.g. `/a/b/`
 * @param {DotSite} site - The site to scope the search (its `identifier` and `hostname` are used)
 * @param {DotFolderService} dotFolderService - The folder service
 * @returns {Observable<FolderTreeHierarchyLevel[]>} one level descriptor per path
 */
export function getFolderHierarchyByPath(
    folderPath: string,
    site: DotSite,
    dotFolderService: DotFolderService
): Observable<FolderTreeHierarchyLevel[]> {
    // The root level (`'/'`) is always fetched first; deeper levels come from the target path.
    const paths = ['/', ...generateAllParentPaths(folderPath)];

    const folderRequests = paths.map((path) =>
        dotFolderService
            .searchFolders({
                siteId: site.identifier,
                path,
                recursive: false,
                orderby: 'name',
                direction: 'ASC',
                page: 1,
                per_page: FOLDER_TREE_HIERARCHY_PAGE_SIZE
            })
            .pipe(
                map(({ folders, pagination }) => ({
                    path,
                    folders: folders.map((view) =>
                        folderSearchViewToDotFolder(view, site.hostname)
                    ),
                    totalEntries: pagination?.totalEntries ?? folders.length
                }))
            )
    );

    return forkJoin(folderRequests);
}

/**
 * Fetches one page of the direct child folders of a path and transforms them into tree nodes.
 * Used to lazily load a node's children when it is expanded, and to load subsequent pages when the
 * "Load more" node is clicked.
 *
 * @param {string} folderPath - The folder path (without hostname) whose children to fetch
 * @param {DotSite} site - The site to scope the search (its `identifier` and `hostname` are used)
 * @param {DotFolderService} dotFolderService - The folder service
 * @param {number} [page=1] - 1-based page to request
 * @returns {Observable<{ folders: TreeNodeItem[]; totalEntries: number }>} the page of
 * child nodes plus the total number of children in the level (to decide whether more remain)
 */
export function getFolderNodesByPath(
    folderPath: string,
    site: DotSite,
    dotFolderService: DotFolderService,
    page = 1
): Observable<{ folders: TreeNodeItem[]; totalEntries: number }> {
    return dotFolderService
        .searchFolders({
            siteId: site.identifier,
            path: folderPath,
            recursive: false,
            orderby: 'name',
            direction: 'ASC',
            page,
            per_page: FOLDER_TREE_PAGE_SIZE
        })
        .pipe(
            map(({ folders, pagination }) => ({
                folders: folders.map((view) =>
                    createTreeNode(folderSearchViewToDotFolder(view, site.hostname))
                ),
                totalEntries: pagination?.totalEntries ?? folders.length
            }))
        );
}

/**
 * Builds the synthetic "Load more" node appended to the end of a paginated folder level. It is not
 * a real folder: it is not selectable and carries the paging cursor (`nextPage`) and how many
 * folders still remain, so clicking it can fetch and append the next page.
 *
 * @param {string} parentPath - Full path of the parent folder whose children are paginated
 * @param {string} hostName - Hostname of the site
 * @param {number} nextPage - The next 1-based page to request
 * @param {number} remaining - How many folders remain to be loaded in the level
 * @returns {TreeNodeItem} the load-more node
 */
export function buildLoadMoreNode(
    parentPath: string,
    hostName: string,
    nextPage: number,
    remaining: number
): TreeNodeItem {
    // Leave `label` empty so DotFolderTree uses the shared loadMoreLabelKey
    // (same (+) Load more chrome as Host Folder Field / Browser Selector).
    return createLoadMoreTreeNode({
        levelKey: parentPath,
        nextPage,
        remaining,
        path: parentPath,
        hostname: hostName
    });
}

/**
 * Appends a "Load more" sentinel when more folders remain beyond the loaded page.
 */
export function appendLoadMoreNodes(
    children: TreeNodeItem[],
    totalEntries: number,
    path: string,
    hostname: string,
    nextPage: number
): TreeNodeItem[] {
    if (children.length >= totalEntries) {
        return [...children];
    }

    return [
        ...children,
        buildLoadMoreNode(path, hostname, nextPage, totalEntries - children.length)
    ];
}

/**
 * Applies load-more sentinels to each level of a freshly built hierarchy.
 * Root-level sentinels sit as siblings of root folders; nested ones go under the parent node.
 *
 * Hierarchy always fetches page 1 (with {@link FOLDER_TREE_HIERARCHY_PAGE_SIZE}), so the next
 * interactive page is always `2` when `totalEntries` exceeds the returned folders.
 */
export function applyLoadMoreToHierarchy(
    rootNodes: TreeNodeItem[],
    levels: FolderTreeHierarchyLevel[],
    hostname: string
): TreeNodeItem[] {
    if (!levels.length) {
        return rootNodes;
    }

    const nextPageAfterHierarchy = 2;

    const roots = appendLoadMoreNodes(
        rootNodes,
        levels[0].totalEntries,
        levels[0].path,
        hostname,
        nextPageAfterHierarchy
    );

    for (let i = 1; i < levels.length; i++) {
        const level = levels[i];
        const parent = findFolderNodeByPath(level.path, roots);

        if (!parent) {
            continue;
        }

        parent.children = appendLoadMoreNodes(
            (parent.children as TreeNodeItem[] | undefined) ?? [],
            level.totalEntries,
            level.path,
            hostname,
            nextPageAfterHierarchy
        );
    }

    return roots;
}

function findFolderNodeByPath(path: string, nodes: TreeNodeItem[]): TreeNodeItem | undefined {
    for (const node of nodes) {
        if (node.data?.type !== LOAD_MORE_NODE_TYPE && node.data?.path === path) {
            return node;
        }

        const found = node.children
            ? findFolderNodeByPath(path, node.children as TreeNodeItem[])
            : undefined;

        if (found) {
            return found;
        }
    }

    return undefined;
}
