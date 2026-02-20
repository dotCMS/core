import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
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

import { DotAnalyticsChartComponent } from '../../../shared/components/dot-analytics-chart/dot-analytics-chart.component';
import { DotAnalyticsMetricComponent } from '../../../shared/components/dot-analytics-metric/dot-analytics-metric.component';
import { ChartData } from '../../../shared/types';
import { DotAnalyticsTopPagesTableComponent } from '../dot-analytics-top-pages-table/dot-analytics-top-pages-table.component';

@Component({
    selector: 'dot-analytics-pageview-report',
    imports: [
        CommonModule,
        DotAnalyticsMetricComponent,
        DotAnalyticsChartComponent,
        DotAnalyticsTopPagesTableComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-pageview-report.component.html',
    styleUrl: './dot-analytics-pageview-report.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex flex-column gap-4 w-full'
    }
})
/**
 * Pageview report component displaying page traffic metrics, timeline chart,
 * device/browser breakdown, and top performing pages table.
 */
export default class DotAnalyticsPageviewReportComponent {
    /** Analytics dashboard store providing pageview data and actions */
    readonly store = inject(DotAnalyticsDashboardStore);
    readonly #globalStore = inject(GlobalStore);
    readonly #messageService = inject(DotMessageService);

    /** Total page views metric data from store */
    protected readonly $totalPageViews = this.store.totalPageViews;
    /** Unique visitors metric data from store */
    protected readonly $uniqueVisitors = this.store.uniqueVisitors;
    /** Top page performance metric data from store */
    protected readonly $topPagePerformance = this.store.topPagePerformance;
    /** Top pages table data from store */
    protected readonly $topPagesTable = this.store.topPagesTable;

    /** Transformed chart data for the pageview timeline line chart */
    protected readonly $pageViewTimeLineData = computed<ChartData>(() =>
        transformPageViewTimeLineData(this.store.pageViewTimeLine().data)
    );
    /** Loading/error status for the pageview timeline chart */
    protected readonly $pageViewTimeLineStatus = computed(
        () => this.store.pageViewTimeLine().status
    );

    /** Transformed chart data for the device & browser breakdown chart */
    protected readonly $pageViewDeviceBrowsersData = computed<ChartData>(() =>
        transformDeviceBrowsersData(this.store.pageViewDeviceBrowsers().data)
    );
    /** Loading/error status for the device & browser chart */
    protected readonly $pageViewDeviceBrowsersStatus = computed(
        () => this.store.pageViewDeviceBrowsers().status
    );

    constructor() {
        this.#setupBreadcrumb();
    }

    #setupBreadcrumb(): void {
        this.#globalStore.addNewBreadcrumb({
            label: this.#messageService.get('analytics.dashboard.tabs.pageview')
        });
    }

    /** Aggregated metric cards data combining store slices with display metadata */
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
