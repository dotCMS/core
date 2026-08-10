import {
    createLoadMoreTreeNode,
    DotPagination,
    LOAD_MORE_NODE_TYPE,
    TreeNodeItem
} from '@dotcms/dotcms-models';

/**
 * Helpers for a folder tree whose roots are **sites** rather than folders of one site.
 *
 * Shared by the legacy Browser Selector and the AssetPicker: both render every site the user can
 * reach as an expandable root, and both page each level (sites and folders alike) behind a
 * "Load more" sentinel. Content Drive does not use these — its tree is scoped to the site picked in
 * the global site switcher.
 */

/** `levelKey` used for the sites level, so its sentinel can't collide with a folder's. */
export const SITES_LOAD_MORE_KEY = 'sites';

/** Whether the paginated response has pages left after the one just returned. */
export function hasMorePages(pagination: DotPagination): boolean {
    return pagination.currentPage * pagination.perPage < pagination.totalEntries;
}

/** Drops the "Load more" sentinel from a level, so a new page can be appended after it. */
export function stripLoadMore(nodes: TreeNodeItem[] | undefined): TreeNodeItem[] {
    return (nodes ?? []).filter((node) => node.type !== LOAD_MORE_NODE_TYPE);
}

/** Appends a "Load more" sentinel to a level when the server says there is more to fetch. */
export function withLoadMore(
    children: TreeNodeItem[],
    hasMore: boolean,
    levelKey: string,
    nextPage: number,
    path: string,
    hostname: string
): TreeNodeItem[] {
    if (!hasMore) {
        return children;
    }

    return [
        ...children,
        createLoadMoreTreeNode({
            levelKey,
            nextPage,
            path,
            hostname
        })
    ];
}

/**
 * Finds a tree node by key, at any depth.
 *
 * The way to address a node across an async gap. `p-tree` and its `p-treeNode`s are `OnPush` and
 * track by object identity, so a level that changed has to be published as **new** objects to
 * re-render — which strands any reference taken before the request. Look the node up again by key
 * after each publish instead of holding on to it.
 */
export function findNodeByKey(nodes: TreeNodeItem[], key: string): TreeNodeItem | undefined {
    for (const node of nodes) {
        if (node.key === key) {
            return node;
        }

        const found = node.children
            ? findNodeByKey(node.children as TreeNodeItem[], key)
            : undefined;

        if (found) {
            return found;
        }
    }

    return undefined;
}

/**
 * Site identifier for a hostname, looked up among the tree roots.
 *
 * Folder nodes carry their hostname but not their site id, and `/folder/search` is scoped by site
 * id — so expanding a folder means resolving its site through the root it hangs from.
 */
export function findSiteIdByHostname(hostname: string, roots: TreeNodeItem[]): string | undefined {
    return roots.find((node) => node.data?.type === 'site' && node.data.hostname === hostname)?.data
        ?.id;
}

/**
 * Depth-first lookup of the node a level belongs to: the site root when the path is the site root,
 * the matching folder otherwise. "Load more" sentinels are skipped — they are not parents.
 */
export function findFolderParent(
    roots: TreeNodeItem[],
    path: string,
    hostname: string
): TreeNodeItem | undefined {
    for (const node of roots) {
        if (node.data?.type === LOAD_MORE_NODE_TYPE) {
            continue;
        }

        if (
            node.data?.type === 'site' &&
            node.data.hostname === hostname &&
            (path === '/' || path === '')
        ) {
            return node;
        }

        if (
            node.data?.type === 'folder' &&
            node.data.hostname === hostname &&
            node.data.path === path
        ) {
            return node;
        }

        const found = node.children
            ? findFolderParent(node.children as TreeNodeItem[], path, hostname)
            : undefined;

        if (found) {
            return found;
        }
    }

    return undefined;
}

/**
 * Site id a node belongs to: its own id when it is a site, otherwise resolved from its hostname.
 */
export function resolveSiteId(node: TreeNodeItem, roots: TreeNodeItem[]): string | undefined {
    const data = node.data;

    if (!data || data.type === LOAD_MORE_NODE_TYPE) {
        return undefined;
    }

    return data.type === 'site' ? data.id : findSiteIdByHostname(data.hostname, roots);
}
