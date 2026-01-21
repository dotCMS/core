import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotAnalyticsService } from './dot-analytics.service';

import { CubeJSQuery } from '../types';

const ANALYTICS_API_ENDPOINT = '/api/v1/analytics/content/_query/cube';

describe('DotAnalyticsService', () => {
    let spectator: SpectatorHttp<DotAnalyticsService>;

    const createHttp = createHttpFactory({
        service: DotAnalyticsService
    });

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('cubeQuery', () => {
        it('should make POST request to analytics endpoint with the provided query', () => {
            const testQuery: CubeJSQuery = {
                measures: ['request.totalRequest'],
                filters: [
                    {
                        member: 'request.eventType',
                        operator: 'equals',
                        values: ['pageview']
                    }
                ]
            };

            spectator.service.cubeQuery(testQuery).subscribe();

            const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
            expect(req.request.url).toBe(ANALYTICS_API_ENDPOINT);
            expect(req.request.body).toEqual(testQuery);
        });

        it('should return entity array from response', () => {
            const testQuery: CubeJSQuery = {
                measures: ['request.totalRequest']
            };
            const mockResponse = {
                entity: [{ 'request.totalRequest': '100' }, { 'request.totalRequest': '200' }],
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                pagination: null,
                permissions: []
            };

            let result: unknown[];
            spectator.service.cubeQuery(testQuery).subscribe((data) => {
                result = data;
            });

            const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
            req.flush(mockResponse);

            expect(result).toEqual(mockResponse.entity);
        });

        it('should return empty array when entity is empty', () => {
            const testQuery: CubeJSQuery = {
                measures: ['request.totalRequest']
            };
            const mockResponse = {
                entity: [],
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                pagination: null,
                permissions: []
            };

            let result: unknown[];
            spectator.service.cubeQuery(testQuery).subscribe((data) => {
                result = data;
            });

            const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
            req.flush(mockResponse);

            expect(result).toEqual([]);
        });

        it('should pass complex query with all CubeJS options', () => {
            const complexQuery: CubeJSQuery = {
                measures: ['request.totalRequest', 'request.totalUsers'],
                dimensions: ['request.path', 'request.pageTitle'],
                filters: [
                    {
                        member: 'request.eventType',
                        operator: 'equals',
                        values: ['pageview']
                    },
                    {
                        member: 'request.siteId',
                        operator: 'equals',
                        values: ['site-123']
                    }
                ],
                timeDimensions: [
                    {
                        dimension: 'request.createdAt',
                        dateRange: 'from 7 days ago to now',
                        granularity: 'day'
                    }
                ],
                order: { 'request.totalRequest': 'desc' },
                limit: 10
            };

            spectator.service.cubeQuery(complexQuery).subscribe();

            const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
            expect(req.request.body).toEqual(complexQuery);
        });
    });

    describe('Service Integration', () => {
        it('should be injectable and create instance', () => {
            expect(spectator.service).toBeTruthy();
            expect(spectator.service).toBeInstanceOf(DotAnalyticsService);
        });
    });
});
