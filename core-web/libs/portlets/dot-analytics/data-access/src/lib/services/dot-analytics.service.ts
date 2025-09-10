import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    AnalyticsApiResponse,
    DEFAULT_COUNT_LIMIT,
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    TimeRangeCubeJS,
    TimeRangeInput,
    TopPagePerformanceEntity,
    TopPerformaceTableEntity,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../index';
import { TIME_RANGE_CUBEJS_MAPPING, TIME_RANGE_OPTIONS } from '../constants';
import { createCubeQuery } from '../utils/cube/cube-query-builder.util';
import {
    determineGranularityForTimeRange,
    fillMissingDates
} from '../utils/data/analytics-data.utils';

@Injectable({
    providedIn: 'root'
})
export class DotAnalyticsService {
    #BASE_URL = '/api/v1/analytics/content/_query/cube';
    #http = inject(HttpClient);

    /**
     * Total de pageviews en el período especificado
     */
    totalPageViews(
        timeRange: TimeRangeInput = TIME_RANGE_OPTIONS.last7days,
        siteId: string | string[]
    ): Observable<TotalPageViewsEntity> {
        const queryBuilder = createCubeQuery()
            .measures(['totalRequest'])
            .pageviews()
            .siteId(siteId)
            .timeRange('createdAt', this.#getTimeRange(timeRange));

        const query = queryBuilder.build();

        return this.#http
            .post<AnalyticsApiResponse<TotalPageViewsEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity[0]));
    }

    /**
     * Visitantes únicos (sesiones únicas) en el período especificado
     */
    uniqueVisitors(
        timeRange: TimeRangeInput = TIME_RANGE_OPTIONS.last7days,
        siteId: string | string[]
    ): Observable<UniqueVisitorsEntity> {
        const queryBuilder = createCubeQuery()
            .measures(['totalUsers'])
            .pageviews()
            .siteId(siteId)
            .timeRange('createdAt', this.#getTimeRange(timeRange));

        const query = queryBuilder.build();

        return this.#http
            .post<AnalyticsApiResponse<UniqueVisitorsEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity[0]));
    }

    /**
     * Top page performance metric (total requests from the most visited page)
     */
    topPagePerformance(
        timeRange: TimeRangeInput = TIME_RANGE_OPTIONS.last7days,
        siteId: string | string[]
    ): Observable<TopPagePerformanceEntity> {
        const queryBuilder = createCubeQuery()
            .dimensions(['path', 'pageTitle'])
            .measures(['totalRequest'])
            .pageviews()
            .siteId(siteId)
            .orderBy('totalRequest', 'desc')
            .timeRange('createdAt', this.#getTimeRange(timeRange))
            .limit(1);

        const query = queryBuilder.build();

        return this.#http
            .post<AnalyticsApiResponse<TopPagePerformanceEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity[0]));
    }

    /**
     * Get page view timeline data
     */
    pageViewTimeLine(
        timeRange: TimeRangeInput = TIME_RANGE_OPTIONS.last7days,
        siteId: string | string[]
    ): Observable<PageViewTimeLineEntity[]> {
        // Determine granularity based on specific timeRange values
        const granularity = determineGranularityForTimeRange(timeRange);

        const queryBuilder = createCubeQuery()
            .measures(['totalRequest'])
            .pageviews()
            .siteId(siteId)
            .timeRange('createdAt', this.#getTimeRange(timeRange), granularity);

        const query = queryBuilder.build();

        return this.#http
            .post<AnalyticsApiResponse<PageViewTimeLineEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => fillMissingDates(response.entity, timeRange, granularity)));
    }

    /**
     * Pageviews by device/browser for distribution chart
     */
    pageViewDeviceBrowsers(
        timeRange: TimeRangeInput = TIME_RANGE_OPTIONS.last7days,
        siteId: string | string[]
    ): Observable<PageViewDeviceBrowsersEntity[]> {
        const queryBuilder = createCubeQuery()
            .dimensions(['userAgent'])
            .measures(['totalRequest'])
            .pageviews()
            .siteId(siteId)
            .orderBy('totalRequest', 'desc')
            .timeRange('createdAt', this.#getTimeRange(timeRange))
            .limit(DEFAULT_COUNT_LIMIT);

        const query = queryBuilder.build();

        return this.#http
            .post<AnalyticsApiResponse<PageViewDeviceBrowsersEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity));
    }

    /**
     * Top pages table with title and pageviews
     */
    getTopPagePerformanceTable(
        timeRange: TimeRangeInput = TIME_RANGE_OPTIONS.last7days,
        siteId: string | string[],
        limit = DEFAULT_COUNT_LIMIT
    ): Observable<TopPerformaceTableEntity[]> {
        const queryBuilder = createCubeQuery()
            .dimensions(['path', 'pageTitle'])
            .measures(['totalRequest'])
            .pageviews()
            .siteId(siteId)
            .orderBy('totalRequest', 'desc')
            .timeRange('createdAt', this.#getTimeRange(timeRange))
            .limit(limit);

        const query = queryBuilder.build();

        return this.#http
            .post<AnalyticsApiResponse<TopPerformaceTableEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity));
    }

    #getTimeRange(timeRange: TimeRangeInput): TimeRangeCubeJS {
        if (Array.isArray(timeRange)) {
            return timeRange;
        }
        return (
            TIME_RANGE_CUBEJS_MAPPING[timeRange as keyof typeof TIME_RANGE_CUBEJS_MAPPING] ||
            TIME_RANGE_CUBEJS_MAPPING.last7days
        );
    }
}
