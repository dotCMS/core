import type { TreeNode } from 'primeng/api';

import { DotFolder } from './dot-folder.model';

/**
 * Data payload for tree nodes in the browser selector.
 * Represents either a site (host) or a folder within the content tree.
 *
 * @type TreeNodeData
 * @property {'site' | 'folder'} type - Whether this node represents a site or a folder
 * @property {string} path - The full path of the node in the content tree
 * @property {string} hostname - The hostname where the site/folder resides
 * @property {string} id - Unique identifier for the node
 */
export type TreeNodeData = {
    type: 'site' | 'folder';
    path: string;
    hostname: string;
    id: string;
};

/**
 * PrimeNG tree node wrapping TreeNodeData.
 * Used for rendering the site/folder browser tree structure.
 *
 * @type TreeNodeItem
 */
export type TreeNodeItem = TreeNode<TreeNodeData>;

/**
 * Event payload when a tree node is selected in the browser selector.
 *
 * @type TreeNodeSelectItem
 */
export type TreeNodeSelectItem = TreeNodeSelectEvent<TreeNodeData>;

/**
 * Custom tree node structure with optional parent context and child folders.
 * Used when navigating or expanding nodes in the browser selector.
 *
 * @type CustomTreeNode
 * @property {TreeNodeItem | null} node - The selected tree node, or null if none
 * @property {object | null} tree - Tree context with parent folder and children, or null
 * @property {DotFolder} [tree.parent] - Parent folder of the current tree level
 * @property {string} tree.path - Path of the current tree level
 * @property {TreeNodeItem[]} tree.folders - Child folder nodes at this level
 */
export type CustomTreeNode = {
    node: null | TreeNodeItem;
    tree: {
        parent?: DotFolder;
        path: string;
        folders: TreeNodeItem[];
    } | null;
};

/**
 * Generic event emitted when a tree node is selected.
 *
 * @interface TreeNodeSelectEvent
 * @template T - The type of data held by the tree node
 * @property {Event} originalEvent - The original DOM event that triggered the selection
 * @property {TreeNode<T>} node - The selected tree node with its data
 */
export interface TreeNodeSelectEvent<T> {
    originalEvent: Event;
    node: TreeNode<T>;
}
