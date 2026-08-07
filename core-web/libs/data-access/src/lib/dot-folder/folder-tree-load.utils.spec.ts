import { describe, expect, it } from '@jest/globals';
import { of, throwError } from 'rxjs';

import { DotPagination, FolderSearchView, isTreeNodeContentData } from '@dotcms/dotcms-models';
import { createFakeFolderSearchView, createFakeSite } from '@dotcms/utils-testing';

import { DotFolderService } from './dot-folder.service';
import {
    applyLoadMoreToHierarchy,
    buildLoadMoreNode,
    folderSearchViewToDotFolder,
    FOLDER_TREE_HIERARCHY_PAGE_SIZE,
    FOLDER_TREE_PAGE_SIZE,
    getFolderHierarchyByPath,
    getFolderNodesByPath
} from './folder-tree-load.utils';
import { createTreeNode } from './folder-tree.utils';

describe('folder-tree-load.utils', () => {
    describe('getFolderHierarchyByPath', () => {
        let mockDotFolderService: jest.Mocked<DotFolderService>;
        const SITE_ID = 'site-123';
        const HOSTNAME = 'test.com';
        const SITE = createFakeSite({ identifier: SITE_ID, hostname: HOSTNAME });

        const searchResult = (folders: FolderSearchView[]) =>
            of({ folders, pagination: {} as DotPagination });

        beforeEach(() => {
            mockDotFolderService = {
                searchFolders: jest.fn().mockReturnValue(searchResult([]))
            } as unknown as jest.Mocked<DotFolderService>;
        });

        it('should search the root and every parent path with the hierarchy page size', (done) => {
            const folderPath = '/main/sub-folder/inner-folder';

            getFolderHierarchyByPath(folderPath, SITE, mockDotFolderService).subscribe({
                next: () => {
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledTimes(4);

                    const expectedPaths = [
                        '/',
                        '/main/',
                        '/main/sub-folder/',
                        '/main/sub-folder/inner-folder/'
                    ];
                    expectedPaths.forEach((path) => {
                        expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                            expect.objectContaining({
                                siteId: SITE_ID,
                                path,
                                recursive: false,
                                page: 1,
                                per_page: FOLDER_TREE_HIERARCHY_PAGE_SIZE
                            })
                        );
                    });
                    done();
                },
                error: done
            });
        });

        it('should adapt search results into DotFolder full paths with the site hostname', (done) => {
            mockDotFolderService.searchFolders.mockReturnValueOnce(
                searchResult([
                    createFakeFolderSearchView({
                        id: 'm',
                        inode: 'im',
                        name: 'main',
                        path: '/',
                        addChildrenAllowed: true,
                        hasChildren: true
                    })
                ])
            );

            getFolderHierarchyByPath('/main', SITE, mockDotFolderService).subscribe({
                next: (levels) => {
                    expect(levels[0].folders[0]).toEqual({
                        id: 'm',
                        inode: 'im',
                        hostName: HOSTNAME,
                        path: '/main/',
                        addChildrenAllowed: true,
                        hasChildren: true
                    });
                    done();
                },
                error: done
            });
        });

        it('should query only the site root for the root path', (done) => {
            getFolderHierarchyByPath('/', SITE, mockDotFolderService).subscribe({
                next: (levels) => {
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledTimes(1);
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ path: '/' })
                    );
                    expect(levels).toHaveLength(1);
                    done();
                },
                error: done
            });
        });

        it('should query only the site root for an empty path', (done) => {
            getFolderHierarchyByPath('', SITE, mockDotFolderService).subscribe({
                next: () => {
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledTimes(1);
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ path: '/' })
                    );
                    done();
                },
                error: done
            });
        });

        it('should request the large hierarchy page size (not the interactive 40)', (done) => {
            const many = Array.from({ length: 45 }, (_, i) =>
                createFakeFolderSearchView({ id: `f${i}`, name: `folder-${i}`, path: '/' })
            );
            mockDotFolderService.searchFolders.mockReturnValue(searchResult(many));

            getFolderHierarchyByPath('/', SITE, mockDotFolderService).subscribe({
                next: (levels) => {
                    expect(levels[0].folders).toHaveLength(45);
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ per_page: FOLDER_TREE_HIERARCHY_PAGE_SIZE })
                    );
                    expect(FOLDER_TREE_HIERARCHY_PAGE_SIZE).toBeGreaterThan(FOLDER_TREE_PAGE_SIZE);
                    done();
                },
                error: done
            });
        });

        it('should include folders past interactive page position 40 for deep-link restore', (done) => {
            // Simulates a level where the deep-linked name sorts after the first 40 siblings.
            const siblings = Array.from({ length: 45 }, (_, i) =>
                createFakeFolderSearchView({
                    id: `f${i}`,
                    name: `qa36151-child-${i}`,
                    path: '/qa36151-many-parent/'
                })
            );
            mockDotFolderService.searchFolders.mockReturnValue(
                of({
                    folders: siblings,
                    pagination: {
                        currentPage: 1,
                        perPage: FOLDER_TREE_HIERARCHY_PAGE_SIZE,
                        totalEntries: siblings.length
                    }
                })
            );

            getFolderHierarchyByPath(
                '/qa36151-many-parent/qa36151-child-9/',
                SITE,
                mockDotFolderService
            ).subscribe({
                next: (levels) => {
                    // Hierarchy returns every sibling in one large page so a late-sorted
                    // name (string-sort: child-9 is past position 40) is still present.
                    const parentLevel = levels.find(
                        (level) => level.path === '/qa36151-many-parent/'
                    );
                    expect(parentLevel).toBeDefined();
                    expect(parentLevel!.folders.length).toBeGreaterThan(FOLDER_TREE_PAGE_SIZE);
                    expect(
                        parentLevel!.folders.some(
                            (folder) => folder.path === '/qa36151-many-parent/qa36151-child-9/'
                        )
                    ).toBe(true);
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({
                            path: '/qa36151-many-parent/',
                            per_page: FOLDER_TREE_HIERARCHY_PAGE_SIZE
                        })
                    );
                    done();
                },
                error: done
            });
        });

        it('should expose totalEntries so callers can append load-more', (done) => {
            mockDotFolderService.searchFolders.mockReturnValue(
                of({
                    folders: [createFakeFolderSearchView({ path: '/' })],
                    pagination: {
                        currentPage: 1,
                        perPage: FOLDER_TREE_HIERARCHY_PAGE_SIZE,
                        totalEntries: FOLDER_TREE_HIERARCHY_PAGE_SIZE + 10
                    }
                })
            );

            getFolderHierarchyByPath('/', SITE, mockDotFolderService).subscribe({
                next: (levels) => {
                    expect(levels[0].totalEntries).toBe(FOLDER_TREE_HIERARCHY_PAGE_SIZE + 10);
                    expect(levels[0].path).toBe('/');
                    done();
                },
                error: done
            });
        });

        it('should propagate service errors', (done) => {
            mockDotFolderService.searchFolders.mockReturnValue(
                throwError(() => new Error('Service error'))
            );

            getFolderHierarchyByPath('/main', SITE, mockDotFolderService).subscribe({
                next: () => done(new Error('Should have thrown an error')),
                error: (error) => {
                    expect(error.message).toBe('Service error');
                    done();
                }
            });
        });
    });

    describe('getFolderNodesByPath', () => {
        let mockDotFolderService: jest.Mocked<DotFolderService>;
        const SITE_ID = 'site-123';
        const HOSTNAME = 'test.com';
        const SITE = createFakeSite({ identifier: SITE_ID, hostname: HOSTNAME });

        const searchResult = (folders: FolderSearchView[]) =>
            of({ folders, pagination: {} as DotPagination });

        beforeEach(() => {
            mockDotFolderService = {
                searchFolders: jest.fn().mockReturnValue(searchResult([]))
            } as unknown as jest.Mocked<DotFolderService>;
        });

        it('should request the given page of children with the paged size', (done) => {
            const testPath = '/main/sub-folder/';

            getFolderNodesByPath(testPath, SITE, mockDotFolderService, 3).subscribe({
                next: () => {
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({
                            siteId: SITE_ID,
                            path: testPath,
                            recursive: false,
                            page: 3,
                            per_page: FOLDER_TREE_PAGE_SIZE
                        })
                    );
                    done();
                },
                error: done
            });
        });

        it('should default to page 1', (done) => {
            getFolderNodesByPath('/main/', SITE, mockDotFolderService).subscribe({
                next: () => {
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ page: 1 })
                    );
                    done();
                },
                error: done
            });
        });

        it('should transform child folders into tree nodes', (done) => {
            mockDotFolderService.searchFolders.mockReturnValue(
                searchResult([
                    createFakeFolderSearchView({
                        id: 'child-1',
                        inode: 'inode-1',
                        name: 'child1',
                        path: '/main/sub-folder/',
                        addChildrenAllowed: true,
                        hasChildren: true
                    }),
                    createFakeFolderSearchView({
                        id: 'child-2',
                        inode: 'inode-2',
                        name: 'child2',
                        path: '/main/sub-folder/',
                        addChildrenAllowed: false,
                        hasChildren: false
                    })
                ])
            );

            getFolderNodesByPath('/main/sub-folder/', SITE, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result.folders).toHaveLength(2);
                    expect(result.folders[0]).toEqual({
                        key: 'child-1',
                        label: '/main/sub-folder/child1/',
                        data: {
                            id: 'child-1',
                            inode: 'inode-1',
                            hostname: HOSTNAME,
                            path: '/main/sub-folder/child1/',
                            type: 'folder'
                        },
                        // hasChildren: true → expandable (chevron shown)
                        leaf: false
                    });
                    expect(result.folders[1].key).toBe('child-2');
                    expect(result.folders[1].label).toBe('/main/sub-folder/child2/');
                    // hasChildren: false → no chevron, cannot expand
                    expect(result.folders[1].leaf).toBe(true);
                    done();
                },
                error: done
            });
        });

        it('should normalize a parent path that is missing its trailing slash', (done) => {
            mockDotFolderService.searchFolders.mockReturnValue(
                searchResult([createFakeFolderSearchView({ id: 'x', name: 'sub', path: '/main' })])
            );

            getFolderNodesByPath('/main/', SITE, mockDotFolderService).subscribe({
                next: (result) => {
                    const folder = result.folders[0];
                    const data = folder?.data;

                    // Guard before isTreeNodeContentData — `data` is optional on TreeNode.
                    if (!data || !isTreeNodeContentData(data)) {
                        done(new Error('Expected a content folder node with path data'));

                        return;
                    }

                    // '/main' (no trailing slash) + 'sub' must yield '/main/sub/', not '/mainsub/'
                    expect(data.path).toBe('/main/sub/');
                    expect(folder.label).toBe('/main/sub/');
                    done();
                },
                error: done
            });
        });

        it('should return an empty folders array when the level has no children', (done) => {
            getFolderNodesByPath('/main/empty/', SITE, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result.folders).toEqual([]);
                    done();
                },
                error: done
            });
        });

        it('should surface the level total so the caller can decide if more remain', (done) => {
            mockDotFolderService.searchFolders.mockReturnValue(
                of({
                    folders: [createFakeFolderSearchView({ path: '/main/' })],
                    pagination: {
                        currentPage: 1,
                        perPage: FOLDER_TREE_PAGE_SIZE,
                        totalEntries: 120
                    }
                })
            );

            getFolderNodesByPath('/main/', SITE, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result.folders).toHaveLength(1);
                    expect(result.totalEntries).toBe(120);
                    done();
                },
                error: done
            });
        });

        it('should propagate service errors', (done) => {
            mockDotFolderService.searchFolders.mockReturnValue(
                throwError(() => new Error('Service error'))
            );

            getFolderNodesByPath('/main/', SITE, mockDotFolderService).subscribe({
                next: () => done(new Error('Should have thrown an error')),
                error: (error) => {
                    expect(error.message).toBe('Service error');
                    done();
                }
            });
        });
    });

    describe('buildLoadMoreNode', () => {
        it('should build a non-selectable leaf load-more node carrying the paging cursor', () => {
            const node = buildLoadMoreNode('/main/', 'test.com', 2, 75);

            expect(node).toEqual({
                key: 'load-more:/main/',
                label: '',
                type: 'load-more',
                data: {
                    type: 'load-more',
                    path: '/main/',
                    hostname: 'test.com',
                    id: 'load-more:/main/',
                    nextPage: 2,
                    remaining: 75
                },
                leaf: true,
                selectable: false
            });
        });

        it('should set node.type and data.type to the same load-more value', () => {
            const node = buildLoadMoreNode('/main/', 'test.com', 2, 75);

            expect(node.type).toBe('load-more');
            expect(node.data?.type).toBe('load-more');
            expect(node.type).toBe(node.data?.type);
        });
    });

    describe('applyLoadMoreToHierarchy', () => {
        it('should append a load-more sentinel with nextPage 2 when more entries remain', () => {
            const rootFolder = createTreeNode({
                id: 'root-1',
                inode: 'inode-1',
                hostName: 'test.com',
                path: '/main/',
                addChildrenAllowed: true
            });

            const roots = applyLoadMoreToHierarchy(
                [rootFolder],
                [
                    {
                        path: '/',
                        folders: [
                            {
                                id: 'root-1',
                                inode: 'inode-1',
                                hostName: 'test.com',
                                path: '/main/',
                                addChildrenAllowed: true
                            }
                        ],
                        totalEntries: 50
                    }
                ],
                'test.com'
            );

            const loadMore = roots[roots.length - 1];
            expect(loadMore.type).toBe('load-more');
            expect(loadMore.data).toEqual(
                expect.objectContaining({
                    type: 'load-more',
                    nextPage: 2,
                    remaining: 49
                })
            );
        });

        it('should not append load-more when the hierarchy page already has all entries', () => {
            const rootFolder = createTreeNode({
                id: 'root-1',
                inode: 'inode-1',
                hostName: 'test.com',
                path: '/main/',
                addChildrenAllowed: true
            });

            const roots = applyLoadMoreToHierarchy(
                [rootFolder],
                [
                    {
                        path: '/',
                        folders: [
                            {
                                id: 'root-1',
                                inode: 'inode-1',
                                hostName: 'test.com',
                                path: '/main/',
                                addChildrenAllowed: true
                            }
                        ],
                        totalEntries: 1
                    }
                ],
                'test.com'
            );

            expect(roots).toHaveLength(1);
            expect(roots[0].type).not.toBe('load-more');
        });
    });

    describe('folderSearchViewToDotFolder', () => {
        it('should carry defaultBaseType through to the DotFolder', () => {
            const view = createFakeFolderSearchView({
                id: 'f1',
                name: 'app',
                path: '/',
                defaultBaseType: 'DOTASSET'
            });

            const folder = folderSearchViewToDotFolder(view, 'demo.dotcms.com');

            expect(folder.defaultBaseType).toBe('DOTASSET');
        });

        it('should leave defaultBaseType undefined when the view has no preference', () => {
            const view = createFakeFolderSearchView({ id: 'f2', name: 'docs', path: '/' });

            const folder = folderSearchViewToDotFolder(view, 'demo.dotcms.com');

            expect(folder.defaultBaseType).toBeUndefined();
        });
    });
});
