import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotFolder } from '@dotcms/dotcms-models';
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
});
