import { TreeNodeItem } from '@dotcms/dotcms-models';

/**
 * The second line of a folder-search result row: `demo.dotcms.com / images / thumbnails`.
 *
 * A flat result list drops the tree's indentation, so the row has to say *where* the folder is or
 * two folders called `containers` on different branches are indistinguishable.
 *
 * Moved here from the Site/Folder field, which built this string inline and is now a consumer of
 * the shared row. Behaviour is deliberately unchanged — that field must come out of the extraction
 * rendering identically.
 *
 * @param node A folder node carrying `data.hostname` and `data.path`
 * @returns The hostname followed by each path segment, joined with ` / `
 */
export function formatFolderSearchPath(node: TreeNodeItem): string {
    // The hostname arrives as `//demo.dotcms.com` from the browse API.
    const hostname = node.data?.hostname?.replace('//', '') ?? '';
    const path = node.data?.path;

    if (!path || path === '/') {
        return hostname;
    }

    const segments = path
        .replace(/^\/+|\/+$/g, '')
        .split('/')
        .filter(Boolean);

    // `filter(Boolean)` on the joined parts too: a node with no hostname would otherwise render a
    // leading separator.
    return [hostname, ...segments].filter(Boolean).join(' / ');
}
