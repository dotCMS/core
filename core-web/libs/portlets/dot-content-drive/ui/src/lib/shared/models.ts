import type { TreeNode } from 'primeng/api';

import type { TreeNodeContentData, TreeNodeLoadMoreData } from '@dotcms/dotcms-models';

/**
 * @export
 * @interface DotFolderListViewColumn
 * @description Column configuration for the folder list view
 */
/**
 * Generic display types for a column's cell value. Kept agnostic of any domain field system so the
 * table can format and size values (dates, booleans, numbers) without knowing where the column came
 * from. Callers map their own field/data types onto these.
 */
export const DOT_FOLDER_LIST_VIEW_COLUMN_TYPE = {
    TEXT: 'text',
    NUMBER: 'number',
    BOOLEAN: 'boolean',
    DATE: 'date',
    DATETIME: 'datetime',
    TIME: 'time',
    /** Image/binary/file field: renders the field's own asset as a thumbnail. */
    IMAGE: 'image'
} as const;

export type DotFolderListViewColumnType =
    (typeof DOT_FOLDER_LIST_VIEW_COLUMN_TYPE)[keyof typeof DOT_FOLDER_LIST_VIEW_COLUMN_TYPE];

export interface DotFolderListViewColumn {
    field: string;
    header: string;
    /**
     * Explicit width (any CSS length). Optional for caller-provided extra columns: when omitted the
     * table sizes the column itself — by content for text/number, by a sensible default per type
     * otherwise. Fixed columns set it explicitly.
     */
    width?: string;
    sortable?: boolean;
    order: number;
    /** How the cell value is rendered and sized. Defaults to `text` when omitted. */
    type?: DotFolderListViewColumnType;
}

/**
 * @export
 * @interface DotContentDriveUploadFiles
 * @description File and host folder for the drop zone
 */
export interface DotContentDriveUploadFiles {
    files: FileList;
    targetFolder: DotFolderTreeNodeData;
}

/**
 * @export
 * @interface DotContentDriveMoveItems
 * @description Move items
 */
export type DotContentDriveMoveItems = Omit<DotContentDriveUploadFiles, 'files'>;

/**
 * Content Drive site/folder node data — shared content fields plus drive-specific extras.
 */
export type DotFolderTreeNodeContentData = TreeNodeContentData & {
    /** Folder inode — carried so the legacy content editor can pre-select this folder when creating content. */
    inode?: string;
    /**
     * Folder upload preference (`DOTASSET`/`FILEASSET`, or `null`/absent for "ask each time").
     * Drives the folder-aware Upload button in the toolbar.
     */
    defaultBaseType?: string | null;
    fromTable?: boolean;
};

/**
 * @export
 * @interface DotFolderTreeNodeData
 * @description Discriminated tree node data (content vs load-more).
 */
export type DotFolderTreeNodeData = DotFolderTreeNodeContentData | TreeNodeLoadMoreData;

/**
 * @export
 * @type DotFolderTreeNodeItem
 * @description Tree node item
 */
export type DotFolderTreeNodeItem = TreeNode<DotFolderTreeNodeData>;
