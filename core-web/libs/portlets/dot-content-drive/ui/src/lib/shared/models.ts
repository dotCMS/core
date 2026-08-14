import type {
    PermissionType,
    TreeNodeContentData,
    TreeNodeData,
    TreeNodeItem,
    TreeNodeLoadMoreData
} from '@dotcms/dotcms-models';
import type { DotUploadFiles } from '@dotcms/ui';

/**
 * File and host folder for the drop zone.
 * Alias of the shared {@link DotUploadFiles}, which now lives in `@dotcms/ui` with the upload kit.
 */
export type DotContentDriveUploadFiles = DotUploadFiles;

/**
 * @export
 * @interface DotContentDriveMoveItems
 * @description Move items
 */
export type DotContentDriveMoveItems = Omit<DotContentDriveUploadFiles, 'files'>;

/**
 * Content Drive site/folder node data — extends the shared browser-selector node with the
 * folder metadata Content Drive needs for context-menu gating and the edit-folder dialog.
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
export type DotFolderTreeNodeData = TreeNodeData;

/**
 * @export
 * @type DotFolderTreeNodeItem
 * @description Tree node item (alias of shared {@link TreeNodeItem}).
 */
export type DotFolderTreeNodeItem = TreeNodeItem;

/** Re-export for consumers that import load-more data via content-drive/ui. */
export type { TreeNodeLoadMoreData };

/**
 * @export
 * @interface DotContentDriveTreeRightClick
 * @description Right-click on a folder row in the sidebar tree. Carries the original event (the
 * shared context menu anchors itself to it) and the folder the row renders.
 */
export interface DotContentDriveTreeRightClick {
    event: MouseEvent;
    data: DotFolderTreeNodeContentData;
}
