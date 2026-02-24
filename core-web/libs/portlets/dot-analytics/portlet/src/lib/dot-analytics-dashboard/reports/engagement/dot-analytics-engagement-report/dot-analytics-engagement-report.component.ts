import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    OnInit,
    signal
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotAnalyticsDashboardStore } from '@dotcms/portlets/dot-analytics/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsChartComponent } from '../../../shared/components/dot-analytics-chart/dot-analytics-chart.component';
import { DotAnalyticsMetricComponent } from '../../../shared/components/dot-analytics-metric/dot-analytics-metric.component';
import { DotAnalyticsSparklineComponent } from '../../../shared/components/dot-analytics-sparkline/dot-analytics-sparkline.component';
import { DotAnalyticsPlatformsTableComponent } from '../dot-analytics-platforms-table/dot-analytics-platforms-table.component';

/**
 * DotAnalyticsEngagementReportComponent displays the engagement dashboard.
 * It includes the engagement rate, trend chart, and platforms table.
 * Each block (KPIs, breakdown, platforms) has independent loading and error state.
 */
@Component({
    selector: 'dot-analytics-engagement-report',
    imports: [
        CommonModule,
        ButtonModule,
        DialogModule,
        DotMessagePipe,
        DotAnalyticsChartComponent,
        DotAnalyticsMetricComponent,
        DotAnalyticsPlatformsTableComponent,
        DotAnalyticsSparklineComponent
    ],
    templateUrl: './dot-analytics-engagement-report.component.html',
    styleUrl: './dot-analytics-engagement-report.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex flex-col gap-6 w-full'
    }
})
export default class DotAnalyticsEngagementReportComponent implements OnInit {
    /** Analytics dashboard store providing engagement data and actions */
    readonly store = inject(DotAnalyticsDashboardStore);
    readonly #globalStore = inject(GlobalStore);
    readonly #messageService = inject(DotMessageService);

    /** Controls visibility of the "How it's calculated" dialog */
    readonly $showCalculationDialog = signal(false);

    ngOnInit(): void {
        this.#globalStore.addNewBreadcrumb({
            id: 'engagement',
            label: this.#messageService.get('analytics.dashboard.tabs.engagement')
        });
    }

    /** KPIs slice: data and status for the metric cards */
    readonly $kpis = computed(() => this.store.engagementKpis().data);
    readonly $kpisStatus = computed(
        () => this.store.engagementKpis().status ?? ComponentStatus.INIT
    );

    /** Breakdown slice: doughnut chart data and status */
    readonly $breakdown = computed(() => this.store.engagementBreakdown().data);
    readonly $breakdownStatus = computed(
        () => this.store.engagementBreakdown().status ?? ComponentStatus.INIT
    );

    /** Platforms slice: device/browser/language and status */
    readonly $platforms = computed(() => this.store.engagementPlatforms().data);
    readonly $platformsStatus = computed(
        () => this.store.engagementPlatforms().status ?? ComponentStatus.INIT
    );

    /** Sparkline slice: data and status (fed by the trend chart request) */
    readonly $sparklineData = computed(() => this.store.engagementSparkline().data ?? []);
    readonly $sparklineStatus = computed(
        () => this.store.engagementSparkline().status ?? ComponentStatus.INIT
    );

    /** Whether KPIs have finished loading successfully */
    readonly $isKpisLoaded = computed(() => this.$kpisStatus() === ComponentStatus.LOADED);

    /** Calculate total sessions from platforms data */
    readonly $totalSessions = computed(() => {
        const platforms = this.$platforms();
        if (!platforms?.device) return 0;

        return platforms.device.reduce((sum, item) => sum + item.views, 0);
    });

    /**
     * True when data is loaded but there are no sessions (empty state).
     * Used to show a clear "no data" banner and avoid showing raw zeros everywhere.
     */
    readonly $hasNoData = computed(() => {
        if (this.$kpisStatus() !== ComponentStatus.LOADED) return false;
        if (this.$breakdownStatus() !== ComponentStatus.LOADED) return false;
        const breakdown = this.$breakdown();
        return !breakdown?.labels?.length && !breakdown?.datasets?.length;
    });
}
