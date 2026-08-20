import { DotCMSContentlet } from './dot-contentlet.model';

/**
 * Pagination/sort/filter event shape compatible with PrimeNG's LazyLoadEvent.
 * Defined locally so dotcms-models does not depend on primeng; consumers can pass
 * a real LazyLoadEvent from p-table/p-dataView.
 */
export interface DotContentDriveLazyLoadEvent {
    first?: number;
    last?: number;
    rows?: number;
    sortField?: string;
    sortOrder?: number;
    multiSortMeta?: Array<{ field: string; order: number }>;
    filters?: Record<string, { value?: unknown; matchMode?: string; operator?: string }>;
    globalFilter?: unknown;
    forceUpdate?: () => void;
}

/**
 * The folder fields required to drive the shared folder actions — the context menu's permission
 * gating and the "Edit folder" dialog's payload.
 *
 * Deliberately narrower than {@link DotContentDriveFolder}: the table sources folders from
 * `POST /api/v1/drive/search` (a full folder row), while the sidebar tree sources them from
 * `GET /api/v1/folder/search`, which returns only the fields listed here. Rather than fabricate
 * the table-only fields (`modDate`, `owner`, `iDate`, …) for sidebar folders, both views converge
 * on this contract, and only consumers that genuinely need the full row ask for
 * `DotContentDriveFolder`.
 */
export interface DotContentDriveActionableFolder {
    type: 'folder';
    identifier: string;
    /** The folder's own name (last path segment). */
    name: string;
    /** The folder's own full path, e.g. `/application/blog/`. */
    path: string;
    title: string;
    sortOrder: number;
    showOnMenu: boolean;
    /** Comma-separated file-name masks allowed in this folder, e.g. `*.jpg,*.png`. */
    filesMasks: string;
    defaultFileType: string;
    /**
     * Folder upload preference: `DOTASSET`/`FILEASSET` forces every upload to that base type,
     * `null`/`undefined` means "ask each time" (no preference). Backed by #35577.
     */
    defaultBaseType?: string | null;
    /**
     * Permission types the requesting user holds on this folder.
     *
     * Required, and always an array by the time a folder reaches an action: whoever builds this
     * object resolves the folder's permissions first (or substitutes `[]` when they cannot be
     * resolved), so gating never runs against `null`/`undefined`. The "not yet resolved" state
     * lives upstream, on `DotFolder.permissions` / the tree node's data.
     */
    permissions: PermissionType[];
}

export interface DotContentDriveFolder extends DotContentDriveActionableFolder {
    __icon__: 'folderIcon';
    description: string;
    extension: 'folder';
    hasTitleImage: boolean;
    hostId: string;
    iDate: number;
    inode: string;
    mimeType: string;
    modDate: number;
    owner: string | null;
    parent: string;
}

export const PERMISSIONS_TYPE = {
    READ: 'READ',
    EDIT: 'EDIT',
    PUBLISH: 'PUBLISH',
    EDIT_PERMISSIONS: 'EDIT_PERMISSIONS',
    CAN_ADD_CHILDREN: 'CAN_ADD_CHILDREN'
} as const;

export type PermissionType = (typeof PERMISSIONS_TYPE)[keyof typeof PERMISSIONS_TYPE];

// This will extend the DotCMSContentlet with more properties,
// but for now we will just use the DotCMSContentlet until we have folders on the request response
export type DotContentDriveItem = DotCMSContentlet | DotContentDriveFolder;

/**
 * An item the shared folder actions (context menu, Edit-folder dialog) can act on.
 *
 * Wider than {@link DotContentDriveItem} on the folder side: the table passes a full
 * {@link DotContentDriveFolder} and the sidebar tree passes a {@link DotContentDriveActionableFolder},
 * so one gating implementation serves both. Every `DotContentDriveItem` is assignable to this.
 */
export type DotContentDriveActionableItem = DotCMSContentlet | DotContentDriveActionableFolder;

/**
 * Pagination event emitted by the folder list view,
 * extending the lazy-load event shape with a resolved 1-indexed page number.
 */
export type DotContentDrivePaginateEvent = DotContentDriveLazyLoadEvent & { page: number };

/**
 * Interface representing data needed for context menu interactions
 * @interface ContextMenuData
 * @property {Event} event - The DOM event that triggered the context menu
 * @property {DotContentDriveActionableItem} contentlet - The item associated with the context menu.
 * Accepts folders from either the table (full row) or the sidebar tree (search view).
 */
export interface ContextMenuData {
    event: Event;
    contentlet: DotContentDriveActionableItem;
}

