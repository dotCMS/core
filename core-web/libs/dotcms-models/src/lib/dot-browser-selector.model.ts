import type { TreeNode } from 'primeng/api';

import { DotFolder } from './dot-folder.model';

/**
 * Data payload for tree nodes in the browser selector and shared folder tree.
 * Represents a site (host), a folder, or a synthetic load-more sentinel.
 *
 * @type TreeNodeData
 * @property {'site' | 'folder' | 'load-more'} type - Node kind in the content tree
 * @property {string} path - The full path of the node in the content tree
 * @property {string} hostname - The hostname where the site/folder resides
 * @property {string} id - Unique identifier for the node
 * @property {number} [nextPage] - For load-more nodes: next 1-based page to request
 * @property {number} [remaining] - For load-more nodes: folders still remaining at the level
 */
export type TreeNodeData = {
    type: 'site' | 'folder' | 'load-more';
    path: string;
    hostname: string;
    id: string;
    nextPage?: number;
    remaining?: number;
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
 * Per-level pagination metadata returned by {@link CustomTreeNode} when a
 * preselected path is resolved across one or more pages of siblings.
 *
 * @type TreeLevelPagination
 * @property {number} page - Last page that was loaded for this level (1-based)
 * @property {boolean} hasMore - Whether additional pages remain beyond `page`
 */
export type TreeLevelPagination = {
    page: number;
    hasMore: boolean;
};

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
 * @property {Record<string, TreeLevelPagination>} [pagination] - Per-level page/hasMore
 *   state keyed by node key (`'root'` for the site root). Present when the tree was
 *   built by paginating until a preselected path was found.
 */
export type CustomTreeNode = {
    node: null | TreeNodeItem;
    tree: {
        parent?: DotFolder;
        path: string;
        folders: TreeNodeItem[];
    } | null;
    pagination?: Record<string, TreeLevelPagination>;
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
