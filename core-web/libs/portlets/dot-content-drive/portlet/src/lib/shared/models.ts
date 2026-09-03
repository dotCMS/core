import {
    DotCMSContentTypeField,
    DotContentDriveActionableFolder,
    DotContentDriveActionableItem,
    DotContentDriveItem,
    DotFolder,
    DotLanguage,
    DotSite
} from '@dotcms/dotcms-models';
import { DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';
import { DotUVEPaletteListTypes } from '@dotcms/portlets/dot-ema/ui';
import { DotUploadBaseType, DotUploadSelection, DotUploadSelectorPayload } from '@dotcms/ui';

import { DIALOG_TYPE } from './constants';

/**
 * The parameters for the buildTreeFolderNodes function.
 *
 * @export
 * @interface BuildTreeFolderNodesParams
 */
export interface BuildTreeFolderNodesParams {
    folderHierarchyLevels: DotFolder[][];
    targetPath: string;
    rootNode: DotFolderTreeNodeItem;
}

/**
 * Upload-flow types now live in `@dotcms/ui`, shared with the AssetPicker. Aliased here so the
 * portlet keeps its own naming.
 */
export type DotContentDriveUploadBaseType = DotUploadBaseType;
export type DotContentDriveUploadSelectorPayload = DotUploadSelectorPayload;
export type DotContentDriveUploadSelection = DotUploadSelection;

/**
 * The status of the content drive.
 *
 * @export
 * @enum {string}
 */
export enum DotContentDriveStatus {
    LOADING = 'loading',
    LOADED = 'loaded',
    ERROR = 'error'
}

/**
 * The sort order of the content drive.
 *
 * @export
 * @enum {string}
 */
export enum DotContentDriveSortOrder {
    ASC = 'asc',
    DESC = 'desc'
}

/**
 * The pagination of the content drive.
 *
 * @export
 * @interface DotContentDrivePagination
 */
export interface DotContentDrivePagination {
    limit: number;
    page: number;
    offset: number;
}

/**
 * The sort of the content drive.
 *
 * @export
 * @interface DotContentDriveSort
 */
export interface DotContentDriveSort {
    field: string;
    order: DotContentDriveSortOrder;
}

/**
 * The init of the content drive.
 *
 * @export
 * @interface DotContentDriveInit
 */
export interface DotContentDriveInit {
    currentSite: DotSite;
    path: string;
    filters: DotContentDriveFilters;
    isTreeExpanded: boolean;
}

/**
 * The context menu data for the content drive.
 *
 * @export
 * @interface DotContentDriveContextMenu
 */
export interface DotContentDriveContextMenu {
    triggeredEvent: Event | null;
    /**
     * The item the menu acts on. Folders arrive from two sources — a full row from the table and a
     * search view from the sidebar tree — so this is the narrower actionable shape both satisfy.
     */
    contentlet: DotContentDriveActionableItem | null;
    showAddToBundle: boolean;
}

/**
 * Header override for a dialog that has drilled into a sub-screen.
 *
 * The shared dialog's header lives in the shell, but a dialog body can navigate within itself — the
 * Workflow Center drilling from its action list into an action's preview. Rather than the body
 * rendering a second title (which reads as a duplicated header), it publishes the replacement here
 * and the shell's one header renders it.
 */
export interface DotContentDriveDialogDrillDown {
    /** Replaces the dialog title. Already-resolved text, not an i18n key. */
    header: string;
    /** Number of items the sub-screen is about to act on; rendered as the header's sub-line. */
    itemCount: number;
}

export interface DotContentDriveDialog {
    type: keyof typeof DIALOG_TYPE;
    header: string;
    payload?:
        | DotContentDriveActionableFolder
        | DotContentDriveContentTypeSelectorPayload
        | DotContentDriveUploadSelectorPayload;
}

/**
 * A workflow action currently being applied to the selection.
 *
 * Held in the store rather than in the Action Center dialog so it survives the dialog being closed:
 * the run continues, the toolbar keeps reporting it, and reopening the dialog sees a run already in
 * progress instead of offering to fire it again.
 */
export interface DotContentDriveActionExecution {
    /** Already-resolved action label, not an i18n key — it goes straight into the indicator. */
    actionName: string;
    /** Number of contentlets the run was fired over. */
    total: number;
    /**
     * What the run is being applied to, when that is one nameable thing.
     *
     * Lets a single-item run read "Applying **Publish** to *My Page*" instead of "to 1 item(s)".
     * Like `actionName` it reaches the indicator's `[innerHTML]`, and unlike `actionName` it is
     * content the author typed, so escaping it is not optional.
     */
    targetLabel?: string;
    /**
     * How many items are done, when the run reports it.
     *
     * **Optional on purpose.** Absent means the run does not report progress, which the indicator
     * shows as activity without a position — never as zero. Progress readback is the backend's
     * largest piece of hidden work, so the indicator must degrade to indeterminate rather than
     * assume a number exists.
     */
    processed?: number;
}

/**
 * Outcome of a finished run, published for the shell to present as a toast.
 *
 * Counts come from the response, never from the number of items submitted: both endpoints answer 200
 * with per-item failures inside, so an item locked by another user would otherwise read as a success.
 */
export interface DotContentDriveActionExecutionResult {
    actionName: string;
    successCount: number;
    skippedCount: number;
    failCount: number;
    /**
     * i18n key for the partial-outcome copy, when the default does not fit.
     *
     * The default names workflow-specific causes next to each number — permissions and locks for
     * failures, "not on their workflow step" for skips. Those are the right causes for a bulk fire and
     * the wrong ones for anything else, and a shortfall explained by the wrong cause sends the user off
     * to fix something that was never the problem. An action whose failures and skips mean something
     * different supplies its own copy rather than borrowing that one.
     */
    partialDetailKey?: string;
    /**
     * Whether this outcome arrived unprompted, from a job that finished in the background.
     *
     * Every other result settles a request the user is waiting on, so closing the dialog and reloading
     * the grid is the natural next step. A backgrounded one can land at any moment — minutes later,
     * while the user is filling in a different action's form — and doing either would throw away work
     * they are in the middle of.
     */
    backgrounded?: boolean;
}

/**
 * Payload for the content-type selector dialog: the palette list type that
 * encodes which base type(s) to show (e.g. ALL_CONTENT_TYPES or a single base type).
 */
export interface DotContentDriveContentTypeSelectorPayload {
    listType: DotUVEPaletteListTypes;
}

export interface DotContentDrivePage {
    hasMoreContent: boolean;
    hasMoreFolders: boolean;
    folderCursor: number;
    contentCursor: number;
    offset: number;
}

/**
 * The state of the content drive.
 *
 * @export
 * @interface DotContentDriveState
 */
export interface DotContentDriveState extends DotContentDriveInit {
    items: DotContentDriveItem[];
    selectedItems: DotContentDriveItem[];
    status: DotContentDriveStatus;
    pagination: DotContentDrivePagination;
    sort: DotContentDriveSort;
    contextMenu?: DotContentDriveContextMenu;
    pages: DotContentDrivePage[];
    /**
     * Eligible searchable fields of the currently-selected single content type. Populated by the
     * field-filter menu after fetching the content type; empty when 0 or >1 content types are
     * selected. Used to render field-filter chips and to reshape the `us.*` filter values into the
     * `userSearchable` payload.
     */
    userSearchableFields: DotCMSContentTypeField[];
    /**
     * Field variables the user has added as chips, in add order. Kept separate from `filters` so
     * adding an (empty) chip doesn't mutate the search request and re-trigger a reload; a `us.*`
     * entry only lands in `filters` once the chip has a value.
     */
    userSearchableActive: string[];
    /**
     * Whether the field metadata for the active content type has been resolved (even to an empty
     * set). Distinguishes "not fetched yet" from "fetched, none eligible" so a cold URL restore can
     * hold the first search until fields load, instead of firing one that drops the `us.*` values.
     */
    userSearchableFieldsLoaded: boolean;
    /**
     * "Show In List" fields (`field.listed`) of the currently-selected single content type.
     * Populated from the same content-type fetch as {@link userSearchableFields}; empty when 0 or
     * >1 content types are selected. Consumed by the results table as extra columns.
     */
    showInListFields: DotCMSContentTypeField[];
    /**
     * Whether the Edit Content side panel is forcing the folder tree visually collapsed on a
     * narrow viewport. Purely transient UI state — never persisted, never read from or written to
     * the URL — kept separate from {@link DotContentDriveInit.isTreeExpanded} (the user's real,
     * shareable preference) so the panel's temporary collapse can never overwrite it. See
     * `isTreeVisuallyExpanded` (the computed both should render from) and `setTreeForceCollapsed`.
     */
    isTreeForceCollapsed: boolean;
    /**
     * Every language configured in the environment, from `DotLanguagesService.get()`.
     *
     * Held here rather than fetched per consumer so the Locale filter and the default-language seed
     * share one request instead of issuing the same call twice.
     */
    languages: DotLanguage[];
    /**
     * Id of the environment's default language — the language flagged `defaultLanguage`, which is
     * NOT necessarily id 1 nor the first entry returned.
     *
     * Seeds the `languageId` filter whenever no language is selected: a cold load, a URL without a
     * language, a clear-all, or a Back/Forward restore. "No language" is not a neutral state — the
     * backend omits the language term entirely, so every language version of a contentlet comes
     * back as its own row (see `withDefaultLanguage`).
     *
     * `undefined` while the request is in flight, and if it fails or the environment declares no
     * default — see {@link defaultLanguageLoaded}.
     */
    defaultLanguageId?: number;
    /**
     * Whether the languages request has settled — to a default id, to no default, or to a failure.
     * Distinguishes "not fetched yet" from "fetched, nothing to seed", which lets the first search
     * wait for the seed instead of firing once without a language (a flash of duplicated rows) and
     * again with it. Always ends up `true`, so a failure degrades to the pre-seeding behaviour
     * rather than hanging the portlet in `LOADING`.
     */
    defaultLanguageLoaded: boolean;
    /**
     * Whether the logged-in user holds the CMS Administrator role, from
     * `DotCurrentUserService.getCurrentUser()`.
     *
     * Fetched once on portlet init rather than per consumer: it never changes within a session, and
     * the Action Center needs it the instant the dialog opens.
     *
     * `false` until the request answers, which deliberately means an unresolved flag behaves exactly
     * like a non-admin. The only consumer is the Unlock warning, whose copy says a foreign lock
     * *may* require administrator permission — so an unresolved flag over-warns rather than letting
     * a non-admin fire with no heads-up at all.
     */
    currentUserIsAdmin: boolean;
}

/**
 * The known filters of the content drive.
 *
 * @export
 * @interface DotKnownContentDriveFilters
 */
export type DotKnownContentDriveFilters = {
    baseType: string[];
    contentType: string[];
    title: string;
    languageId: string[];
    // Each entry is `schemeId` or `schemeId:stepId` (single step pinned per scheme)
    workflow: string[];
    // Content states to filter by: 'ARCHIVED' | 'UNPUBLISHED' | 'LOCKED'. Entries combine with OR,
    // like `contentType` and `languageId` — selecting more returns more. Deliberately NOT seeded by
    // `withFilterDefaults`: empty genuinely means "no status filtering", unlike `languageId` and
    // `sharedAssets` where an absent key is not a neutral state.
    status: string[];
    // `'false'` hides SYSTEM_HOST (shared) assets, `'true'` shows them. Always present: the filter is
    // seeded to `'true'` on a cold load, on "Clear all", and on a Back/Forward restore (see
    // `withFilterDefaults`), so the value is explicit in the URL rather than implied by the key's
    // absence.
    sharedAssets: string;
};

/**
 * The filters of the content drive.
 *
 * @export
 * @interface DotContentDriveFilters
 */
export type DotContentDriveFilters = Partial<DotKnownContentDriveFilters> & {
    [key: string]: string | string[];
};

/**
 * The decode function of the content drive.
 *
 * @export
 * @interface DotContentDriveDecodeFunction
 */
export type DotContentDriveDecodeFunction = (value: string) => string | string[];
