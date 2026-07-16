import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotFolderService, DotSiteService } from '@dotcms/data-access';
import {
    ContentByFolderParams,
    DotCMSContentlet,
    DotFolder,
    DotPagination,
    FolderSearchView,
    SiteEntity
} from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeFolder, createFakeSite } from '@dotcms/utils-testing';

import {
    DotBrowsingService,
    normalizeHostFolderBrowsePath,
    SITE_PAGE_LIMIT,
    TREE_ROOT_NODE_KEY
} from './dot-browsing.service';

describe('DotBrowsingService', () => {
    let spectator: SpectatorService<DotBrowsingService>;
    let dotSiteService: SpyObject<DotSiteService>;
    let dotFolderService: SpyObject<DotFolderService>;

    const createService = createServiceFactory({
        service: DotBrowsingService,
        providers: [mockProvider(DotSiteService), mockProvider(DotFolderService)]
    });

    beforeEach(() => {
        spectator = createService();
        dotSiteService = spectator.inject(DotSiteService);
        dotFolderService = spectator.inject(DotFolderService);
    });

    describe('normalizeHostFolderBrowsePath', () => {
        it('should strip the leading double slash', () => {
            expect(normalizeHostFolderBrowsePath('//demo.com/level1/')).toBe('demo.com/level1/');
        });

        it('should convert the colon-separated hostname:path format', () => {
            expect(normalizeHostFolderBrowsePath('demo.com:/level1/level2/')).toBe(
                'demo.com/level1/level2/'
            );
        });

        it('should convert the colon-separated format for the site root', () => {
            expect(normalizeHostFolderBrowsePath('demo.com:/')).toBe('demo.com/');
        });

        it('should leave an already-plain path untouched', () => {
            expect(normalizeHostFolderBrowsePath('demo.com/level1/')).toBe('demo.com/level1/');
        });

        it('should leave a site-only value (no slash, no colon) untouched', () => {
            expect(normalizeHostFolderBrowsePath('demo.com')).toBe('demo.com');
        });
    });

    const mockPagination: DotPagination = {
        currentPage: 1,
        perPage: 40,
        totalEntries: 2
    };

    describe('getSitesTreePath', () => {
        it('should transform sites into TreeNodeItems', (done) => {
            const mockSites: SiteEntity[] = [
                createFakeSite({ identifier: 'site-1', hostname: 'example.com' }),
                createFakeSite({ identifier: 'site-2', hostname: 'test.com' })
            ];

            dotSiteService.getSites.mockReturnValue(
                of({ sites: mockSites, pagination: mockPagination })
            );

            spectator.service.getSitesTreePath({ filter: 'test' }).subscribe((result) => {
                expect(result).toHaveLength(2);
                expect(result[0]).toEqual({
                    key: 'site-1',
                    label: 'example.com',
                    data: {
                        id: 'site-1',
                        hostname: 'example.com',
                        path: '',
                        type: 'site'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false
                });
                expect(result[1]).toEqual({
                    key: 'site-2',
                    label: 'test.com',
                    data: {
                        id: 'site-2',
                        hostname: 'test.com',
                        path: '',
                        type: 'site'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false
                });
                expect(dotSiteService.getSites).toHaveBeenCalledWith({
                    filter: 'test',
                    per_page: undefined,
                    page: undefined
                });
                done();
            });
        });

        it('should pass perPage and page parameters to getSites', (done) => {
            const mockSites: SiteEntity[] = [createFakeSite()];
            dotSiteService.getSites.mockReturnValue(
                of({ sites: mockSites, pagination: mockPagination })
            );

            spectator.service
                .getSitesTreePath({ filter: 'test', perPage: 10, page: 2 })
                .subscribe(() => {
                    expect(dotSiteService.getSites).toHaveBeenCalledWith({
                        filter: 'test',
                        per_page: 10,
                        page: 2
                    });
                    done();
                });
        });

        it('should return empty array when no sites are found', (done) => {
            dotSiteService.getSites.mockReturnValue(
                of({ sites: [], pagination: { ...mockPagination, totalEntries: 0 } })
            );

            spectator.service.getSitesTreePath({ filter: 'test' }).subscribe((result) => {
                expect(result).toEqual([]);
                done();
            });
        });

        it('should handle errors from getSites', (done) => {
            const error = new Error('Failed to fetch sites');
            dotSiteService.getSites.mockReturnValue(throwError(() => error));

            spectator.service.getSitesTreePath({ filter: 'test' }).subscribe({
                next: () => fail('should have thrown an error'),
                error: (err) => {
                    expect(err).toBe(error);
                    done();
                }
            });
        });
    });

    describe('getSitesPage', () => {
        it('should return sites and pagination metadata', (done) => {
            const mockSites: SiteEntity[] = [
                createFakeSite({ identifier: 'site-1', hostname: 'example.com' })
            ];

            dotSiteService.getSites.mockReturnValue(
                of({ sites: mockSites, pagination: mockPagination })
            );

            spectator.service
                .getSitesPage({ filter: 'demo', perPage: 40, page: 1 })
                .subscribe((result) => {
                    expect(result.sites).toHaveLength(1);
                    expect(result.sites[0].key).toBe('site-1');
                    expect(result.pagination).toEqual(mockPagination);
                    expect(dotSiteService.getSites).toHaveBeenCalledWith({
                        filter: 'demo',
                        per_page: 40,
                        page: 1
                    });
                    done();
                });
        });
    });

    describe('resolveSiteByHostname', () => {
        it('should return the site with an exact hostname match', (done) => {
            const mockSites: SiteEntity[] = [
                createFakeSite({ identifier: 'site-1', hostname: 'demo.com' }),
                createFakeSite({ identifier: 'site-2', hostname: 'demo.com.other' })
            ];

            dotSiteService.getSites.mockReturnValue(
                of({ sites: mockSites, pagination: mockPagination })
            );

            spectator.service.resolveSiteByHostname('demo.com').subscribe((result) => {
                expect(result?.key).toBe('site-1');
                expect(dotSiteService.getSites).toHaveBeenCalledWith({
                    filter: 'demo.com',
                    per_page: SITE_PAGE_LIMIT,
                    page: 1
                });
                done();
            });
        });

        it('should return null when no exact hostname match exists', (done) => {
            const mockSites: SiteEntity[] = [
                createFakeSite({ identifier: 'site-1', hostname: 'other.com' })
            ];

            dotSiteService.getSites.mockReturnValue(
                of({ sites: mockSites, pagination: mockPagination })
            );

            spectator.service.resolveSiteByHostname('demo.com').subscribe((result) => {
                expect(result).toBeNull();
                done();
            });
        });
    });

    describe('getFolders', () => {
        it('should fetch folders by path using folderService', (done) => {
            const mockFolders: DotFolder[] = [
                createFakeFolder({
                    id: 'folder-1',
                    hostName: 'example.com',
                    path: '/folder1',
                    addChildrenAllowed: true
                }),
                createFakeFolder({
                    id: 'folder-2',
                    hostName: 'example.com',
                    path: '/folder2',
                    addChildrenAllowed: false
                })
            ];

            dotFolderService.getFolders.mockReturnValue(of(mockFolders));

            spectator.service.getFolders('/example.com/folder1').subscribe((result) => {
                expect(result).toEqual(mockFolders);
                expect(dotFolderService.getFolders).toHaveBeenCalledWith('/example.com/folder1');
                done();
            });
        });

        it('should return empty array when no folders are found', (done) => {
            dotFolderService.getFolders.mockReturnValue(of([]));

            spectator.service.getFolders('/example.com').subscribe((result) => {
                expect(result).toEqual([]);
                expect(dotFolderService.getFolders).toHaveBeenCalledWith('/example.com');
                done();
            });
        });

        it('should handle errors from folderService', (done) => {
            const error = new Error('Failed to fetch folders');
            dotFolderService.getFolders.mockReturnValue(throwError(() => error));

            spectator.service.getFolders('/example.com').subscribe({
                next: () => fail('should have thrown an error'),
                error: (err) => {
                    expect(err).toBe(error);
                    done();
                }
            });
        });
    });

    describe('getFoldersTreeNode', () => {
        it('should transform folders into tree node structure', (done) => {
            const mockFolders: DotFolder[] = [
                createFakeFolder({
                    id: 'parent-1',
                    hostName: 'example.com',
                    path: '/parent',
                    addChildrenAllowed: true
                }),
                createFakeFolder({
                    id: 'child-1',
                    hostName: 'example.com',
                    path: '/parent/child1',
                    addChildrenAllowed: true
                }),
                createFakeFolder({
                    id: 'child-2',
                    hostName: 'example.com',
                    path: '/parent/child2',
                    addChildrenAllowed: false
                })
            ];

            dotFolderService.getFolders.mockReturnValue(of(mockFolders));

            spectator.service.getFoldersTreeNode('example.com/parent').subscribe((result) => {
                expect(result.parent).toEqual({
                    id: 'parent-1',
                    hostName: 'example.com',
                    path: '/parent',
                    addChildrenAllowed: true
                });
                expect(result.folders).toHaveLength(2);
                expect(result.folders[0]).toEqual({
                    key: 'child-1',
                    label: 'example.com/parent/child1',
                    data: {
                        id: 'child-1',
                        hostname: 'example.com',
                        path: '/parent/child1',
                        type: 'folder'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false
                });
                expect(dotFolderService.getFolders).toHaveBeenCalledWith('//example.com/parent');
                done();
            });
        });

        it('should filter out empty folder arrays', (done) => {
            dotFolderService.getFolders.mockReturnValue(of([]));

            spectator.service.getFoldersTreeNode('example.com').subscribe({
                next: () => fail('should not emit when folders array is empty'),
                error: () => fail('should not throw error'),
                complete: () => {
                    // Observable completes without emitting due to filter
                    done();
                }
            });
        });

        it('should handle folders with only parent (no children)', (done) => {
            const expectedParent = createFakeFolder({
                id: 'parent-1',
                hostName: 'example.com',
                path: '/parent',
                addChildrenAllowed: true
            });

            const mockFolders: DotFolder[] = [expectedParent];

            dotFolderService.getFolders.mockReturnValue(of([...mockFolders]));

            spectator.service.getFoldersTreeNode('example.com/parent').subscribe((result) => {
                expect(result.parent).toEqual(expectedParent);
                expect(result.folders).toEqual([]);
                expect(dotFolderService.getFolders).toHaveBeenCalledWith('//example.com/parent');
                done();
            });
        });

        it('should handle errors from folderService', (done) => {
            const error = new Error('Failed to fetch folders');
            dotFolderService.getFolders.mockReturnValue(throwError(() => error));

            spectator.service.getFoldersTreeNode('example.com').subscribe({
                next: () => fail('should have thrown an error'),
                error: (err) => {
                    expect(err).toBe(error);
                    done();
                }
            });
        });
    });

    describe('searchFolders', () => {
        it('should transform FolderSearchView results into TreeNodeItems using the given hostname', (done) => {
            const mockFolders: FolderSearchView[] = [
                {
                    id: 'folder-1',
                    inode: 'inode-1',
                    name: 'folder1',
                    path: '/',
                    addChildrenAllowed: true,
                    hasChildren: true
                },
                {
                    id: 'folder-2',
                    inode: 'inode-2',
                    name: 'folder2',
                    path: '/',
                    addChildrenAllowed: false,
                    hasChildren: false
                }
            ];
            const mockPagination: DotPagination = { currentPage: 1, perPage: 40, totalEntries: 2 };

            dotFolderService.searchFolders.mockReturnValue(
                of({ folders: mockFolders, pagination: mockPagination })
            );

            spectator.service
                .searchFolders({ siteId: 'site-1' }, 'example.com')
                .subscribe(({ folders, pagination }) => {
                    expect(pagination).toEqual(mockPagination);
                    expect(folders).toEqual([
                        {
                            key: 'folder-1',
                            label: 'example.com/folder1/',
                            data: {
                                id: 'folder-1',
                                hostname: 'example.com',
                                path: '/folder1/',
                                type: 'folder'
                            },
                            expandedIcon: 'pi pi-folder-open',
                            collapsedIcon: 'pi pi-folder',
                            leaf: false
                        },
                        {
                            key: 'folder-2',
                            label: 'example.com/folder2/',
                            data: {
                                id: 'folder-2',
                                hostname: 'example.com',
                                path: '/folder2/',
                                type: 'folder'
                            },
                            expandedIcon: 'pi pi-folder-open',
                            collapsedIcon: 'pi pi-folder',
                            leaf: true
                        }
                    ]);
                    expect(dotFolderService.searchFolders).toHaveBeenCalledWith({
                        siteId: 'site-1'
                    });
                    done();
                });
        });

        it('should set leaf from hasChildren, not addChildrenAllowed', (done) => {
            const mockFolders: FolderSearchView[] = [
                {
                    id: 'folder-3',
                    inode: 'inode-3',
                    name: 'allowed-but-empty',
                    path: '/',
                    addChildrenAllowed: true,
                    hasChildren: false
                }
            ];
            const mockPagination: DotPagination = { currentPage: 1, perPage: 40, totalEntries: 1 };

            dotFolderService.searchFolders.mockReturnValue(
                of({ folders: mockFolders, pagination: mockPagination })
            );

            spectator.service
                .searchFolders({ siteId: 'site-1' }, 'example.com')
                .subscribe(({ folders }) => {
                    expect(folders[0].leaf).toBe(true);
                    done();
                });
        });

        it('should mark recursive search results as leaves even when hasChildren is true', (done) => {
            const mockFolders: FolderSearchView[] = [
                {
                    id: 'folder-1',
                    inode: 'inode-1',
                    name: 'folder1',
                    path: '/',
                    addChildrenAllowed: true,
                    hasChildren: true
                }
            ];
            const mockPagination: DotPagination = { currentPage: 1, perPage: 40, totalEntries: 1 };

            dotFolderService.searchFolders.mockReturnValue(
                of({ folders: mockFolders, pagination: mockPagination })
            );

            spectator.service
                .searchFolders({ siteId: 'site-1', recursive: true, name: 'folder' }, 'example.com')
                .subscribe(({ folders }) => {
                    expect(folders[0].leaf).toBe(true);
                    done();
                });
        });

        it('should build nested folder paths from a non-root parent path', (done) => {
            const mockFolders: FolderSearchView[] = [
                {
                    id: 'folder-3',
                    inode: 'inode-3',
                    name: 'child',
                    path: '/level1',
                    addChildrenAllowed: true,
                    hasChildren: true
                }
            ];
            const mockPagination: DotPagination = { currentPage: 1, perPage: 40, totalEntries: 1 };

            dotFolderService.searchFolders.mockReturnValue(
                of({ folders: mockFolders, pagination: mockPagination })
            );

            spectator.service
                .searchFolders({ siteId: 'site-1', path: '/level1' }, 'example.com')
                .subscribe(({ folders }) => {
                    expect(folders[0].label).toBe('example.com/level1/child/');
                    expect(folders[0].data.path).toBe('/level1/child/');
                    done();
                });
        });

        it('should return an empty array when no folders are found', (done) => {
            const mockPagination: DotPagination = { currentPage: 1, perPage: 40, totalEntries: 0 };
            dotFolderService.searchFolders.mockReturnValue(
                of({ folders: [], pagination: mockPagination })
            );

            spectator.service
                .searchFolders({ siteId: 'site-1' }, 'example.com')
                .subscribe(({ folders }) => {
                    expect(folders).toEqual([]);
                    done();
                });
        });

        it('should handle errors from folderService', (done) => {
            const error = new Error('Failed to search folders');
            dotFolderService.searchFolders.mockReturnValue(throwError(() => error));

            spectator.service.searchFolders({ siteId: 'site-1' }, 'example.com').subscribe({
                next: () => fail('should have thrown an error'),
                error: (err) => {
                    expect(err).toBe(error);
                    done();
                }
            });
        });
    });

    describe('buildTreeByPaths', () => {
        const siteId = 'site-1';
        const hostname = 'example.com';
        const defaultPagination: DotPagination = { currentPage: 1, perPage: 40, totalEntries: 2 };

        const createSearchFolder = (options: {
            id: string;
            name: string;
            path: string;
            hasChildren?: boolean;
        }): FolderSearchView => ({
            id: options.id,
            inode: `${options.id}-inode`,
            name: options.name,
            path: options.path,
            addChildrenAllowed: true,
            hasChildren: options.hasChildren ?? false
        });

        it('should build hierarchical tree structure from folder path using searchFolders', (done) => {
            const folderPath = '/level1/level2/';

            dotFolderService.searchFolders.mockImplementation((params) => {
                if (params.path === '/') {
                    return of({
                        folders: [
                            createSearchFolder({
                                id: 'parent-level1',
                                name: 'level1',
                                path: '/',
                                hasChildren: true
                            })
                        ],
                        pagination: defaultPagination
                    });
                }

                if (params.path === '/level1/') {
                    return of({
                        folders: [
                            createSearchFolder({
                                id: 'parent-level2',
                                name: 'level2',
                                path: '/level1/',
                                hasChildren: true
                            }),
                            createSearchFolder({
                                id: 'child-level2',
                                name: 'child',
                                path: '/level1/level2/',
                                hasChildren: false
                            })
                        ],
                        pagination: defaultPagination
                    });
                }

                return of({ folders: [], pagination: defaultPagination });
            });

            spectator.service.buildTreeByPaths(siteId, hostname, folderPath).subscribe((result) => {
                expect(result.tree?.folders).toHaveLength(1);
                expect(result.node?.key).toBe('parent-level2');
                expect(result.node?.data?.path).toBe('/level1/level2/');
                expect(result.node?.children).toBeUndefined();

                const level1Node = result.tree?.folders.find(
                    (folder) => folder.key === 'parent-level1'
                );
                expect(level1Node?.expanded).toBe(true);
                expect(level1Node?.children).toHaveLength(2);
                expect(result.pagination?.[TREE_ROOT_NODE_KEY]).toEqual({
                    page: 1,
                    hasMore: false
                });
                expect(result.pagination?.['parent-level1']).toEqual({
                    page: 1,
                    hasMore: false
                });
                expect(dotFolderService.searchFolders).toHaveBeenCalledTimes(2);
                expect(dotFolderService.searchFolders).toHaveBeenCalledWith({
                    siteId,
                    path: '/',
                    recursive: false,
                    page: 1,
                    per_page: 40
                });
                done();
            });
        });

        it('should paginate a level until the target segment is found beyond page 1', (done) => {
            const folderPath = '/gallery/';

            dotFolderService.searchFolders.mockImplementation((params) => {
                if (params.path === '/' && params.page === 1) {
                    return of({
                        folders: [
                            createSearchFolder({
                                id: 'images',
                                name: 'images',
                                path: '/',
                                hasChildren: true
                            })
                        ],
                        pagination: { currentPage: 1, perPage: 40, totalEntries: 45 }
                    });
                }

                if (params.path === '/' && params.page === 2) {
                    return of({
                        folders: [
                            createSearchFolder({
                                id: 'gallery',
                                name: 'gallery',
                                path: '/',
                                hasChildren: false
                            })
                        ],
                        pagination: { currentPage: 2, perPage: 40, totalEntries: 45 }
                    });
                }

                return of({ folders: [], pagination: defaultPagination });
            });

            spectator.service.buildTreeByPaths(siteId, hostname, folderPath).subscribe((result) => {
                expect(result.node?.key).toBe('gallery');
                expect(result.node?.data?.path).toBe('/gallery/');
                expect(result.tree?.folders.map((folder) => folder.key)).toEqual([
                    'images',
                    'gallery'
                ]);
                expect(result.pagination?.[TREE_ROOT_NODE_KEY]).toEqual({
                    page: 2,
                    hasMore: false
                });
                expect(dotFolderService.searchFolders).toHaveBeenCalledTimes(2);
                expect(dotFolderService.searchFolders).toHaveBeenNthCalledWith(1, {
                    siteId,
                    path: '/',
                    recursive: false,
                    page: 1,
                    per_page: 40
                });
                expect(dotFolderService.searchFolders).toHaveBeenNthCalledWith(2, {
                    siteId,
                    path: '/',
                    recursive: false,
                    page: 2,
                    per_page: 40
                });
                done();
            });
        });

        it('should keep hasMore true when the target is found but more siblings remain', (done) => {
            const folderPath = '/gallery/';

            dotFolderService.searchFolders.mockImplementation((params) => {
                if (params.path === '/' && params.page === 1) {
                    return of({
                        folders: [
                            createSearchFolder({
                                id: 'aaa',
                                name: 'aaa',
                                path: '/',
                                hasChildren: false
                            })
                        ],
                        pagination: { currentPage: 1, perPage: 1, totalEntries: 3 }
                    });
                }

                if (params.path === '/' && params.page === 2) {
                    return of({
                        folders: [
                            createSearchFolder({
                                id: 'gallery',
                                name: 'gallery',
                                path: '/',
                                hasChildren: false
                            })
                        ],
                        pagination: { currentPage: 2, perPage: 1, totalEntries: 3 }
                    });
                }

                return of({ folders: [], pagination: defaultPagination });
            });

            spectator.service.buildTreeByPaths(siteId, hostname, folderPath).subscribe((result) => {
                expect(result.node?.key).toBe('gallery');
                expect(result.pagination?.[TREE_ROOT_NODE_KEY]).toEqual({
                    page: 2,
                    hasMore: true
                });
                expect(dotFolderService.searchFolders).toHaveBeenCalledTimes(2);
                done();
            });
        });

        it('should handle a single folder segment', (done) => {
            dotFolderService.searchFolders.mockReturnValue(
                of({
                    folders: [
                        createSearchFolder({
                            id: 'parent-level1',
                            name: 'level1',
                            path: '/',
                            hasChildren: false
                        })
                    ],
                    pagination: defaultPagination
                })
            );

            spectator.service.buildTreeByPaths(siteId, hostname, '/level1/').subscribe((result) => {
                expect(result.node?.key).toBe('parent-level1');
                expect(result.node?.leaf).toBe(true);
                expect(dotFolderService.searchFolders).toHaveBeenCalledTimes(1);
                done();
            });
        });

        it('should handle empty path segments in folder path', (done) => {
            dotFolderService.searchFolders.mockReturnValue(
                of({
                    folders: [
                        createSearchFolder({
                            id: 'parent-level1',
                            name: 'level1',
                            path: '/',
                            hasChildren: false
                        })
                    ],
                    pagination: defaultPagination
                })
            );

            spectator.service.buildTreeByPaths(siteId, hostname, '/level1/').subscribe((result) => {
                expect(result).toBeDefined();
                expect(dotFolderService.searchFolders).toHaveBeenCalledTimes(1);
                done();
            });
        });

        it('should handle errors when building tree', (done) => {
            const error = new Error('Failed to fetch folders');

            dotFolderService.searchFolders.mockReturnValue(throwError(() => error));

            spectator.service.buildTreeByPaths(siteId, hostname, '/level1/').subscribe({
                next: () => fail('should have thrown an error'),
                error: (err) => {
                    expect(err).toBe(error);
                    done();
                }
            });
        });

        it('should set leaf from hasChildren for navigation tree nodes', (done) => {
            dotFolderService.searchFolders.mockReturnValue(
                of({
                    folders: [
                        createSearchFolder({
                            id: 'folder-with-children',
                            name: 'level1',
                            path: '/',
                            hasChildren: true
                        }),
                        createSearchFolder({
                            id: 'folder-without-children',
                            name: 'empty',
                            path: '/',
                            hasChildren: false
                        })
                    ],
                    pagination: defaultPagination
                })
            );

            spectator.service.buildTreeByPaths(siteId, hostname, '/level1/').subscribe((result) => {
                const withChildren = result.tree?.folders.find(
                    (folder) => folder.key === 'folder-with-children'
                );
                const withoutChildren = result.tree?.folders.find(
                    (folder) => folder.key === 'folder-without-children'
                );

                expect(withChildren?.leaf).toBe(false);
                expect(withoutChildren?.leaf).toBe(true);
                done();
            });
        });

        it('should stop gracefully when a target segment is missing from paginated results', (done) => {
            dotFolderService.searchFolders.mockImplementation((params) => {
                if (params.path === '/') {
                    return of({
                        folders: [
                            createSearchFolder({
                                id: 'parent-level1',
                                name: 'level1',
                                path: '/',
                                hasChildren: true
                            })
                        ],
                        pagination: defaultPagination
                    });
                }

                if (params.path === '/level1/') {
                    return of({
                        folders: [
                            createSearchFolder({
                                id: 'sibling-a',
                                name: 'sibling-a',
                                path: '/level1/',
                                hasChildren: false
                            })
                        ],
                        pagination: defaultPagination
                    });
                }

                return of({ folders: [], pagination: defaultPagination });
            });

            spectator.service
                .buildTreeByPaths(siteId, 'demo.com', '/level1/level2/')
                .subscribe((result) => {
                    expect(result.node?.key).toBe('parent-level1');
                    expect(result.node?.data?.path).toBe('/level1/');
                    done();
                });
        });

        it('should mark ancestor folders as expanded so the tree opens to the target node', (done) => {
            dotFolderService.searchFolders.mockImplementation((params) => {
                if (params.path === '/') {
                    return of({
                        folders: [
                            createSearchFolder({
                                id: 'application',
                                name: 'application',
                                path: '/',
                                hasChildren: true
                            })
                        ],
                        pagination: defaultPagination
                    });
                }

                if (params.path === '/application/') {
                    return of({
                        folders: [
                            createSearchFolder({
                                id: 'apivtl',
                                name: 'apivtl',
                                path: '/application/',
                                hasChildren: false
                            })
                        ],
                        pagination: defaultPagination
                    });
                }

                return of({ folders: [], pagination: defaultPagination });
            });

            spectator.service
                .buildTreeByPaths(siteId, 'demo.com', '/application/apivtl/')
                .subscribe((result) => {
                    const applicationNode = result.tree?.folders.find(
                        (folder) => folder.key === 'application'
                    );
                    expect(applicationNode?.expanded).toBe(true);
                    expect(result.node?.key).toBe('apivtl');
                    done();
                });
        });
    });

    describe('getCurrentSiteAsTreeNodeItem', () => {
        it('should transform current site into TreeNodeItem', (done) => {
            const mockSite: SiteEntity = createFakeSite({
                identifier: 'site-1',
                hostname: 'example.com'
            });

            dotSiteService.getCurrentSite.mockReturnValue(of(mockSite));

            spectator.service.getCurrentSiteAsTreeNodeItem().subscribe((result) => {
                expect(result).toEqual({
                    key: 'site-1',
                    label: 'example.com',
                    data: {
                        id: 'site-1',
                        hostname: 'example.com',
                        path: '',
                        type: 'site'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false
                });
                expect(dotSiteService.getCurrentSite).toHaveBeenCalled();
                done();
            });
        });

        it('should handle errors from getCurrentSite', (done) => {
            const error = new Error('Failed to fetch current site');
            dotSiteService.getCurrentSite.mockReturnValue(throwError(() => error));

            spectator.service.getCurrentSiteAsTreeNodeItem().subscribe({
                next: () => fail('should have thrown an error'),
                error: (err) => {
                    expect(err).toBe(error);
                    done();
                }
            });
        });
    });

    describe('getContentByFolder', () => {
        it('should pass params directly to siteService', () => {
            const mockContent: DotCMSContentlet[] = [];
            const params: ContentByFolderParams = {
                hostFolderId: '123'
            };
            dotSiteService.getContentByFolder.mockReturnValue(of(mockContent));

            spectator.service.getContentByFolder(params);

            expect(dotSiteService.getContentByFolder).toHaveBeenCalledWith(params);
        });

        it('should pass params with mimeTypes to siteService', () => {
            const mockContent: DotCMSContentlet[] = [];
            const mimeTypes = ['image/jpeg', 'image/png'];
            const params: ContentByFolderParams = {
                hostFolderId: '123',
                mimeTypes
            };
            dotSiteService.getContentByFolder.mockReturnValue(of(mockContent));

            spectator.service.getContentByFolder(params);

            expect(dotSiteService.getContentByFolder).toHaveBeenCalledWith(params);
        });

        it('should pass params with all options to siteService', () => {
            const mockContent: DotCMSContentlet[] = [];
            const params: ContentByFolderParams = {
                hostFolderId: '123',
                showLinks: true,
                showDotAssets: false,
                showPages: true,
                showFiles: false,
                showFolders: true,
                showWorking: false,
                showArchived: true,
                sortByDesc: false,
                mimeTypes: ['image/jpeg'],
                extensions: ['.jpg', '.png']
            };
            dotSiteService.getContentByFolder.mockReturnValue(of(mockContent));

            spectator.service.getContentByFolder(params);

            expect(dotSiteService.getContentByFolder).toHaveBeenCalledWith(params);
        });

        it('should return content from siteService', (done) => {
            const mockContent: DotCMSContentlet[] = [
                createFakeContentlet({
                    inode: 'content-1',
                    title: 'Test Content',
                    identifier: 'content-1'
                })
            ];
            const params: ContentByFolderParams = {
                hostFolderId: '123'
            };
            dotSiteService.getContentByFolder.mockReturnValue(of(mockContent));

            spectator.service.getContentByFolder(params).subscribe((result) => {
                expect(result).toEqual(mockContent);
                done();
            });
        });

        it('should handle errors from getContentByFolder', (done) => {
            const error = new Error('Failed to fetch content');
            const params: ContentByFolderParams = {
                hostFolderId: '123'
            };
            dotSiteService.getContentByFolder.mockReturnValue(throwError(() => error));

            spectator.service.getContentByFolder(params).subscribe({
                next: () => {
                    fail('should have thrown an error');
                },
                error: (err) => {
                    expect(err).toBe(error);
                    done();
                }
            });
        });

        it('should handle empty mimeTypes array', () => {
            const mockContent: DotCMSContentlet[] = [];
            const params: ContentByFolderParams = {
                hostFolderId: '123',
                mimeTypes: []
            };
            dotSiteService.getContentByFolder.mockReturnValue(of(mockContent));

            spectator.service.getContentByFolder(params);

            expect(dotSiteService.getContentByFolder).toHaveBeenCalledWith(params);
        });
    });
});
