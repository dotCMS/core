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

import { DotAnalyticsDashboardChartComponent } from '../dot-analytics-dashboard-chart/dot-analytics-dashboard-chart.component';
import { DotAnalyticsDashboardMetricsComponent } from '../dot-analytics-dashboard-metrics/dot-analytics-dashboard-metrics.component';
import { DotAnalyticsPlatformsTableComponent } from '../dot-analytics-platforms-table/dot-analytics-platforms-table.component';
import { DotAnalyticsSparklineComponent } from '../dot-analytics-sparkline/dot-analytics-sparkline.component';

/**
 * DotAnalyticsDashboardEngagementReportComponent displays the engagement dashboard.
 * It includes the engagement rate, trend chart, and platforms table.
 */
@Component({
    selector: 'dot-analytics-dashboard-engagement-report',
    imports: [
        CommonModule,
        ButtonModule,
        DialogModule,
        DotMessagePipe,
        DotAnalyticsDashboardChartComponent,
        DotAnalyticsDashboardMetricsComponent,
        DotAnalyticsPlatformsTableComponent,
        DotAnalyticsSparklineComponent
    ],
    templateUrl: './dot-analytics-dashboard-engagement-report.component.html',
    styleUrl: './dot-analytics-dashboard-engagement-report.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export default class DotAnalyticsDashboardEngagementReportComponent implements OnInit {
    readonly store = inject(DotAnalyticsDashboardStore);
    readonly #globalStore = inject(GlobalStore);
    readonly #messageService = inject(DotMessageService);

    readonly engagementData = this.store.engagementData;

    /** Controls visibility of the "How it's calculated" dialog */
    readonly $showCalculationDialog = signal(false);

    ngOnInit(): void {
        this.#globalStore.addNewBreadcrumb({
            label: this.#messageService.get('analytics.dashboard.tabs.engagement')
        });
    }

    readonly $kpis = computed(() => this.engagementData().data?.kpis);
    readonly $trend = computed(() => this.engagementData().data?.trend);
    readonly $breakdown = computed(() => this.engagementData().data?.breakdown);
    readonly $platforms = computed(() => this.engagementData().data?.platforms);
    readonly $status = computed(() => this.engagementData().status ?? ComponentStatus.INIT);
    readonly $isLoaded = computed(() => this.$status() === ComponentStatus.LOADED);
}
