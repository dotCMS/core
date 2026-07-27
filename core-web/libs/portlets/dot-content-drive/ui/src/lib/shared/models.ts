import type { TreeNode } from 'primeng/api';

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
    TIME: 'time'
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
 * @export
 * @interface DotFolderTreeNodeData
 * @description Tree node data
 */
export type DotFolderTreeNodeData = {
    type: 'site' | 'folder' | 'load-more';
    path: string;
    hostname: string;
    id: string;
    /** Folder inode — carried so the legacy content editor can pre-select this folder when creating content. */
    inode?: string;
    fromTable?: boolean;
    /** For a `load-more` node: the next 1-based page to request when it is clicked. */
    nextPage?: number;
    /** For a `load-more` node: how many folders remain to be loaded in the level. */
    remaining?: number;
};

/**
 * @export
 * @type DotFolderTreeNodeItem
 * @description Tree node item
 */
export type DotFolderTreeNodeItem = TreeNode<DotFolderTreeNodeData>;
