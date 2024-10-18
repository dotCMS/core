import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotAnalyticsSearchService } from '@dotcms/data-access';
import { AnalyticsQueryType } from '@dotcms/dotcms-models';

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
        expect(req.request.body).toEqual({ query });
        req.flush(mockResponse);
    });

    it('should perform a POST request to the cube URL and return results', (done) => {
        spectator.service.get(query, AnalyticsQueryType.CUBE).subscribe((results) => {
            expect(results).toEqual(mockResponse.entity);
            done();
        });

        const req = spectator.expectOne('/api/v1/analytics/content/_query/cube', HttpMethod.POST);
        expect(req.request.body).toEqual({ query });
        req.flush(mockResponse);
    });
});
