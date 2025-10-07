import { TreeNode } from 'primeng/api';

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
    targetFolder: string;
}

/**
 * @export
 * @interface TreeNodeData
 * @description Tree node data
 */
export type TreeNodeData = {
    type: 'site' | 'folder';
    path: string;
    hostname: string;
    id: string;
};

/**
 * @export
 * @type TreeNodeItem
 * @description Tree node item
 */
export type TreeNodeItem = TreeNode<TreeNodeData>;
