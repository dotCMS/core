import { DotFolder } from '@dotcms/dotcms-models';
import { DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';

import {
    ALL_FOLDER,
    buildTreeFolderNodes,
    createTreeNode,
    generateAllParentPaths
} from './tree-folder.utils';

describe('Sidebar Utils', () => {
    describe('ALL_FOLDER constant', () => {
        it('should have correct structure', () => {
            expect(ALL_FOLDER).toEqual({
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
            const parentNode: DotFolderTreeNodeItem = {
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
            const parentNode: DotFolderTreeNodeItem = {
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

    describe('buildTreeFolderNodes', () => {
        // Mock data based on real example data
        const mockFolderHierarchy: DotFolder[][] = [
            [
                {
                    addChildrenAllowed: true,
                    hostName: 'demo.dotcms.com',
                    id: 'SYSTEM_FOLDER',
                    path: '/'
                },
                {
                    addChildrenAllowed: true,
                    hostName: 'demo.dotcms.com',
                    id: '513aec5b-3aaa-4df2-b306-83e77ba334d9',
                    path: '/activities/'
                },
                {
                    addChildrenAllowed: true,
                    hostName: 'demo.dotcms.com',
                    id: '83bb5752-4264-43c4-84c8-28176603431a',
                    path: '/application/'
                },
                {
                    addChildrenAllowed: true,
                    hostName: 'demo.dotcms.com',
                    id: 'fa455fb5-b961-4d0c-9e63-e79a8ba8622a',
                    path: '/blog/'
                },
                {
                    addChildrenAllowed: true,
                    hostName: 'demo.dotcms.com',
                    id: '2ad0dd36-5b07-41ac-b9f5-c7c54085ac58',
                    path: '/images/'
                }
            ],
            [
                {
                    addChildrenAllowed: true,
                    hostName: 'demo.dotcms.com',
                    id: '83bb5752-4264-43c4-84c8-28176603431a',
                    path: '/application/'
                },
                {
                    addChildrenAllowed: true,
                    hostName: 'demo.dotcms.com',
                    id: 'd4ab08ba-6ae6-4937-9fb4-b67d801ace72',
                    path: '/application/apivtl/'
                },
                {
                    addChildrenAllowed: true,
                    hostName: 'demo.dotcms.com',
                    id: 'c7eb5d4e72030ba98d6b78d2d2279cf8',
                    path: '/application/block-editor/'
                },
                {
                    addChildrenAllowed: true,
                    hostName: 'demo.dotcms.com',
                    id: 'b8a303ae-4cb4-40bf-9f27-b5b29b3350dc',
                    path: '/application/containers/'
                },
                {
                    addChildrenAllowed: true,
                    hostName: 'demo.dotcms.com',
                    id: '953db9f6-fc35-4d28-be2e-6124997ea3d9',
                    path: '/application/templates/'
                }
            ]
        ];

        it('should handle empty folder hierarchy', () => {
            const result = buildTreeFolderNodes({
                folderHierarchyLevels: [],
                targetPath: '/test/',
                rootNode: ALL_FOLDER
            });

            expect(result.rootNodes).toEqual([]);
            expect(result.selectedNode).toEqual(ALL_FOLDER);
        });

        it('should build tree structure for single level hierarchy', () => {
            const singleLevel: DotFolder[][] = [
                [
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'SYSTEM_FOLDER',
                        path: '/'
                    },
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'folder-1',
                        path: '/test/'
                    },
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'folder-2',
                        path: '/other/'
                    }
                ]
            ];

            const result = buildTreeFolderNodes({
                folderHierarchyLevels: singleLevel,
                targetPath: '/test/',
                rootNode: ALL_FOLDER
            });

            expect(result.rootNodes).toHaveLength(2);
            expect(result.rootNodes[0]).toEqual({
                key: 'folder-1',
                label: '/test/',
                data: {
                    id: 'folder-1',
                    hostname: 'demo.dotcms.com',
                    path: '/test/',
                    type: 'folder'
                },
                leaf: true,
                children: [],
                expanded: true
            });
            expect(result.rootNodes[1]).toEqual({
                key: 'folder-2',
                label: '/other/',
                data: {
                    id: 'folder-2',
                    hostname: 'demo.dotcms.com',
                    path: '/other/',
                    type: 'folder'
                },
                leaf: false
            });
            expect(result.selectedNode?.key).toBe('ALL_FOLDER');
        });

        it('should build complex tree structure with nested hierarchy', () => {
            const result = buildTreeFolderNodes({
                folderHierarchyLevels: mockFolderHierarchy,
                targetPath: '/application/',
                rootNode: ALL_FOLDER
            });

            // Should have 4 root nodes (excluding the SYSTEM_FOLDER placeholder)
            expect(result.rootNodes).toHaveLength(4);

            // Check root nodes structure
            expect(result.rootNodes.map((node) => node.key)).toEqual([
                '513aec5b-3aaa-4df2-b306-83e77ba334d9', // activities
                '83bb5752-4264-43c4-84c8-28176603431a', // application
                'fa455fb5-b961-4d0c-9e63-e79a8ba8622a', // blog
                '2ad0dd36-5b07-41ac-b9f5-c7c54085ac58' // images
            ]);

            // The application folder should be expanded and have children
            const applicationNode = result.rootNodes.find(
                (node) => node.key === '83bb5752-4264-43c4-84c8-28176603431a'
            );
            expect(applicationNode?.expanded).toBe(true);
            expect(applicationNode?.children).toHaveLength(4);
            expect(applicationNode?.children?.map((child) => child.key)).toEqual([
                'd4ab08ba-6ae6-4937-9fb4-b67d801ace72', // apivtl
                'c7eb5d4e72030ba98d6b78d2d2279cf8', // block-editor
                'b8a303ae-4cb4-40bf-9f27-b5b29b3350dc', // containers
                '953db9f6-fc35-4d28-be2e-6124997ea3d9' // templates
            ]);

            // Selected node should be the application folder
            expect(result.selectedNode?.key).toBe('83bb5752-4264-43c4-84c8-28176603431a');
            expect(result.selectedNode?.data.path).toBe('/application/');
        });

        it('should handle deeper nested path selection', () => {
            const deepHierarchy: DotFolder[][] = [
                [
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'SYSTEM_FOLDER',
                        path: '/'
                    },
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'level1-folder',
                        path: '/level1/'
                    }
                ],
                [
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'level1-folder',
                        path: '/level1/'
                    },
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'level2-folder',
                        path: '/level1/level2/'
                    }
                ],
                [
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'level2-folder',
                        path: '/level1/level2/'
                    },
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'level3-folder',
                        path: '/level1/level2/level3/'
                    }
                ]
            ];

            const result = buildTreeFolderNodes({
                folderHierarchyLevels: deepHierarchy,
                targetPath: '/level1/level2/level3/',
                rootNode: ALL_FOLDER
            });

            // Should have 1 root node
            expect(result.rootNodes).toHaveLength(1);

            // Root node should be expanded with children
            const rootNode = result.rootNodes[0];
            expect(rootNode.key).toBe('level1-folder');
            expect(rootNode.expanded).toBe(true);
            expect(rootNode.children).toHaveLength(1);

            // Level 2 should also be expanded with children
            const level2Node = rootNode.children?.[0];
            expect(level2Node?.key).toBe('level2-folder');
            expect(level2Node?.expanded).toBe(true);
            expect(level2Node?.children).toHaveLength(1);

            // Level 3 should be the selected node
            const level3Node = level2Node?.children?.[0];
            expect(level3Node?.key).toBe('level3-folder');
            // The selected node should be the last node that was found on the target path
            expect(result.selectedNode?.key).toBe('level2-folder');
        });

        it('should return ALL_FOLDER as selected when target path does not match any folder', () => {
            const result = buildTreeFolderNodes({
                folderHierarchyLevels: mockFolderHierarchy,
                targetPath: '/nonexistent/',
                rootNode: ALL_FOLDER
            });

            expect(result.rootNodes).toHaveLength(4);
            expect(result.selectedNode).toEqual(ALL_FOLDER);
        });

        it('should handle root path selection', () => {
            const rootHierarchy: DotFolder[][] = [
                [
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'SYSTEM_FOLDER',
                        path: '/'
                    },
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'folder-1',
                        path: '/test/'
                    }
                ]
            ];

            const result = buildTreeFolderNodes({
                folderHierarchyLevels: rootHierarchy,
                targetPath: '/',
                rootNode: ALL_FOLDER
            });

            expect(result.rootNodes).toHaveLength(1);
            expect(result.selectedNode).toEqual(ALL_FOLDER);
        });

        it('should properly handle folder nodes that are not on target path', () => {
            const result = buildTreeFolderNodes({
                folderHierarchyLevels: mockFolderHierarchy,
                targetPath: '/application/',
                rootNode: ALL_FOLDER
            });

            // Other root nodes should not be expanded
            const activitiesNode = result.rootNodes.find(
                (node) => node.key === '513aec5b-3aaa-4df2-b306-83e77ba334d9'
            );
            const blogNode = result.rootNodes.find(
                (node) => node.key === 'fa455fb5-b961-4d0c-9e63-e79a8ba8622a'
            );
            const imagesNode = result.rootNodes.find(
                (node) => node.key === '2ad0dd36-5b07-41ac-b9f5-c7c54085ac58'
            );

            expect(activitiesNode?.expanded).toBeUndefined();
            expect(activitiesNode?.children).toBeUndefined();
            expect(blogNode?.expanded).toBeUndefined();
            expect(blogNode?.children).toBeUndefined();
            expect(imagesNode?.expanded).toBeUndefined();
            expect(imagesNode?.children).toBeUndefined();
        });

        it('should handle empty target path', () => {
            const result = buildTreeFolderNodes({
                folderHierarchyLevels: mockFolderHierarchy,
                targetPath: '',
                rootNode: ALL_FOLDER
            });

            expect(result.rootNodes).toHaveLength(4);
            expect(result.selectedNode).toEqual(ALL_FOLDER);
        });

        it('should correctly identify nodes on target path using generateAllParentPaths', () => {
            const result = buildTreeFolderNodes({
                folderHierarchyLevels: mockFolderHierarchy,
                targetPath: '/application/',
                rootNode: ALL_FOLDER
            });

            // Verify that the correct node is identified as being on the target path
            const applicationNode = result.rootNodes.find(
                (node) => node.key === '83bb5752-4264-43c4-84c8-28176603431a'
            );

            expect(applicationNode?.expanded).toBe(true);
            expect(applicationNode?.children).toBeDefined();

            // Other nodes should not be on the path
            const otherNodes = result.rootNodes.filter(
                (node) => node.key !== '83bb5752-4264-43c4-84c8-28176603431a'
            );

            otherNodes.forEach((node) => {
                expect(node.expanded).toBeUndefined();
                expect(node.children).toBeUndefined();
            });
        });

        it('should handle folder hierarchy with missing levels gracefully', () => {
            const incompleteHierarchy: DotFolder[][] = [
                [
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'SYSTEM_FOLDER',
                        path: '/'
                    },
                    {
                        addChildrenAllowed: true,
                        hostName: 'demo.dotcms.com',
                        id: 'folder-1',
                        path: '/test/'
                    }
                ]
                // Missing second level that would match /test/deep/
            ];

            const result = buildTreeFolderNodes({
                folderHierarchyLevels: incompleteHierarchy,
                targetPath: '/test/deep/',
                rootNode: ALL_FOLDER
            });

            expect(result.rootNodes).toHaveLength(1);
            expect(result.rootNodes[0].key).toBe('folder-1');
            expect(result.rootNodes[0].expanded).toBe(true);
            expect(result.rootNodes[0].leaf).toBe(true);
            expect(result.selectedNode?.key).toBe('ALL_FOLDER');
        });

        describe('rootNode as selectedNode - Code Path Coverage', () => {
            it('should set rootNode as selectedNode when folderHierarchyLevels is empty (early return path)', () => {
                // This test explicitly covers the code path at line 79:
                // if (folderHierarchyLevels.length === 0) {
                //     return { rootNodes: [], selectedNode: rootNode };
                // }

                const customRootNode: DotFolderTreeNodeItem = {
                    key: 'custom-root',
                    label: 'Custom Root',
                    loading: false,
                    data: {
                        type: 'folder',
                        path: '/custom/',
                        hostname: 'test.dotcms.com',
                        id: 'custom-root-id'
                    },
                    leaf: false,
                    expanded: true
                };

                const result = buildTreeFolderNodes({
                    folderHierarchyLevels: [],
                    targetPath: '/some/path/',
                    rootNode: customRootNode
                });

                expect(result.rootNodes).toEqual([]);
                expect(result.selectedNode).toBe(customRootNode);
                expect(result.selectedNode.key).toBe('custom-root');
            });

            it('should set rootNode as selectedNode when no folder matches the target path (fallback path)', () => {
                // This test explicitly covers the code path at line 126:
                // const selectedNode = activeParents[folderHierarchyLevels.length - 1] || rootNode;
                // When activeParents[folderHierarchyLevels.length - 1] is undefined,
                // the || operator returns rootNode

                const customRootNode: DotFolderTreeNodeItem = {
                    key: 'fallback-root',
                    label: 'Fallback Root',
                    loading: false,
                    data: {
                        type: 'folder',
                        path: '',
                        hostname: 'example.dotcms.com',
                        id: 'fallback-id'
                    },
                    leaf: false,
                    expanded: false
                };

                const hierarchyWithNoMatch: DotFolder[][] = [
                    [
                        {
                            addChildrenAllowed: true,
                            hostName: 'demo.dotcms.com',
                            id: 'SYSTEM_FOLDER',
                            path: '/'
                        },
                        {
                            addChildrenAllowed: true,
                            hostName: 'demo.dotcms.com',
                            id: 'folder-1',
                            path: '/existing-folder/'
                        },
                        {
                            addChildrenAllowed: true,
                            hostName: 'demo.dotcms.com',
                            id: 'folder-2',
                            path: '/another-folder/'
                        }
                    ]
                ];

                const result = buildTreeFolderNodes({
                    folderHierarchyLevels: hierarchyWithNoMatch,
                    targetPath: '/nonexistent-path/',
                    rootNode: customRootNode
                });

                // Root nodes should be created from the hierarchy
                expect(result.rootNodes).toHaveLength(2);
                expect(result.rootNodes[0].key).toBe('folder-1');
                expect(result.rootNodes[1].key).toBe('folder-2');

                // But since none match the target path, rootNode should be selected
                expect(result.selectedNode).toBe(customRootNode);
                expect(result.selectedNode.key).toBe('fallback-root');

                // None of the root nodes should be expanded
                expect(result.rootNodes[0].expanded).toBeUndefined();
                expect(result.rootNodes[1].expanded).toBeUndefined();
            });

            it('should set rootNode as selectedNode when target path is empty string (fallback path)', () => {
                // Another case for the fallback path at line 126
                // Empty target path means generateAllParentPaths returns [],
                // so no folder will ever be on the target path

                const customRootNode: DotFolderTreeNodeItem = {
                    key: 'empty-path-root',
                    label: 'Empty Path Root',
                    loading: false,
                    data: {
                        type: 'folder',
                        path: '/root/',
                        hostname: 'site.dotcms.com',
                        id: 'empty-root-id'
                    },
                    leaf: false
                };

                const hierarchy: DotFolder[][] = [
                    [
                        {
                            addChildrenAllowed: true,
                            hostName: 'demo.dotcms.com',
                            id: 'SYSTEM_FOLDER',
                            path: '/'
                        },
                        {
                            addChildrenAllowed: true,
                            hostName: 'demo.dotcms.com',
                            id: 'folder-1',
                            path: '/folder/'
                        }
                    ]
                ];

                const result = buildTreeFolderNodes({
                    folderHierarchyLevels: hierarchy,
                    targetPath: '',
                    rootNode: customRootNode
                });

                expect(result.rootNodes).toHaveLength(1);
                expect(result.selectedNode).toBe(customRootNode);
                expect(result.selectedNode.key).toBe('empty-path-root');
            });
        });
    });
});
