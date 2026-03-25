import {
    DotContentDriveFolder,
    DotContentDriveItem,
    DotFolder,
    DotSite
} from '@dotcms/dotcms-models';
import { DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';

import { DIALOG_TYPE } from './constants';

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
    payload?: DotContentDriveFolder;
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
