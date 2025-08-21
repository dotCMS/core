import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotAnalyticsService } from './dot-analytics.service';

import { TIME_RANGE_OPTIONS } from '../constants';

const ANALYTICS_API_ENDPOINT = '/api/v1/analytics/content/_query/cube';
const TEST_SITE_ID = 'test-site-123';
const DEFAULT_TIME_RANGE = TIME_RANGE_OPTIONS.last7days;

describe('DotAnalyticsService', () => {
    let spectator: SpectatorHttp<DotAnalyticsService>;

    const createHttp = createHttpFactory({
        service: DotAnalyticsService
    });

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('Endpoints', () => {
        describe('totalPageViews', () => {
            it('should make POST request to analytics endpoint with required parameters', () => {
                spectator.service.totalPageViews(DEFAULT_TIME_RANGE, TEST_SITE_ID).subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.url).toBe(ANALYTICS_API_ENDPOINT);
                expect(req.request.body).toBeDefined();
                expect(req.request.body.measures).toContain('request.totalRequest');
                expect(req.request.body.filters).toEqual(
                    expect.arrayContaining([
                        expect.objectContaining({
                            member: 'request.eventType',
                            operator: 'equals',
                            values: ['pageview']
                        }),
                        expect.objectContaining({
                            member: 'request.siteId',
                            operator: 'equals',
                            values: [TEST_SITE_ID]
                        })
                    ])
                );
            });

            it('should make POST request with custom timeRange', () => {
                spectator.service
                    .totalPageViews(TIME_RANGE_OPTIONS.last30days, TEST_SITE_ID)
                    .subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.url).toBe(ANALYTICS_API_ENDPOINT);
                expect(req.request.body).toBeDefined();
                expect(req.request.body.timeDimensions[0].dateRange).toBe(
                    'from 30 days ago to now'
                );
            });
        });

        describe('uniqueVisitors', () => {
            it('should make POST request to analytics endpoint with required parameters', () => {
                spectator.service.uniqueVisitors(DEFAULT_TIME_RANGE, TEST_SITE_ID).subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.url).toBe(ANALYTICS_API_ENDPOINT);
                expect(req.request.body).toBeDefined();
                expect(req.request.body.measures).toContain('request.totalUsers');
                expect(req.request.body.filters).toEqual(
                    expect.arrayContaining([
                        expect.objectContaining({
                            member: 'request.eventType',
                            operator: 'equals',
                            values: ['pageview']
                        }),
                        expect.objectContaining({
                            member: 'request.siteId',
                            operator: 'equals',
                            values: [TEST_SITE_ID]
                        })
                    ])
                );
            });
        });

        describe('topPagePerformance', () => {
            it('should make POST request to analytics endpoint with required parameters', () => {
                spectator.service.topPagePerformance(DEFAULT_TIME_RANGE, TEST_SITE_ID).subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.url).toBe(ANALYTICS_API_ENDPOINT);
                expect(req.request.body).toBeDefined();
                expect(req.request.body.dimensions).toEqual(['request.path', 'request.pageTitle']);
                expect(req.request.body.measures).toContain('request.totalRequest');
                expect(req.request.body.order).toEqual({ 'request.totalRequest': 'desc' });
                expect(req.request.body.limit).toBe(1);
            });
        });

        describe('pageViewTimeLine', () => {
            it('should make POST request to analytics endpoint with required parameters', () => {
                spectator.service.pageViewTimeLine(DEFAULT_TIME_RANGE, TEST_SITE_ID).subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.url).toBe(ANALYTICS_API_ENDPOINT);
                expect(req.request.body).toBeDefined();
                expect(req.request.body.measures).toContain('request.totalRequest');
                expect(req.request.body.timeDimensions[0].granularity).toBeDefined();
            });
        });

        describe('pageViewDeviceBrowsers', () => {
            it('should make POST request to analytics endpoint with required parameters', () => {
                spectator.service
                    .pageViewDeviceBrowsers(DEFAULT_TIME_RANGE, TEST_SITE_ID)
                    .subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.url).toBe(ANALYTICS_API_ENDPOINT);
                expect(req.request.body).toBeDefined();
                expect(req.request.body.dimensions).toContain('request.userAgent');
                expect(req.request.body.measures).toContain('request.totalRequest');
                expect(req.request.body.order).toEqual({ 'request.totalRequest': 'desc' });
            });
        });

        describe('getTopPagePerformanceTable', () => {
            it('should make POST request to analytics endpoint with default parameters', () => {
                spectator.service
                    .getTopPagePerformanceTable(DEFAULT_TIME_RANGE, TEST_SITE_ID)
                    .subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.url).toBe(ANALYTICS_API_ENDPOINT);
                expect(req.request.body).toBeDefined();
                expect(req.request.body.dimensions).toEqual(['request.path', 'request.pageTitle']);
                expect(req.request.body.limit).toBe(50); // DEFAULT_COUNT_LIMIT
            });

            it('should make POST request with custom limit parameter', () => {
                const customLimit = 25;
                spectator.service
                    .getTopPagePerformanceTable(DEFAULT_TIME_RANGE, TEST_SITE_ID, customLimit)
                    .subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.url).toBe(ANALYTICS_API_ENDPOINT);
                expect(req.request.body).toBeDefined();
                expect(req.request.body.limit).toBe(customLimit);
            });
        });
    });

    describe('Service Integration', () => {
        it('should be injectable and create instance', () => {
            expect(spectator.service).toBeTruthy();
            expect(spectator.service).toBeInstanceOf(DotAnalyticsService);
        });

        it('should use correct base URL for all requests', () => {
            spectator.service.totalPageViews(DEFAULT_TIME_RANGE, TEST_SITE_ID).subscribe();
            const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
            expect(req.request.url).toBe(ANALYTICS_API_ENDPOINT);
        });
    });
});
