import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ESSearchResponse } from '@dotcms/dotcms-models';

import { DotEsSearchService } from './dot-es-search.service';

const MOCK_QUERY = '{ "query": { "match_all": {} } }';

const MOCK_SEARCH_RESPONSE: ESSearchResponse = {
    contentlets: [],
    esresponse: [
        {
            hits: { total: 5, hits: [] },
            took: 42,
            timed_out: false
        }
    ]
};

describe('DotEsSearchService', () => {
    let service: DotEsSearchService;
    let httpController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
        service = TestBed.inject(DotEsSearchService);
        httpController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpController.verify();
    });

    describe('search()', () => {
        it('should POST to /api/es/search with default params', () => {
            service.search(MOCK_QUERY, {}).subscribe((res) => {
                expect(res).toEqual(MOCK_SEARCH_RESPONSE);
            });

            const req = httpController.expectOne((r) => r.url === '/api/es/search');
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual(JSON.parse(MOCK_QUERY));
            expect(req.request.params.get('depth')).toBe('1');
            expect(req.request.params.get('live')).toBe('true');
            expect(req.request.params.get('allCategoriesInfo')).toBe('false');
            req.flush(MOCK_SEARCH_RESPONSE);
        });

        it('should include userid param when provided', () => {
            service.search(MOCK_QUERY, { userid: 'admin@dotcms.com' }).subscribe();
            const req = httpController.expectOne((r) => r.url === '/api/es/search');
            expect(req.request.params.get('userid')).toBe('admin@dotcms.com');
            req.flush(MOCK_SEARCH_RESPONSE);
        });

        it('should forward depth and live params', () => {
            service.search(MOCK_QUERY, { depth: 2, live: false }).subscribe();
            const req = httpController.expectOne((r) => r.url === '/api/es/search');
            expect(req.request.params.get('depth')).toBe('2');
            expect(req.request.params.get('live')).toBe('false');
            req.flush(MOCK_SEARCH_RESPONSE);
        });
    });
});
