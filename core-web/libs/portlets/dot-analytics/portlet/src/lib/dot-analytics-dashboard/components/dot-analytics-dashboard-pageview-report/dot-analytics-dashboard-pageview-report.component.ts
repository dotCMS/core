import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import {
    DotAnalyticsDashboardStore,
    extractPageTitle,
    extractPageViews,
    extractSessions,
    extractTopPageValue,
    MetricData,
    transformDeviceBrowsersData,
    transformPageViewTimeLineData
} from '@dotcms/portlets/dot-analytics/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';

import { ChartData } from '../../types';
import { DotAnalyticsDashboardChartComponent } from '../dot-analytics-dashboard-chart/dot-analytics-dashboard-chart.component';
import { DotAnalyticsDashboardMetricsComponent } from '../dot-analytics-dashboard-metrics/dot-analytics-dashboard-metrics.component';
import { DotAnalyticsDashboardTableComponent } from '../dot-analytics-dashboard-table/dot-analytics-dashboard-table.component';

@Component({
    selector: 'dot-analytics-dashboard-pageview-report',
    imports: [
        CommonModule,
        DotAnalyticsDashboardMetricsComponent,
        DotAnalyticsDashboardChartComponent,
        DotAnalyticsDashboardTableComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-dashboard-pageview-report.component.html',
    styleUrl: './dot-analytics-dashboard-pageview-report.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export default class DotAnalyticsDashboardPageviewReportComponent {
    readonly store = inject(DotAnalyticsDashboardStore);
    readonly #globalStore = inject(GlobalStore);

    // Metrics signals from store
    protected readonly $totalPageViews = this.store.totalPageViews;
    protected readonly $uniqueVisitors = this.store.uniqueVisitors;
    protected readonly $topPagePerformance = this.store.topPagePerformance;
    protected readonly $topPagesTable = this.store.topPagesTable;

    // Chart signals - transformed data
    protected readonly $pageViewTimeLineData = computed<ChartData>(() =>
        transformPageViewTimeLineData(this.store.pageViewTimeLine().data)
    );
    protected readonly $pageViewTimeLineStatus = computed(
        () => this.store.pageViewTimeLine().status
    );

    protected readonly $pageViewDeviceBrowsersData = computed<ChartData>(() =>
        transformDeviceBrowsersData(this.store.pageViewDeviceBrowsers().data)
    );
    protected readonly $pageViewDeviceBrowsersStatus = computed(
        () => this.store.pageViewDeviceBrowsers().status
    );

    constructor() {
        this.#setupBreadcrumb();
    }

    #setupBreadcrumb(): void {
        this.#globalStore.addNewBreadcrumb({
            label: 'Pageview'
        });
    }

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
}
