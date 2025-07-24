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
        timeRange: TimeRangeInput = DEFAULT_TIME_RANGE
    ): Observable<TotalPageViewsEntity> {
        const query = createCubeQuery()
            .measures(['totalRequest'])
            .pageviews()
            .timeRange('createdAt', timeRange)
            .build();

        return this.#http
            .post<AnalyticsApiResponse<TotalPageViewsEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity[0]));
    }

    /**
     * Visitantes únicos (sesiones únicas) en el período especificado
     */
    uniqueVisitors(
        timeRange: TimeRangeInput = DEFAULT_TIME_RANGE
    ): Observable<UniqueVisitorsEntity> {
        const query = createCubeQuery()
            .measures(['totalUsers'])
            .pageviews()
            .timeRange('createdAt', timeRange)
            .build();

        return this.#http
            .post<AnalyticsApiResponse<UniqueVisitorsEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity[0]));
    }

    /**
     * Top page performance metric (total requests from the most visited page)
     */
    topPagePerformance(
        timeRange: TimeRangeInput = DEFAULT_TIME_RANGE
    ): Observable<TopPagePerformanceEntity> {
        const query = createCubeQuery()
            .dimensions(['path', 'pageTitle'])
            .measures(['totalRequest'])
            .pageviews()
            .orderBy('createdAt', 'desc')
            .timeRange('createdAt', timeRange)
            .limit(1)
            .build();

        return this.#http
            .post<AnalyticsApiResponse<TopPagePerformanceEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity[0]));
    }

    /**
     * Get page view timeline data
     */
    pageViewTimeLine(
        timeRange: TimeRangeInput = DEFAULT_TIME_RANGE
    ): Observable<PageViewTimeLineEntity[]> {
        // Determine granularity based on specific timeRange values
        const granularity = Array.isArray(timeRange)
            ? 'day' // For custom date ranges, default to day granularity
            : determineGranularityForTimeRange(timeRange);

        const query = createCubeQuery()
            .measures(['totalRequest'])
            .pageviews()
            .timeRange('createdAt', timeRange, granularity)
            .build();

        return this.#http
            .post<AnalyticsApiResponse<PageViewTimeLineEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity));
    }

    /**
     * Pageviews by device/browser for distribution chart
     */
    pageViewDeviceBrowsers(
        timeRange: TimeRangeInput = DEFAULT_TIME_RANGE
    ): Observable<PageViewDeviceBrowsersEntity[]> {
        const query = createCubeQuery()
            .dimensions(['userAgent'])
            .measures(['totalRequest'])
            .pageviews()
            .orderBy('totalRequest', 'desc')
            .timeRange('createdAt', timeRange)
            .limit(DEFAULT_COUNT_LIMIT)
            .build();

        return this.#http
            .post<AnalyticsApiResponse<PageViewDeviceBrowsersEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity));
    }

    /**
     * Top pages table with title and pageviews
     */
    getTopPagePerformanceTable(
        timeRange: TimeRangeInput = DEFAULT_TIME_RANGE,
        limit = DEFAULT_COUNT_LIMIT
    ): Observable<TopPerformaceTableEntity[]> {
        const query = createCubeQuery()
            .dimensions(['path', 'pageTitle'])
            .measures(['totalRequest'])
            .pageviews()
            .orderBy('totalRequest', 'desc')
            .timeRange('createdAt', timeRange)
            .limit(limit)
            .build();

        return this.#http
            .post<AnalyticsApiResponse<TopPerformaceTableEntity>>(this.#BASE_URL, query)
            .pipe(map((response) => response.entity));
    }
}
