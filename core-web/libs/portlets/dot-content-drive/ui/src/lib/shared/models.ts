import type { TreeNode } from 'primeng/api';

import type { TreeNodeContentData, TreeNodeLoadMoreData } from '@dotcms/dotcms-models';

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