/**
 * Query filters for text-based content filtering.
 * Provides Elasticsearch-powered text search capabilities.
 */
export interface DotContentDriveQueryFilters {
    /**
     * By default, we filter folders. When text is provided but we can always override this.
     * @default true
     */
    filterFolders?: boolean;

    /**
     * Text to search for.
     */
    text: string;
}

/**
 * Request body for the /api/v1/drive/search endpoint.
 *
 * @example
 * // Basic folder browsing
 * {
 *   assetPath: "//demo.dotcms.com/documents/"
 * }
 *
 * @example
 * // Search with filtering and pagination
 * {
 *   assetPath: "//demo.dotcms.com/",
 *   filters: { text: "product review" },
 *   contentTypes: ["Blog", "News"],
 *   sortBy: "title:asc",
 *   offset: 0,
 *   maxResults: 20
 * }
 */
export interface DotContentDriveSearchRequest {
    /**
     * The path to the asset/folder to browse.
     * Supports dotCMS path format: //sitename/folder/subfolder/
     *
     * @example "//demo.dotcms.com/"
     * @example "//demo.dotcms.com/documents/"
     */
    assetPath: string;

    /**
     * Whether to include system host content in the results.
     * @default true
     */
    includeSystemHost?: boolean;

    /**
     * List of language identifiers to include in the search.
     * Supports both language codes (e.g., "en", "es") and language IDs.
     * @default system default language
     */
    language?: string[];

    /**
     * List of specific content type identifiers or variable names to include.
     * Can use either content type IDs or variable names (e.g., "Blog", "News", "webPageContent").
     */
    contentTypes?: string[];

    /**
     * List of base content types to include in the search.
     * Available values: "CONTENT", "FILEASSET", "DOTASSET", "HTMLPAGE", "PERSONA", "FORM"
     */
    baseTypes?: string[];

    /**
     * List of MIME types to filter file assets.
     * @example ["image/jpeg", "image/png", "image/gif"]
     * @example ["application/pdf"]
     */
    mimeTypes?: string[];

    /**
     * Search filters for text-based content filtering.
     * Provides Elasticsearch-powered text search capabilities.
     */
    filters?: DotContentDriveQueryFilters;

    /**
     * Number of content items to skip for pagination.
     * @default 0
     */
    contentCursor?: number;

    /**
     * Number of folder items to skip for pagination.
     * @default 0
     */
    folderCursor?: number;

    /**
     * Maximum number of results to return.
     * @default 2000
     */
    maxResults?: number;

    /**
     * Field and direction for sorting results.
     * Format: "fieldName:direction" where direction is "asc" or "desc".
     * Supported fields: "title", "modDate", "modUser", "sortOrder", "name"
     * @default "modDate"
     * @example "title:asc"
     * @example "modDate:desc"
     */
    sortBy?: string;

    /**
     * Whether to include only live (published) content.
     * @default false
     */
    live?: boolean;

    /**
     * Whether to include archived content in results.
     * @default false
     */
    archived?: boolean;

    /**
     * Whether to show folders in results.
     * @default true
     */
    showFolders?: boolean;

    /**
     * Workflow filter entries. Each entry is one workflow scheme, optionally pinned to a
     * single step (omit `step` to match the whole scheme). Entries combine with OR.
     * @example [{ scheme: "d61a59e1-…", step: "dc3c9cd0-…" }, { scheme: "2a4e1d2e-…" }]
     */
    workflow?: { scheme: string; step?: string }[];

    /**
     * Field-based search criteria, keyed by the content-type field variable. Only offered when a
     * single content type is selected and only for fields flagged User Searchable + System Indexed.
     * The value shape depends on the field type:
     * - text/select/radio → a single string (contains / equals)
     * - multi-select/checkbox → a list of values
     * - date/time/date-and-time → an inclusive `{ from, to }` ISO range
     *
     * @example
     * {
     *   title: "product review",
     *   category: ["news", "press"],
     *   publishDate: { from: "2024-01-01T00:00:00Z", to: "2024-12-31T23:59:59Z" }
     * }
     */
    userSearchable?: Record<string, DotContentDriveUserSearchableValue>;
}

/**
 * Inclusive date range used by date/time field-based search criteria.
 */
export interface DotContentDriveDateRange {
    from: string;
    to: string;
}

/**
 * Value shape for a single {@link DotContentDriveSearchRequest.userSearchable} entry.
 * String for text/select, string[] for multi-select and multi-option checkbox, a boolean for a
 * binary checkbox, and a range for date/time fields.
 */
