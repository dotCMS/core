import { SpectatorHttp, createHttpFactory } from '@ngneat/spectator/jest';

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
    let spectator: SpectatorHttp<DotEsSearchService>;

    const createHttp = createHttpFactory(DotEsSearchService);

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('search()', () => {
        it('should POST to /api/es/search with default params', () => {
            spectator.service.search(MOCK_QUERY, {}).subscribe((res) => {
                expect(res).toEqual(MOCK_SEARCH_RESPONSE);
            });

            const req = spectator.controller.expectOne((r) => r.url === '/api/es/search');
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual(JSON.parse(MOCK_QUERY));
            expect(req.request.params.get('depth')).toBe('1');
            expect(req.request.params.get('live')).toBe('true');
            expect(req.request.params.get('userid')).toBeNull();
            req.flush(MOCK_SEARCH_RESPONSE);
        });

        it('should include userid param when provided', () => {
            spectator.service.search(MOCK_QUERY, { userid: 'admin@dotcms.com' }).subscribe();
            const req = spectator.controller.expectOne((r) => r.url === '/api/es/search');
            expect(req.request.params.get('userid')).toBe('admin@dotcms.com');
            req.flush(MOCK_SEARCH_RESPONSE);
        });

        it('should always send depth=1 and forward live param', () => {
            spectator.service.search(MOCK_QUERY, { live: false }).subscribe();
            const req = spectator.controller.expectOne((r) => r.url === '/api/es/search');
            expect(req.request.params.get('depth')).toBe('1');
            expect(req.request.params.get('live')).toBe('false');
            req.flush(MOCK_SEARCH_RESPONSE);
        });

        it('should emit a SyntaxError when query is not valid JSON', (done) => {
            spectator.service.search('{invalid json}', {}).subscribe({
                error: (err: unknown) => {
                    expect(err).toBeInstanceOf(SyntaxError);
                    expect((err as SyntaxError).message).toBe('Invalid JSON query');
                    done();
                }
            });
        });
    });
});
