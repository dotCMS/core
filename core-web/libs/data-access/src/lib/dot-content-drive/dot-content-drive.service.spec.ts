import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { DotContentDriveSearchRequest } from '@dotcms/dotcms-models';

import { DotContentDriveService } from './dot-content-drive.service';

describe('DotContentDriveService', () => {
    let spectator: SpectatorHttp<DotContentDriveService>;

    const createHttp = createHttpFactory(DotContentDriveService);

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('search', () => {
        // The drive-search view returns folders without an `inode`, but the table keys rows on it
        // (`dataKey="inode"`), and the Action Center's contentlet-only actions read it too. dotCMS
        // keeps a folder's inode equal to its identifier, so backfilling it here is what lets a
        // folder row be keyed like any other without every consumer special-casing folders.
        describe('folder inode backfill', () => {
            it('should backfill a folder row inode from its identifier', () => {
                let result: unknown;
                spectator.service.search({ assetPath: '/' }).subscribe((r) => (result = r));

                spectator.expectOne('/api/v1/drive/search', HttpMethod.POST).flush({
                    entity: {
                        list: [{ type: 'folder', identifier: 'folder-id-1', name: 'gallery' }]
                    }
                });

                expect(result).toEqual({
                    list: [
                        {
                            type: 'folder',
                            identifier: 'folder-id-1',
                            inode: 'folder-id-1',
                            name: 'gallery'
                        }
                    ]
                });
            });

            it('should leave a folder that already carries an inode untouched', () => {
                let result: { list: { inode: string }[] } | undefined;
                spectator.service
                    .search({ assetPath: '/' })
                    .subscribe((r) => (result = r as { list: { inode: string }[] }));

                spectator.expectOne('/api/v1/drive/search', HttpMethod.POST).flush({
                    entity: {
                        list: [{ type: 'folder', identifier: 'folder-id-1', inode: 'legacy-inode' }]
                    }
                });

                expect(result?.list[0].inode).toBe('legacy-inode');
            });

            it('should leave contentlets untouched', () => {
                let result: { list: { inode: string }[] } | undefined;
                spectator.service
                    .search({ assetPath: '/' })
                    .subscribe((r) => (result = r as { list: { inode: string }[] }));

                spectator.expectOne('/api/v1/drive/search', HttpMethod.POST).flush({
                    entity: {
                        list: [{ identifier: 'content-id', inode: 'content-inode' }]
                    }
                });

                expect(result?.list[0].inode).toBe('content-inode');
            });
        });

        it('should call the endpoint with basic request body', () => {
            const request: DotContentDriveSearchRequest = {
                assetPath: '//demo.dotcms.com/documents/'
            };

            spectator.service.search(request).subscribe();

            const req = spectator.expectOne('/api/v1/drive/search', HttpMethod.POST);
            expect(req.request.body).toEqual(request);
            req.flush([]);
        });

        it('should call the endpoint with request body including filters', () => {
            const request: DotContentDriveSearchRequest = {
                assetPath: '//demo.dotcms.com/',
                filters: {
                    text: 'product review',
                    filterFolders: true
                }
            };

            spectator.service.search(request).subscribe();

            const req = spectator.expectOne('/api/v1/drive/search', HttpMethod.POST);
            expect(req.request.body).toEqual(request);
            req.flush([]);
        });

        it('should call the endpoint with request body including content types and pagination', () => {
            const request: DotContentDriveSearchRequest = {
                assetPath: '//demo.dotcms.com/',
                contentTypes: ['Blog', 'News'],
                offset: 0,
                maxResults: 20
            };

            spectator.service.search(request).subscribe();

            const req = spectator.expectOne('/api/v1/drive/search', HttpMethod.POST);
            expect(req.request.body).toEqual(request);
            req.flush([]);
        });

        it('should call the endpoint with request body including sort and language', () => {
            const request: DotContentDriveSearchRequest = {
                assetPath: '//demo.dotcms.com/',
                sortBy: 'title:asc',
                language: ['en', 'es']
            };

            spectator.service.search(request).subscribe();

            const req = spectator.expectOne('/api/v1/drive/search', HttpMethod.POST);
            expect(req.request.body).toEqual(request);
            req.flush([]);
        });

        it('should call the endpoint with complete request body', () => {
            const request: DotContentDriveSearchRequest = {
                assetPath: '//demo.dotcms.com/documents/',
                includeSystemHost: false,
                language: ['en'],
                contentTypes: ['Blog'],
                baseTypes: ['CONTENT'],
                mimeTypes: ['image/jpeg', 'image/png'],
                filters: {
                    text: 'test search',
                    filterFolders: false
                },
                offset: 10,
                maxResults: 50,
                sortBy: 'modDate:desc',
                live: true,
                archived: false,
                showFolders: true
            };

            spectator.service.search(request).subscribe();

            const req = spectator.expectOne('/api/v1/drive/search', HttpMethod.POST);
            expect(req.request.body).toEqual(request);
            req.flush([]);
        });
    });
});
