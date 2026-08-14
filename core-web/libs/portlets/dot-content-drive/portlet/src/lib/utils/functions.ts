import { format } from 'date-fns';
import { forkJoin, Observable, of } from 'rxjs';

import { catchError, map, switchMap } from 'rxjs/operators';

import { DotFolderService } from '@dotcms/data-access';
import {
    createLoadMoreTreeNode,
    DotCMSContentTypeField,
    DotContentDriveDateRange,
    DotContentDriveActionableFolder,
    DotContentDriveActionableItem,
    DotContentDriveUserSearchableValue,
    DotFolder,
    DotSite,
    FolderSearchView,
    LOAD_MORE_NODE_TYPE
} from '@dotcms/dotcms-models';
import { getSingleSelectableFieldOptions } from '@dotcms/edit-content';
import { DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';

import { createTreeNode, generateAllParentPaths } from './tree-folder.utils';

import {
    FIELD_FILTER_CHECKBOX_TYPE,
    FIELD_FILTER_DATE_TYPES,
    FIELD_FILTER_KEY_VALUE_TYPE,
    FIELD_FILTER_MULTI_VALUE_TYPES,
    FOLDER_NAME_FILTER_MIN_LENGTH,
    FOLDER_TREE_HIERARCHY_PAGE_SIZE,
    FOLDER_TREE_PAGE_SIZE,
    USER_SEARCHABLE_PREFIX,
    USER_SEARCHABLE_VALUE_SEPARATOR
} from '../shared/constants';
import {
    DotContentDriveDecodeFunction,
    DotContentDriveFilters,
    DotKnownContentDriveFilters
} from '../shared/models';

/**
 * Decodes a multi-selector value.
 *
 * @param {string} value
 * @return {*}  {string[]}
 */
const multiSelector: DotContentDriveDecodeFunction = (value = ''): string[] =>
    value
        .split(',')
        .map((v) => v.trim())
        .filter((v) => v !== '');

/**
 * Decodes a single-selector value.
 *
 * @param {string} value
 * @return {*}  {string}
 */
const singleSelector: DotContentDriveDecodeFunction = (value = ''): string => value.trim();

/** A single workflow filter entry: one scheme, optionally pinned to a step. */
export interface WorkflowFilterEntry {
    scheme: string;
    step?: string;
}

/** Separator for the `schemeId[:stepId]` workflow token encoding. */
export const WORKFLOW_TOKEN_SEPARATOR = ':';

/**
 * Canonical parse for one `workflow` token. Splits on the FIRST separator only,
 * so any separator inside the step id is preserved.
 * `'A:X'` → `{ scheme: 'A', step: 'X' }`; `'B'` → `{ scheme: 'B' }`.
 *
 * @param {string} token
 * @return {*}  {WorkflowFilterEntry}
 */
export function parseWorkflowToken(token: string): WorkflowFilterEntry {
    const index = token.indexOf(WORKFLOW_TOKEN_SEPARATOR);
    return index === -1
        ? { scheme: token }
        : { scheme: token.slice(0, index), step: token.slice(index + 1) };
}

/**
 * Canonical serialize, inverse of {@link parseWorkflowToken}.
 * `{ scheme: 'A', step: 'X' }` → `'A:X'`; `{ scheme: 'B' }` → `'B'`.
 *
 * @param {WorkflowFilterEntry} entry
 * @return {*}  {string}
 */
export function workflowEntryToToken({ scheme, step }: WorkflowFilterEntry): string {
    return step ? `${scheme}${WORKFLOW_TOKEN_SEPARATOR}${step}` : scheme;
}

/**
 * Parses the `workflow` filter tokens (`schemeId` or `schemeId:stepId`) into the
 * `{ scheme, step? }` entries the drive-search request expects.
 *
 * @param {string[]} tokens
 * @return {*}  {WorkflowFilterEntry[]}
 */
export function parseWorkflowFilter(tokens: string[] = []): WorkflowFilterEntry[] {
    return tokens.map(parseWorkflowToken);
}

/**
 * Decodes the value by the key. This is a dictionary of functions that will be used to decode the value by the key.
 *
 * @example
 *
 * ```typescript
 * decodeByFilterKey.baseType('1,2,3')
 * // Output: ['1', '2', '3']
 * ```
 *
 * @return {*}  {Record<keyof DotKnownContentDriveFilters, (value: string) => string | string[]>}
 */
export const decodeByFilterKey: Record<
    keyof DotKnownContentDriveFilters,
    DotContentDriveDecodeFunction
> = {
    // Should always return an array
    baseType: multiSelector,
    // Should always return an array
    contentType: multiSelector,
    title: singleSelector,
    languageId: multiSelector,
    // Each entry is `schemeId` or `schemeId:stepId`; comma-separated in the URL
    workflow: multiSelector
};

/**
 * Decodes the filters string into a record of key-value pairs.
 *
 * @example
 *
 * ```typescript
 * decodeFilters('contentType:Blog;language:en;folder:123')
 * // Output:
 * // { contentType: 'Blog', language: 'en', folder: '123' }
 * ```
 *
 * @export
 * @param {string} filters
 * @return {*}  {DotContentDriveFilters}
 */
export function decodeFilters(filters: string): DotContentDriveFilters {
    if (!filters) {
        return {};
    }

    const filtersArray = filters.split(';').filter((filter) => filter.trim() !== '');

    return filtersArray.reduce((acc, filter) => {
        // Get the first colon index
        const colonIndex = filter.indexOf(':');

        if (colonIndex === -1) {
            return acc;
        }

        // Handle the case where the filter has a colon in the value
        // Ex. someContentType.url:http://some.url (Looking forward for complex filters)
        const key = filter.substring(0, colonIndex).trim();
        const value = filter.substring(colonIndex + 1).trim();

        // key stays `string` for assignment so the open index signature applies;
        // narrowing happens only inside decodeFilterValue for the known-key lookup.
        acc[key] = decodeFilterValue(key, value);

        return acc;
    }, {} as DotContentDriveFilters);
}

function isKnownFilterKey(key: string): key is keyof DotKnownContentDriveFilters {
    return Object.hasOwn(decodeByFilterKey, key);
}

/**
 * Decodes a single filter value. Known keys use {@link decodeByFilterKey};
 * user-searchable field filters stay raw; unknown keys sniff for commas.
 */
function decodeFilterValue(key: string, value: string): string | string[] {
    // Field-filter (user-searchable) values are stored raw: the field type — not comma
    // sniffing — decides their shape downstream, so never split/trim them here.
    if (key.startsWith(USER_SEARCHABLE_PREFIX)) {
        return singleSelector(value);
    }

    if (isKnownFilterKey(key)) {
        return decodeByFilterKey[key](value);
    }

    return value.includes(',') ? multiSelector(value) : singleSelector(value);
}

/**
 * Encodes the filters into a string.
 *
 * @example
 *
 * ```typescript
 * encodeFilters({ contentType: 'Blog', language: 'en', folder: '123' })
 * // Output:
 * // 'contentType:Blog;language:en;folder:123'
 * ```
 *
 * @export
 * @param {DotContentDriveFilters} filters
 * @return {*}  {string}
 */
export function encodeFilters(filters: DotContentDriveFilters): string {
    if (!filters) {
        return '';
    }

    // Filter out empty values (empty strings, null, undefined)
    const filtersArray = Object.entries(filters).filter(
        ([_key, value]) => value !== '' && value !== null && value !== undefined
    );

    if (filtersArray.length === 0) {
        return '';
    }

    // Join the filters with semicolons
    return filtersArray
        .reduce((acc, filter) => {
            const [key, value] = filter;

            // Handle the multiselector (,)
            if (Array.isArray(value)) {
                acc.push(`${key}:${value.join(',')}`);
            } else {
                acc.push(`${key}:${value}`);
            }

            return acc;
        }, [] as string[])
        .join(';');
}

/**
 * Guarantees the language filter always carries a value, seeding the environment's default
 * language whenever nothing is selected.
 *
 * "No language selected" is not the neutral state it looks like: the backend omits the language
 * term from the query entirely (`LuceneQueryBuilder.getSystemSearchableQueryTerms`), so every
 * language version of a contentlet comes back as its own row. Selecting the default explicitly is
 * both what users expect to see and an honest reflection of what is applied — so the seeded value
 * lands in `filters` (and therefore in the URL) like any other selection.
 *
 * Returns the filters untouched when the default is unknown — the languages request has not
 * answered yet, or failed — so the portlet degrades to exactly its pre-seeding behaviour instead
 * of inventing a language. Never mutates the input.
 *
 * @param {DotContentDriveFilters} filters The filters to seed.
 * @param {number} [defaultLanguageId] The environment's default language id, when known.
 * @return {*} {DotContentDriveFilters} The filters, with `languageId` guaranteed when possible.
 */
export function withDefaultLanguage(
    filters: DotContentDriveFilters,
    defaultLanguageId?: number
): DotContentDriveFilters {
    if (!defaultLanguageId || filters?.languageId?.length) {
        return filters;
    }

    return { ...filters, languageId: [String(defaultLanguageId)] };
}

/**
 * Encodes the filters with their keys in a stable (alphabetical) order, for **comparison only**.
 *
 * {@link encodeFilters} follows insertion order, which makes two equivalent filter sets encode
 * differently — `title:x;languageId:1` vs `languageId:1;title:x`. That is harmless in the URL but
 * not when the encoded string is used to decide whether state changed. Never use this to write the
 * URL; it would reorder the params users see.
 *
 * @param {DotContentDriveFilters} filters The filters to encode.
 * @return {*} {string} A key-order-independent encoding of the filters.
 */
export function sortedEncodedFilters(filters: DotContentDriveFilters): string {
    if (!filters) {
        return '';
    }

    return encodeFilters(
        Object.fromEntries(
            Object.entries(filters).sort(([keyA], [keyB]) => keyA.localeCompare(keyB))
        )
    );
}

/**
 * Adapts a `FolderSearchView` (returned by `GET /api/v1/folder/search`) into the `DotFolder`
 * shape the tree builder consumes.
 *
 * The search view exposes the folder's own `name` and its parent `path` separately and omits the
 * hostname (the search is already scoped by site), so the folder's own full path is recomposed as
 * `<parentPath><name>/` and the current site hostname is injected.
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
        defaultBaseType: view.defaultBaseType,
        name: view.name,
        title: view.title,
        sortOrder: view.sortOrder,
        filesMasks: view.filesMasks,
        defaultFileType: view.defaultFileType,
        showOnMenu: view.showOnMenu,
        // `null` (not requested) and `[]` (requested, no grants) mean different things, and the
        // difference drives behavior: a node whose permissions were never fetched must resolve them
        // on demand before its context menu can gate correctly, while an empty array is a final
        // answer. Collapse `null` to `undefined` (the optional-field idiom) and keep `[]` intact.
        permissions: view.permissions ?? undefined
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
    /**
     * The 1-based page "Load more" should request next for this level, expressed in
     * {@link FOLDER_TREE_PAGE_SIZE} units because that is what load-more pages by.
     *
     * Derived from the folders actually fetched, never from the rendered node count: a level can
     * carry one extra folder that {@link resolveHierarchyAncestor} appended out of sort order, and
     * counting that as paged-through would make load-more skip a page of real folders.
     */
    nextPage: number;
};

