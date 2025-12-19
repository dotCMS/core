import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotCMSContentlet, ESContent } from '@dotcms/dotcms-models';

import { DotPageListService, ListPagesParams } from './dot-page-list.service';

import { FAVORITE_PAGE_LIMIT } from '../dot-pages-store/dot-pages.store';

const MOCK_ES_CONTENT: ESContent = {
    contentTook: 0,
    jsonObjectView: {
        contentlets: [
            {
                identifier: 'page-1',
                title: 'Home Page',
                url: '/home',
                languageId: 1,
                inode: 'inode-1',
                working: true,
                live: true,
                deleted: false,
                baseType: 'htmlpage'
            } as DotCMSContentlet,
            {
                identifier: 'page-2',
                title: 'About Page',
                url: '/about',
                languageId: 1,
                inode: 'inode-2',
                working: true,
                live: true,
                deleted: false,
                baseType: 'htmlpage'
            } as DotCMSContentlet
        ]
    },
    queryTook: 0,
    resultsSize: 2
};

const MOCK_SINGLE_PAGE: DotCMSContentlet = {
    identifier: 'page-1',
    title: 'Home Page',
    url: '/home',
    languageId: 1,
    inode: 'inode-1',
    working: true,
    live: true,
    deleted: false,
    baseType: 'htmlpage'
} as DotCMSContentlet;

const DEFAULT_LIST_PARAMS: ListPagesParams = {
    search: '',
    sort: 'title ASC',
    limit: 30,
    offset: 0,
    languageId: 1,
    host: 'demo.dotcms.com',
    archived: false
};

