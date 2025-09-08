import { DotFolder } from '@dotcms/data-access';

import {
    ALL_FOLDER,
    createTreeNode,
    generateAllParentPaths,
    TreeNodeItem
} from './tree-folder.utils';

describe('Sidebar Utils', () => {
    describe('ALL_FOLDER constant', () => {
        it('should have correct structure', () => {
            expect(ALL_FOLDER).toEqual({
                key: 'ALL_FOLDER',
                label: 'ROOT',
                loading: false,
                data: {
                    type: 'folder',
                    path: '',
                    hostname: '',
                    id: ''
                },
                leaf: false,
                expanded: true
            });
        });

        it('should be a folder type', () => {
            expect(ALL_FOLDER.data.type).toBe('folder');
        });

        it('should be expanded by default', () => {
            expect(ALL_FOLDER.expanded).toBe(true);
        });

        it('should not be a leaf node', () => {
            expect(ALL_FOLDER.leaf).toBe(false);
        });
    });

    describe('generateAllParentPaths', () => {
        it('should generate parent paths for a simple path', () => {
            const result = generateAllParentPaths('/folder1/');
            expect(result).toEqual(['/folder1/']);
        });

        it('should generate parent paths for nested folders', () => {
            const result = generateAllParentPaths('/folder1/folder2/folder3/');
            expect(result).toEqual(['/folder1/', '/folder1/folder2/', '/folder1/folder2/folder3/']);
        });

        it('should handle paths without trailing slash', () => {
            const result = generateAllParentPaths('/folder1/folder2');
            expect(result).toEqual(['/folder1/', '/folder1/folder2/']);
        });

        it('should handle empty path', () => {
            const result = generateAllParentPaths('');
            expect(result).toEqual([]);
        });

        it('should handle single slash', () => {
            const result = generateAllParentPaths('/');
            expect(result).toEqual([]);
        });

        it('should handle path with multiple consecutive slashes', () => {
            const result = generateAllParentPaths('/folder1//folder2/');
            expect(result).toEqual(['/folder1/', '/folder1/folder2/']);
        });

        it('should handle complex nested path', () => {
            const result = generateAllParentPaths('/path1/path2/path3/');
            expect(result).toEqual(['/path1/', '/path1/path2/', '/path1/path2/path3/']);
        });

        it('should handle path with special characters', () => {
            const result = generateAllParentPaths('/folder-1/folder_2/folder.3/');
            expect(result).toEqual([
                '/folder-1/',
                '/folder-1/folder_2/',
                '/folder-1/folder_2/folder.3/'
            ]);
        });
    });

    describe('createTreeNode', () => {
        const mockFolder: DotFolder = {
            id: 'folder-123',
            path: '/documents/',
            hostName: 'demo.dotcms.com',
            addChildrenAllowed: true
        };

        it('should create a tree node without parent', () => {
            const result = createTreeNode(mockFolder);

            expect(result).toEqual({
                key: 'folder-123',
                label: '/documents/',
                data: {
                    id: 'folder-123',
                    hostname: 'demo.dotcms.com',
                    path: '/documents/',
                    type: 'folder'
                },
                leaf: false
            });
        });

        it('should create a tree node with parent', () => {
            const parentNode: TreeNodeItem = {
                key: 'parent-123',
                label: 'Parent',
                data: {
                    id: 'parent-123',
                    hostname: 'demo.dotcms.com',
                    path: '/parent/',
                    type: 'folder'
                },
                leaf: false
            };

            const result = createTreeNode(mockFolder, parentNode);

            expect(result).toEqual({
                parent: parentNode,
                key: 'folder-123',
                label: '/documents/',
                data: {
                    id: 'folder-123',
                    hostname: 'demo.dotcms.com',
                    path: '/documents/',
                    type: 'folder'
                },
                leaf: false
            });
        });

        it('should always set leaf to false', () => {
            const result = createTreeNode(mockFolder);
            expect(result.leaf).toBe(false);
        });

        it('should use folder id as key', () => {
            const result = createTreeNode(mockFolder);
            expect(result.key).toBe(mockFolder.id);
        });

        it('should use folder path as label', () => {
            const result = createTreeNode(mockFolder);
            expect(result.label).toBe(mockFolder.path);
        });

        it('should set correct data properties', () => {
            const result = createTreeNode(mockFolder);

            expect(result.data).toEqual({
                id: mockFolder.id,
                hostname: mockFolder.hostName,
                path: mockFolder.path,
                type: 'folder'
            });
        });

        it('should handle folder with different hostname', () => {
            const folderWithDifferentHost: DotFolder = {
                ...mockFolder,
                hostName: 'other.dotcms.com'
            };

            const result = createTreeNode(folderWithDifferentHost);

            expect(result.data.hostname).toBe('other.dotcms.com');
        });

        it('should handle folder with empty path', () => {
            const folderWithEmptyPath: DotFolder = {
                ...mockFolder,
                path: ''
            };

            const result = createTreeNode(folderWithEmptyPath);

            expect(result.label).toBe('');
            expect(result.data.path).toBe('');
        });

        it('should maintain parent reference correctly', () => {
            const parentNode: TreeNodeItem = {
                key: 'parent-456',
                label: 'Parent Folder',
                data: {
                    id: 'parent-456',
                    hostname: 'demo.dotcms.com',
                    path: '/parent/',
                    type: 'folder'
                },
                leaf: false
            };

            const result = createTreeNode(mockFolder, parentNode);

            expect(result.parent).toBe(parentNode);
            expect(result.parent?.key).toBe('parent-456');
        });
    });
});
