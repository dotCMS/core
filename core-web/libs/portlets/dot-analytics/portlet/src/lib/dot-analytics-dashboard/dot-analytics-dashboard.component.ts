import { CommonModule } from '@angular/common';
import { Component, effect, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import {
    DotAnalyticsDashboardStore,
    TimeRange
} from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';


import { DotAnalyticsDashboardChartComponent } from './components/dot-analytics-dashboard-chart/dot-analytics-dashboard-chart.component';
import { DotAnalyticsDashboardFiltersComponent } from './components/dot-analytics-dashboard-filters/dot-analytics-dashboard-filters.component';
import { DotAnalyticsDashboardLoadingComponent } from './components/dot-analytics-dashboard-loading/dot-analytics-dashboard-loading.component';
import { DotAnalyticsDashboardMetricsComponent } from './components/dot-analytics-dashboard-metrics/dot-analytics-dashboard-metrics.component';
import { DotAnalyticsDashboardTableComponent } from './components/dot-analytics-dashboard-table/dot-analytics-dashboard-table.component';

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
    standalone: true,
    imports: [
        CommonModule,
        ButtonModule,
        DotAnalyticsDashboardMetricsComponent,
        DotAnalyticsDashboardChartComponent,
        DotAnalyticsDashboardTableComponent,
        DotAnalyticsDashboardFiltersComponent,
        DotAnalyticsDashboardLoadingComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-dashboard.component.html',
    styleUrl: './dot-analytics-dashboard.component.scss'
})
export default class DotAnalyticsDashboardComponent {
    private readonly store = inject(DotAnalyticsDashboardStore);

    protected readonly $currentTimeRange = this.store.timeRange;
    protected readonly $metricsData = this.store.metricsData;
    protected readonly $topPagesTableData = this.store.topPagesTableData;
    protected readonly $topPagesTableStatus = this.store.topPagesTable.status;

    protected readonly $pageviewsTimelineData = this.store.pageViewTimeLineData;
    protected readonly $deviceBreakdownData = this.store.pageViewDeviceBrowsersData;

    constructor() {
        effect(() => {
            const timeRange = this.$currentTimeRange();

            this.store.loadTotalPageViews(timeRange);

            this.store.loadPageViewDeviceBrowsers(timeRange);
            this.store.loadPageViewTimeLine(timeRange);
            this.store.loadTopPagePerformance(timeRange);
            this.store.loadUniqueVisitors(timeRange);
            this.store.loadTopPagesTable(timeRange);
        }, { allowSignalWrites: true });
    }

    /**
     * Handles time period filter changes.
     * Updates the store and URL query parameters.
     *
     * @param period - Selected time period value
     */
    onTimeRangeChange(timeRange: TimeRange): void {
        this.store.setTimeRange(timeRange);
    }

    /**
     * Refresh dashboard data
     */
    onRefresh(): void {
        const timeRange = this.$currentTimeRange();

        this.store.loadTotalPageViews(timeRange);
        this.store.loadTopPagePerformance(timeRange);
        this.store.loadUniqueVisitors(timeRange);
        this.store.loadTopPagesTable(timeRange);
        this.store.loadPageViewTimeLine(timeRange);
        this.store.loadPageViewDeviceBrowsers(timeRange);
    }

    /**
     * Reset dashboard to initial state
     */
    onReset(): void {
        // TODO: Implement reset logic
    }
}
