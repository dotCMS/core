import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotAnalyticsService } from './dot-analytics.service';

import { CubeJSQuery, Granularity } from '../types';

const ANALYTICS_API_ENDPOINT = '/api/v1/analytics/content/_query/cube';
const ANALYTICS_EVENT_TOTAL_EVENTS = '/api/v1/analytics/event/total-events';

/** SpectatorHttp.expectOne always wraps URL in an object, so function matchers break; use the real backend matcher. */
function expectTotalEventsReq(httpMock: HttpTestingController) {
    return httpMock.expectOne(
        (req) =>
            req.method === 'GET' &&
            (req.urlWithParams === ANALYTICS_EVENT_TOTAL_EVENTS ||
                req.urlWithParams.startsWith(`${ANALYTICS_EVENT_TOTAL_EVENTS}?`))
    );
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

        it('should append impression eventType without granularity', () => {
            spectator.service
                .getTotalEvents({ range: 'last_30_days', eventType: 'impressions' })
                .subscribe();

            const req = expectTotalEventsReq(TestBed.inject(HttpTestingController));
            expect(req.request.params.get('range')).toBe('last_30_days');
            expect(req.request.params.get('eventType')).toBe('impressions');
            expect(req.request.params.get('granularity')).toBeNull();

            req.flush({
                entity: { data: { totalEvents: 99 } },
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                pagination: null,
                permissions: []
            });
        });
    });

    describe('Service Integration', () => {
        it('should be injectable and create instance', () => {
            expect(spectator.service).toBeTruthy();
            expect(spectator.service).toBeInstanceOf(DotAnalyticsService);
        });
    });
});
