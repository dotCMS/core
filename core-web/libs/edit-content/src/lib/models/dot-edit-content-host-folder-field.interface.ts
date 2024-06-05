import { TreeNode } from 'primeng/api';

import { Site } from '@dotcms/dotcms-js';

export type TreeSiteData = Site & { type: 'site'; path: string };

export type TreeFolderData = DotFolder & { type: 'folder'; path: string };

export type TreeDataItems = TreeSiteData | TreeFolderData;

export type TreeNodeItem = TreeNode<TreeDataItems>;

export type TreeNodeSelectItem = TreeNodeSelectEvent<TreeDataItems>;

export interface TreeNodeSelectEvent<T> {
    originalEvent: Event;
    node: TreeNode<T>;
}

export interface DotFolder {
    hostName: string;
    path: string;
    addChildrenAllowed: boolean;
}
