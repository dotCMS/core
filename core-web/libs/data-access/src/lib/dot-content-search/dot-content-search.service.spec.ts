import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotContentSearchService, EsQueryParamsSearch } from './dot-content-search.service';

describe('DotContentSearchService', () => {
    let spectator: SpectatorHttp<DotContentSearchService>;
    const createHttp = createHttpFactory(DotContentSearchService);

    beforeEach(() => (spectator = createHttp()));

    it('should call the search method with the right EsQueryParamsSearch', (done) => {
        const params: EsQueryParamsSearch = {
            query: 'test',
            limit: 10,
            offset: 0
        };

        spectator.service.get(params).subscribe((resp) => {
            expect(resp).toEqual({ contentlets: [] });
            done();
        });

        const req = spectator.expectOne('/api/content/_search', HttpMethod.POST);
        expect(req.request.body).toEqual({
            query: 'test',
            sort: 'score,modDate desc',
            limit: 10,
            offset: 0
        });
        req.flush({
            entity: {
                contentlets: []
            }
        });
    });
});
