/**
 * Data contracts for {@link DotPagesBrowserService}: browsing folders, listing pages and
 * reading a page's lock state. All shapes are UI-agnostic — mapping to PrimeNG `TreeNode`
 * or table rows is the consumer's responsibility.
 */

/**
 * Publication state of a page version, derived from the version flags returned by
 * `GET /api/v1/page/search` (`archived`, `live`, `working`, `hasLiveVersion`).
 */
export const DotPageBrowserState = {
    /** The page version is archived. */
    ARCHIVED: 'ARCHIVED',
    /** Live version with no pending working changes. */
    PUBLISHED: 'PUBLISHED',
    /** There is a live version plus newer, unpublished working changes. */
    CHANGED: 'CHANGED',
    /** Working version that was never published. */
    DRAFT: 'DRAFT'
} as const;

export type DotPageBrowserState = (typeof DotPageBrowserState)[keyof typeof DotPageBrowserState];

/**
 * A folder available for browsing, with its full site-relative path already resolved.
 *
 * `GET /api/v1/folder/search` reports the folder's own `name` and its *parent* `path`
 * separately; this shape exposes the joined path so callers can query pages under it
 * without rebuilding it.
 */
export interface DotPageBrowserFolder {
    /** Folder identifier. */
    id: string;
    /** Folder inode. */
    inode: string;
    /** Folder name, without any path segment. */
    name: string;
    /** Full site-relative path of the folder, always leading and trailing slashed (e.g. `/about-us/`). */
    path: string;
    /** Hostname of the site the folder belongs to. */
    hostname: string;
    /** Whether the folder has at least one child folder visible to the current user. */
    hasChildren: boolean;
}

/** A page of folder children plus the pagination metadata needed for "load more" behavior. */
export interface DotPageBrowserFolderChildren {
    folders: DotPageBrowserFolder[];
    /** Total children available for the requested level, so callers can decide whether to page again. */
    totalFolders: number;
    /** Page that produced `folders` (1-based). */
    page: number;
    /** Page size used for the request. */
    perPage: number;
}

/** Scope of a folder-children request. */
export interface DotPageBrowserFolderParams {
    /** Site identifier to scope the search. */
    siteId: string;
    /** Hostname of `siteId`; the folder-search endpoint does not return it, so callers must supply it. */
    hostname: string;
    /** Parent folder path whose direct children are requested. Defaults to the site root. */
    path?: string;
    /** Page number (1-based). */
    page?: number;
    /** Page size. */
    perPage?: number;
}

/** Filters for a page listing request. */
export interface DotPagesBrowserSearchParams {
    /**
     * Hostname (not the site identifier) to scope the search — `GET /api/v1/page/search`
     * filters by site through the `//hostname/...` path prefix, it has no `hostId` parameter.
     */
    hostname?: string;
    /** Folder path to list, e.g. `/about-us/`. Defaults to the site root. */
    path?: string;
    /**
     * Case-insensitive term matched against each row's title and url. The endpoint has no
     * free-text parameter, so this is applied to the returned rows.
     */
    query?: string;
    /** When true, only live versions are returned. Defaults to false so drafts are included. */
    live?: boolean;
    /** When true, pages living on non-live sites are filtered out. Defaults to false. */
    onlyLiveSites?: boolean;
}

/** A page row ready to be rendered in a page-picker table. */
export interface DotPageBrowserPage {
    /** Page identifier — the value experiments use as `pageId`. */
    identifier: string;
    /** Inode of the returned version. */
    inode: string;
    /** Page title, falling back to the page url when the title is empty. */
    title: string;
    /** Page url within its folder, e.g. `/index`. */
    url: string;
    /** Full site-relative path of the page, e.g. `/about-us/index`. */
    path: string;
    /** Hostname of the site that owns the page. */
    hostname: string;
    /** Identifier of the site that owns the page. */
    hostId: string;
    /**
     * Template *identifier*. `GET /api/v1/page/search` returns the raw `template` field of the
     * page contentlet, so no template name is available from this call.
     */
    templateId: string;
    /** Last modification date, as serialized by the endpoint. */
    modDate: string;
    /** Language of the returned version. */
    languageId: number;
    /** Publication state derived from the version flags. */
    state: DotPageBrowserState;
}

/**
 * Lock state of a page, with no comparison against the current user performed.
 *
 * Callers that need `lockedByAnotherUser` must compare {@link lockedBy} with the current
 * user id (e.g. `DotCurrentUser.userId` from `DotCurrentUserService`, or `LoginService`),
 * replicating `DotPageRenderState`'s formula.
 */
export interface DotPageLockInfo {
    /** True when the page currently holds a lock. */
    locked: boolean;
    /** User id holding the lock, absent when the page is unlocked. */
    lockedBy?: string;
    /** Display name of the user holding the lock, when the endpoint reports it. */
    lockedByName?: string;
}

/**
 * Raw page contentlet as serialized by `GET /api/v1/page/search` and `POST /api/es/search`
 * (both go through `ContentletUtil.getContentPrintableMap`, plus `PageViewStrategy` for pages).
 * Exposed for test fixtures; production code should consume the mapped shapes above.
 */
export interface DotPageBrowserContentlet {
    identifier: string;
    inode: string;
    title?: string;
    url?: string;
    path?: string;
    host: string;
    hostName: string;
    template?: string;
    modDate?: string;
    languageId: number;
    live?: boolean;
    working?: boolean;
    archived?: boolean;
    locked?: boolean;
    hasLiveVersion?: boolean;
    /**
     * Present only while the page is locked. `PageViewStrategy` writes the plain user id for
     * pages, while the default contentlet strategy writes a `{ userId, firstName, lastName }`
     * object — both shapes are handled when reading.
     */
    lockedBy?: string | { userId: string; firstName?: string; lastName?: string };
    lockedByName?: string;
}
