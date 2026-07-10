import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { DotFolder, DotFolderEntity, DotPagination, FolderSearchView } from '@dotcms/dotcms-models';
import { createFakeFolder } from '@dotcms/utils-testing';

import { DotFolderService } from './dot-folder.service';

describe('DotFolderService', () => {
    let spectator: SpectatorHttp<DotFolderService>;

    const createHttp = createHttpFactory(DotFolderService);

    const mockFolders: DotFolder[] = [
        createFakeFolder({
            id: '1',
            hostName: 'demo.dotcms.com',
            path: '//application/folder1',
            addChildrenAllowed: true
        }),
        createFakeFolder({
            id: '2',
            hostName: 'demo.dotcms.com',
            path: '//application/folder2',
            addChildrenAllowed: false
        })
    ];

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('getFolders', () => {
        it('should call the correct endpoint with POST method and normalize path without leading slash', () => {
            const inputPath = 'application/test';

            spectator.service.getFolders(inputPath).subscribe((folders: DotFolder[]) => {
                expect(folders).toEqual(mockFolders);
            });

            const req = spectator.expectOne('/api/v1/folder/byPath', HttpMethod.POST);
            expect(req.request.body).toEqual({ path: '//application/test' });
            req.flush({ entity: mockFolders });
        });

        it('should normalize path that starts with single slash', () => {
            const inputPath = '/application/test';

            spectator.service.getFolders(inputPath).subscribe((folders: DotFolder[]) => {
                expect(folders).toEqual(mockFolders);
            });

            const req = spectator.expectOne('/api/v1/folder/byPath', HttpMethod.POST);
            expect(req.request.body).toEqual({ path: '//application/test' });
            req.flush({ entity: mockFolders });
        });

        it('should keep path unchanged when it already starts with double slash', () => {
            const inputPath = '//application/test';

            spectator.service.getFolders(inputPath).subscribe((folders: DotFolder[]) => {
                expect(folders).toEqual(mockFolders);
            });

            const req = spectator.expectOne('/api/v1/folder/byPath', HttpMethod.POST);
            expect(req.request.body).toEqual({ path: '//application/test' });
            req.flush({ entity: mockFolders });
        });

        it('should handle empty path', () => {
            const inputPath = '';

            spectator.service.getFolders(inputPath).subscribe((folders: DotFolder[]) => {
                expect(folders).toEqual(mockFolders);
            });

            const req = spectator.expectOne('/api/v1/folder/byPath', HttpMethod.POST);
            expect(req.request.body).toEqual({ path: '//' });
            req.flush({ entity: mockFolders });
        });

        it('should handle root path with single slash', () => {
            const inputPath = '/';

            spectator.service.getFolders(inputPath).subscribe((folders: DotFolder[]) => {
                expect(folders).toEqual(mockFolders);
            });

            const req = spectator.expectOne('/api/v1/folder/byPath', HttpMethod.POST);
            expect(req.request.body).toEqual({ path: '//' });
            req.flush({ entity: mockFolders });
        });

        it('should handle complex nested path', () => {
            const inputPath = '/application/content/images/2024/january';

            spectator.service.getFolders(inputPath).subscribe((folders: DotFolder[]) => {
                expect(folders).toEqual(mockFolders);
            });

            const req = spectator.expectOne('/api/v1/folder/byPath', HttpMethod.POST);
            expect(req.request.body).toEqual({ path: '//application/content/images/2024/january' });
            req.flush({ entity: mockFolders });
        });

        it('should return empty array when API returns empty entity', () => {
            const inputPath = 'application/empty';

            spectator.service.getFolders(inputPath).subscribe((folders: DotFolder[]) => {
                expect(folders).toEqual([]);
            });

            const req = spectator.expectOne('/api/v1/folder/byPath', HttpMethod.POST);
            expect(req.request.body).toEqual({ path: '//application/empty' });
            req.flush({ entity: [] });
        });

        it('should handle paths with multiple leading slashes by keeping them as-is', () => {
            const inputPath = '///application/test';

            spectator.service.getFolders(inputPath).subscribe((folders: DotFolder[]) => {
                expect(folders).toEqual(mockFolders);
            });

            const req = spectator.expectOne('/api/v1/folder/byPath', HttpMethod.POST);
            expect(req.request.body).toEqual({ path: '///application/test' });
            req.flush({ entity: mockFolders });
        });

        it('should handle paths with special characters', () => {
            const inputPath = '/application/test-folder_2024/images';

            spectator.service.getFolders(inputPath).subscribe((folders: DotFolder[]) => {
                expect(folders).toEqual(mockFolders);
            });

            const req = spectator.expectOne('/api/v1/folder/byPath', HttpMethod.POST);
            expect(req.request.body).toEqual({ path: '//application/test-folder_2024/images' });
            req.flush({ entity: mockFolders });
        });
    });

    describe('searchFolders', () => {
        const mockSearchFolders: FolderSearchView[] = [
            { id: '1', inode: 'inode-1', name: 'folder1', path: '/', addChildrenAllowed: true },
            { id: '2', inode: 'inode-2', name: 'folder2', path: '/', addChildrenAllowed: false }
        ];
        const mockPagination: DotPagination = { currentPage: 1, perPage: 40, totalEntries: 2 };

        it('should call GET /api/v1/folder/search with required siteId and defaults', () => {
            spectator.service
                .searchFolders({ siteId: 'site-1' })
                .subscribe(({ folders, pagination }) => {
                    expect(folders).toEqual(mockSearchFolders);
                    expect(pagination).toEqual(mockPagination);
                });

            const url = '/api/v1/folder/search?siteId=site-1&page=1&per_page=40';
            const req = spectator.expectOne(url, HttpMethod.GET);
            req.flush({ entity: mockSearchFolders, pagination: mockPagination });
        });

        it('should include path, recursive, name, orderby, direction and pagination params when provided', () => {
            spectator.service
                .searchFolders({
                    siteId: 'site-1',
                    path: '/level1/',
                    recursive: true,
                    name: 'foo',
                    orderby: 'mod_date',
                    direction: 'DESC',
                    page: 2,
                    per_page: 10
                })
                .subscribe();

            const url =
                '/api/v1/folder/search?siteId=site-1&path=/level1/&recursive=true&name=foo&orderby=mod_date&direction=DESC&page=2&per_page=10';
            const req = spectator.expectOne(url, HttpMethod.GET);
            req.flush({ entity: [], pagination: { currentPage: 2, perPage: 10, totalEntries: 0 } });
        });

        it('should set recursive param to false when explicitly false', () => {
            spectator.service.searchFolders({ siteId: 'site-1', recursive: false }).subscribe();

            const url = '/api/v1/folder/search?siteId=site-1&recursive=false&page=1&per_page=40';
            const req = spectator.expectOne(url, HttpMethod.GET);
            req.flush({ entity: [], pagination: mockPagination });
        });

        it('should return an empty folders array when the API returns no entities', () => {
            spectator.service.searchFolders({ siteId: 'site-1' }).subscribe(({ folders }) => {
                expect(folders).toEqual([]);
            });

            const url = '/api/v1/folder/search?siteId=site-1&page=1&per_page=40';
            const req = spectator.expectOne(url, HttpMethod.GET);
            req.flush({ entity: [], pagination: { currentPage: 1, perPage: 40, totalEntries: 0 } });
        });
    });

    describe('createFolder', () => {
        it('should call the correct endpoint with POST method and return created folder', () => {
            const folderEntity: DotFolderEntity = {
                assetPath: '//application/new-folder',
                data: {
                    title: 'New Folder',
                    showOnMenu: true,
                    sortOrder: 1
                }
            };
            const createdFolder: DotFolder = createFakeFolder({
                id: 'new-folder-id',
                path: '//application/new-folder'
            });

            spectator.service.createFolder(folderEntity).subscribe((folder: DotFolder) => {
                expect(folder).toEqual(createdFolder);
            });

            const req = spectator.expectOne('/api/v1/assets/folders', HttpMethod.POST);
            expect(req.request.body).toEqual(folderEntity);
            req.flush({ entity: createdFolder });
        });

        it('should handle folder entity with all optional fields', () => {
            const folderEntity: DotFolderEntity = {
                assetPath: '//application/complex-folder',
                data: {
                    title: 'Complex Folder',
                    showOnMenu: false,
                    sortOrder: 5,
                    fileMasks: ['*.jpg', '*.png'],
                    defaultAssetType: 'image'
                }
            };
            const createdFolder: DotFolder = createFakeFolder({
                id: 'complex-folder-id',
                path: '//application/complex-folder'
            });

            spectator.service.createFolder(folderEntity).subscribe((folder: DotFolder) => {
                expect(folder).toEqual(createdFolder);
            });

            const req = spectator.expectOne('/api/v1/assets/folders', HttpMethod.POST);
            expect(req.request.body).toEqual(folderEntity);
            req.flush({ entity: createdFolder });
        });

        it('should handle folder entity with minimal required fields', () => {
            const folderEntity: DotFolderEntity = {
                assetPath: '//application/minimal-folder',
                data: {
                    title: 'Minimal Folder'
                }
            };
            const createdFolder: DotFolder = createFakeFolder({
                id: 'minimal-folder-id',
                path: '//application/minimal-folder'
            });

            spectator.service.createFolder(folderEntity).subscribe((folder: DotFolder) => {
                expect(folder).toEqual(createdFolder);
            });

            const req = spectator.expectOne('/api/v1/assets/folders', HttpMethod.POST);
            expect(req.request.body).toEqual(folderEntity);
            req.flush({ entity: createdFolder });
        });
    });

    describe('saveFolder', () => {
        it('should call the correct endpoint with PUT method and return saved folder', () => {
            const folderEntity: DotFolderEntity = {
                assetPath: '//application/existing-folder',
                data: {
                    title: 'Updated Folder',
                    showOnMenu: true,
                    sortOrder: 2
                }
            };
            const savedFolder: DotFolder = createFakeFolder({
                id: 'existing-folder-id',
                path: '//application/existing-folder'
            });

            spectator.service.saveFolder(folderEntity).subscribe((folder: DotFolder) => {
                expect(folder).toEqual(savedFolder);
            });

            const req = spectator.expectOne('/api/v1/assets/folders', HttpMethod.PUT);
            expect(req.request.body).toEqual(folderEntity);
            req.flush({ entity: savedFolder });
        });

        it('should handle folder entity with all optional fields', () => {
            const folderEntity: DotFolderEntity = {
                assetPath: '//application/updated-complex-folder',
                data: {
                    title: 'Updated Complex Folder',
                    showOnMenu: true,
                    sortOrder: 10,
                    fileMasks: ['*.pdf', '*.doc'],
                    defaultAssetType: 'document'
                }
            };
            const savedFolder: DotFolder = createFakeFolder({
                id: 'updated-complex-folder-id',
                path: '//application/updated-complex-folder'
            });

            spectator.service.saveFolder(folderEntity).subscribe((folder: DotFolder) => {
                expect(folder).toEqual(savedFolder);
            });

            const req = spectator.expectOne('/api/v1/assets/folders', HttpMethod.PUT);
            expect(req.request.body).toEqual(folderEntity);
            req.flush({ entity: savedFolder });
        });

        it('should handle folder entity with minimal required fields', () => {
            const folderEntity: DotFolderEntity = {
                assetPath: '//application/updated-minimal-folder',
                data: {
                    title: 'Updated Minimal Folder'
                }
            };
            const savedFolder: DotFolder = createFakeFolder({
                id: 'updated-minimal-folder-id',
                path: '//application/updated-minimal-folder'
            });

            spectator.service.saveFolder(folderEntity).subscribe((folder: DotFolder) => {
                expect(folder).toEqual(savedFolder);
            });

            const req = spectator.expectOne('/api/v1/assets/folders', HttpMethod.PUT);
            expect(req.request.body).toEqual(folderEntity);
            req.flush({ entity: savedFolder });
        });
    });
});
