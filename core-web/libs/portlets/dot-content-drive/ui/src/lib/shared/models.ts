import { TreeNode } from 'primeng/api';

import { DotContentDriveItem } from '@dotcms/dotcms-models';

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
    targetFolderId: string;
}

/**
 * @export
 * @interface DotContentDriveMoveItems
 * @description Move items
 */
export interface DotContentDriveMoveItems extends Omit<DotContentDriveUploadFiles, 'files'> {
    items: DotContentDriveItem[];
}

/**
 * @export
 * @interface DotFolderTreeNodeData
 * @description Tree node data
 */
export type DotFolderTreeNodeData = {
    type: 'site' | 'folder';
    path: string;
    hostname: string;
    id: string;
};

/**
 * @export
 * @type DotFolderTreeNodeItem
 * @description Tree node item
 */
export type DotFolderTreeNodeItem = TreeNode<DotFolderTreeNodeData>;