/**
 * The last segment of a folder path: `/a/b/` → `b`, `/a/` → `a`.
 *
 * @param {string} folderPath - A folder's own path, with or without a trailing slash
 * @returns {string} the folder's own name, or `''` for the site root
 */
export function getPathLeafName(folderPath: string): string {
    const segments = folderPath.split('/').filter(Boolean);

    return segments[segments.length - 1] ?? '';
}

/**
 * Fetches one specific folder of a level directly, for the case where a deep-linked ancestor sorts
 * past the level's first hierarchy page and would otherwise be missing from the tree.
 *
 * The tree has to show the folder the drive is open on. Widening the hierarchy page to guarantee
 * that is not an option: `includePermissions=true` caps `per_page`
 * (`content.drive.folder.search.permissions.max.per.page`), and the nodes on first paint need
 * permissions to gate their context menu. Paging the level until the folder turns up is not one
 * either: it trades one request for an unbounded chain to find something we already know the exact
 * path of. So the level is queried once more, narrowed by the folder's own name, and matched on
 * exact path.
 *
 * `POST /api/v1/folder/byPath` would be the natural "fetch this one folder" call, but it is
 * deprecated for removal, returns a path's *subfolders* rather than the folder itself, and carries
 * no permissions.
 *
 * Resolves to `undefined` rather than failing, in three cases, all of which leave the folder
 * unpinned. The `name` filter needs {@link FOLDER_NAME_FILTER_MIN_LENGTH} characters, so a
 * one-character folder name drops the filter and falls back to the level's first page; `name` is a
 * case-insensitive *partial* match, so a level holding more same-substring siblings than fit one
 * page can still exclude the target (both need a level wide enough to have overflowed in the first
 * place; an exact-match or identifier filter would close them); and the request itself can fail.
 *
 * Swallowing that failure is the point. This is a best-effort extra request on top of the page the
 * level already has, and its caller runs inside a `forkJoin`: letting a transient 500 through would
 * reject the whole hierarchy load, which `loadFolders` turns into an empty tree. A folder that
 * cannot be pinned must cost that folder's pin, not every readable folder on screen.
 *
 * @param {string} levelPath - Parent path being listed, e.g. `/a/`
 * @param {string} ancestorPath - Full path of the folder to resolve, e.g. `/a/b/`
 * @param {DotSite} site - The site to scope the search
 * @param {DotFolderService} dotFolderService - The folder service
 * @returns {Observable<DotFolder | undefined>} the folder, or `undefined` if it could not be reached
 */
