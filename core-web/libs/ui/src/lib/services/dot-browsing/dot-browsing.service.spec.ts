import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotFolderService, DotSiteService } from '@dotcms/data-access';
import {
    ContentByFolderParams,
    DotCMSContentlet,
    DotFolder,
    SiteEntity
} from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeFolder, createFakeSite } from '@dotcms/utils-testing';

import { DotBrowsingService } from './dot-browsing.service';

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

    describe('getSitesTreePath', () => {
        it('should transform sites into TreeNodeItems', (done) => {
            const mockSites: SiteEntity[] = [
                createFakeSite({ identifier: 'site-1', hostname: 'example.com' }),
                createFakeSite({ identifier: 'site-2', hostname: 'test.com' })
            ];

            dotSiteService.getSites.mockReturnValue(of(mockSites));

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
                expect(dotSiteService.getSites).toHaveBeenCalledWith('test', undefined, undefined);
                done();
            });
        });

        it('should pass perPage and page parameters to getSites', (done) => {
            const mockSites: SiteEntity[] = [createFakeSite()];
            dotSiteService.getSites.mockReturnValue(of(mockSites));

            spectator.service
                .getSitesTreePath({ filter: 'test', perPage: 10, page: 2 })
                .subscribe(() => {
                    expect(dotSiteService.getSites).toHaveBeenCalledWith('test', 10, 2);
                    done();
                });
        });

        it('should return empty array when no sites are found', (done) => {
            dotSiteService.getSites.mockReturnValue(of([]));

            spectator.service.getSitesTreePath({ filter: 'test' }).subscribe((result) => {
                expect(result).toEqual([]);
                done();
            });
        });

        it('should handle errors from getSites', (done) => {
            const error = new Error('Failed to fetch sites');
            dotSiteService.getSites.mockReturnValue(throwError(error));

            spectator.service.getSitesTreePath({ filter: 'test' }).subscribe({
                next: () => fail('should have thrown an error'),
                error: (err) => {
                    expect(err).toBe(error);
                    done();
                }
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
            dotFolderService.getFolders.mockReturnValue(throwError(error));

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
            dotFolderService.getFolders.mockReturnValue(throwError(error));

            spectator.service.getFoldersTreeNode('example.com').subscribe({
                next: () => fail('should have thrown an error'),
                error: (err) => {
                    expect(err).toBe(error);
                    done();
                }
            });
        });
    });

    describe('buildTreeByPaths', () => {
        it('should build hierarchical tree structure from path', (done) => {
            const path = 'example.com/level1/level2';

            // Mock responses for each path segment
            const level2Folders: DotFolder[] = [
                createFakeFolder({
                    id: 'parent-level2',
                    hostName: 'example.com',
                    path: '/level1/level2',
                    addChildrenAllowed: true
                }),
                createFakeFolder({
                    id: 'child-level2',
                    hostName: 'example.com',
                    path: '/level1/level2/child',
                    addChildrenAllowed: true
                })
            ];

            const level1Folders: DotFolder[] = [
                createFakeFolder({
                    id: 'parent-level1',
                    hostName: 'example.com',
                    path: '/level1',
                    addChildrenAllowed: true
                }),
                createFakeFolder({
                    id: 'parent-level2',
                    hostName: 'example.com',
                    path: '/level1/level2',
                    addChildrenAllowed: true
                })
            ];

            const rootFolders: DotFolder[] = [
                createFakeFolder({
                    id: 'root',
                    hostName: 'example.com',
                    path: '/',
                    addChildrenAllowed: true
                }),
                createFakeFolder({
                    id: 'parent-level1',
                    hostName: 'example.com',
                    path: '/level1',
                    addChildrenAllowed: true
                })
            ];

            // Mock responses for each path in reverse order (as paths are reversed in the service)
            dotFolderService.getFolders.mockImplementation((requestedPath: string) => {
                if (requestedPath === '//example.com/level1/level2/') {
                    return of(level2Folders);
                } else if (requestedPath === '//example.com/level1/') {
                    return of(level1Folders);
                } else if (requestedPath === '//example.com/') {
                    return of(rootFolders);
                }

                return of([]);
            });

            spectator.service.buildTreeByPaths(path).subscribe((result) => {
                expect(result).toBeDefined();
                expect(result.tree).toBeDefined();
                expect(result.tree?.folders).toBeDefined();
                expect(dotFolderService.getFolders).toHaveBeenCalledTimes(3);
                done();
            });
        });

        it('should handle single level path', (done) => {
            const path = 'example.com';

            const rootFolders: DotFolder[] = [
                createFakeFolder({
                    id: 'root',
                    hostName: 'example.com',
                    path: '/',
                    addChildrenAllowed: true
                })
            ];

            dotFolderService.getFolders.mockReturnValue(of(rootFolders));

            spectator.service.buildTreeByPaths(path).subscribe((result) => {
                expect(result).toBeDefined();
                expect(result.tree).toBeDefined();
                expect(dotFolderService.getFolders).toHaveBeenCalledWith('//example.com/');
                done();
            });
        });

        it('should handle empty path segments', (done) => {
            const path = 'example.com//level1';

            const rootFolders: DotFolder[] = [
                createFakeFolder({
                    id: 'root',
                    hostName: 'example.com',
                    path: '/',
                    addChildrenAllowed: true
                }),
                createFakeFolder({
                    id: 'parent-level1',
                    hostName: 'example.com',
                    path: '/level1',
                    addChildrenAllowed: true
                })
            ];

            const level1Folders: DotFolder[] = [
                createFakeFolder({
                    id: 'parent-level1',
                    hostName: 'example.com',
                    path: '/level1',
                    addChildrenAllowed: true
                })
            ];

            dotFolderService.getFolders.mockImplementation((requestedPath: string) => {
                if (requestedPath === '//example.com/level1/') {
                    return of(level1Folders);
                } else if (requestedPath === '//example.com/') {
                    return of(rootFolders);
                }

                return of([]);
            });

            spectator.service.buildTreeByPaths(path).subscribe((result) => {
                expect(result).toBeDefined();
                expect(dotFolderService.getFolders).toHaveBeenCalledTimes(2);
                done();
            });
        });

        it('should handle errors when building tree', (done) => {
            const path = 'example.com/level1';
            const error = new Error('Failed to fetch folders');

            dotFolderService.getFolders.mockReturnValue(throwError(error));

            spectator.service.buildTreeByPaths(path).subscribe({
                next: () => fail('should have thrown an error'),
                error: (err) => {
                    expect(err).toBe(error);
                    done();
                }
            });
        });

        it('should always return tree with defined parent and hostName for single level path', (done) => {
            const path = 'example.com';
            const rootParent = createFakeFolder({
                id: 'root',
                hostName: 'example.com',
                path: '/',
                addChildrenAllowed: true
            });

            const rootFolders: DotFolder[] = [rootParent];

            dotFolderService.getFolders.mockReturnValue(of(rootFolders));

            spectator.service.buildTreeByPaths(path).subscribe((result) => {
                expect(result.tree).toBeDefined();
                expect(result.tree?.parent).toBeDefined();
                expect(result.tree?.parent?.hostName).toBe('example.com');
                expect(result.tree?.path).toBe('/');
                // Consumer (host-folder-field.store) can safely use tree.parent.hostName
                expect(() => {
                    const hostName = result.tree?.parent?.hostName;
                    return hostName;
                }).not.toThrow();
                done();
            });
        });

        it('should always return tree with defined parent and hostName for multi level path (regression: tree.parent.hostName in host-folder-field.store)', (done) => {
            const path = 'demo.com/level1/level2';

            const level2Parent = createFakeFolder({
                id: 'parent-level2',
                hostName: 'demo.com',
                path: '/level1/level2',
                addChildrenAllowed: true
            });

            const level2Folders: DotFolder[] = [
                level2Parent,
                createFakeFolder({
                    id: 'child-level2',
                    hostName: 'demo.com',
                    path: '/level1/level2/child',
                    addChildrenAllowed: true
                })
            ];

            const level1Folders: DotFolder[] = [
                createFakeFolder({
                    id: 'parent-level1',
                    hostName: 'demo.com',
                    path: '/level1',
                    addChildrenAllowed: true
                }),
                createFakeFolder({
                    id: 'parent-level2',
                    hostName: 'demo.com',
                    path: '/level1/level2',
                    addChildrenAllowed: true
                })
            ];

            const rootFolders: DotFolder[] = [
                createFakeFolder({
                    id: 'root',
                    hostName: 'demo.com',
                    path: '/',
                    addChildrenAllowed: true
                }),
                createFakeFolder({
                    id: 'parent-level1',
                    hostName: 'demo.com',
                    path: '/level1',
                    addChildrenAllowed: true
                })
            ];

            dotFolderService.getFolders.mockImplementation((requestedPath: string) => {
                if (requestedPath === '//demo.com/level1/level2/') {
                    return of(level2Folders);
                }
                if (requestedPath === '//demo.com/level1/') {
                    return of(level1Folders);
                }
                if (requestedPath === '//demo.com/') {
                    return of(rootFolders);
                }
                return of([]);
            });

            spectator.service.buildTreeByPaths(path).subscribe((result) => {
                expect(result.tree).toBeDefined();
                expect(result.tree?.parent).toBeDefined();
                expect(result.tree?.parent?.hostName).toBeDefined();
                expect(result.tree?.parent?.hostName).toBe('demo.com');
                // Simulates host-folder-field.store: item.data.hostname === tree.parent.hostName
                expect(() => {
                    const tree = result.tree;
                    if (tree) {
                        return tree.parent?.hostName;
                    }
                }).not.toThrow();
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
            dotSiteService.getCurrentSite.mockReturnValue(throwError(error));

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
            dotSiteService.getContentByFolder.mockReturnValue(throwError(error));

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
