import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotCMSResponse, HealthStatusTypes } from '@dotcms/dotcms-models';

import { DotAnalyticsService } from './dot-analytics.service';

import { ANALYTICS_CONTENT_URL, ANALYTICS_EVENTS_URL, ANALYTICS_SESSIONS_URL } from '../constants';
import { AnalyticsPagination, CubeJSQuery, Granularity, HealthEntity } from '../types';

const ANALYTICS_API_ENDPOINT = '/api/v1/analytics/content/_query/cube';
const ANALYTICS_HEALTH_URL = '/api/v1/analytics/health';

/** SpectatorHttp.expectOne always wraps URL in an object, so function matchers break; use the real backend matcher. */

/** Matches the domain-driven query endpoints (dotCMS/core#36628): events/sessions/content. */
function expectAnalyticsEventsReq(httpMock: HttpTestingController) {
    return httpMock.expectOne(
        (req) =>
            req.method === 'GET' &&
            (req.urlWithParams === ANALYTICS_EVENTS_URL ||
                req.urlWithParams.startsWith(`${ANALYTICS_EVENTS_URL}?`))
    );
}

function expectAnalyticsSessionsReq(httpMock: HttpTestingController) {
    return httpMock.expectOne(
        (req) =>
            req.method === 'GET' &&
            (req.urlWithParams === ANALYTICS_SESSIONS_URL ||
                req.urlWithParams.startsWith(`${ANALYTICS_SESSIONS_URL}?`))
    );
}

function expectAnalyticsContentReq(httpMock: HttpTestingController) {
    return httpMock.expectOne(
        (req) =>
            req.method === 'GET' &&
            (req.urlWithParams === ANALYTICS_CONTENT_URL ||
                req.urlWithParams.startsWith(`${ANALYTICS_CONTENT_URL}?`))
    );
}

/** Wraps rows in the unified tabular envelope (dotCMS/core#36628) for the domain-driven endpoints. */
function dotCMSWrapAnalytics<T extends Record<string, string | number>>(
    rows: T[],
    opts?: { totals?: Record<string, number>; pagination?: AnalyticsPagination }
) {
    return {
        entity: { params: {}, columns: [], rows, ...opts },
        errors: [],
        i18nMessagesMap: {},
        messages: [],
        pagination: null,
        permissions: []
    };
}

