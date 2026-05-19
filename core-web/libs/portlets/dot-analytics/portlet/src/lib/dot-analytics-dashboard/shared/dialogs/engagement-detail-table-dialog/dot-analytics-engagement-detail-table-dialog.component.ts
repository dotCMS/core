import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';
import { TableModule } from 'primeng/table';

import { DotMessagePipe } from '@dotcms/ui';

import { ANALYTICS_DETAIL_DIALOG_TABLE } from '../../../shared/constants';
import { DotAnalyticsStackedBarComponent } from '../../components/dot-analytics-stacked-bar/dot-analytics-stacked-bar.component';

import type { DotAnalyticsEngagementDetailTableDialogData } from './dot-analytics-engagement-detail-table-dialog.models';

@Component({
    selector: 'dot-analytics-engagement-detail-table-dialog',
    imports: [DotMessagePipe, DotAnalyticsStackedBarComponent, TableModule],
    templateUrl: './dot-analytics-engagement-detail-table-dialog.component.html',
    styleUrl: './dot-analytics-engagement-detail-table-dialog.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsEngagementDetailTableDialogComponent {
    readonly COLUMN_SESSIONS_BAR_KEY = 'analytics.engagement.charts.detail.column.sessions-bar';
    readonly COLUMN_ENGAGEMENT_RATE_KEY =
        'analytics.engagement.charts.detail.column.engagement-rate';
    readonly COLUMN_TIME_KEY = 'analytics.engagement.table.headers.time';
    readonly COLUMN_TOTAL_SESSIONS_KEY = 'analytics.engagement.metrics.total-sessions';
    readonly PAGE_REPORT_KEY = 'analytics.detail.table.page-report';
    readonly EMPTY_TABLE_KEY = 'analytics.detail.table.empty';

    readonly DETAIL_TABLE = ANALYTICS_DETAIL_DIALOG_TABLE;

    readonly #dialogConfig = inject(
        DynamicDialogConfig
    ) as DynamicDialogConfig<DotAnalyticsEngagementDetailTableDialogData>;

    protected readonly $rows = computed(() => this.#dialogConfig.data?.rows ?? []);

    protected readonly $dimensionHeaderKey = computed(
        () => this.#dialogConfig.data?.firstColumnHeaderKey ?? ''
    );

    protected formatCount(value: number): string {
        if (!Number.isFinite(value)) return '0';
        return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(
            Math.round(value)
        );
    }
}