export function resolveHierarchyAncestor(
    levelPath: string,
    ancestorPath: string,
    site: DotSite,
    dotFolderService: DotFolderService
): Observable<DotFolder | undefined> {
    const name = getPathLeafName(ancestorPath);

    return dotFolderService
        .searchFolders({
            siteId: site.identifier,
            path: levelPath,
            recursive: false,
            name: name.length >= FOLDER_NAME_FILTER_MIN_LENGTH ? name : undefined,
            orderby: 'name',
            direction: 'ASC',
            page: 1,
            per_page: FOLDER_TREE_HIERARCHY_PAGE_SIZE,
            includePermissions: true
        })
        .pipe(
            map(({ folders }) =>
                folders
                    .map((view) => folderSearchViewToDotFolder(view, site.hostname))
                    .find((folder) => folder.path === ancestorPath)
            ),
            catchError(() => of(undefined))
        );
}

/**
 * Fetches the folders for every level of a target path using parallel search calls, so the sidebar
 * tree can be rendered expanded down to that path (deep-link restore).
 *
 * One `GET /api/v1/folder/search` (non-recursive) call is made per level, starting at the site root
 * (`'/'`) and descending through each parent path, all in parallel. Every level requests
 * `includePermissions`, so each node the tree renders on first paint can gate its context menu
 * without a second round-trip. That pins the page to {@link FOLDER_TREE_HIERARCHY_PAGE_SIZE}, the
 * backend's cap when permissions are requested.
 *
 * Because the page is capped, a level wide enough can sort the next ancestor past it. The drive
 * still has to show the folder it is open on, so that one folder is fetched individually (see
 * {@link resolveHierarchyAncestor}) rather than the page being widened, and is *pinned to the top*
 * of its level. Pinning rather than appending is deliberate: dropped in at the end it would read as
 * the next folder in sort order, which it is not, and it would sit next to the level's "Load more"
 * where it is easy to miss. At the top it reads as "the folder you are in". If the user later pages
 * far enough to reach its real position, {@link mergeFolderNodePage} moves it there.
 *
 * Interactive expand/load-more use {@link getFolderNodesByPath} with {@link FOLDER_TREE_PAGE_SIZE}.
 * Callers should append load-more via {@link applyLoadMoreToHierarchy} when `totalEntries` exceeds
 * the returned page.
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

    // Level `i` is the one that must contain `expectedPaths[i]` for the tree to keep descending.
    // The deepest level has no entry here: it holds the target folder's own children, so there is
    // nothing further to reach and its first page is all the tree needs.
    const expectedPaths = generateAllParentPaths(folderPath);

    const folderRequests = paths.map((path, levelIndex) =>
        dotFolderService
            .searchFolders({
                siteId: site.identifier,
                path,
                recursive: false,
                orderby: 'name',
                direction: 'ASC',
                page: 1,
                per_page: FOLDER_TREE_HIERARCHY_PAGE_SIZE,
                includePermissions: true
            })
            .pipe(
                map(({ folders, pagination }) => ({
                    path,
                    folders: folders.map((view) =>
                        folderSearchViewToDotFolder(view, site.hostname)
                    ),
                    totalEntries: pagination?.totalEntries ?? folders.length,
                    // Whole pages consumed, converted to load-more's page size. Safe because the
                    // hierarchy page is a multiple of it; a partial page means the level is fully
                    // loaded and no "Load more" is appended, so the value goes unused.
                    nextPage: Math.floor(folders.length / FOLDER_TREE_PAGE_SIZE) + 1
                })),
                switchMap((level) => {
                    const expectedPath = expectedPaths[levelIndex];

                    if (!expectedPath || level.folders.some(({ path }) => path === expectedPath)) {
                        return of(level);
                    }

                    return resolveHierarchyAncestor(
                        path,
                        expectedPath,
                        site,
                        dotFolderService
                    ).pipe(
                        map((ancestor) =>
                            ancestor ? { ...level, folders: [ancestor, ...level.folders] } : level
                        )
                    );
                })
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
 * @returns {Observable<{ folders: DotFolderTreeNodeItem[]; totalEntries: number }>} the page of
 * child nodes plus the total number of children in the level (to decide whether more remain)
 */
