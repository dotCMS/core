import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotAnalyticsService } from './dot-analytics.service';

import type {
    AnalyticsApiResponse,
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    TimeRange,
    TopPagePerformanceEntity,
    TopPerformaceTableEntity,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../index';

const ANALYTICS_API_ENDPOINT = '/api/v1/analytics/content/_query/cube';

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
            it('should make POST request to analytics endpoint', () => {
                spectator.service.totalPageViews().subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.body).toBeDefined();
            });

            it('should make POST request with custom timeRange', () => {
                const timeRange: TimeRange = 'from 30 days ago to now';
                spectator.service.totalPageViews(timeRange).subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.body).toBeDefined();
            });

            it('should return total page views entity from response', (done) => {
                const mockResponse: AnalyticsApiResponse<TotalPageViewsEntity> = {
                    entity: [{ 'request.totalRequest': '1250' }],
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    pagination: null,
                    permissions: []
                };

                spectator.service.totalPageViews().subscribe((result) => {
                    expect(result).toEqual(mockResponse.entity[0]);
                    done();
                });

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                req.flush(mockResponse);
            });
        });

        describe('uniqueVisitors', () => {
            it('should make POST request to analytics endpoint', () => {
                spectator.service.uniqueVisitors().subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.body).toBeDefined();
            });

            it('should make POST request with custom timeRange', () => {
                const timeRange: TimeRange = 'this week';
                spectator.service.uniqueVisitors(timeRange).subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.body).toBeDefined();
            });

            it('should return unique visitors entity from response', (done) => {
                const mockResponse: AnalyticsApiResponse<UniqueVisitorsEntity> = {
                    entity: [{ 'request.totalUser': '342' }],
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    pagination: null,
                    permissions: []
                };

                spectator.service.uniqueVisitors().subscribe((result) => {
                    expect(result).toEqual(mockResponse.entity[0]);
                    done();
                });

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                req.flush(mockResponse);
            });
        });

        describe('topPagePerformance', () => {
            it('should make POST request to analytics endpoint', () => {
                spectator.service.topPagePerformance().subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.body).toBeDefined();
            });

            it('should return top page performance entity from response', (done) => {
                const mockResponse: AnalyticsApiResponse<TopPagePerformanceEntity> = {
                    entity: [
                        {
                            'request.totalRequest': '890',
                            'request.pageTitle': 'Home Page',
                            'request.path': '/home'
                        }
                    ],
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    pagination: null,
                    permissions: []
                };

                spectator.service.topPagePerformance().subscribe((result) => {
                    expect(result).toEqual(mockResponse.entity[0]);
                    done();
                });

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                req.flush(mockResponse);
            });
        });

        describe('pageViewTimeLine', () => {
            it('should make POST request to analytics endpoint', () => {
                spectator.service.pageViewTimeLine().subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.body).toBeDefined();
            });

            it('should make POST request with custom timeRange', () => {
                spectator.service.pageViewTimeLine('this week').subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.body).toBeDefined();
            });

            it('should return page view timeline entities array from response', (done) => {
                const mockResponse: AnalyticsApiResponse<PageViewTimeLineEntity> = {
                    entity: [
                        {
                            'request.totalRequest': '100',
                            'request.createdAt': '2023-12-01T00:00:00Z',
                            'request.createdAt.day': '2023-12-01'
                        },
                        {
                            'request.totalRequest': '150',
                            'request.createdAt': '2023-12-02T00:00:00Z',
                            'request.createdAt.day': '2023-12-02'
                        }
                    ],
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    pagination: null,
                    permissions: []
                };

                spectator.service.pageViewTimeLine().subscribe((result) => {
                    expect(result).toEqual(mockResponse.entity);
                    expect(result).toHaveLength(2);
                    done();
                });

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                req.flush(mockResponse);
            });
        });

        describe('pageViewDeviceBrowsers', () => {
            it('should make POST request to analytics endpoint', () => {
                spectator.service.pageViewDeviceBrowsers().subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.body).toBeDefined();
            });

            it('should return device browsers entities array from response', (done) => {
                const mockResponse: AnalyticsApiResponse<PageViewDeviceBrowsersEntity> = {
                    entity: [
                        {
                            'request.userAgent':
                                'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
                            'request.totalRequest': '500'
                        },
                        {
                            'request.userAgent':
                                'Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Mobile/15E148 Safari/604.1',
                            'request.totalRequest': '300'
                        }
                    ],
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    pagination: null,
                    permissions: []
                };

                spectator.service.pageViewDeviceBrowsers().subscribe((result) => {
                    expect(result).toEqual(mockResponse.entity);
                    expect(result).toHaveLength(2);
                    done();
                });

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                req.flush(mockResponse);
            });
        });

        describe('getTopPagePerformanceTable', () => {
            it('should make POST request to analytics endpoint with default parameters', () => {
                spectator.service.getTopPagePerformanceTable().subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.body).toBeDefined();
            });

            it('should make POST request with custom parameters', () => {
                const timeRange: TimeRange = 'from 30 days ago to now';
                const limit = 25;
                spectator.service.getTopPagePerformanceTable(timeRange, limit).subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.body).toBeDefined();
            });

            it('should return top pages table entities array from response', (done) => {
                const mockResponse: AnalyticsApiResponse<TopPerformaceTableEntity> = {
                    entity: [
                        {
                            'request.totalRequest': '1250',
                            'request.pageTitle': 'Home Page',
                            'request.path': '/home'
                        },
                        {
                            'request.totalRequest': '890',
                            'request.pageTitle': 'About Us',
                            'request.path': '/about'
                        },
                        {
                            'request.totalRequest': '567',
                            'request.pageTitle': 'Contact',
                            'request.path': '/contact'
                        }
                    ],
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    pagination: null,
                    permissions: []
                };

                spectator.service.getTopPagePerformanceTable().subscribe((result) => {
                    expect(result).toEqual(mockResponse.entity);
                    expect(result).toHaveLength(3);
                    done();
                });

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                req.flush(mockResponse);
            });
        });
    });

    describe('Service Behavior', () => {
        it('should handle different timeRange formats', () => {
            const timeRanges: TimeRange[] = [
                'from 7 days ago to now',
                'from 30 days ago to now',
                'this week'
            ];

            timeRanges.forEach((timeRange) => {
                spectator.service.totalPageViews(timeRange).subscribe();

                const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
                expect(req.request.body).toBeDefined();
                req.flush({
                    entity: [{ 'request.totalRequest': '100' }],
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    pagination: null,
                    permissions: []
                });
            });
        });

        it('should use default parameters when none provided', () => {
            // Test totalPageViews with default parameters
            spectator.service.totalPageViews().subscribe();
            const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
            expect(req.request.body).toBeDefined();
            req.flush({
                entity: [{ 'request.totalRequest': '100' }],
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                pagination: null,
                permissions: []
            });
        });
    });

    describe('Error Handling', () => {
        it('should handle HTTP errors properly', (done) => {
            spectator.service.totalPageViews().subscribe({
                next: () => {
                    // Should not reach here
                    fail('Expected an error, but got a successful response');
                },
                error: (error) => {
                    expect(error).toBeDefined();
                    expect(error.status).toBe(500);
                    done();
                }
            });

            const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
            req.flush('Internal Server Error', {
                status: 500,
                statusText: 'Internal Server Error'
            });
        });

        it('should handle empty response arrays', (done) => {
            const mockResponse: AnalyticsApiResponse<PageViewTimeLineEntity> = {
                entity: [],
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                pagination: null,
                permissions: []
            };

            spectator.service.pageViewTimeLine().subscribe((result) => {
                expect(result).toEqual([]);
                expect(result).toHaveLength(0);
                done();
            });

            const req = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
            req.flush(mockResponse);
        });
    });

    describe('Service Integration', () => {
        it('should be injectable and create instance', () => {
            expect(spectator.service).toBeTruthy();
            expect(spectator.service).toBeInstanceOf(DotAnalyticsService);
        });

        it('should have correct base URL configured', () => {
            // Verify the service uses the correct endpoint
            spectator.service.totalPageViews().subscribe();
            spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
        });

        it('should make separate requests for different method calls', () => {
            // Test that calling different methods makes different requests
            spectator.service.totalPageViews().subscribe();
            const req1 = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
            req1.flush({
                entity: [{ 'request.totalRequest': '100' }],
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                pagination: null,
                permissions: []
            });

            spectator.service.uniqueVisitors().subscribe();
            const req2 = spectator.expectOne(ANALYTICS_API_ENDPOINT, HttpMethod.POST);
            req2.flush({
                entity: [{ 'request.totalUser': '50' }],
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                pagination: null,
                permissions: []
            });

            // Both requests should have been made
            expect(req1.request.body).toBeDefined();
            expect(req2.request.body).toBeDefined();
        });
    });
});
