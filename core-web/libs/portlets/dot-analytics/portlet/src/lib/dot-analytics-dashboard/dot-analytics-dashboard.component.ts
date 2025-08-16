import { CommonModule } from '@angular/common';
import { Component, computed, DestroyRef, effect, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import {
    DotAnalyticsDashboardStore,
    extractPageTitle,
    extractPageViews,
    extractSessions,
    extractTopPageValue,
    MetricData,
    TimeRangeInput
} from '@dotcms/portlets/dot-analytics/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsDashboardChartComponent } from './components/dot-analytics-dashboard-chart/dot-analytics-dashboard-chart.component';
import { DotAnalyticsDashboardFiltersComponent } from './components/dot-analytics-dashboard-filters/dot-analytics-dashboard-filters.component';
import { DotAnalyticsDashboardMetricsComponent } from './components/dot-analytics-dashboard-metrics/dot-analytics-dashboard-metrics.component';
import { DotAnalyticsDashboardTableComponent } from './components/dot-analytics-dashboard-table/dot-analytics-dashboard-table.component';
import { CUSTOM_TIME_RANGE } from './constants';
import { DateRange } from './types';
import {
    fromUrlFriendly,
    isValidCustomDateRange,
    isValidTimeRange
} from './utils/dot-analytics.utils';
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
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly store = inject(DotAnalyticsDashboardStore);
    private readonly destroyRef = inject(DestroyRef);

    // Current site ID
    private readonly $currentSiteId = inject(GlobalStore).currentSiteId;

    // Current time range
    protected readonly $currentTimeRange = this.store.timeRange;

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

    constructor() {
        effect(() => {
            console.log(this.store.timeRange());
        });
    }

    /**
     * Refresh dashboard data
     */
    onRefresh(): void {
        const timeRange = this.$currentTimeRange();
        const currentSiteId = this.$currentSiteId();

        if (currentSiteId && timeRange) {
            this.store.loadAllDashboardData(timeRange, currentSiteId);
        }


    }

    ngOnInit(): void {

        const queryParams = getProperQueryParamsFromUrl(this.route.snapshot.queryParamMap);

        if (queryParams) {
            this.router.navigate([], {
                relativeTo: this.route,
                queryParams,
                queryParamsHandling: 'replace',
                replaceUrl: true
            });
        }

        this.store.init(this.route.snapshot.queryParamMap);

    }
}
