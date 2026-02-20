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
    changeDetection: ChangeDetectionStrategy.OnPush
})
export default class DotAnalyticsEngagementReportComponent implements OnInit {
    /** Analytics dashboard store providing engagement data and actions */
    readonly store = inject(DotAnalyticsDashboardStore);
    readonly #globalStore = inject(GlobalStore);
    readonly #messageService = inject(DotMessageService);

    /** Raw engagement data slice from the store */
    readonly engagementData = this.store.engagementData;

    /** Controls visibility of the "How it's calculated" dialog */
    readonly $showCalculationDialog = signal(false);

    ngOnInit(): void {
        this.#globalStore.addNewBreadcrumb({
            id: 'engagement',
            label: this.#messageService.get('analytics.dashboard.tabs.engagement')
        });
    }

    /** Key performance indicators (engagement rate, avg session time, etc.) */
    readonly $kpis = computed(() => this.engagementData().data?.kpis);
    /** Engagement trend data for the sparkline/trend chart */
    readonly $trend = computed(() => this.engagementData().data?.trend);
    /** Engagement breakdown data for the doughnut chart */
    readonly $breakdown = computed(() => this.engagementData().data?.breakdown);
    /** Platform analytics data (device, browser, language) */
    readonly $platforms = computed(() => this.engagementData().data?.platforms);
    /** Current component status derived from store data */
    readonly $status = computed(() => this.engagementData().status ?? ComponentStatus.INIT);
    /** Whether data has finished loading successfully */
    readonly $isLoaded = computed(() => this.$status() === ComponentStatus.LOADED);

    /** Calculate total sessions from platforms data */
    readonly $totalSessions = computed(() => {
        const platforms = this.$platforms();
        if (!platforms?.device) return 0;

        return platforms.device.reduce((sum, item) => sum + item.views, 0);
    });
}
