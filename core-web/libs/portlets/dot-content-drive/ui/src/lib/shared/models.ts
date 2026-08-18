import type { TreeNode } from 'primeng/api';

import type {
    PermissionType,
    TreeNodeContentData,
    TreeNodeLoadMoreData
} from '@dotcms/dotcms-models';

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

/**
 * The table's fixed columns, by field. A closed set so `visibleColumns` and the body's per-cell
 * checks are compiler-checked against `HEADER_COLUMNS` — a typo used to render nothing at all.
 * Caller-provided extra columns are not part of this; their fields are arbitrary.
 */
export type DotFolderListViewColumnField =
    | 'title'
    | 'live'
    | 'languageId'
    | 'contentType'
    | 'modUser'
    | 'modDate'
    | 'actions';

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
    /**
     * Absent when nothing is selected — a drop can happen before any folder has been chosen, and
     * `onRequestUpload` in the shell already branches on it (`targetFolder && ...`).
     */
    targetFolder?: DotFolderTreeNodeData;
}

/**
 * @export
 * @interface DotContentDriveMoveItems
 * @description Move items
 */
// Declared directly rather than as `Omit<DotContentDriveUploadFiles, 'files'>`: a move always has
// a destination, whereas the upload payload it used to derive from allows one to be absent.
export type DotContentDriveMoveItems = { targetFolder: DotFolderTreeNodeData };

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
    /**
     * Fields below back the shared folder context menu and the "Edit folder" dialog, so a
     * right-click on a tree node can gate and pre-populate without a refetch. Populated from
     * `GET /api/v1/folder/search`; optional because other node sources (e.g. the synthetic
     * "All folders" root) do not carry them.
     */
    name?: string;
    title?: string;
    sortOrder?: number;
    filesMasks?: string;
    defaultFileType?: string;
    showOnMenu?: boolean;
    /**
     * Permission types the user holds on this folder. Every source that builds a folder node
     * requests them — expand, load-more and the deep-link hierarchy load alike — so in practice
     * this is populated and an empty array means the user holds none. Optional only because a
     * folder can still arrive without it from a source that did not opt in, in which case gating
     * degrades to "no actions" rather than throwing.
     */
    permissions?: PermissionType[];
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

/**
 * @export
 * @interface DotContentDriveTreeRightClick
 * @description Right-click on a folder row in the sidebar tree. Carries the original event (the
 * shared context menu anchors itself to it) and the folder the row renders.
 *
 * Folder data rather than the `TreeNode`: the tree reads the clicked row straight from the DOM, as
 * its drag-and-drop already does, instead of searching its own input for a matching node. That
 * keeps the component presentational, and the data is all a consumer needs to act on the folder.
 */
export interface DotContentDriveTreeRightClick {
    event: MouseEvent;
    data: DotFolderTreeNodeContentData;
}
