import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotCMSResponse, HealthStatusTypes } from '@dotcms/dotcms-models';

import { DotAnalyticsService } from './dot-analytics.service';

import {
    ANALYTICS_CONVERSION_CONTENT_ATTRIBUTION_URL,
    ANALYTICS_CONVERSION_URL
} from '../constants';
import { CubeJSQuery, Granularity, HealthEntity } from '../types';

const ANALYTICS_API_ENDPOINT = '/api/v1/analytics/content/_query/cube';
const ANALYTICS_EVENT_TOTAL_EVENTS = '/api/v1/analytics/event/total-events';
const ANALYTICS_EVENT_UNIQUE_VISITORS = '/api/v1/analytics/event/unique-visitors';
const ANALYTICS_EVENT_TOP_CONTENT = '/api/v1/analytics/event/top-content';
const ANALYTICS_EVENT_PAGE_VIEWS_BY_DEVICE_BROWSER =
    '/api/v1/analytics/event/pageviews-by-device-browser';
const ANALYTICS_SESSION_ENGAGEMENT = '/api/v1/analytics/session/engagement';
const ANALYTICS_HEALTH_URL = '/api/v1/analytics/health';

/** SpectatorHttp.expectOne always wraps URL in an object, so function matchers break; use the real backend matcher. */
function expectTotalEventsReq(httpMock: HttpTestingController) {
    return httpMock.expectOne(
        (req) =>
            req.method === 'GET' &&
            (req.urlWithParams === ANALYTICS_EVENT_TOTAL_EVENTS ||
                req.urlWithParams.startsWith(`${ANALYTICS_EVENT_TOTAL_EVENTS}?`))
    );
}

function expectUniqueVisitorsReq(httpMock: HttpTestingController) {
    return httpMock.expectOne(
        (req) =>
            req.method === 'GET' &&
            (req.urlWithParams === ANALYTICS_EVENT_UNIQUE_VISITORS ||
                req.urlWithParams.startsWith(`${ANALYTICS_EVENT_UNIQUE_VISITORS}?`))
    );
}

function expectTopContentReq(httpMock: HttpTestingController) {
    return httpMock.expectOne(
        (req) =>
            req.method === 'GET' &&
            (req.urlWithParams === ANALYTICS_EVENT_TOP_CONTENT ||
                req.urlWithParams.startsWith(`${ANALYTICS_EVENT_TOP_CONTENT}?`))
    );
}

function expectPageviewsByDeviceBrowserReq(httpMock: HttpTestingController) {
    return httpMock.expectOne(
        (req) =>
            req.method === 'GET' &&
            (req.urlWithParams === ANALYTICS_EVENT_PAGE_VIEWS_BY_DEVICE_BROWSER ||
                req.urlWithParams.startsWith(`${ANALYTICS_EVENT_PAGE_VIEWS_BY_DEVICE_BROWSER}?`))
    );
}

function expectSessionEngagementReq(httpMock: HttpTestingController) {
    return httpMock.expectOne(
        (req) =>
            req.method === 'GET' &&
            (req.urlWithParams === ANALYTICS_SESSION_ENGAGEMENT ||
                req.urlWithParams.startsWith(`${ANALYTICS_SESSION_ENGAGEMENT}?`))
    );
}

function expectContentAttributionReq(httpMock: HttpTestingController) {
    const base = ANALYTICS_CONVERSION_CONTENT_ATTRIBUTION_URL;
    return httpMock.expectOne(
        (req) =>
            req.method === 'GET' &&
            (req.urlWithParams === base || req.urlWithParams.startsWith(`${base}?`))
    );
}

function expectConversionsOverviewReq(httpMock: HttpTestingController) {
    return httpMock.expectOne(
        (req) =>
            req.method === 'GET' &&
            (req.urlWithParams === ANALYTICS_CONVERSION_URL ||
                req.urlWithParams.startsWith(`${ANALYTICS_CONVERSION_URL}?`))
    );
}

