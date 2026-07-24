import type { TreeNode } from 'primeng/api';

/**
 * @export
 * @interface DotFolderListViewColumn
 * @description Column configuration for the folder list view
 */
export interface DotFolderListViewColumn {
    field: string;
    header: string;
    width: string;
    sortable?: boolean;
    order: number;
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
    /**
     * Folder upload preference (`DOTASSET`/`FILEASSET`, or `null`/absent for "ask each time").
     * Drives the folder-aware Upload button in the toolbar.
     */
    defaultBaseType?: string | null;
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
