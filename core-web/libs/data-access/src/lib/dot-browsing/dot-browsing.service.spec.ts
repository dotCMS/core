import {
    createHttpFactory,
    HttpMethod,
    mockProvider,
    SpectatorHttp,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotFolder, DotCMSAPIResponse, SiteEntity, DotCMSContentlet } from '@dotcms/dotcms-models';
import { createFakeSite, createFakeFolder, createFakeContentlet } from '@dotcms/utils-testing';

import { DotBrowsingService } from './dot-browsing.service';

import { DotSiteService } from '../dot-site/dot-site.service';

const FOLDER_API_ENDPOINT = '/api/v1/folder/byPath';

describe('DotBrowsingService', () => {
    let spectator: SpectatorHttp<DotBrowsingService>;
    let dotSiteService: SpyObject<DotSiteService>;

    const createHttp = createHttpFactory({
        service: DotBrowsingService,
        providers: [mockProvider(DotSiteService)]
    });

    beforeEach(() => {
        spectator = createHttp();
        dotSiteService = spectator.inject(DotSiteService);
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

    describe('getFolders', () => {
        it('should fetch folders by path', (done) => {
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

            const mockResponse: DotCMSAPIResponse<DotFolder[]> = {
                entity: mockFolders,
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {}
            };

            spectator.service.getFolders('/example.com/folder1').subscribe((result) => {
                expect(result).toEqual(mockFolders);
                done();
            });

            const req = spectator.expectOne(FOLDER_API_ENDPOINT, HttpMethod.POST);
            expect(req.request.body).toEqual({ path: '/example.com/folder1' });
            req.flush(mockResponse);
        });

        it('should return empty array when no folders are found', (done) => {
            const mockResponse: DotCMSAPIResponse<DotFolder[]> = {
                entity: [],
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {}
            };

            spectator.service.getFolders('/example.com').subscribe((result) => {
                expect(result).toEqual([]);
                done();
            });

            const req = spectator.expectOne(FOLDER_API_ENDPOINT, HttpMethod.POST);
            req.flush(mockResponse);
        });

        it('should handle HTTP errors', (done) => {
            spectator.service.getFolders('/example.com').subscribe({
                next: () => fail('should have thrown an error'),
                error: (error) => {
                    expect(error.status).toBe(500);
                    done();
                }
            });

            const req = spectator.expectOne(FOLDER_API_ENDPOINT, HttpMethod.POST);
            req.flush(null, { status: 500, statusText: 'Internal Server Error' });
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

            const mockResponse: DotCMSAPIResponse<DotFolder[]> = {
                entity: mockFolders,
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {}
            };

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
                done();
            });

            const req = spectator.expectOne(FOLDER_API_ENDPOINT, HttpMethod.POST);
            expect(req.request.body).toEqual({ path: '//example.com/parent' });
            req.flush(mockResponse);
        });

        it('should filter out empty folder arrays', (done) => {
            const mockResponse: DotCMSAPIResponse<DotFolder[]> = {
                entity: [],
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {}
            };

            spectator.service.getFoldersTreeNode('example.com').subscribe({
                next: () => fail('should not emit when folders array is empty'),
                error: () => fail('should not throw error'),
                complete: () => {
                    // Observable completes without emitting due to filter
                    done();
                }
            });

            const req = spectator.expectOne(FOLDER_API_ENDPOINT, HttpMethod.POST);
            req.flush(mockResponse);
        });

        it('should handle folders with only parent (no children)', (done) => {
            const mockFolders: DotFolder[] = [
                createFakeFolder({
                    id: 'parent-1',
                    hostName: 'example.com',
                    path: '/parent',
                    addChildrenAllowed: true
                })
            ];

            const mockResponse: DotCMSAPIResponse<DotFolder[]> = {
                entity: mockFolders,
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {}
            };

            spectator.service.getFoldersTreeNode('example.com/parent').subscribe((result) => {
                expect(result.parent).toEqual(mockFolders[0]);
                expect(result.folders).toEqual([]);
                done();
            });

            const req = spectator.expectOne(FOLDER_API_ENDPOINT, HttpMethod.POST);
            req.flush(mockResponse);
        });

        it('should handle HTTP errors', (done) => {
            spectator.service.getFoldersTreeNode('example.com').subscribe({
                next: () => fail('should have thrown an error'),
                error: (error) => {
                    expect(error.status).toBe(404);
                    done();
                }
            });

            const req = spectator.expectOne(FOLDER_API_ENDPOINT, HttpMethod.POST);
            req.flush(null, { status: 404, statusText: 'Not Found' });
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
                    id: 'level2-folder',
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
                    id: 'level1-folder',
                    hostName: 'example.com',
                    path: '/level1',
                    addChildrenAllowed: true
                })
            ];

            spectator.service.buildTreeByPaths(path).subscribe((result) => {
                expect(result).toBeDefined();
                expect(result.tree).toBeDefined();
                expect(result.tree?.folders).toBeDefined();
                done();
            });

            // Expect 3 requests (one for each path segment)
            const requests = spectator.controller.match(
                (req) => req.url === FOLDER_API_ENDPOINT && req.method === HttpMethod.POST
            );
            expect(requests).toHaveLength(3);

            // Flush responses in reverse order (as paths are reversed)
            requests[0].flush({
                entity: level2Folders,
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {}
            });
            requests[1].flush({
                entity: level1Folders,
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {}
            });
            requests[2].flush({
                entity: rootFolders,
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {}
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

            spectator.service.buildTreeByPaths(path).subscribe((result) => {
                expect(result).toBeDefined();
                expect(result.tree).toBeDefined();
                done();
            });

            const req = spectator.expectOne(FOLDER_API_ENDPOINT, HttpMethod.POST);
            req.flush({
                entity: rootFolders,
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {}
            });
        });

        it('should handle empty path segments', (done) => {
            const path = 'example.com//level1';

            const folders: DotFolder[] = [
                createFakeFolder({
                    id: 'root',
                    hostName: 'example.com',
                    path: '/',
                    addChildrenAllowed: true
                })
            ];

            spectator.service.buildTreeByPaths(path).subscribe((result) => {
                expect(result).toBeDefined();
                done();
            });

            const req = spectator.expectOne(FOLDER_API_ENDPOINT, HttpMethod.POST);
            req.flush({
                entity: folders,
                errors: [],
                messages: [],
                permissions: [],
                i18nMessagesMap: {}
            });
        });

        it('should handle errors when building tree', (done) => {
            const path = 'example.com/level1';

            spectator.service.buildTreeByPaths(path).subscribe({
                next: () => fail('should have thrown an error'),
                error: (error) => {
                    expect(error).toBeDefined();
                    done();
                }
            });

            const requests = spectator.controller.match(
                (req) => req.url === FOLDER_API_ENDPOINT && req.method === HttpMethod.POST
            );
            if (requests.length > 0) {
                requests[0].flush(null, { status: 500, statusText: 'Internal Server Error' });
            }
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
        it('should call siteService with correct params when only folderId is provided', () => {
            const mockContent: DotCMSContentlet[] = [];
            dotSiteService.getContentByFolder.mockReturnValue(of(mockContent));

            spectator.service.getContentByFolder({ folderId: '123' });

            expect(dotSiteService.getContentByFolder).toHaveBeenCalledWith({
                hostFolderId: '123',
                showLinks: false,
                showDotAssets: true,
                showPages: false,
                showFiles: true,
                showFolders: false,
                showWorking: true,
                showArchived: false,
                sortByDesc: true,
                mimeTypes: []
            });
        });

        it('should call siteService with mimeTypes when provided', () => {
            const mockContent: DotCMSContentlet[] = [];
            const mimeTypes = ['image/jpeg', 'image/png'];
            dotSiteService.getContentByFolder.mockReturnValue(of(mockContent));

            spectator.service.getContentByFolder({ folderId: '123', mimeTypes });

            expect(dotSiteService.getContentByFolder).toHaveBeenCalledWith({
                hostFolderId: '123',
                showLinks: false,
                showDotAssets: true,
                showPages: false,
                showFiles: true,
                showFolders: false,
                showWorking: true,
                showArchived: false,
                sortByDesc: true,
                mimeTypes
            });
        });

        it('should return content from siteService', (done) => {
            const mockContent: DotCMSContentlet[] = [
                createFakeContentlet({
                    inode: 'content-1',
                    title: 'Test Content',
                    identifier: 'content-1'
                })
            ];
            dotSiteService.getContentByFolder.mockReturnValue(of(mockContent));

            spectator.service.getContentByFolder({ folderId: '123' }).subscribe((result) => {
                expect(result).toEqual(mockContent);
                done();
            });
        });

        it('should handle errors from getContentByFolder', (done) => {
            const error = new Error('Failed to fetch content');
            dotSiteService.getContentByFolder.mockReturnValue(throwError(() => error));

            spectator.service.getContentByFolder({ folderId: '123' }).subscribe({
                next: () => fail('should have thrown an error'),
                error: (err) => {
                    expect(err).toBe(error);
                    done();
                }
            });
        });

        it('should handle empty mimeTypes array', () => {
            const mockContent: DotCMSContentlet[] = [];
            dotSiteService.getContentByFolder.mockReturnValue(of(mockContent));

            spectator.service.getContentByFolder({ folderId: '123', mimeTypes: [] });

            expect(dotSiteService.getContentByFolder).toHaveBeenCalledWith(
                expect.objectContaining({
                    mimeTypes: []
                })
            );
        });
    });
});
