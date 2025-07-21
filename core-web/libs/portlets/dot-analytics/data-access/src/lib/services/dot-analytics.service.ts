import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import { createCubeQuery } from '../utils/cube/cube-query-builder.util';

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

@Injectable({
    providedIn: 'root'
})
export class DotAnalyticsService {
    #BASE_URL = '/api/v1/analytics/content/_query/cube';
    #http = inject(HttpClient);


    /**
     * Total de pageviews en el período especificado
     */
    totalPageViews(timeRange: TimeRange = 'from 7 days ago to now'): Observable<TotalPageViewsEntity> {
        const query = createCubeQuery()
            .measures(['totalRequest'])
            .pageviews()
            .timeRange('createdAt', timeRange)
            .build();

        return this.#http.post<AnalyticsApiResponse<TotalPageViewsEntity>>(this.#BASE_URL, query).pipe(
            map((response) => response.entity[0])
        );
    }

    /**
     * Visitantes únicos (sesiones únicas) en el período especificado
     */
    uniqueVisitors(timeRange: TimeRange = 'from 7 days ago to now'): Observable<UniqueVisitorsEntity> {
        const query = createCubeQuery()
            .measures(['totalUser'])
            .pageviews()
            .timeRange('createdAt', timeRange)
            .build();

        return this.#http.post<AnalyticsApiResponse<UniqueVisitorsEntity>>(this.#BASE_URL, query).pipe(
            map((response) => response.entity[0])
        );
    }

    /**
     * Top page performance metric (total requests from the most visited page)
     */
    topPagePerformance(timeRange: TimeRange = 'from 7 days ago to now'): Observable<TopPagePerformanceEntity> {
        const query = createCubeQuery()
            .dimensions(['path', 'pageTitle'])
            .measures(['totalRequest'])
            .pageviews()
            .orderBy('createdAt', 'desc')
            .timeRange('createdAt', timeRange)
            .limit(1)
            .build();

        return this.#http.post<AnalyticsApiResponse<TopPagePerformanceEntity>>(this.#BASE_URL, query).pipe(
            map((response) => response.entity[0])
        );
    }



    /**
     * Get page view timeline data
     */
    pageViewTimeLine(timeRange: TimeRange = 'from 7 days ago to now'): Observable<PageViewTimeLineEntity[]> {
        // Determinar granularidad basada en el timeRange
        let granularity: 'day' | 'week' | 'month' = 'day';
        if (timeRange.includes('week')) {
            granularity = timeRange.includes('1 week') ? 'day' : 'week';
        } else if (timeRange.includes('month')) {
            granularity = 'week';
        }

        const query = createCubeQuery()
            .measures(['totalRequest'])
            .pageviews()
            .timeRange('createdAt', timeRange, granularity)
            .build();

        return this.#http.post<AnalyticsApiResponse<PageViewTimeLineEntity>>(this.#BASE_URL, query).pipe(
            map((response) => response.entity)
        );
    }

    /**
     * Pageviews by device/browser for distribution chart
     */
    pageViewDeviceBrowsers(timeRange: TimeRange = 'from 7 days ago to now'): Observable<PageViewDeviceBrowsersEntity[]> {
        const query = createCubeQuery()
            .dimensions(['userAgent'])
            .measures(['totalRequest'])
            .pageviews()
            .orderBy('totalRequest', 'desc')
            .timeRange('createdAt', timeRange)
            .limit(10)
            .build();

        return this.#http.post<AnalyticsApiResponse<PageViewDeviceBrowsersEntity>>(this.#BASE_URL, query).pipe(
            map((response) => response.entity)
        );
    }



    /**
     * Top pages table with title and pageviews
     */
    getTopPagePerformanceTable(timeRange: TimeRange = 'from 7 days ago to now', limit = 50): Observable<TopPerformaceTableEntity[]> {
        const query = createCubeQuery()
            .dimensions(['path', 'pageTitle'])
            .measures(['totalRequest'])
            .pageviews()
            .orderBy('totalRequest', 'desc')
            .timeRange('createdAt', timeRange)
            .limit(limit)
            .build();

        return this.#http.post<AnalyticsApiResponse<TopPerformaceTableEntity>>(this.#BASE_URL, query).pipe(
            map((response) => response.entity)
        );
    }

}