export function getFolderNodesByPath(
    folderPath: string,
    site: DotSite,
    dotFolderService: DotFolderService,
    page = 1
): Observable<{ folders: DotFolderTreeNodeItem[]; totalEntries: number }> {
    return dotFolderService
        .searchFolders({
            siteId: site.identifier,
            path: folderPath,
            recursive: false,
            orderby: 'name',
            direction: 'ASC',
            page,
            per_page: FOLDER_TREE_PAGE_SIZE,
            // Safe to request here: this page size (40) is well under the backend cap, so nodes
            // loaded by expanding a folder carry their permissions and their context menu opens
            // without a second round-trip.
            includePermissions: true
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
 * @returns {DotFolderTreeNodeItem} the load-more node
 */
export function buildLoadMoreNode(
    parentPath: string,
    hostName: string,
    nextPage: number,
    remaining: number
): DotFolderTreeNodeItem {
    // Leave `label` empty so DotFolderTree uses the shared loadMoreLabelKey
    // (same (+) Load more chrome as Host Folder Field / Browser Selector).
    return createLoadMoreTreeNode({
        levelKey: parentPath,
        nextPage,
        remaining,
        path: parentPath,
        hostname: hostName
    }) as DotFolderTreeNodeItem;
}

/**
 * Merges a freshly loaded page of folder nodes into the ones a level already shows.
 *
 * Plain concatenation is not enough because the hierarchy load can pin a folder to the top of a
 * level out of sort order (see {@link getFolderHierarchyByPath}). Page far enough and that same
 * folder arrives again in its real position, which would render it twice.
 *
 * The already-rendered node wins on identity but takes the incoming node's position: it may be
 * expanded, hold loaded children and carry the current selection, none of which the fresh copy has.
 * So the pinned folder stops being pinned and settles where it belongs, with its subtree intact.
 *
 * @param {DotFolderTreeNodeItem[]} loaded - Nodes already rendered for the level (no "Load more")
 * @param {DotFolderTreeNodeItem[]} page - The newly fetched page, in sort order
 * @returns {DotFolderTreeNodeItem[]} the merged level, free of duplicates
 */
export function mergeFolderNodePage(
    loaded: DotFolderTreeNodeItem[],
    page: DotFolderTreeNodeItem[]
): DotFolderTreeNodeItem[] {
    const nodeId = (node: DotFolderTreeNodeItem): string | undefined =>
        node.data?.type !== LOAD_MORE_NODE_TYPE ? node.data?.id : undefined;

    const existingById = new Map(
        loaded.flatMap((node) => {
            const id = nodeId(node);

            return id ? [[id, node] as const] : [];
        })
    );

    const incomingIds = new Set(page.flatMap((node) => nodeId(node) ?? []));

    return [
        ...loaded.filter((node) => {
            const id = nodeId(node);

            return !id || !incomingIds.has(id);
        }),
        ...page.map((node) => {
            const id = nodeId(node);

            return (id && existingById.get(id)) || node;
        })
    ];
}

/**
 * Appends a "Load more" sentinel when more folders remain beyond the loaded page.
 */
export function appendLoadMoreNodes(
    children: DotFolderTreeNodeItem[],
    totalEntries: number,
    path: string,
    hostname: string,
    nextPage: number
): DotFolderTreeNodeItem[] {
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
 * Each level carries its own `nextPage`, because the hierarchy pages at
 * {@link FOLDER_TREE_HIERARCHY_PAGE_SIZE} while load-more pages at {@link FOLDER_TREE_PAGE_SIZE}.
 * Resuming at a fixed page would re-request folders already on screen.
 */
export function applyLoadMoreToHierarchy(
    rootNodes: DotFolderTreeNodeItem[],
    levels: FolderTreeHierarchyLevel[],
    hostname: string
): DotFolderTreeNodeItem[] {
    if (!levels.length) {
        return rootNodes;
    }

    const roots = appendLoadMoreNodes(
        rootNodes,
        levels[0].totalEntries,
        levels[0].path,
        hostname,
        levels[0].nextPage
    );

    for (let i = 1; i < levels.length; i++) {
        const level = levels[i];
        const parent = findFolderNodeByPath(level.path, roots);

        if (!parent) {
            continue;
        }

        parent.children = appendLoadMoreNodes(
            (parent.children as DotFolderTreeNodeItem[] | undefined) ?? [],
            level.totalEntries,
            level.path,
            hostname,
            level.nextPage
        );
    }

    return roots;
}

function findFolderNodeByPath(
    path: string,
    nodes: DotFolderTreeNodeItem[]
): DotFolderTreeNodeItem | undefined {
    for (const node of nodes) {
        if (node.data?.type !== LOAD_MORE_NODE_TYPE && node.data?.path === path) {
            return node;
        }

        const found = node.children
            ? findFolderNodeByPath(path, node.children as DotFolderTreeNodeItem[])
            : undefined;

        if (found) {
            return found;
        }
    }

    return undefined;
}

/**
 * Checks if an item is a folder.
 *
 * Narrows to the actionable folder shape rather than the full table row, so it serves folders from
 * both views. Called with a `DotContentDriveItem` (the table's list) it still narrows to
 * `DotContentDriveFolder`, since that is the only folder member of that union.
 *
 * @param {DotContentDriveActionableItem} item - The item to check
 * @returns {boolean} True if the item is a folder, false otherwise
 */
export function isFolder(
    item: DotContentDriveActionableItem
): item is DotContentDriveActionableFolder {
    return item != null && 'type' in item && item.type === 'folder';
}

/** True when the field type stores a `{ from, to }` date range (Date / Date-and-Time / Time). */
export function isDateFieldFilterType(fieldType: string): boolean {
    return (FIELD_FILTER_DATE_TYPES as readonly string[]).includes(fieldType);
}

/** True when the field type stores a list of values (Multi-Select / Checkbox / Tag / …). */
export function isMultiValueFieldFilterType(fieldType: string): boolean {
    return FIELD_FILTER_MULTI_VALUE_TYPES.includes(fieldType);
}

/**
 * The field variables that have a `us.*` field-filter entry in the bag, in insertion order.
 * Parsed at the same layer as {@link decodeFilters} so the store just stores the result.
 *
 * @param {DotContentDriveFilters} filters
 * @return {*}  {string[]}
 */
export function getUserSearchableActive(filters: DotContentDriveFilters): string[] {
    return Object.keys(filters ?? {})
        .filter((key) => key.startsWith(USER_SEARCHABLE_PREFIX))
        .map((key) => key.slice(USER_SEARCHABLE_PREFIX.length));
}

/**
 * True for a binary (boolean) checkbox — a Checkbox field with a single option (e.g. `|true`).
 * Unlike a multi-option checkbox, this is a single boolean *value* (true/false), not a selection.
 */
export function isBinaryCheckboxField(field: DotCMSContentTypeField): boolean {
    return (
        field.fieldType === FIELD_FILTER_CHECKBOX_TYPE &&
        getSingleSelectableFieldOptions(field.values ?? '', field.dataType).length <= 1
    );
}

/**
 * Reshapes a raw stored field-filter string into the payload value for its field type:
 * date → `{ from, to }`, multi-select → `string[]`, everything else → the raw string.
 * Returns `undefined` when the value is effectively empty (so callers can skip it).
 *
 * @param {string} raw - The raw value stored in the filter bag.
 * @param {string} fieldType - The content-type field type (e.g. `Text`, `Date`, `Multi-Select`).
 * @return {*}  {(DotContentDriveUserSearchableValue | undefined)}
 */
export function parseUserSearchableValue(
    raw: string,
    fieldType: string
): DotContentDriveUserSearchableValue | undefined {
    if (!raw) {
        return undefined;
    }

    if (isDateFieldFilterType(fieldType)) {
        const [from = '', to = ''] = raw.split(USER_SEARCHABLE_VALUE_SEPARATOR);

        return from || to ? { from, to } : undefined;
    }

    if (isMultiValueFieldFilterType(fieldType)) {
        const values = parseMultiValue(raw);

        return values.length ? values : undefined;
    }

    if (fieldType === FIELD_FILTER_KEY_VALUE_TYPE) {
        return toKeyValueTerm(raw);
    }

    return raw;
}

/**
 * Translates a Key/Value filter input into the term the backend contains-matches against the
 * indexed `.key_value` subfield (stored as `key_value` = `key + "_" + value`).
 *
 * The term is lowercased to match the indexed `.key_value` sub-field, which dotCMS stores as
 * `(key + "_" + value).toLowerCase()` — so `Color:Red` matches the same content as `color:red`.
 *
 * Shorthand rules (the **first** colon is the key/value separator — everything after it is the
 * value, so a value may itself contain colons):
 * - `key:value`         → `key_value`           (exact-pair match; e.g. `Deploy:HTTPS://x` → `deploy_https://x`)
 * - `key:` / `:value`   → `key` / `value`       (only the filled side)
 * - bare term (no `:`)  → the term              (loose match on a key OR a value)
 *
 * ⚠️ Greedy shorthand: because *any* colon is treated as the separator, a **bare** value that
 * happens to contain a colon (a URL like `https://x`, a time like `12:30`, a ratio like `16:9`) is
 * read as `key:value` (`https_//x`, `12_30`, `16_9`) and will likely match nothing. To search a
 * colon-bearing value, prefix it with its key (`myKey:12:30`) so the intended value is preserved.
 * A raw colon is never sent to the backend — that path is metadata-only and wouldn't match a
 * regular Key/Value field anyway.
 *
 * @param {string} raw - The literal value the user typed (also what's kept in the URL/chip).
 * @return {*}  {(string | undefined)} Returns `undefined` when the input is empty.
 */
function toKeyValueTerm(raw: string): string | undefined {
    const trimmed = raw.trim();
    if (!trimmed) {
        return undefined;
    }

    // Split on the FIRST colon only, so a value may contain further colons (e.g. `key:12:30`).
    const separator = trimmed.indexOf(':');
    // The index stores `.key_value` as `(key + "_" + value).toLowerCase()`, so the term is
    // lowercased to match regardless of the case the user typed (e.g. `Color:Red` → `color_red`).
    if (separator === -1) {
        return trimmed.toLowerCase();
    }

    const key = trimmed.slice(0, separator).trim();
    const value = trimmed.slice(separator + 1).trim();

    if (key && value) {
        return `${key}_${value}`.toLowerCase();
    }

    // Only one side of the `key:value` was filled — match on whichever is present.
    return (key || value).toLowerCase() || undefined;
}

/** Safe `decodeURIComponent` that returns the input unchanged on a malformed sequence. */
const safeDecode = (value: string): string => {
    try {
        return decodeURIComponent(value);
    } catch {
        return value;
    }
};

/**
 * Splits a stored multi-value string back into its values. Each value is percent-encoded on
 * serialize (see {@link serializeMultiValue}) so a value containing the separator — e.g. a tag
 * label like `"News, Press"` — round-trips intact.
 *
 * @param {string} raw
 * @return {*}  {string[]}
 */
export function parseMultiValue(raw: string): string[] {
    if (!raw) {
        return [];
    }

    return raw
        .split(USER_SEARCHABLE_VALUE_SEPARATOR)
        .map((value) => safeDecode(value.trim()))
        .filter(Boolean);
}

/**
 * Joins multi-value entries into the stored string, percent-encoding each value so it can safely
 * contain the separator. Inverse of {@link parseMultiValue}.
 *
 * @param {string[]} values
 * @return {*}  {string}
 */
export function serializeMultiValue(values: string[]): string {
    return values.map(encodeURIComponent).join(USER_SEARCHABLE_VALUE_SEPARATOR);
}

/**
 * Serializes a shaped field-filter value back into the raw string stored in the filter bag,
 * inverse of {@link parseUserSearchableValue}. Empty values serialize to `''` so the URL encoder
 * (which drops empty entries) leaves no dangling criterion.
 *
 * @param {(DotContentDriveUserSearchableValue | null | undefined)} value
 * @param {string} fieldType
 * @return {*}  {string}
 */
/** Narrows a user-searchable value to a `{ from, to }` date range (object, not array). */
function isDateRange(value: DotContentDriveUserSearchableValue): value is DotContentDriveDateRange {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Formats a Date as a timezone-naive local wall-clock ISO string (`yyyy-MM-ddTHH:mm:ss`, no `Z` or
 * offset) — i.e. the exact date/time the user sees in the picker.
 *
 * Date/Date-and-Time/Time filters must send the wall-clock, not a UTC instant: `toISOString()`
 * shifts by the browser's offset (a UTC-3 user's 10:00 becomes `13:00Z`), and the backend then
 * parses that `Z` value as an instant and reformats it in the SERVER zone — so the bound no longer
 * matches what the user picked. A no-offset value instead round-trips as identity: the backend
 * parses it in the server zone and formats it back in the server zone (see
 * `BrowserAPIImpl#parseFlexibleDate` → `LocalDateTime.parse(...)` and `normalizeDateBound`). On the
 * FE, `new Date('…T10:00:00')` (no offset) also parses as local, so the picker round-trips too.
 *
 * Returns `''` for an invalid/absent Date: the typeable Time picker (`[keepInvalid]="true"`) can
 * emit an `Invalid Date` mid-typing, and `date-fns` `format` throws `RangeError` on one — so a
 * partial time simply clears that bound instead of blowing up `#applyRange`.
 */
export function toLocalIsoString(date: Date): string {
    if (!date || Number.isNaN(date.getTime())) {
        return '';
    }

    // `date-fns` formats by the Date's LOCAL components, so this is the wall-clock with no offset/Z.
    return format(date, "yyyy-MM-dd'T'HH:mm:ss");
}

export function serializeUserSearchableValue(
    value: DotContentDriveUserSearchableValue | null | undefined,
    fieldType: string
): string {
    if (value == null) {
        return '';
    }

    if (isDateFieldFilterType(fieldType)) {
        // Guard the shape rather than blindly casting: a mismatched fieldType/value pair yields ''
        // (not filtering) instead of a misleading partial range.
        if (!isDateRange(value)) {
            return '';
        }

        if (!value.from && !value.to) {
            return '';
        }

        return `${value.from ?? ''}${USER_SEARCHABLE_VALUE_SEPARATOR}${value.to ?? ''}`;
    }

    if (isMultiValueFieldFilterType(fieldType)) {
        return serializeMultiValue(Array.isArray(value) ? value : []);
    }

    return String(value);
}

/**
 * Builds the `userSearchable` payload object from the flat filter bag, keyed by field variable.
 * Only `us.`-prefixed entries whose field metadata is known (loaded) are considered. A binary
 * checkbox emits its boolean value when set (`true`/`false`); every field type is included only
 * when its value is non-empty. Returns `undefined` when there are no active field filters.
 *
 * @param {DotContentDriveFilters} filters - The full filter bag.
 * @param {DotCMSContentTypeField[]} fields - The active content type's searchable fields.
 * @return {*}  {(Record<string, DotContentDriveUserSearchableValue> | undefined)}
 */
export function buildUserSearchablePayload(
    filters: DotContentDriveFilters,
    fields: DotCMSContentTypeField[]
): Record<string, DotContentDriveUserSearchableValue> | undefined {
    const fieldByVariable = new Map(fields.map((field) => [field.variable, field]));
    const payload: Record<string, DotContentDriveUserSearchableValue> = {};

    for (const [key, raw] of Object.entries(filters ?? {})) {
        if (!key.startsWith(USER_SEARCHABLE_PREFIX)) {
            continue;
        }

        const variable = key.slice(USER_SEARCHABLE_PREFIX.length);
        const field = fieldByVariable.get(variable);
        if (!field) {
            continue;
        }

        const rawValue = Array.isArray(raw)
            ? raw.join(USER_SEARCHABLE_VALUE_SEPARATOR)
            : (raw ?? '');

        // A binary checkbox filters for the chosen boolean; empty means not filtering.
        if (isBinaryCheckboxField(field)) {
            if (rawValue === 'true' || rawValue === 'false') {
                payload[variable] = rawValue === 'true';
            }

            continue;
        }

        const value = parseUserSearchableValue(rawValue, field.fieldType);
        if (value === undefined) {
            continue;
        }

        payload[variable] = value;
    }

    return Object.keys(payload).length ? payload : undefined;
}
