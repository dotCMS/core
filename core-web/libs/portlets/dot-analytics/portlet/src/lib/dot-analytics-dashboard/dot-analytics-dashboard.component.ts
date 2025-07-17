import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsDashboardChartComponent } from './components/dot-analytics-dashboard-chart/dot-analytics-dashboard-chart.component';
import { DotAnalyticsDashboardFiltersComponent } from './components/dot-analytics-dashboard-filters/dot-analytics-dashboard-filters.component';
import { DotAnalyticsDashboardLoadingComponent } from './components/dot-analytics-dashboard-loading/dot-analytics-dashboard-loading.component';
import { DotAnalyticsDashboardMetricsComponent } from './components/dot-analytics-dashboard-metrics/dot-analytics-dashboard-metrics.component';
import { DotAnalyticsDashboardTableComponent } from './components/dot-analytics-dashboard-table/dot-analytics-dashboard-table.component';
import { DASHBOARD_MOCK_DATA } from './mocks/dashboard.mocks';

/**
 * Main analytics dashboard component for DotCMS.
 * Displays comprehensive analytics including metrics, charts, and tables.
 *
 * Features:
 * - Key metric cards (pageviews, visitors, performance)
 * - Time-based line chart for pageview trends
 * - Device/browser breakdown pie chart
 * - Top performing pages table
 * - Time period filtering
 * - Loading states with skeletons
 *
 */
@Component({
    selector: 'lib-dot-analytics-dashboard',
    standalone: true,
    imports: [
        CommonModule,
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
    /** Mock data for dashboard demonstration */
    protected readonly $mockData = DASHBOARD_MOCK_DATA;

    /** Loading state for showing skeleton placeholders */
    protected readonly $isLoading = false;

    /**
     * Handles time period filter changes.
     * Currently logs the selection - will be connected to real data service.
     *
     * @param _period - Selected time period value
     */
    onPeriodChange(_period: string): void {
        // TODO: Implement period change logic
        // This will update the data based on the selected period
    }
}
