import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import {
    DotAnalyticsDashboardStore,
    extractPageTitle,
    extractPageViews,
    extractSessions,
    extractTopPageValue,
    MetricData,
    TIME_RANGE_OPTIONS
} from '@dotcms/portlets/dot-analytics/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsDashboardChartComponent } from './components/dot-analytics-dashboard-chart/dot-analytics-dashboard-chart.component';
import { DotAnalyticsDashboardFiltersComponent } from './components/dot-analytics-dashboard-filters/dot-analytics-dashboard-filters.component';
import { DotAnalyticsDashboardMetricsComponent } from './components/dot-analytics-dashboard-metrics/dot-analytics-dashboard-metrics.component';
import { DotAnalyticsDashboardTableComponent } from './components/dot-analytics-dashboard-table/dot-analytics-dashboard-table.component';
import { getProperQueryParamsFromUrl } from './utils/state-from-url';

/**
 * Main analytics dashboard component for DotCMS.
 * Displays comprehensive analytics including metrics, charts, and tables.
 *
 * Features:
 * - Key metric cards (pageviews, visitors, performance)
 * - Time-based line chart for pageview trends
 * - Device/browser breakdown pie chart
 * - Top performing pages table
 * - Time period filtering with URL persistence
 * - Loading states with skeletons
 * - Error handling
 *
 */
@Component({
    selector: 'lib-dot-analytics-dashboard',
    imports: [
        CommonModule,
        ButtonModule,
        DotAnalyticsDashboardMetricsComponent,
        DotAnalyticsDashboardChartComponent,
        DotAnalyticsDashboardTableComponent,
        DotAnalyticsDashboardFiltersComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-dashboard.component.html',
    styleUrl: './dot-analytics-dashboard.component.scss',
    standalone: true
})
export default class DotAnalyticsDashboardComponent implements OnInit {
    store = inject(DotAnalyticsDashboardStore);

    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);

    // Current site ID
    private readonly $currentSiteId = inject(GlobalStore).currentSiteId;

    // Metrics signals
    protected readonly $totalPageViews = this.store.totalPageViews;
    protected readonly $uniqueVisitors = this.store.uniqueVisitors;
    protected readonly $topPagePerformance = this.store.topPagePerformance;
    protected readonly $pageViewTimeLine = this.store.pageViewTimeLine;
    protected readonly $pageViewDeviceBrowsers = this.store.pageViewDeviceBrowsers;
    protected readonly $topPagesTable = this.store.topPagesTable;

    // Computed signals for data transformations
    protected readonly $metricsData = computed((): MetricData[] => [
        {
            name: 'analytics.metrics.total-pageviews',
            value: extractPageViews(this.$totalPageViews().data),
            subtitle: 'analytics.metrics.total-pageviews.subtitle',
            icon: 'pi-eye',
            status: this.$totalPageViews().status,
            error: this.$totalPageViews().error
        },
        {
            name: 'analytics.metrics.unique-visitors',
            value: extractSessions(this.$uniqueVisitors().data),
            subtitle: 'analytics.metrics.unique-visitors.subtitle',
            icon: 'pi-users',
            status: this.$uniqueVisitors().status,
            error: this.$uniqueVisitors().error
        },
        {
            name: 'analytics.metrics.top-page-performance',
            value: extractTopPageValue(this.$topPagePerformance().data),
            subtitle: extractPageTitle(this.$topPagePerformance().data),
            icon: 'pi-chart-bar',
            status: this.$topPagePerformance().status,
            error: this.$topPagePerformance().error
        }
    ]);

    /**
     * Refresh dashboard data
     */
    onRefresh(): void {
        const timeRange = this.store.timeRange();
        const currentSiteId = this.$currentSiteId();

        if (currentSiteId && timeRange) {
            this.store.loadAllDashboardData(timeRange, currentSiteId);
        }
    }

    ngOnInit(): void {
        const queryParams = getProperQueryParamsFromUrl(this.route.snapshot.queryParamMap);

        if (queryParams.type === 'params') {
            this.router.navigate([], {
                relativeTo: this.route,
                queryParams: queryParams.params,
                queryParamsHandling: 'replace',
                replaceUrl: true
            });
        } else if (queryParams.type === 'timeRange') {
            this.store.setTimeRange(queryParams.timeRange);
        } else {
            this.store.setTimeRange(TIME_RANGE_OPTIONS.last7days);
        }
    }
}
