import { forkJoin, Observable, of } from 'rxjs';

import { catchError, map, switchMap } from 'rxjs/operators';

import { DotFolderService } from '@dotcms/data-access';
import {
    createLoadMoreTreeNode,
    PERMISSIONS_TYPE,
    DotContentDriveActionableFolder,
    DotContentDriveActionableItem,
    DotFolder,
    DotSite,
    FolderSearchView,
    LOAD_MORE_NODE_TYPE
} from '@dotcms/dotcms-models';
import { DotFolderTreeNodeData, DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';

import { createTreeNode, generateAllParentPaths } from './tree-folder.utils';

import {
    CONTENT_STATUS,
    FOLDER_NAME_FILTER_MIN_LENGTH,
    FOLDER_TREE_HIERARCHY_PAGE_SIZE,
    FOLDER_TREE_PAGE_SIZE,
    SHARED_ASSETS_ENABLED_VALUE,
    SHARED_ASSETS_FILTER_KEY,
    USER_SEARCHABLE_PREFIX
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
// Deliberately NOT annotated as DotContentDriveDecodeFunction: that type returns
// `string | string[]`, which would hide the array from callers that compose on top of this one
// (the `status` decoder filters the result). Still assignable where a decode function is expected.
const multiSelector = (value = ''): string[] =>
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
 * Whether a raw string names a real {@link CONTENT_STATUS}. Used to sanitize the URL on the way in.
 */
function isContentStatus(value: string): boolean {
    return (Object.values(CONTENT_STATUS) as string[]).includes(value);
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
    workflow: multiSelector,
    // MUST be listed explicitly. Unknown keys fall through to the comma sniff in
    // `decodeFilterValue`, so a single selected status (`status:ARCHIVED`) would decode to the
    // STRING 'ARCHIVED' while two would decode to an array. Every consumer checks
    // `filters()?.status?.length`, which is 8 for that string — the filter would appear to work
    // right up until someone selected exactly one status.
    //
    // Unrecognized values are dropped here rather than sent on. The endpoint rejects an unknown
    // status with a 400 (it will not silently widen the result set), and that 400 would surface as
    // a stopped spinner over a stale grid. A stale or hand-edited URL should degrade to "no status
    // filter", which is how the other filters already behave — an unknown contentType id is
    // dropped server-side rather than failing the request.
    status: (value) => multiSelector(value).filter(isContentStatus),
    sharedAssets: singleSelector
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
        const decoded = decodeFilterValue(key, value);

        // A multi-value key that decoded to nothing is not a filter. Dropping it keeps a sanitized
        // `status:BOGUS` from round-tripping back into the URL as a bare `status:`.
        if (Array.isArray(decoded) && decoded.length === 0) {
            return acc;
        }

        acc[key] = decoded;

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
 * Seeds the shared-assets toggle with its default when the filters do not carry it.
 *
 * The toggle is on by default, which could have been left implicit — no key meaning on. It is
 * seeded instead so the state that is applied is always visible in the URL rather than inferred from
 * something missing, and so "Clear all" lands on the same explicit value a fresh load does.
 *
 * Never mutates the input.
 *
 * @param {DotContentDriveFilters} filters The filters to seed.
 * @return {*} {DotContentDriveFilters} The filters, with the shared-assets key guaranteed.
 */
export function withDefaultSharedAssets(filters: DotContentDriveFilters): DotContentDriveFilters {
    if (filters?.[SHARED_ASSETS_FILTER_KEY]) {
        return filters;
    }

    return { ...filters, [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_ENABLED_VALUE };
}

/**
 * Whether any filter is set to something other than its default.
 *
 * Not the same question as "are there filters at all": the seeded defaults — the environment language
 * and the shared-assets toggle — are always present, so counting keys would answer yes on a drive
 * nobody has filtered. Consumers use this to decide whether there is anything worth offering to
 * clear.
 *
 * A filter explicitly set to its default value counts as default, which is deliberate: selecting the
 * default language by hand is indistinguishable from the seeded state, and clearing it would just
 * re-select the same thing.
 *
 * @param {DotContentDriveFilters} filters The filters to inspect.
 * @param {number} [defaultLanguageId] The environment's default language id, when known.
 * @return {*} {boolean} True when at least one filter differs from its default.
 */
export function hasNonDefaultFilters(
    filters: DotContentDriveFilters,
    defaultLanguageId?: number
): boolean {
    return Object.entries(filters ?? {}).some(([key, value]) => {
        if (key === SHARED_ASSETS_FILTER_KEY) {
            return value !== SHARED_ASSETS_ENABLED_VALUE;
        }

        if (key === 'languageId') {
            const languages = Array.isArray(value) ? value : [value];

            return !(
                defaultLanguageId &&
                languages.length === 1 &&
                languages[0] === String(defaultLanguageId)
            );
        }

        return true;
    });
}

/**
 * Applies every filter default in one pass, for the paths that build a filter set from scratch or
 * from the URL: init, "Clear all", removing a single filter, and history restore. Keeping them
 * together is what stops one of those paths from quietly missing a default.
 *
 * @param {DotContentDriveFilters} filters The filters to seed.
 * @param {number} [defaultLanguageId] The environment's default language id, when known.
 * @return {*} {DotContentDriveFilters} The filters, with defaults applied.
 */
export function withFilterDefaults(
    filters: DotContentDriveFilters,
    defaultLanguageId?: number
): DotContentDriveFilters {
    return withDefaultSharedAssets(withDefaultLanguage(filters, defaultLanguageId));
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

// The `us.*` value layer moved to `@dotcms/ui` with the field-filter chips: both surfaces build
// the same request from the same bag, so the reshaping has to be one implementation rather than two
// that can drift. Re-exported here so this portlet's own importers — the store's request builder,
// the URL decode layer and their specs — keep their imports.
export {
    buildUserSearchablePayload,
    getUserSearchableActive,
    isBinaryCheckboxField,
    isDateFieldFilterType,
    isMultiValueFieldFilterType,
    parseMultiValue,
    parseUserSearchableValue,
    serializeMultiValue,
    serializeUserSearchableValue,
    toLocalIsoString
} from '@dotcms/ui';

/**
 * Whether the user may add children to a drop target.
 *
 * The one rule behind every creation affordance in the drive — the New menu, Upload, the grid drop
 * zone, and a drag onto a tree folder — so the four cannot disagree about the same folder.
 *
 * A node with no permissions is the site root: its parent is the host rather than a folder, and no
 * folder endpoint reports on it, so `siteCanAddChildren` answers that case. Both unknowns resolve to
 * **allowed** — a lookup still in flight, and an instance too old to report the field — because
 * denying on an unknown takes the action away from users who hold the permission, and the server
 * still refuses what it enforces.
 *
 * Note what the server actually enforces, since the gate is not uniformly a preview of it: creating
 * a folder checks this (`FolderAPIImpl:673`) and so does moving a contentlet
 * (`ESContentletAPIImpl:607`), but the contentlet checkin path does **not**, so an upload is not
 * refused server-side. The gate is still applied there, so that one route into a folder does not
 * quietly allow what the other two forbid.
 *
 * @param {DotFolderTreeNodeData} [target] - The folder being dropped on or browsed
 * @param {boolean} [siteCanAddChildren] - The site-level answer, for the root
 * @returns {boolean} Whether creation should be offered
 */
export function canAddChildrenTo(
    target: DotFolderTreeNodeData | undefined | null,
    siteCanAddChildren: boolean | undefined
): boolean {
    if (!target) {
        return true;
    }

    const permissions = (target as { permissions?: string[] }).permissions;

    if (!permissions?.length) {
        return siteCanAddChildren !== false;
    }

    return permissions.includes(PERMISSIONS_TYPE.CAN_ADD_CHILDREN);
}