export type DotContentDriveUserSearchableValue =
    | string
    | string[]
    | boolean
    | DotContentDriveDateRange;

/**
 * Response from the /api/v1/drive/search endpoint.
 * @interface DotContentDriveSearchResponse
 * @property {number} contentTotalCount - The total number of content items
 * @property {number} folderCount - The total number of folder items
 * @property {number} contentCount - The total number of content items
 * @property {DotContentDriveItem[]} list - The list of content items
 */
export interface DotContentDriveSearchResponse {
    folderCount: number;
    contentCount: number;
    list: DotContentDriveItem[];
    hasMoreContent: boolean;
    hasMoreFolders: boolean;
    nextContentCursor: number;
    nextFolderCursor: number;
}

/**
 * The `202 Accepted` body of `POST /api/v1/content/_bulkrefresh`.
 *
 * Reindexing a selection is job-backed: the submit call only accepts the work, so this carries the
 * handle to follow it rather than any outcome.
 */
export interface DotBulkRefreshSubmitResponse {
    /** The job's id — the handle for the status and cancel calls. */
    jobId: string;
    /** Absolute URL of the status snapshot. Poll it until the job reaches a terminal state. */
    statusUrl: string;
    /**
     * Inodes accepted, before the server collapses them by identifier. The de-duplicated `total`
     * arrives with the result and is often smaller, so this is not a count of reindexed items.
     */
    submitted: number;
}

/** Every state a job can report. Mirrors the backend `JobState` enum. */
export const DOT_BULK_REFRESH_JOB_STATES = [
    'PENDING',
    'RUNNING',
    'CANCEL_REQUESTED',
    'CANCELLING',
    'SUCCESS',
    'CANCELED',
    'FAILED',
    'FAILED_PERMANENTLY',
    'ABANDONED',
    'ABANDONED_PERMANENTLY'
] as const;

export type DotBulkRefreshJobState = (typeof DOT_BULK_REFRESH_JOB_STATES)[number];

/** States the job will not move on from. Anything else means it is still going. */
export const DOT_BULK_REFRESH_TERMINAL_STATES = [
    'SUCCESS',
    'CANCELED',
    'FAILED_PERMANENTLY',
    'ABANDONED_PERMANENTLY'
] as const;

/**
 * Terminal states whose counters describe work that actually happened.
 *
 * `CANCELED` belongs here: the run stopped early but every item is still accounted for, so the
 * counters are reportable. `FAILED_PERMANENTLY` and `ABANDONED_PERMANENTLY` do not — the job died
 * mid-run, so its counters describe only how far it got and cannot be read as an outcome.
 */
export const DOT_BULK_REFRESH_REPORTABLE_STATES = ['SUCCESS', 'CANCELED'] as const;

/**
 * Counters a finished bulk refresh reports.
 *
 * `successCount + failedCount + skippedCount === total` in every terminal state, which is what lets a
 * caller know it can stop waiting and settle every row it asked about.
 */
export interface DotBulkRefreshCounts {
    /** Unique identifiers reindexed, after de-duplication — not the submitted inode count. */
    total: number;
    successCount: number;
    failedCount: number;
    /** Never attempted, because the run was cancelled before reaching them. */
    skippedCount: number;
    /** Index writes across the whole run; higher than `total` when content has several versions. */
    versionsIndexed: number;
}

/** `GET /api/v1/content/_bulkrefresh/{jobId}` — the job as the status endpoint reports it. */
export interface DotBulkRefreshJob {
    id: string;
    state: DotBulkRefreshJobState;
    /** 0.0–1.0. The only progress signal available while the job runs. */
    progress: number;
    /**
     * Present once the job is terminal.
     *
     * The counters sit **directly** on this object, not under a `metadata` key: the server's
     * `OptionalJobResultSerializer` flattens the job's metadata map straight into `result`. The Java
     * accessor really is `JobResult.metadata()`, which makes `result.metadata` a tempting but wrong
     * path over HTTP — it is always `undefined`.
     */
    result?: DotBulkRefreshCounts & {
        /** Written by the same serializer when the job carried an error detail. */
        error?: { code: string; message: string }[];
    };
}

/**
 * A settled bulk refresh: the state it settled in, plus its counters.
 *
 * State is carried alongside the counters rather than discarded, because counters alone cannot say
 * whether they describe a finished run or one that died partway — and an all-zero result from a
 * failed job is indistinguishable from a clean run over nothing.
 */
export interface DotBulkRefreshOutcome {
    state: DotBulkRefreshJobState;
    counts: DotBulkRefreshCounts | null;
}
