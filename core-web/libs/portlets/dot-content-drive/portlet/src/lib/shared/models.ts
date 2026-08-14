import { BuildTreeFolderNodesParams as SharedBuildTreeFolderNodesParams } from '@dotcms/data-access';
import {
    DotCMSContentTypeField,
    DotContentDriveActionableFolder,
    DotContentDriveActionableItem,
    DotContentDriveItem,
    DotSite
} from '@dotcms/dotcms-models';
import { DotUVEPaletteListTypes } from '@dotcms/portlets/dot-ema/ui';
import { DotUploadBaseType, DotUploadSelection, DotUploadSelectorPayload } from '@dotcms/ui';

import { DIALOG_TYPE } from './constants';

/** @deprecated Import {@link BuildTreeFolderNodesParams} from `@dotcms/data-access` instead. */
export type BuildTreeFolderNodesParams = SharedBuildTreeFolderNodesParams;

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
