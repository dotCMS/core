import { TreeNode } from 'primeng/api';

import { DotFolder } from '@dotcms/dotcms-models';

export type TreeNodeData = {
    type: 'site' | 'folder';
    path: string;
    hostname: string;
    id: string;
};

export type TreeNodeItem = TreeNode<TreeNodeData>;

export const ALL_FOLDER: TreeNodeItem = {
    key: 'ALL_FOLDER',
    label: 'Root',
    loading: false,
    data: {
        type: 'folder',
        path: '',
        hostname: '',
        id: ''
    },
    leaf: false,
    expanded: true
};

/**
 * Generates all parent paths from a target path
 *
 * Example:
 * '/path1/path2/path3/' → ['/path1/', '/path1/path2/', '/path1/path2/path3/']
 */
export const generateAllParentPaths = (path: string): string[] => {
    const segments = path.split('/').filter(Boolean);
    const paths: string[] = [];

    let current = '';
    for (const segment of segments) {
        current += `/${segment}`;
        paths.push(current + '/');
    }

    return paths;
};

/**
 * Transforms a DotFolder into a TreeNodeItem
 *
 * @param {DotFolder} folder - The folder to transform
 * @returns {TreeNodeItem} The tree node item
 */
export const createTreeNode = (folder: DotFolder, parent?: TreeNodeItem): TreeNodeItem => {
    let node: TreeNodeItem = {
        key: folder.id,
        label: folder.path,
        data: {
            id: folder.id,
            hostname: folder.hostName,
            path: folder.path,
            type: 'folder'
        },
        leaf: false
    };

    if (parent) {
        node = { parent, ...node };
    }

    return node;
};

/**
 * Extracts all expanded node keys from a tree structure
 */
export const extractExpandedNodeKeys = (nodes: TreeNodeItem[]): string[] => {
    const expandedKeys: string[] = [];

    const traverse = (nodeList: TreeNodeItem[]) => {
        nodeList.forEach(node => {
            if (node.expanded && node.key) {
                expandedKeys.push(node.key);
            }
            if (node.children) {
                traverse(node.children);
            }
        });
    };

    traverse(nodes);
    return expandedKeys;
};

/**
 * Finds a node in the tree by its key
 */
export const findNodeByKey = (nodes: TreeNodeItem[], key: string): TreeNodeItem | null => {
    for (const node of nodes) {
        if (node.key === key) {
            return node;
        }
        if (node.children) {
            const found = findNodeByKey(node.children, key);
            if (found) return found;
        }
    }
    return null;
};

/**
 * Merges new folders into existing tree structure while preserving state
 */
export const mergeTreeFolderNodes = (
    existingNodes: TreeNodeItem[],
    folderHierarchyLevels: DotFolder[][],
    targetPath: string,
    expandedNodeKeys: string[] = []
): { rootNodes: TreeNodeItem[]; selectedNode?: TreeNodeItem } => {
    if (folderHierarchyLevels.length === 0) {
        return { rootNodes: existingNodes, selectedNode: ALL_FOLDER };
    }

    // Create a map of existing nodes for quick lookup
    const existingNodesMap = new Map<string, TreeNodeItem>();
    const addToMap = (nodeList: TreeNodeItem[]) => {
        nodeList.forEach(node => {
            if (node.key) {
                existingNodesMap.set(node.key, node);
            }
            if (node.children) {
                addToMap(node.children);
            }
        });
    };
    addToMap(existingNodes);

    const rootNodes: TreeNodeItem[] = [...existingNodes];
    const expectedPaths = generateAllParentPaths(targetPath);
    const activeParents: Record<number, TreeNodeItem> = {};

    /**
     * Checks if a folder node belongs to the active target path
     */
    const isOnTargetPath = (levelIndex: number, node: TreeNodeItem) =>
        expectedPaths[levelIndex] === node.data.path;

    /**
     * Checks if a folder node is a leaf
     */
    const isLeaf = (levelIndex: number) => folderHierarchyLevels.length >= levelIndex + 1;

    folderHierarchyLevels.forEach((levelFolders, levelIndex) => {
        // Each level starts with a placeholder parent we don't render
        const [, ...folders] = levelFolders;
        const parentNode = activeParents[levelIndex];

        folders.forEach((folder) => {
            let node = existingNodesMap.get(folder.id);

            // If node doesn't exist, create it
            if (!node) {
                node = createTreeNode(folder);

                // Add to appropriate parent
                if (levelIndex === 0) {
                    rootNodes.push(node);
                } else if (parentNode) {
                    parentNode.children = parentNode.children || [];
                    parentNode.children.push(node);
                }
            } else {
                // Update existing node data but preserve state
                node.data = {
                    id: folder.id,
                    hostname: folder.hostName,
                    path: folder.path,
                    type: 'folder'
                };
                node.label = folder.path;

                // Restore expanded state if it was previously expanded
                if (node.key && expandedNodeKeys.includes(node.key)) {
                    node.expanded = true;
                }
            }

            // If this node is along the target path, mark it as active parent for the next level
            if (isOnTargetPath(levelIndex, node)) {
                activeParents[levelIndex + 1] = node;
                node.children = node.children || [];
                node.expanded = true;
                node.leaf = isLeaf(levelIndex);
            }
        });
    });

    // The last expanded parent is the "selected" node
    const selectedNode = activeParents[folderHierarchyLevels.length - 1] || ALL_FOLDER;

    return { rootNodes, selectedNode };
};

/**
 * Builds the tree folder nodes (legacy function - kept for compatibility)
 *
 * @param {DotFolder[][]} folderHierarchyLevels - The folder hierarchy levels
 * @param {string} targetPath - The target path
 * @returns {TreeNodeItem[]} The tree folder nodes
 * @returns {TreeNodeItem} The selected node
 */
export const buildTreeFolderNodes = (
    folderHierarchyLevels: DotFolder[][],
    targetPath: string
): { rootNodes: TreeNodeItem[]; selectedNode?: TreeNodeItem } => {
    if (folderHierarchyLevels.length === 0) {
        return { rootNodes: [], selectedNode: ALL_FOLDER };
    }

    const rootNodes: TreeNodeItem[] = [];
    const expectedPaths = generateAllParentPaths(targetPath);
    const activeParents: Record<number, TreeNodeItem> = {};

    /**
     * Checks if a folder node belongs to the active target path
     */
    const isOnTargetPath = (levelIndex: number, node: TreeNodeItem) =>
        expectedPaths[levelIndex] === node.data.path;

    /**
     * Checks if a folder node is a leaf
     */
    const isLeaf = (levelIndex: number) => folderHierarchyLevels.length >= levelIndex + 1;

    folderHierarchyLevels.forEach((levelFolders, levelIndex) => {

        // Each level starts with a placeholder parent we don't render
        const [, ...folders] = levelFolders;
        const parentNode = activeParents[levelIndex];

        folders.forEach((folder) => {
            const node = createTreeNode(folder);

            // Root level nodes are added directly
            if (levelIndex === 0) {
                rootNodes.push(node);
            }
            // Deeper levels get attached to the active parent
            else if (parentNode) {
                parentNode.children = parentNode.children || [];
                parentNode.children.push(node);
            }

            // If this node is along the target path, mark it as active parent for the next level
            if (isOnTargetPath(levelIndex, node)) {
                activeParents[levelIndex + 1] = node;
                node.children = [];
                node.expanded = true;
                node.leaf = isLeaf(levelIndex);
            }
        });
    });

    // The last expanded parent is the "selected" node
    const selectedNode = activeParents[folderHierarchyLevels.length - 1] || ALL_FOLDER;

    return { rootNodes, selectedNode };
};
