import {
    DotCMSContentTypeField,
    DotContentDriveFolder,
    DotContentDriveItem,
    DotFolder,
    DotSite
} from '@dotcms/dotcms-models';
import { DotFolderTreeNodeData, DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';
import { DotUVEPaletteListTypes } from '@dotcms/portlets/dot-ema/ui';

import { DIALOG_TYPE, UPLOAD_SELECTOR_OPTIONS } from './constants';

/**
 * Base types the upload selector can produce, derived from the selector options so the type and the
 * rendered choices never drift apart.
 */
export type DotContentDriveUploadBaseType = (typeof UPLOAD_SELECTOR_OPTIONS)[number]['baseType'];

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
    triggeredEvent: Event;
    contentlet: DotContentDriveItem;
    showAddToBundle: boolean;
}

export interface DotContentDriveDialog {
    type: keyof typeof DIALOG_TYPE;
    header: string;
    payload?:
        | DotContentDriveFolder
        | DotContentDriveContentTypeSelectorPayload
        | DotContentDriveUploadSelectorPayload;
}

/**
 * Payload for the content-type selector dialog: the palette list type that
 * encodes which base type(s) to show (e.g. ALL_CONTENT_TYPES or a single base type).
 */
export interface DotContentDriveContentTypeSelectorPayload {
    listType: DotUVEPaletteListTypes;
}

/**
 * Payload passed INTO the upload-type selector dialog. `files` is present for the drag-and-drop
 * flow (the dropped files are already known) and absent for the Upload-button flow (the OS file
 * picker opens after the user picks a type).
 */
export interface DotContentDriveUploadSelectorPayload {
    targetFolder?: DotFolderTreeNodeData;
    files?: FileList;
}

/**
 * Object emitted BACK by the upload-type selector dialog. Carries everything needed to trigger the
 * upload (and, in the future, to remember the chosen type per folder — see epic #35436).
 * `targetFolder` is omitted when nothing is selected (uploads to the site root).
 */
export interface DotContentDriveUploadSelection {
    baseType: DotContentDriveUploadBaseType;
    targetFolder?: DotFolderTreeNodeData;
    files?: FileList;
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

/**
 * The parameters for the buildTreeFolderNodes function.
 *
 * @export
 * @interface buildTreeFolderNodesParams
 */
export interface BuildTreeFolderNodesParams {
    folderHierarchyLevels: DotFolder[][];
    targetPath: string;
    rootNode: DotFolderTreeNodeItem;
}
