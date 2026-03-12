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
 *
 * `type` values:
 * - `'site'`        — top-level site root node (the "ALL FOLDERS" banner entry)
 * - `'folder'`      — regular folder within a site
 * - `'nested-host'` — a sub-site that is rendered inside the parent site's folder tree;
 *                     `hostname` holds the sub-site's own hostname and `path` is always `'/'`
 */
export type DotFolderTreeNodeData = {
    type: 'site' | 'folder' | 'nested-host';
    path: string;
    hostname: string;
    id: string;
    fromTable?: boolean;
};

/**
 * @export
 * @type DotFolderTreeNodeItem
 * @description Tree node item
 */
export type DotFolderTreeNodeItem = TreeNode<DotFolderTreeNodeData>;