function createAnalyticsHealthResponse(available: string | boolean): DotCMSResponse<HealthEntity> {
    return {
        entity: {
            available
        },
        errors: [],
        i18nMessagesMap: {},
        messages: [],
        pagination: null,
        permissions: []
    };
}

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

            let result!: unknown[];
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

            let result!: unknown[];
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
                        granularity: Granularity.DAY
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

    describe('getTotalEvents', () => {
        it('should GET /analytics/events with metrics=totalEvents, range only, and omit optional query keys', () => {
            let result!: unknown;
            spectator.service
                .getTotalEvents({ range: 'last_7_days' })
                .subscribe((data) => (result = data));

            const req = expectAnalyticsEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_7_days');
            expect(req.request.params.get('metrics')).toBe('totalEvents');
            expect(req.request.params.get('dimensions')).toBeNull();
            expect(req.request.params.get('eventType')).toBeNull();
            expect(req.request.params.get('siteId')).toBeNull();

            req.flush(dotCMSWrapAnalytics([{ totalEvents: 42 }]));

            expect(result).toEqual({ totalEvents: 42 });
        });

        it('should GET /analytics/events with from and to', () => {
            spectator.service.getTotalEvents({ from: '2026-01-01', to: '2026-01-31' }).subscribe();

            const req = expectAnalyticsEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('from')).toBe('2026-01-01');
            expect(req.request.params.get('to')).toBe('2026-01-31');
            expect(req.request.params.get('metrics')).toBe('totalEvents');

            req.flush(dotCMSWrapAnalytics([{ totalEvents: 10 }]));
        });

        it('should append eventType, siteId, and dimensions=day when granularity=day is provided', () => {
            let result!: unknown;
            spectator.service
                .getTotalEvents({
                    range: 'last_7_days',
                    granularity: 'day',
                    eventType: 'pageview',
                    siteId: 'site-abc'
                })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectAnalyticsEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_7_days');
            expect(req.request.params.get('dimensions')).toBe('day');
            expect(req.request.params.get('metrics')).toBe('totalEvents');
            expect(req.request.params.get('eventType')).toBe('pageview');
            expect(req.request.params.get('siteId')).toBe('site-abc');

            req.flush(
                dotCMSWrapAnalytics([
                    { day: '2026-05-01', totalEvents: 3 },
                    { day: '2026-05-02', totalEvents: 5 }
                ])
            );

            expect(result).toEqual([
                { day: '2026-05-01', totalEvents: 3 },
                { day: '2026-05-02', totalEvents: 5 }
            ]);
        });

        it('should append conversion eventType without a dimensions param', () => {
            spectator.service
                .getTotalEvents({ range: 'last_30_days', eventType: 'conversion' })
                .subscribe();

            const req = expectAnalyticsEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_30_days');
            expect(req.request.params.get('eventType')).toBe('conversion');
            expect(req.request.params.get('dimensions')).toBeNull();

            req.flush(dotCMSWrapAnalytics([{ totalEvents: 99 }]));
        });

        it('should propagate HTTP errors for total-events', (done) => {
            spectator.service.getTotalEvents({ range: 'last_7_days' }).subscribe({
                error: (e) => {
                    expect(e.status).toBe(500);
                    done();
                }
            });

            const req = expectAnalyticsEventsReq(TestBed.inject(HttpTestingController));
            req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
        });
    });

    describe('getUniqueVisitors', () => {
        it('should GET /analytics/events with metrics=uniqueVisitors, range only, and omit optional query keys', () => {
            let result!: unknown;
            spectator.service
                .getUniqueVisitors({ range: 'last_7_days' })
                .subscribe((data) => (result = data));

            const req = expectAnalyticsEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_7_days');
            expect(req.request.params.get('metrics')).toBe('uniqueVisitors');
            expect(req.request.params.get('dimensions')).toBeNull();
            expect(req.request.params.get('siteId')).toBeNull();

            req.flush(dotCMSWrapAnalytics([{ uniqueVisitors: 100 }]));

            expect(result).toEqual({ uniqueVisitors: 100 });
        });

        it('should GET /analytics/events with from, to, dimensions=day, and siteId', () => {
            let result!: unknown;
            spectator.service
                .getUniqueVisitors({
                    from: '2026-01-01',
                    to: '2026-01-31',
                    granularity: 'day',
                    siteId: 'site-x'
                })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectAnalyticsEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('from')).toBe('2026-01-01');
            expect(req.request.params.get('to')).toBe('2026-01-31');
            expect(req.request.params.get('dimensions')).toBe('day');
            expect(req.request.params.get('metrics')).toBe('uniqueVisitors');
            expect(req.request.params.get('siteId')).toBe('site-x');

            req.flush(
                dotCMSWrapAnalytics([
                    { day: '2026-01-01', uniqueVisitors: 1 },
                    { day: '2026-01-02', uniqueVisitors: 2 }
                ])
            );

            expect(result).toEqual([
                { day: '2026-01-01', uniqueVisitors: 1 },
                { day: '2026-01-02', uniqueVisitors: 2 }
            ]);
        });

        it('should GET /analytics/events with eventType when provided', () => {
            spectator.service
                .getUniqueVisitors({
                    range: 'last_7_days',
                    eventType: 'conversion',
                    siteId: 'site-1'
                })
                .subscribe();

            const req = expectAnalyticsEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('eventType')).toBe('conversion');
            expect(req.request.params.get('siteId')).toBe('site-1');
            expect(req.request.params.get('metrics')).toBe('uniqueVisitors');

            req.flush(dotCMSWrapAnalytics([{ uniqueVisitors: 3 }]));
        });

        it('should propagate HTTP errors for unique-visitors', (done) => {
            spectator.service.getUniqueVisitors({ range: 'last_7_days' }).subscribe({
                error: (e) => {
                    expect(e.status).toBe(500);
                    done();
                }
            });

            const req = expectAnalyticsEventsReq(TestBed.inject(HttpTestingController));
            req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
        });
    });

    describe('getContentAttribution', () => {
        it('should GET /analytics/content (attribution mode) with range and siteId, renaming totalEvents to events', () => {
            let result!: unknown;
            spectator.service
                .getContentAttribution({
                    range: 'last_7_days',
                    siteId: 's1'
                })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectAnalyticsContentReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_7_days');
            expect(req.request.params.get('siteId')).toBe('s1');

            // New envelope names the metric column 'totalEvents' — the mapping must rename it
            // back to 'events', which ContentAttributionData (and transformContentConversionsData)
            // expect.
            req.flush(
                dotCMSWrapAnalytics([
                    {
                        eventType: 'pageview',
                        identifier: '/home',
                        title: 'Home',
                        totalEvents: 10,
                        attributionCount: 2,
                        attributionRate: 20
                    }
                ])
            );

            expect(result).toEqual([
                {
                    eventType: 'pageview',
                    identifier: '/home',
                    title: 'Home',
                    events: 10,
                    attributionCount: 2,
                    attributionRate: 20
                }
            ]);
        });

        it('should pass through orderBy, orderDir, page, and pageSize unchanged', () => {
            spectator.service
                .getContentAttribution({
                    range: 'last_7_days',
                    orderBy: 'attributionCount',
                    orderDir: 'desc',
                    page: 1,
                    pageSize: 20
                })
                .subscribe();

            const req = expectAnalyticsContentReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('orderBy')).toBe('attributionCount');
            expect(req.request.params.get('orderDir')).toBe('desc');
            expect(req.request.params.get('page')).toBe('1');
            expect(req.request.params.get('pageSize')).toBe('20');

            req.flush(dotCMSWrapAnalytics([]));
        });

        it('should propagate HTTP errors for content attribution', (done) => {
            spectator.service.getContentAttribution({ range: 'last_7_days' }).subscribe({
                error: (e) => {
                    expect(e.status).toBe(500);
                    done();
                }
            });

            const req = expectAnalyticsContentReq(TestBed.inject(HttpTestingController));
            req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
        });
    });

    describe('getTopContent', () => {
        it('should GET /analytics/content (top-content mode) with range, eventType filter, metrics, and explicit ordering', () => {
            let result!: unknown;
            spectator.service
                .getTopContent({
                    range: 'last_7_days',
                    eventType: 'pageview',
                    siteId: 's1'
                })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectAnalyticsContentReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_7_days');
            // eventType is a plain filter here, not a dimension — it must NOT flip the endpoint
            // into attribution mode.
            expect(req.request.params.get('eventType')).toBe('pageview');
            expect(req.request.params.get('siteId')).toBe('s1');
            expect(req.request.params.get('metrics')).toBe('totalEvents');
            expect(req.request.params.get('orderBy')).toBe('totalEvents');
            expect(req.request.params.get('orderDir')).toBe('desc');

            req.flush(
                dotCMSWrapAnalytics([
                    { identifier: '1', title: 'A', totalEvents: 5 },
                    { identifier: '2', title: 'B', totalEvents: 3 }
                ])
            );

            expect(result).toEqual([
                { identifier: '1', title: 'A', totalEvents: 5 },
                { identifier: '2', title: 'B', totalEvents: 3 }
            ]);
        });

        it('should GET /analytics/content with from and to omitting optional keys', () => {
            spectator.service.getTopContent({ from: '2026-05-01', to: '2026-05-07' }).subscribe();

            const req = expectAnalyticsContentReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('from')).toBe('2026-05-01');
            expect(req.request.params.get('to')).toBe('2026-05-07');
            expect(req.request.params.get('eventType')).toBeNull();
            expect(req.request.params.get('metrics')).toBe('totalEvents');

            req.flush(dotCMSWrapAnalytics([]));
        });

        it('should propagate HTTP errors for top-content', (done) => {
            spectator.service.getTopContent({ range: 'last_7_days' }).subscribe({
                error: (e) => {
                    expect(e.status).toBe(500);
                    done();
                }
            });

            const req = expectAnalyticsContentReq(TestBed.inject(HttpTestingController));
            req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
        });
    });

    describe('getPageviewsByDeviceBrowser', () => {
        it('should GET /analytics/events with metrics=pageviews, dimensions=device, and params', () => {
            let result!: unknown;
            spectator.service
                .getPageviewsByDeviceBrowser({
                    range: 'last_30_days',
                    groupBy: 'device',
                    eventType: 'pageview',
                    siteId: 'host1'
                })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectAnalyticsEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_30_days');
            expect(req.request.params.get('metrics')).toBe('pageviews');
            expect(req.request.params.get('dimensions')).toBe('device');
            expect(req.request.params.get('eventType')).toBe('pageview');
            expect(req.request.params.get('siteId')).toBe('host1');

            // New envelope names the metric column 'pageviews' — the mapping must rename it back
            // to 'total', which DeviceBreakdownData and the pie-chart transform utils expect.
            req.flush(dotCMSWrapAnalytics([{ device: 'Desktop', pageviews: 22 }]));

            expect(result).toEqual([{ device: 'Desktop', total: 22 }]);
        });

        it('should GET /analytics/events with metrics=pageviews, dimensions=browser, and params', () => {
            let result!: unknown;
            spectator.service
                .getPageviewsByDeviceBrowser({
                    from: '2026-04-20',
                    to: '2026-05-28',
                    groupBy: 'browser',
                    eventType: 'pageview',
                    siteId: 'host1'
                })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectAnalyticsEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('from')).toBe('2026-04-20');
            expect(req.request.params.get('to')).toBe('2026-05-28');
            expect(req.request.params.get('dimensions')).toBe('browser');
            expect(req.request.params.get('metrics')).toBe('pageviews');

            req.flush(
                dotCMSWrapAnalytics([
                    { browser: 'Firefox', pageviews: 8 },
                    { browser: 'Safari', pageviews: 8 }
                ])
            );

            expect(result).toEqual([
                { browser: 'Firefox', total: 8 },
                { browser: 'Safari', total: 8 }
            ]);
        });

        it('should propagate HTTP errors for pageviews-by-device-browser', (done) => {
            spectator.service
                .getPageviewsByDeviceBrowser({
                    range: 'last_30_days',
                    groupBy: 'device'
                })
                .subscribe({
                    error: (e) => {
                        expect(e.status).toBe(500);
                        done();
                    }
                });

            const req = expectAnalyticsEventsReq(TestBed.inject(HttpTestingController));
            req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
        });
    });

    describe('getSessionEngagement', () => {
        const mockAggregate = {
            avgEngagedSessionTimeSeconds: 120,
            avgInteractionsPerEngagedSession: 3.5,
            avgSessionTimeSeconds: 200,
            conversionRate: 5.3,
            engagedConversionSessions: 10,
            engagedSessions: 100,
            engagementRate: 28.5,
            totalSessions: 350
        };

        it('should GET /analytics/sessions aggregate without a dimensions param', () => {
            let result!: unknown;
            spectator.service
                .getSessionEngagement({ range: 'last_7_days', siteId: 'site-1' })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectAnalyticsSessionsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_7_days');
            expect(req.request.params.get('siteId')).toBe('site-1');
            expect(req.request.params.get('dimensions')).toBeNull();

            req.flush(dotCMSWrapAnalytics([mockAggregate]));

            expect(result).toEqual(mockAggregate);
        });

        it('should GET /analytics/sessions time series with dimensions=day', () => {
            let result!: unknown;
            const byDay = [
                { ...mockAggregate, day: '2026-05-01' },
                { ...mockAggregate, day: '2026-05-02' }
            ];
            spectator.service
                .getSessionEngagement({
                    from: '2026-05-01',
                    to: '2026-05-07',
                    granularity: 'day',
                    siteId: 's-x'
                })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectAnalyticsSessionsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('from')).toBe('2026-05-01');
            expect(req.request.params.get('to')).toBe('2026-05-07');
            expect(req.request.params.get('dimensions')).toBe('day');

            req.flush(dotCMSWrapAnalytics(byDay));

            expect(result).toEqual(byDay);
        });

        it('should propagate HTTP errors for session engagement', (done) => {
            spectator.service.getSessionEngagement({ range: 'last_7_days' }).subscribe({
                error: (e) => {
                    expect(e.status).toBe(503);
                    done();
                }
            });

            const req = expectAnalyticsSessionsReq(TestBed.inject(HttpTestingController));
            req.flush('Unavailable', { status: 503, statusText: 'Service Unavailable' });
        });
    });

    describe('getSessionEngagementGroupBy', () => {
        it('should GET /analytics/sessions with dimensions=device, the 4 basic metrics only, and normalize device to name', () => {
            let result!: unknown;
            spectator.service
                .getSessionEngagementGroupBy({
                    range: 'last_30_days',
                    groupBy: 'device',
                    siteId: 'host1'
                })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectAnalyticsSessionsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('dimensions')).toBe('device');
            expect(req.request.params.get('range')).toBe('last_30_days');
            expect(req.request.params.get('siteId')).toBe('host1');
            // The extended metrics (conversionRate, avgSessionTimeSeconds, etc.) are invalid on a
            // grouped dimension and 400 — metrics MUST be explicitly restricted to these 4.
            expect(req.request.params.get('metrics')).toBe(
                'totalSessions,engagedSessions,engagementRate,avgEngagedSessionTimeSeconds'
            );

            req.flush(
                dotCMSWrapAnalytics([
                    {
                        device: 'desktop',
                        avgEngagedSessionTimeSeconds: 90,
                        engagedSessions: 40,
                        engagementRate: 30,
                        totalSessions: 100
                    }
                ])
            );

            expect(result).toEqual([
                {
                    name: 'desktop',
                    avgEngagedSessionTimeSeconds: 90,
                    engagedSessions: 40,
                    engagementRate: 30,
                    totalSessions: 100
                }
            ]);
        });

        it('should normalize browser groupBy response to name', () => {
            let result!: unknown;
            spectator.service
                .getSessionEngagementGroupBy({ range: 'last_7_days', groupBy: 'browser' })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectAnalyticsSessionsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('dimensions')).toBe('browser');
            req.flush(
                dotCMSWrapAnalytics([
                    {
                        browser: 'Chrome',
                        avgEngagedSessionTimeSeconds: 1,
                        engagedSessions: 2,
                        engagementRate: 3,
                        totalSessions: 4
                    }
                ])
            );

            expect(result).toEqual([
                {
                    name: 'Chrome',
                    avgEngagedSessionTimeSeconds: 1,
                    engagedSessions: 2,
                    engagementRate: 3,
                    totalSessions: 4
                }
            ]);
        });

        it('should normalize language groupBy response to name', () => {
            let result!: unknown;
            spectator.service
                .getSessionEngagementGroupBy({ range: 'last_7_days', groupBy: 'language' })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectAnalyticsSessionsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('dimensions')).toBe('language');
            req.flush(
                dotCMSWrapAnalytics([
                    {
                        language: 'en-US',
                        avgEngagedSessionTimeSeconds: 5,
                        engagedSessions: 6,
                        engagementRate: 7,
                        totalSessions: 8
                    }
                ])
            );

            expect(result).toEqual([
                {
                    name: 'en-US',
                    avgEngagedSessionTimeSeconds: 5,
                    engagedSessions: 6,
                    engagementRate: 7,
                    totalSessions: 8
                }
            ]);
        });

        it('should pass through "n/a" language unchanged, not remap it to Other', () => {
            // "n/a" (sessions with no recorded locale) is a real, expected bucket — it must not
            // be treated the same as a blank/empty value.
            let result!: unknown;
            spectator.service
                .getSessionEngagementGroupBy({ range: 'last_7_days', groupBy: 'language' })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectAnalyticsSessionsReq(TestBed.inject(HttpTestingController));
            req.flush(
                dotCMSWrapAnalytics([
                    {
                        language: 'n/a',
                        avgEngagedSessionTimeSeconds: 0,
                        engagedSessions: 1,
                        engagementRate: 10,
                        totalSessions: 2
                    }
                ])
            );

            expect(result).toEqual([
                {
                    name: 'n/a',
                    avgEngagedSessionTimeSeconds: 0,
                    engagedSessions: 1,
                    engagementRate: 10,
                    totalSessions: 2
                }
            ]);
        });

        it('should propagate HTTP errors for session engagement groupBy', (done) => {
            spectator.service
                .getSessionEngagementGroupBy({ range: 'last_7_days', groupBy: 'language' })
                .subscribe({
                    error: (e) => {
                        expect(e.status).toBe(500);
                        done();
                    }
                });

            const req = expectAnalyticsSessionsReq(TestBed.inject(HttpTestingController));
            req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
        });
    });

    describe('healthCheck', () => {
        it('should return AVAILABLE when entity.available is string "true"', () => {
            let result!: HealthStatusTypes;
            spectator.service.healthCheck().subscribe((status) => {
                result = status;
            });

            const req = spectator.expectOne(ANALYTICS_HEALTH_URL, HttpMethod.GET);
            req.flush(createAnalyticsHealthResponse('true'));

            expect(result).toBe(HealthStatusTypes.AVAILABLE);
        });

        it('should return AVAILABLE when entity.available is boolean true', () => {
            let result!: HealthStatusTypes;
            spectator.service.healthCheck().subscribe((status) => {
                result = status;
            });

            const req = spectator.expectOne(ANALYTICS_HEALTH_URL, HttpMethod.GET);
            req.flush(createAnalyticsHealthResponse(true));

            expect(result).toBe(HealthStatusTypes.AVAILABLE);
        });

        it('should return NOT_AVAILABLE when entity.available is string "false"', () => {
            let result!: HealthStatusTypes;
            spectator.service.healthCheck().subscribe((status) => {
                result = status;
            });

            const req = spectator.expectOne(ANALYTICS_HEALTH_URL, HttpMethod.GET);
            req.flush(createAnalyticsHealthResponse('false'));

            expect(result).toBe(HealthStatusTypes.NOT_AVAILABLE);
        });

        it('should return ERROR on HTTP failure', () => {
            let result!: HealthStatusTypes;
            spectator.service.healthCheck().subscribe((status) => {
                result = status;
            });

            const req = spectator.expectOne(ANALYTICS_HEALTH_URL, HttpMethod.GET);
            req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });

            expect(result).toBe(HealthStatusTypes.ERROR);
        });

        it('should issue a new HTTP request for each healthCheck subscription', () => {
            let first!: HealthStatusTypes;
            let second!: HealthStatusTypes;

            spectator.service.healthCheck().subscribe((status) => {
                first = status;
            });
            spectator.service.healthCheck().subscribe((status) => {
                second = status;
            });

            const reqs = spectator.controller.match((req) => req.url === ANALYTICS_HEALTH_URL);
            expect(reqs.length).toBe(2);
            reqs[0].flush(createAnalyticsHealthResponse('true'));
            reqs[1].flush(createAnalyticsHealthResponse('false'));

            expect(first).toBe(HealthStatusTypes.AVAILABLE);
            expect(second).toBe(HealthStatusTypes.NOT_AVAILABLE);
        });
    });

    describe('Service Integration', () => {
        it('should be injectable and create instance', () => {
            expect(spectator.service).toBeTruthy();
            expect(spectator.service).toBeInstanceOf(DotAnalyticsService);
        });
    });
});