describe('DotPageListService', () => {
    let spectator: SpectatorService<DotPageListService>;
    let httpMock: HttpTestingController;

    const createService = createServiceFactory({
        service: DotPageListService,
        providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    beforeEach(() => {
        spectator = createService();
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify(); // Ensure no outstanding HTTP requests
    });

    it('should create', () => {
        expect(spectator.service).toBeTruthy();
    });

    describe('getPages', () => {
        it('should make POST request to correct endpoint', () => {
            spectator.service.getPages(DEFAULT_LIST_PARAMS).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.method).toBe('POST');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should send correct query parameters in request body', () => {
            spectator.service.getPages(DEFAULT_LIST_PARAMS).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body).toEqual({
                query: '+working:true +(urlmap:* OR basetype:5)  +languageId:1 +deleted:false +conhost:demo.dotcms.com',
                sort: 'title ASC',
                limit: 30,
                offset: 0
            });

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should return ESContent from response entity', (done) => {
            spectator.service.getPages(DEFAULT_LIST_PARAMS).subscribe((result) => {
                expect(result).toEqual(MOCK_ES_CONTENT);
                done();
            });

            const req = httpMock.expectOne('/api/content/_search');
            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should include search term in query when provided', () => {
            const paramsWithSearch: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                search: 'home'
            };

            spectator.service.getPages(paramsWithSearch).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain(
                '+(title:home* OR path:*home* OR urlmap:*home*)'
            );

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should use wildcard languageId when null', () => {
            const paramsWithNullLang: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                languageId: null
            };

            spectator.service.getPages(paramsWithNullLang).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+languageId:*');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should include archived query when archived is true', () => {
            const paramsWithArchived: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                archived: true
            };

            spectator.service.getPages(paramsWithArchived).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+deleted:true');
            expect(req.request.body.query).not.toContain('+deleted:false');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should exclude archived query when archived is false', () => {
            spectator.service.getPages(DEFAULT_LIST_PARAMS).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+deleted:false');
            expect(req.request.body.query).not.toContain('+deleted:true');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should include host query when host is provided', () => {
            spectator.service.getPages(DEFAULT_LIST_PARAMS).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+conhost:demo.dotcms.com');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should omit host query when host is empty', () => {
            const paramsWithoutHost: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                host: ''
            };

            spectator.service.getPages(paramsWithoutHost).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).not.toContain('+conhost:');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should use custom sort parameter', () => {
            const paramsWithSort: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                sort: 'modDate DESC'
            };

            spectator.service.getPages(paramsWithSort).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.sort).toBe('modDate DESC');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should use custom limit and offset', () => {
            const paramsWithPagination: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                limit: 50,
                offset: 100
            };

            spectator.service.getPages(paramsWithPagination).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.limit).toBe(50);
            expect(req.request.body.offset).toBe(100);

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should build complex query with multiple search criteria', () => {
            const complexParams: ListPagesParams = {
                search: 'test',
                sort: 'title ASC',
                limit: 20,
                offset: 10,
                languageId: 2,
                host: 'mysite.com',
                archived: false
            };

            spectator.service.getPages(complexParams).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            const query = req.request.body.query;

            expect(query).toContain('+working:true');
            expect(query).toContain('+(urlmap:* OR basetype:5)');
            expect(query).toContain('+(title:test* OR path:*test* OR urlmap:*test*)');
            expect(query).toContain('+languageId:2');
            expect(query).toContain('+deleted:false');
            expect(query).toContain('+conhost:mysite.com');

            req.flush({ entity: MOCK_ES_CONTENT });
        });
    });

    describe('getFavoritePages', () => {
        it('should make POST request to correct endpoint', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                userId: 'user-123'
            };

            spectator.service.getFavoritePages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.method).toBe('POST');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should send correct query for favorite pages', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                userId: 'user-123'
            };

            spectator.service.getFavoritePages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body).toEqual({
                query: '+contentType:dotFavoritePage +deleted:false +working:true +conhost:demo.dotcms.com +owner:user-123',
                sort: 'title ASC',
                limit: FAVORITE_PAGE_LIMIT,
                offset: 0
            });

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should use fixed sort order for favorite pages', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                sort: 'modDate DESC', // This should be ignored
                userId: 'user-123'
            };

            spectator.service.getFavoritePages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.sort).toBe('title ASC');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should use FAVORITE_PAGE_LIMIT constant for limit', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                limit: 100, // This should be ignored
                userId: 'user-123'
            };

            spectator.service.getFavoritePages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.limit).toBe(FAVORITE_PAGE_LIMIT);

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should always use offset 0 for favorite pages', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                offset: 50, // This should be ignored
                userId: 'user-123'
            };

            spectator.service.getFavoritePages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.offset).toBe(0);

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should include host in query when provided', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                host: 'mysite.com',
                userId: 'user-123'
            };

            spectator.service.getFavoritePages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+conhost:mysite.com');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should omit host from query when empty', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                host: '',
                userId: 'user-123'
            };

            spectator.service.getFavoritePages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).not.toContain('+conhost:');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should include userId in query', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                userId: 'user-456'
            };

            spectator.service.getFavoritePages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+owner:user-456');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should return ESContent from response entity', (done) => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                userId: 'user-123'
            };

            spectator.service.getFavoritePages(params).subscribe((result) => {
                expect(result).toEqual(MOCK_ES_CONTENT);
                done();
            });

            const req = httpMock.expectOne('/api/content/_search');
            req.flush({ entity: MOCK_ES_CONTENT });
        });
    });

    describe('getSinglePage', () => {
        it('should make POST request to correct endpoint', () => {
            spectator.service.getSinglePage('page-1').subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.method).toBe('POST');

            req.flush({
                entity: {
                    jsonObjectView: {
                        contentlets: [MOCK_SINGLE_PAGE]
                    }
                }
            });
        });

        it('should send correct query with identifier', () => {
            spectator.service.getSinglePage('page-123').subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body).toEqual({
                query: '+identifier:page-123',
                sort: 'title ASC',
                limit: 1,
                offset: 0
            });

            req.flush({
                entity: {
                    jsonObjectView: {
                        contentlets: [MOCK_SINGLE_PAGE]
                    }
                }
            });
        });

        it('should use limit of 1 for single page request', () => {
            spectator.service.getSinglePage('page-1').subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.limit).toBe(1);

            req.flush({
                entity: {
                    jsonObjectView: {
                        contentlets: [MOCK_SINGLE_PAGE]
                    }
                }
            });
        });

        it('should return first contentlet from response', (done) => {
            spectator.service.getSinglePage('page-1').subscribe((result) => {
                expect(result).toEqual(MOCK_SINGLE_PAGE);
                expect(result.identifier).toBe('page-1');
                expect(result.title).toBe('Home Page');
                done();
            });

            const req = httpMock.expectOne('/api/content/_search');
            req.flush({
                entity: {
                    jsonObjectView: {
                        contentlets: [MOCK_SINGLE_PAGE]
                    }
                }
            });
        });

        it('should handle identifier with special characters', () => {
            const specialIdentifier = 'page-with-special-chars-123-abc';

            spectator.service.getSinglePage(specialIdentifier).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toBe(`+identifier:${specialIdentifier}`);

            req.flush({
                entity: {
                    jsonObjectView: {
                        contentlets: [MOCK_SINGLE_PAGE]
                    }
                }
            });
        });
    });

    describe('Query Building', () => {
        it('should build query without search term', () => {
            spectator.service.getPages(DEFAULT_LIST_PARAMS).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            const query = req.request.body.query;

            // Should not contain search-specific patterns
            expect(query).not.toContain('+(title:');
            expect(query).not.toContain('path:*');

            // urlmap:* is always present as part of "+(urlmap:* OR basetype:5)"
            expect(query).toContain('+(urlmap:* OR basetype:5)');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should build search query with wildcard patterns', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                search: 'blog'
            };

            spectator.service.getPages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            const query = req.request.body.query;

            expect(query).toContain('title:blog*'); // Suffix wildcard for title
            expect(query).toContain('path:*blog*'); // Prefix and suffix for path
            expect(query).toContain('urlmap:*blog*'); // Prefix and suffix for urlmap

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should always include working:true in query', () => {
            spectator.service.getPages(DEFAULT_LIST_PARAMS).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+working:true');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should always include basetype or urlmap in query', () => {
            spectator.service.getPages(DEFAULT_LIST_PARAMS).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+(urlmap:* OR basetype:5)');

            req.flush({ entity: MOCK_ES_CONTENT });
        });
    });

    describe('Error Handling', () => {
        it('should propagate HTTP errors from getPages', (done) => {
            spectator.service.getPages(DEFAULT_LIST_PARAMS).subscribe({
                next: () => fail('Should have failed'),
                error: (error) => {
                    expect(error.status).toBe(500);
                    expect(error.statusText).toBe('Server Error');
                    done();
                }
            });

            const req = httpMock.expectOne('/api/content/_search');
            req.flush('Server error', { status: 500, statusText: 'Server Error' });
        });

        it('should propagate HTTP errors from getFavoritePages', (done) => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                userId: 'user-123'
            };

            spectator.service.getFavoritePages(params).subscribe({
                next: () => fail('Should have failed'),
                error: (error) => {
                    expect(error.status).toBe(404);
                    expect(error.statusText).toBe('Not Found');
                    done();
                }
            });

            const req = httpMock.expectOne('/api/content/_search');
            req.flush('Not found', { status: 404, statusText: 'Not Found' });
        });

        it('should propagate HTTP errors from getSinglePage', (done) => {
            spectator.service.getSinglePage('invalid-id').subscribe({
                next: () => fail('Should have failed'),
                error: (error) => {
                    expect(error.status).toBe(403);
                    expect(error.statusText).toBe('Forbidden');
                    done();
                }
            });

            const req = httpMock.expectOne('/api/content/_search');
            req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
        });
    });

    describe('Edge Cases', () => {
        it('should handle empty search string', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                search: ''
            };

            spectator.service.getPages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            const query = req.request.body.query;

            expect(query).not.toContain('+(title:');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should handle whitespace-only search string', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                search: '   '
            };

            spectator.service.getPages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            const query = req.request.body.query;

            // Whitespace search should be included as-is
            expect(query).toContain('+(title:   *');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should handle zero offset', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                offset: 0
            };

            spectator.service.getPages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.offset).toBe(0);

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should handle large pagination values', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                limit: 1000,
                offset: 5000
            };

            spectator.service.getPages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.limit).toBe(1000);
            expect(req.request.body.offset).toBe(5000);

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should handle undefined userId in favorite pages', () => {
            const params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                userId: undefined
            };

            spectator.service.getFavoritePages(params).subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+owner:undefined');

            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should handle empty string identifier in getSinglePage', () => {
            spectator.service.getSinglePage('').subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toBe('+identifier:');

            req.flush({
                entity: {
                    jsonObjectView: {
                        contentlets: [MOCK_SINGLE_PAGE]
                    }
                }
            });
        });

        it('should handle response with empty contentlets array', (done) => {
            spectator.service.getSinglePage('page-1').subscribe((result) => {
                expect(result).toBeUndefined();
                done();
            });

            const req = httpMock.expectOne('/api/content/_search');
            req.flush({
                entity: {
                    jsonObjectView: {
                        contentlets: []
                    }
                }
            });
        });
    });

    describe('Integration Workflows', () => {
        it('should handle complete pagination workflow', () => {
            // Page 1
            const page1Params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                limit: 10,
                offset: 0
            };

            spectator.service.getPages(page1Params).subscribe();

            let req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.limit).toBe(10);
            expect(req.request.body.offset).toBe(0);
            req.flush({ entity: MOCK_ES_CONTENT });

            // Page 2
            const page2Params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                limit: 10,
                offset: 10
            };

            spectator.service.getPages(page2Params).subscribe();

            req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.limit).toBe(10);
            expect(req.request.body.offset).toBe(10);
            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should handle search refinement workflow', () => {
            // Initial search
            const search1Params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                search: 'home'
            };

            spectator.service.getPages(search1Params).subscribe();

            let req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('home');
            req.flush({ entity: MOCK_ES_CONTENT });

            // Refined search
            const search2Params: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                search: 'home page'
            };

            spectator.service.getPages(search2Params).subscribe();

            req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('home page');
            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should handle switching between live and archived pages', () => {
            // Live pages
            const liveParams: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                archived: false
            };

            spectator.service.getPages(liveParams).subscribe();

            let req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+deleted:false');
            req.flush({ entity: MOCK_ES_CONTENT });

            // Archived pages
            const archivedParams: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                archived: true
            };

            spectator.service.getPages(archivedParams).subscribe();

            req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+deleted:true');
            req.flush({ entity: MOCK_ES_CONTENT });
        });

        it('should handle language switching workflow', () => {
            // English (languageId: 1)
            const englishParams: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                languageId: 1
            };

            spectator.service.getPages(englishParams).subscribe();

            let req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+languageId:1');
            req.flush({ entity: MOCK_ES_CONTENT });

            // Spanish (languageId: 2)
            const spanishParams: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                languageId: 2
            };

            spectator.service.getPages(spanishParams).subscribe();

            req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+languageId:2');
            req.flush({ entity: MOCK_ES_CONTENT });

            // All languages (languageId: null)
            const allLanguagesParams: ListPagesParams = {
                ...DEFAULT_LIST_PARAMS,
                languageId: null
            };

            spectator.service.getPages(allLanguagesParams).subscribe();

            req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toContain('+languageId:*');
            req.flush({ entity: MOCK_ES_CONTENT });
        });
    });
});
