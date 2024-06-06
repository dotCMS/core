import { TreeNode } from 'primeng/api';

export type TreeNodeData = { type: 'site' | 'folder'; path: string; hostname: string };

export type TreeNodeItem = TreeNode<TreeNodeData>;

export type TreeNodeSelectItem = TreeNodeSelectEvent<TreeNodeData>;

export interface TreeNodeSelectEvent<T> {
    originalEvent: Event;
    node: TreeNode<T>;
}

export interface DotFolder {
    hostName: string;
    path: string;
    addChildrenAllowed: boolean;
}
