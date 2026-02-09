import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotContentDriveSearchRequest } from '@dotcms/dotcms-models';

import { DotContentDriveService } from './dot-content-drive.service';

describe('DotContentDriveService', () => {
    let spectator: SpectatorHttp<DotContentDriveService>;

    const createHttp = createHttpFactory(DotContentDriveService);

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('search', () => {
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
