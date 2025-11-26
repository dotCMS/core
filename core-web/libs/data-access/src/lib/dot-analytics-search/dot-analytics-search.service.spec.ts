import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { AnalyticsQueryType } from '@dotcms/dotcms-models';

import { DotAnalyticsSearchService } from './dot-analytics-search.service';

describe('DotAnalyticsSearchService', () => {
    let spectator: SpectatorHttp<DotAnalyticsSearchService>;
    const createHttp = createHttpFactory(DotAnalyticsSearchService);
    const mockResponse = { entity: [{ id: '1', name: 'result1' }] };
    const query = { measures: ['request.count'], orders: 'request.count DESC' };

    beforeEach(() => (spectator = createHttp()));

    it('should perform a POST request to the base URL and return results', (done) => {
        spectator.service.get(query).subscribe((results) => {
            expect(results).toEqual(mockResponse.entity);
            done();
        });

        const req = spectator.expectOne('/api/v1/analytics/content/_query', HttpMethod.POST);
        req.flush(mockResponse);

        expect(req.request.body).toEqual({ ...query });
    });

    it('should perform a POST request to the cube URL and return results', (done) => {
        spectator.service.get(query, AnalyticsQueryType.CUBE).subscribe((results) => {
            expect(results).toEqual(mockResponse.entity);
            done();
        });

        const req = spectator.expectOne('/api/v1/analytics/content/_query/cube', HttpMethod.POST);
        req.flush(mockResponse);

        expect(req.request.body).toEqual({ ...query });
    });
});
