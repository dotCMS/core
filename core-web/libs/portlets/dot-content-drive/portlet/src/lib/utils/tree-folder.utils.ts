import { DotFolder } from '@dotcms/dotcms-models';
import { DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';

import { BuildTreeFolderNodesParams } from '../shared/models';

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
 * Transforms a DotFolder into a TreeNodeItem.
 *
 * When {@link DotFolder.isHost} is {@code true} the folder entry actually represents a nested
 * host (sub-site).  In that case the tree node is created with:
 * - `data.type   = 'nested-host'`
 * - `data.hostname` = the nested host's own hostname (so expanding the node loads its root
 *   folders via `//hostname/`)
 * - `data.path   = '/'` (root of the nested host)
 * - `label`      = the hostname string (the {@link FolderNamePipe} returns it unchanged)
 *
 * @param {DotFolder} folder - The folder (or nested-host entry) to transform
 * @returns {DotFolderTreeNodeItem} The tree node item
 */
export const createTreeNode = (
    folder: DotFolder,
    parent?: DotFolderTreeNodeItem
): DotFolderTreeNodeItem => {
    const isNestedHost = folder.isHost === true;

    let node: DotFolderTreeNodeItem = {
        key: folder.id,
        // For nested hosts use the hostname as label so the FolderNamePipe returns it as-is.
        // For regular folders the FolderNamePipe extracts the last path segment from path.
        label: isNestedHost ? folder.hostName : folder.path,
        data: {
            id: folder.id,
            hostname: folder.hostName,
            // For nested hosts, path is always '/' so expanding loads the host root folders.
            path: isNestedHost ? '/' : folder.path,
            type: isNestedHost ? 'nested-host' : 'folder'
        },
        leaf: false
    };

    if (parent) {
        node = { parent, ...node };
    }

    return node;
};

/**
 * Builds the tree folder nodes
 *
 * @param {DotFolder[][]} folderHierarchyLevels - The folder hierarchy levels
 * @param {string} targetPath - The target path
 * @returns {DotFolderTreeNodeItem[]} The tree folder nodes
 * @returns {DotFolderTreeNodeItem} The selected node
 */
export const buildTreeFolderNodes = ({
    folderHierarchyLevels,
    targetPath,
    rootNode
}: BuildTreeFolderNodesParams): {
    rootNodes: DotFolderTreeNodeItem[];
    selectedNode: DotFolderTreeNodeItem;
} => {
    if (folderHierarchyLevels.length === 0) {
        return { rootNodes: [], selectedNode: rootNode };
    }

    const rootNodes: DotFolderTreeNodeItem[] = [];
    const expectedPaths = generateAllParentPaths(targetPath);
    const activeParents: Record<number, DotFolderTreeNodeItem> = {};

    /**
     * Checks if a folder node belongs to the active target path
     */
    const isOnTargetPath = (levelIndex: number, node: DotFolderTreeNodeItem) =>
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
    const selectedNode = activeParents[folderHierarchyLevels.length - 1] || rootNode;

    return { rootNodes, selectedNode };
};
