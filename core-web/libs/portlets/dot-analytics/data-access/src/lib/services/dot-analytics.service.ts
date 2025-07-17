import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { AnalyticsDashboardData } from '../store/dot-analytics-dashboard.store';

@Injectable({
    providedIn: 'root'
})
export class DotAnalyticsService {
    private readonly BASE_URL = '/api/v1/analytics';

    constructor(private readonly http: HttpClient) {}

    getDashboardData(
        pageId: string,
        timeRange: 'day' | 'week' | 'month'
    ): Observable<AnalyticsDashboardData> {
        const params = {
            pageId,
            timeRange
        };

        return this.http.get<AnalyticsDashboardData>(`${this.BASE_URL}/dashboard`, {
            params
        });
    }
}