function dotCMSWrap<T>(data: T) {
    return {
        entity: { data },
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
        it('should GET total-events with range only and omit optional query keys', () => {
            spectator.service.getTotalEvents({ range: 'last_7_days' }).subscribe();

            const req = expectTotalEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_7_days');
            expect(req.request.params.get('granularity')).toBeNull();
            expect(req.request.params.get('eventType')).toBeNull();
            expect(req.request.params.get('siteId')).toBeNull();

            req.flush({
                entity: { data: { totalEvents: 42 } },
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                pagination: null,
                permissions: []
            });
        });

        it('should GET total-events with from and to', () => {
            spectator.service.getTotalEvents({ from: '2026-01-01', to: '2026-01-31' }).subscribe();

            const req = expectTotalEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('from')).toBe('2026-01-01');
            expect(req.request.params.get('to')).toBe('2026-01-31');

            req.flush({
                entity: { data: { totalEvents: 10 } },
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                pagination: null,
                permissions: []
            });
        });

        it('should append eventType, siteId, and granularity when provided in options', () => {
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

            const req = expectTotalEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_7_days');
            expect(req.request.params.get('granularity')).toBe('day');
            expect(req.request.params.get('eventType')).toBe('pageview');
            expect(req.request.params.get('siteId')).toBe('site-abc');

            req.flush({
                entity: {
                    data: [
                        { day: '2026-05-01', totalEvents: 3 },
                        { day: '2026-05-02', totalEvents: 5 }
                    ]
                },
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                pagination: null,
                permissions: []
            });

            expect(result).toEqual([
                { day: '2026-05-01', totalEvents: 3 },
                { day: '2026-05-02', totalEvents: 5 }
            ]);
        });

        it('should append conversion eventType without granularity', () => {
            spectator.service
                .getTotalEvents({ range: 'last_30_days', eventType: 'conversion' })
                .subscribe();

            const req = expectTotalEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_30_days');
            expect(req.request.params.get('eventType')).toBe('conversion');
            expect(req.request.params.get('granularity')).toBeNull();

            req.flush(dotCMSWrap({ totalEvents: 99 }));
        });

        it('should propagate HTTP errors for total-events', (done) => {
            spectator.service.getTotalEvents({ range: 'last_7_days' }).subscribe({
                error: (e) => {
                    expect(e.status).toBe(500);
                    done();
                }
            });

            const req = expectTotalEventsReq(TestBed.inject(HttpTestingController));
            req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
        });
    });

    describe('getUniqueVisitors', () => {
        it('should GET unique-visitors with range only and omit optional query keys', () => {
            spectator.service.getUniqueVisitors({ range: 'last_7_days' }).subscribe();

            const req = expectUniqueVisitorsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_7_days');
            expect(req.request.params.get('granularity')).toBeNull();
            expect(req.request.params.get('siteId')).toBeNull();

            req.flush(dotCMSWrap({ uniqueVisitors: 100 }));
        });

        it('should GET unique-visitors with from, to, granularity, and siteId', () => {
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

            const req = expectUniqueVisitorsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('from')).toBe('2026-01-01');
            expect(req.request.params.get('to')).toBe('2026-01-31');
            expect(req.request.params.get('granularity')).toBe('day');
            expect(req.request.params.get('siteId')).toBe('site-x');

            req.flush(
                dotCMSWrap([
                    { day: '2026-01-01', uniqueVisitors: 1 },
                    { day: '2026-01-02', uniqueVisitors: 2 }
                ])
            );

            expect(result).toEqual([
                { day: '2026-01-01', uniqueVisitors: 1 },
                { day: '2026-01-02', uniqueVisitors: 2 }
            ]);
        });

        it('should GET unique-visitors with eventType when provided', () => {
            spectator.service
                .getUniqueVisitors({
                    range: 'last_7_days',
                    eventType: 'conversion',
                    siteId: 'site-1'
                })
                .subscribe();

            const req = expectUniqueVisitorsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('eventType')).toBe('conversion');
            expect(req.request.params.get('siteId')).toBe('site-1');

            req.flush(dotCMSWrap({ uniqueVisitors: 3 }));
        });

        it('should propagate HTTP errors for unique-visitors', (done) => {
            spectator.service.getUniqueVisitors({ range: 'last_7_days' }).subscribe({
                error: (e) => {
                    expect(e.status).toBe(500);
                    done();
                }
            });

            const req = expectUniqueVisitorsReq(TestBed.inject(HttpTestingController));
            req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
        });
    });

    describe('getContentAttribution', () => {
        it('should GET conversion content attribution with range and siteId', () => {
            let result!: unknown;
            spectator.service
                .getContentAttribution({
                    range: 'last_7_days',
                    siteId: 's1'
                })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectContentAttributionReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_7_days');
            expect(req.request.params.get('siteId')).toBe('s1');

            req.flush(
                dotCMSWrap([
                    {
                        eventType: 'pageview',
                        identifier: '/home',
                        title: 'Home',
                        events: 10,
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
    });

    describe('getConversionsOverview', () => {
        it('should GET conversions overview and return data array', () => {
            let result!: unknown;
            spectator.service
                .getConversionsOverview({
                    from: '2026-05-01',
                    to: '2026-05-07',
                    siteId: 's1',
                    page: 1,
                    pageSize: 20
                })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectConversionsOverviewReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('from')).toBe('2026-05-01');
            expect(req.request.params.get('to')).toBe('2026-05-07');
            expect(req.request.params.get('siteId')).toBe('s1');
            expect(req.request.params.get('page')).toBe('1');
            expect(req.request.params.get('pageSize')).toBe('20');

            req.flush({
                entity: {
                    data: [
                        {
                            conversionName: 'purchase',
                            conversionRate: 10,
                            totalConversions: 5,
                            totalEvents: 50,
                            topContent: []
                        }
                    ],
                    pagination: { page: 1, pageSize: 20, totalItems: 1, totalPages: 1 },
                    params: {}
                },
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                pagination: null,
                permissions: []
            });

            expect(result).toEqual([
                {
                    conversionName: 'purchase',
                    conversionRate: 10,
                    totalConversions: 5,
                    totalEvents: 50,
                    topContent: []
                }
            ]);
        });
    });

    describe('getTopContent', () => {
        it('should GET top-content with range and optional filters', () => {
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

            const req = expectTopContentReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_7_days');
            expect(req.request.params.get('eventType')).toBe('pageview');
            expect(req.request.params.get('siteId')).toBe('s1');

            req.flush(
                dotCMSWrap([
                    { identifier: '1', title: 'A', totalEvents: 5 },
                    { identifier: '2', title: 'B', totalEvents: 3 }
                ])
            );

            expect(result).toEqual([
                { identifier: '1', title: 'A', totalEvents: 5 },
                { identifier: '2', title: 'B', totalEvents: 3 }
            ]);
        });

        it('should GET top-content with from and to omitting optional keys', () => {
            spectator.service.getTopContent({ from: '2026-05-01', to: '2026-05-07' }).subscribe();

            const req = expectTopContentReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('from')).toBe('2026-05-01');
            expect(req.request.params.get('to')).toBe('2026-05-07');
            expect(req.request.params.get('eventType')).toBeNull();

            req.flush(dotCMSWrap([]));
        });

        it('should propagate HTTP errors for top-content', (done) => {
            spectator.service.getTopContent({ range: 'last_7_days' }).subscribe({
                error: (e) => {
                    expect(e.status).toBe(500);
                    done();
                }
            });

            const req = expectTopContentReq(TestBed.inject(HttpTestingController));
            req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
        });
    });

    describe('getPageviewsByDeviceBrowser', () => {
        it('should GET pageviews-by-device-browser with groupBy=device and params', () => {
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

            const req = expectPageviewsByDeviceBrowserReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_30_days');
            expect(req.request.params.get('groupBy')).toBe('device');
            expect(req.request.params.get('eventType')).toBe('pageview');
            expect(req.request.params.get('siteId')).toBe('host1');

            req.flush(dotCMSWrap([{ device: 'Desktop', total: 22 }]));

            expect(result).toEqual([{ device: 'Desktop', total: 22 }]);
        });

        it('should GET pageviews-by-device-browser with groupBy=browser and params', () => {
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

            const req = expectPageviewsByDeviceBrowserReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('from')).toBe('2026-04-20');
            expect(req.request.params.get('to')).toBe('2026-05-28');
            expect(req.request.params.get('groupBy')).toBe('browser');

            req.flush(
                dotCMSWrap([
                    { browser: 'Firefox', total: 8 },
                    { browser: 'Safari', total: 8 }
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

            const req = expectPageviewsByDeviceBrowserReq(TestBed.inject(HttpTestingController));
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

        it('should GET session engagement aggregate without granularity', () => {
            let result!: unknown;
            spectator.service
                .getSessionEngagement({ range: 'last_7_days', siteId: 'site-1' })
                .subscribe((data) => {
                    result = data;
                });

            const req = expectSessionEngagementReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_7_days');
            expect(req.request.params.get('siteId')).toBe('site-1');
            expect(req.request.params.get('granularity')).toBeNull();
            expect(req.request.params.get('groupBy')).toBeNull();

            req.flush(dotCMSWrap(mockAggregate));

            expect(result).toEqual(mockAggregate);
        });

        it('should GET session engagement time series with granularity=day', () => {
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

            const req = expectSessionEngagementReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('from')).toBe('2026-05-01');
            expect(req.request.params.get('to')).toBe('2026-05-07');
            expect(req.request.params.get('granularity')).toBe('day');

            req.flush(dotCMSWrap(byDay));

            expect(result).toEqual(byDay);
        });

        it('should propagate HTTP errors for session engagement', (done) => {
            spectator.service.getSessionEngagement({ range: 'last_7_days' }).subscribe({
                error: (e) => {
                    expect(e.status).toBe(503);
                    done();
                }
            });

            const req = expectSessionEngagementReq(TestBed.inject(HttpTestingController));
            req.flush('Unavailable', { status: 503, statusText: 'Service Unavailable' });
        });
    });

    describe('getSessionEngagementGroupBy', () => {
        it('should GET session engagement with groupBy=device and normalize device to name', () => {
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

            const req = expectSessionEngagementReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('groupBy')).toBe('device');
            expect(req.request.params.get('range')).toBe('last_30_days');
            expect(req.request.params.get('siteId')).toBe('host1');
            expect(req.request.params.get('granularity')).toBeNull();

            req.flush(
                dotCMSWrap([
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

            const req = expectSessionEngagementReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('groupBy')).toBe('browser');
            req.flush(
                dotCMSWrap([
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

            const req = expectSessionEngagementReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('groupBy')).toBe('language');
            req.flush(
                dotCMSWrap([
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

        it('should propagate HTTP errors for session engagement groupBy', (done) => {
            spectator.service
                .getSessionEngagementGroupBy({ range: 'last_7_days', groupBy: 'language' })
                .subscribe({
                    error: (e) => {
                        expect(e.status).toBe(500);
                        done();
                    }
                });

            const req = expectSessionEngagementReq(TestBed.inject(HttpTestingController));
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

        it('should cache result with healthCheckWithCache', () => {
            let first!: HealthStatusTypes;
            let second!: HealthStatusTypes;

            spectator.service.healthCheckWithCache().subscribe((status) => {
                first = status;
            });
            spectator.service.healthCheckWithCache().subscribe((status) => {
                second = status;
            });

            const req = spectator.expectOne(ANALYTICS_HEALTH_URL, HttpMethod.GET);
            req.flush(createAnalyticsHealthResponse('true'));

            expect(first).toBe(HealthStatusTypes.AVAILABLE);
            expect(second).toBe(HealthStatusTypes.AVAILABLE);
        });

        it('should clear cache with clearHealthCache', () => {
            let first!: HealthStatusTypes;
            let second!: HealthStatusTypes;

            spectator.service.healthCheckWithCache().subscribe((status) => {
                first = status;
            });
            const req1 = spectator.expectOne(ANALYTICS_HEALTH_URL, HttpMethod.GET);
            req1.flush(createAnalyticsHealthResponse('true'));
            expect(first).toBe(HealthStatusTypes.AVAILABLE);

            spectator.service.clearHealthCache();

            spectator.service.healthCheckWithCache().subscribe((status) => {
                second = status;
            });
            const req2 = spectator.expectOne(ANALYTICS_HEALTH_URL, HttpMethod.GET);
            req2.flush(createAnalyticsHealthResponse('false'));

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
