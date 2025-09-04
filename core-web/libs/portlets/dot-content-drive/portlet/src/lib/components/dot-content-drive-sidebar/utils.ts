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
export const createTreeNode = (folder: DotFolder): TreeNodeItem => {
    return {
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
