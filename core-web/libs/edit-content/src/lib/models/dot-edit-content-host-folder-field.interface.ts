import { TreeNode } from 'primeng/api';

export type TreeNodeData = {
    type: 'site' | 'folder';
    path: string;
    hostname: string;
    id: string;
};

export type TreeNodeItem = TreeNode<TreeNodeData>;

export type TreeNodeSelectItem = TreeNodeSelectEvent<TreeNodeData>;

export type CustomTreeNode = {
    node: null | TreeNodeItem;
    tree: {
        path: string;
        folders: TreeNodeItem[];
    } | null;
};

export interface TreeNodeSelectEvent<T> {
    originalEvent: Event;
    node: TreeNode<T>;
}

export interface DotFolder {
    id: string;
    hostName: string;
    path: string;
    addChildrenAllowed: boolean;
}
