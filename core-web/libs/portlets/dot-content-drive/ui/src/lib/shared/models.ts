import type {
    TreeNodeContentData,
    TreeNodeData,
    TreeNodeItem,
    TreeNodeLoadMoreData
} from '@dotcms/dotcms-models';

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
 * Content Drive site/folder node data — alias of shared {@link TreeNodeContentData}
 * (inode / defaultBaseType / fromTable live on the shared type).
 */
export type DotFolderTreeNodeContentData = TreeNodeContentData;

/**
 * @export
 * @interface DotFolderTreeNodeData
 * @description Discriminated tree node data (content vs load-more).
 */
export type DotFolderTreeNodeData = TreeNodeData;

/**
 * @export
 * @type DotFolderTreeNodeItem
 * @description Tree node item (alias of shared {@link TreeNodeItem}).
 */
export type DotFolderTreeNodeItem = TreeNodeItem;

/** Re-export for consumers that import load-more data via content-drive/ui. */
export type { TreeNodeLoadMoreData };
