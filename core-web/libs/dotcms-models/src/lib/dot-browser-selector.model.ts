import { TreeNode } from 'primeng/api';

import { DotFolder } from './dot-folder.model';

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
        parent?: DotFolder;
        path: string;
        folders: TreeNodeItem[];
    } | null;
};

export interface TreeNodeSelectEvent<T> {
    originalEvent: Event;
    node: TreeNode<T>;
}
