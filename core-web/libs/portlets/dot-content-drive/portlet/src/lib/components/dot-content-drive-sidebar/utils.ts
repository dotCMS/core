import { TreeNode } from 'primeng/api';

export interface DotFolder {
    id: string;
    hostName: string;
    path: string;
    addChildrenAllowed: boolean;
}

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
 * Converts: '/path1/path2/path3/' to:
 * ['/', '/path1/', '/path1/path2/', '/path1/path2/path3/']
 *
 * @param {string} targetPath - The full path (e.g., '/main/sub-folder/inner-folder/child-folder')
 * @returns {string[]} Array of paths from root to target
 */
export const generateAllParentPaths = (path: string): string[] => {
    const segments = path.split('/').filter(Boolean);
    const paths: string[] = [];

    for (let i = 0; i < segments.length; i++) {
        const path = '/' + segments.slice(0, i + 1).join('/') + '/';
        paths.push(path);
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
        label: getFolderLabel(folder.path),
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
 * Extracts the folder name from a full path
 *
 * @param {string} path - The full folder path
 * @returns {string} The folder name
 */
export const getFolderLabel = (path: string): string => {
    return path.split('/').filter(Boolean).pop() || '';
};

export const buildContentDriveFolderTree = (allLevels: DotFolder[][]): TreeNodeItem[] => {
    if (allLevels.length === 0) return [];

    const nodeMap = new Map<string, TreeNodeItem>(); // id → node
    const allChildren = new Set<string>(); // track which nodes are children

    // --- Helpers ---
    const getNode = (folder: DotFolder, parent?: TreeNodeItem): TreeNodeItem => {
        if (!nodeMap.has(folder.id)) {
            nodeMap.set(folder.id, createTreeNode(folder, parent));
        }

        const node = nodeMap.get(folder.id);
        if (!node) {
            throw new Error(`Node not found for folder ${folder.id}`);
        }
        return node;
    };

    const attachChildren = (parentNode: TreeNodeItem, childFolders: DotFolder[]) => {
        parentNode.children = parentNode.children ?? [];

        for (const folder of childFolders) {
            const childNode = getNode(folder, parentNode);
            parentNode.children.push(childNode);
            allChildren.add(childNode.key);
        }

        if (parentNode.children.length > 0) {
            parentNode.expanded = true;
        }
    };

    const processFirstLevel = (level: DotFolder[]) => {
        if (level.length <= 1) return;

        // Skip the first parent placeholder
        const [, ...rootFolders] = level;

        // Each remaining folder becomes its own root node
        rootFolders.forEach((folder) => getNode(folder));
    };

    const processLevel = (level: DotFolder[]) => {
        if (level.length === 0) return;
        const [parentFolder, ...childFolders] = level;
        const parentNode = getNode(parentFolder);
        attachChildren(parentNode, childFolders);
    };

    // --- Build the tree ---
    allLevels.forEach((level, index) => {
        if (index === 0) {
            processFirstLevel(level);
        } else {
            processLevel(level);
        }
    });

    // Roots = nodes that were never marked as children
    return [...nodeMap.values()].filter((node) => !allChildren.has(node.key));
};

// export const buildTreeFromFlatList = (folders: DotFolder[]): TreeNodeItem[] => {
//     if (folders.length === 0) return [];

//     const nodeMap = new Map<string, TreeNodeItem>();
//     const roots: TreeNodeItem[] = [];

//     const getNode = (folder: DotFolder, parent?: TreeNodeItem): TreeNodeItem => {
//         if (!nodeMap.has(folder.id)) {
//             nodeMap.set(folder.id, createTreeNode(folder, parent));
//         }
//         return nodeMap.get(folder.id)!;
//     };

//     for (const folder of folders) {
//         const parentNode = folder.parentId ? getNode({ id: folder.parentId } as DotFolder) : null;
//         const node = getNode(folder, parentNode);

//         if (parentNode) {
//             parentNode.children = parentNode.children ?? [];
//             parentNode.children.push(node);
//             parentNode.expanded = true; // expand since it has children
//         } else {
//             roots.push(node);
//         }
//     }

//     return roots;
// };
