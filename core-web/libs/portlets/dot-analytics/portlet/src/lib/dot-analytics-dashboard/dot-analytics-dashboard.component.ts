import { signalMethod } from '@ngrx/signals';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, ParamMap, Params } from '@angular/router';

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
    changeDetection: ChangeDetectionStrategy.OnPush
})
export default class DotAnalyticsDashboardComponent {
    store = inject(DotAnalyticsDashboardStore);

    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);

    /** Query params */
    $queryParams = toSignal(this.route.queryParamMap, {
        requireSync: true
    });

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

    constructor() {
        this.#handleQueryParamsChanges(this.$queryParams);
    }

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

    /** Handle query params changes */
    readonly #handleQueryParamsChanges = signalMethod<ParamMap>((queryParamMap) => {
        const queryParams = getProperQueryParamsFromUrl(queryParamMap);

        if (queryParams.type === 'params') {
            this.refreshQueryParams(queryParams.params);
        } else {
            this.store.setTimeRange(queryParams.timeRange ?? TIME_RANGE_OPTIONS.last7days);
        }
    });

    refreshQueryParams(queryParams: Params): void {
        this.router.navigate([], {
            relativeTo: this.route,
            queryParams: queryParams,
            queryParamsHandling: 'replace',
            replaceUrl: true
        });
    }
}
