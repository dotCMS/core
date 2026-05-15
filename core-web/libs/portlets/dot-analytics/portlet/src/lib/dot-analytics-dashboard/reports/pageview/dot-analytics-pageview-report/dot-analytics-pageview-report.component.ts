import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { CardModule } from 'primeng/card';

import {
    DotAnalyticsDashboardStore,
    EngagementPlatformMetrics,
    extractPageTitle,
    extractPageViews,
    extractSessions,
    extractTopPageValue,
    MetricData,
    PieChartEntry,
    transformPageViewTimeLineData
} from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsBarChartComponent } from '../../../shared/components/dot-analytics-bar-chart/dot-analytics-bar-chart.component';
import { DotAnalyticsChartComponent } from '../../../shared/components/dot-analytics-chart/dot-analytics-chart.component';
import { DotAnalyticsMetricComponent } from '../../../shared/components/dot-analytics-metric/dot-analytics-metric.component';
import { ChartData } from '../../../shared/types';
import { DotAnalyticsTopPagesTableComponent } from '../dot-analytics-top-pages-table/dot-analytics-top-pages-table.component';

/** Maps store breakdown slices to bar-chart rows (percentage share of total value). */
function breakdownEntriesToBarChartMetrics(entries: PieChartEntry[]): EngagementPlatformMetrics[] {
    const total = entries.reduce((sum, e) => sum + e.value, 0);
    if (total <= 0) {
        return [];
    }

    return entries.map((e) => ({
        name: e.name,
        views: e.value,
        percentage: Math.round((e.value / total) * 100),
        totalSessions: total,
        time: ''
    }));
}

@Component({
    selector: 'dot-analytics-pageview-report',
    imports: [
        CardModule,
        DotAnalyticsMetricComponent,
        DotAnalyticsChartComponent,
        DotAnalyticsBarChartComponent,
        DotAnalyticsTopPagesTableComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-pageview-report.component.html',
    styleUrl: './dot-analytics-pageview-report.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex w-full flex-col gap-0 pt-0 pb-4 text-gray-900 dark:text-gray-100'
    }
})
/**
 * Pageview report component displaying page traffic metrics, timeline chart,
 * browser/device bar breakdown charts, and top performing pages table.
 */
export default class DotAnalyticsPageviewReportComponent {
    /** Analytics dashboard store providing pageview data and actions */
    readonly store = inject(DotAnalyticsDashboardStore);

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

    protected readonly $pageViewBrowserBreakdownStatus = computed(
        () => this.store.pageViewBrowserBreakdown().status
    );

    protected readonly $pageViewDeviceBreakdownStatus = computed(
        () => this.store.pageViewDeviceBreakdown().status
    );

    /** Bar-chart rows for browser breakdown (groupBy=browser). */
    protected readonly $browserBreakdownBarData = computed<EngagementPlatformMetrics[]>(() =>
        breakdownEntriesToBarChartMetrics(this.store.pageViewBrowserBreakdown().data ?? [])
    );

    /** Bar-chart rows for device breakdown (groupBy=device). */
    protected readonly $deviceBreakdownBarData = computed<EngagementPlatformMetrics[]>(() =>
        breakdownEntriesToBarChartMetrics(this.store.pageViewDeviceBreakdown().data ?? [])
    );
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
