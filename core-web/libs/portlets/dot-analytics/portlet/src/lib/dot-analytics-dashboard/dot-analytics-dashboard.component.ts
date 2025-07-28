import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import {
    DotAnalyticsDashboardStore,
    TimeRangeInput
} from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsDashboardChartComponent } from './components/dot-analytics-dashboard-chart/dot-analytics-dashboard-chart.component';
import { DotAnalyticsDashboardFiltersComponent } from './components/dot-analytics-dashboard-filters/dot-analytics-dashboard-filters.component';
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
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-dashboard.component.html',
    styleUrl: './dot-analytics-dashboard.component.scss'
})
export default class DotAnalyticsDashboardComponent {
    private readonly store = inject(DotAnalyticsDashboardStore);

    // Direct access to raw store signals - flexible and powerful
    protected readonly $currentTimeRange = this.store.timeRange;

    // Individual resource signals - you can access .data(), .status(), .error()
    protected readonly $totalPageViews = this.store.totalPageViews;
    protected readonly $uniqueVisitors = this.store.uniqueVisitors;
    protected readonly $topPagePerformance = this.store.topPagePerformance;
    protected readonly $pageViewTimeLine = this.store.pageViewTimeLine;
    protected readonly $pageViewDeviceBrowsers = this.store.pageViewDeviceBrowsers;
    protected readonly $topPagesTable = this.store.topPagesTable;

    // Computed/transformed data from store
    protected readonly $metricsData = this.store.metricsData;
    protected readonly $topPagesTableData = this.store.topPagesTableData;
    protected readonly $pageviewsTimelineData = this.store.pageViewTimeLineData;
    protected readonly $deviceBreakdownData = this.store.pageViewDeviceBrowsersData;
    protected readonly $deviceBreakdownStatus = this.store.pageViewDeviceBrowsers.status;

    /**
     * Handles time period filter changes.
     * Updates the store and URL query parameters.
     *
     * @param period - Selected time period value
     */
    onTimeRangeChange(timeRange: TimeRangeInput): void {
        this.store.setTimeRange(timeRange);
    }

    /**
     * Refresh dashboard data
     */
    onRefresh(): void {
        const timeRange = this.$currentTimeRange();

        // Refresh all dashboard data using the coordinated method
        this.store.loadAllDashboardData(timeRange);
    }
}
