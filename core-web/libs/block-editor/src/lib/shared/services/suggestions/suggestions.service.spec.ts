import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SuggestionsService } from './suggestions.service';

describe('SuggestionsService', () => {
    let service: SuggestionsService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [SuggestionsService],
            teardown: { destroyAfterEach: false }
        });
        service = TestBed.inject(SuggestionsService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('getContentlets', () => {
        const flushEmpty = (req: { flush: (body: unknown) => void }) =>
            req.flush({ entity: { jsonObjectView: { contentlets: [] } } });

        it('builds a multi-token query that requires ALL tokens (AC2)', () => {
            service
                .getContentlets({
                    contentType: 'Blog',
                    filter: 'White Water',
                    currentLanguage: 1,
                    contentletIdentifier: undefined
                })
                .subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.method).toBe('POST');
            expect(req.request.body.query).toBe(
                '+contentType:Blog +languageId:1 +deleted:false +working:true +catchall:*White* +catchall:*Water* title:"White Water"^15'
            );
            flushEmpty(req);
        });

        it('builds a single-word query (AC3)', () => {
            service
                .getContentlets({
                    contentType: 'Blog',
                    filter: 'Water',
                    currentLanguage: 1,
                    contentletIdentifier: undefined
                })
                .subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.method).toBe('POST');
            expect(req.request.body.query).toBe(
                '+contentType:Blog +languageId:1 +deleted:false +working:true +catchall:*Water* title:"Water"^15'
            );
            flushEmpty(req);
        });

        it('omits catchall/title clauses for an empty filter (AC4)', () => {
            service
                .getContentlets({
                    contentType: 'Blog',
                    filter: '',
                    currentLanguage: 1,
                    contentletIdentifier: undefined
                })
                .subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.method).toBe('POST');
            expect(req.request.body.query).toBe(
                '+contentType:Blog +languageId:1 +deleted:false +working:true'
            );
            flushEmpty(req);
        });

        it('omits catchall/title clauses for a whitespace-only filter (AC4 edge case)', () => {
            service
                .getContentlets({
                    contentType: 'Blog',
                    filter: '   ',
                    currentLanguage: 1,
                    contentletIdentifier: undefined
                })
                .subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.body.query).toBe(
                '+contentType:Blog +languageId:1 +deleted:false +working:true'
            );
            flushEmpty(req);
        });

        it('preserves the identifier branch for hyphenated filters (AC5)', () => {
            service
                .getContentlets({
                    contentType: 'Blog',
                    filter: 'abc-def',
                    currentLanguage: 1,
                    contentletIdentifier: undefined
                })
                .subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.method).toBe('POST');
            expect(req.request.body.query).toBe(
                '+contentType:Blog +languageId:1 +deleted:false +working:true +catchall:abc-def'
            );
            flushEmpty(req);
        });

        it('includes the -identifier exclusion when contentletIdentifier is provided', () => {
            service
                .getContentlets({
                    contentType: 'Blog',
                    filter: 'foo',
                    currentLanguage: 1,
                    contentletIdentifier: 'xyz'
                })
                .subscribe();

            const req = httpMock.expectOne('/api/content/_search');
            expect(req.request.method).toBe('POST');
            expect(req.request.body.query).toBe(
                '+contentType:Blog -identifier:xyz +languageId:1 +deleted:false +working:true +catchall:*foo* title:"foo"^15'
            );
            flushEmpty(req);
        });

        it('maps the response to entity.jsonObjectView.contentlets', (done) => {
            const contentlets = [{ identifier: '1' }, { identifier: '2' }];

            service
                .getContentlets({
                    contentType: 'Blog',
                    filter: 'foo',
                    currentLanguage: 1,
                    contentletIdentifier: undefined
                })
                .subscribe((result) => {
                    expect(result).toEqual(contentlets);
                    done();
                });

            const req = httpMock.expectOne('/api/content/_search');
            req.flush({ entity: { jsonObjectView: { contentlets } } });
        });
    });
});
