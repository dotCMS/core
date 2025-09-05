import { TreeNode } from 'primeng/api';

import { DotFolder } from '@dotcms/data-access';

export type TreeNodeData = {
    type: 'site' | 'folder';
    path: string;
    hostname: string;
    id: string;
};

export type TreeNodeItem = TreeNode<TreeNodeData>;

export const ALL_FOLDER: TreeNodeItem = {
    key: 'ALL_FOLDER',
    label: 'All',
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

// FUNCTION GENERATE BY CHATGPT
export const buildTreeFromHierarchicalFolders = (
    folderHierarchyLevels: DotFolder[][]
): TreeNodeItem[] => {
    if (folderHierarchyLevels.length === 0) return [];

    const nodeRegistry = new Map<string, TreeNodeItem>(); // id → node
    const childNodeKeys = new Set<string>(); // track which nodes are children

    // --- Helper Functions ---
    const getOrCreateTreeNode = (folder: DotFolder, parentNode?: TreeNodeItem): TreeNodeItem => {
        if (!nodeRegistry.has(folder.id)) {
            nodeRegistry.set(folder.id, createTreeNode(folder, parentNode));
        }

        const node = nodeRegistry.get(folder.id);
        if (!node) {
            throw new Error(`Node not found for folder ${folder.id}`);
        }
        return node;
    };

    const attachChildrenToParent = (parentNode: TreeNodeItem, childFolders: DotFolder[]) => {
        parentNode.children = parentNode.children ?? [];

        for (const childFolder of childFolders) {
            const childNode = getOrCreateTreeNode(childFolder, parentNode);
            parentNode.children.push(childNode);
            childNodeKeys.add(childNode.key);
        }

        if (parentNode.children.length > 0) {
            parentNode.expanded = true;
        }
    };

    const processRootLevel = (rootLevelFolders: DotFolder[]) => {
        if (rootLevelFolders.length <= 1) return;

        // Skip the first parent placeholder
        const [, ...actualRootFolders] = rootLevelFolders;

        // Each remaining folder becomes its own root node
        actualRootFolders.forEach((folder) => getOrCreateTreeNode(folder));
    };

    const processHierarchyLevel = (levelFolders: DotFolder[]) => {
        if (levelFolders.length === 0) return;
        const [parentFolder, ...childFolders] = levelFolders;
        const parentNode = getOrCreateTreeNode(parentFolder);
        attachChildrenToParent(parentNode, childFolders);
    };

    // --- Build the tree structure ---
    folderHierarchyLevels.forEach((levelFolders, levelIndex) => {
        if (levelIndex === 0) {
            processRootLevel(levelFolders);
        } else {
            processHierarchyLevel(levelFolders);
        }
    });

    // Return root nodes (nodes that were never marked as children)
    return [...nodeRegistry.values()].filter((node) => !childNodeKeys.has(node.key));
};

// FUNCTION GENERATE BY ME
// SHOW THIS FUNCTION TO CHATGPT TO DISCUSS APPROACHES
export const buildTreeFolderNodes = (
    folderHierarchyLevels: DotFolder[][],
    targetPath: string
): { rootNodes: TreeNodeItem[]; selectedNode?: TreeNodeItem } => {
    if (folderHierarchyLevels.length === 0) return { rootNodes: [], selectedNode: ALL_FOLDER };

    const rootNodes: TreeNodeItem[] = [];
    const expectedPaths = generateAllParentPaths(targetPath);
    const parentsByLevel: Record<number, TreeNodeItem> = {};

    folderHierarchyLevels.forEach((levelFolders, levelIndex) => {
        const [, ...folders] = levelFolders; // Skip the parent placeholder
        const parentNode = parentsByLevel[levelIndex];

        folders.forEach((folder) => {
            const node = createTreeNode(folder);
            const isAParentPath = expectedPaths[levelIndex] === node.data.path;

            if (levelIndex === 0) {
                rootNodes.push(node);
            } else if (parentNode) {
                parentNode.children = parentNode.children || [];
                parentNode.children.push(node);
            }

            if (isAParentPath) {
                parentsByLevel[levelIndex + 1] = node;
                node.children = [];
                node.expanded = true;
            }
        });
    });

    // Last level parent is the selected node
    const selectedNode = parentsByLevel[folderHierarchyLevels.length - 1] || ALL_FOLDER;

    return {
        rootNodes,
        selectedNode
    };
};
