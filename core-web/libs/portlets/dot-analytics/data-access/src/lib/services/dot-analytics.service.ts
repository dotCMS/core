import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    AnalyticsApiResponse,
    DEFAULT_COUNT_LIMIT,
    DEFAULT_TIME_RANGE,
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    TimeRangeInput,
    TopPagePerformanceEntity,
    TopPerformaceTableEntity,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../index';
import { createCubeQuery } from '../utils/cube/cube-query-builder.util';
import { determineGranularityForTimeRange } from '../utils/data/analytics-data.utils';

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
        timeRange: TimeRangeInput = DEFAULT_TIME_RANGE,
        siteId: string | string[]
    ): Observable<TotalPageViewsEntity> {
        const queryBuilder = createCubeQuery()
            .measures(['totalRequest'])
            .pageviews()
            .siteId(siteId)
            .timeRange('createdAt', timeRange);

        const query = queryBuilder.build();

        return this.#http
            .post<AnalyticsApiResponse<TotalPageViewsEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity[0]));
    }

    /**
     * Visitantes únicos (sesiones únicas) en el período especificado
     */
    uniqueVisitors(
        timeRange: TimeRangeInput = DEFAULT_TIME_RANGE,
        siteId: string | string[]
    ): Observable<UniqueVisitorsEntity> {
        const queryBuilder = createCubeQuery()
            .measures(['totalUsers'])
            .pageviews()
            .siteId(siteId)
            .timeRange('createdAt', timeRange);

        const query = queryBuilder.build();

        return this.#http
            .post<AnalyticsApiResponse<UniqueVisitorsEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity[0]));
    }

    /**
     * Top page performance metric (total requests from the most visited page)
     */
    topPagePerformance(
        timeRange: TimeRangeInput = DEFAULT_TIME_RANGE,
        siteId: string | string[]
    ): Observable<TopPagePerformanceEntity> {
        const queryBuilder = createCubeQuery()
            .dimensions(['path', 'pageTitle'])
            .measures(['totalRequest'])
            .pageviews()
            .siteId(siteId)
            .orderBy('totalRequest', 'desc')
            .timeRange('createdAt', timeRange)
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
        timeRange: TimeRangeInput = DEFAULT_TIME_RANGE,
        siteId: string | string[]
    ): Observable<PageViewTimeLineEntity[]> {
        // Determine granularity based on specific timeRange values
        const granularity = Array.isArray(timeRange)
            ? 'day' // For custom date ranges, default to day granularity
            : determineGranularityForTimeRange(timeRange);

        const queryBuilder = createCubeQuery()
            .measures(['totalRequest'])
            .pageviews()
            .siteId(siteId)
            .timeRange('createdAt', timeRange, granularity);

        const query = queryBuilder.build();

        return this.#http
            .post<AnalyticsApiResponse<PageViewTimeLineEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity));
    }

    /**
     * Pageviews by device/browser for distribution chart
     */
    pageViewDeviceBrowsers(
        timeRange: TimeRangeInput = DEFAULT_TIME_RANGE,
        siteId: string | string[]
    ): Observable<PageViewDeviceBrowsersEntity[]> {
        const queryBuilder = createCubeQuery()
            .dimensions(['userAgent'])
            .measures(['totalRequest'])
            .pageviews()
            .siteId(siteId)
            .orderBy('totalRequest', 'desc')
            .timeRange('createdAt', timeRange)
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
        timeRange: TimeRangeInput = DEFAULT_TIME_RANGE,
        siteId: string | string[],
        limit = DEFAULT_COUNT_LIMIT
    ): Observable<TopPerformaceTableEntity[]> {
        const queryBuilder = createCubeQuery()
            .dimensions(['path', 'pageTitle'])
            .measures(['totalRequest'])
            .pageviews()
            .siteId(siteId)
            .orderBy('totalRequest', 'desc')
            .timeRange('createdAt', timeRange)
            .limit(limit);

        const query = queryBuilder.build();

        return this.#http
            .post<AnalyticsApiResponse<TopPerformaceTableEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity));
    }
}
