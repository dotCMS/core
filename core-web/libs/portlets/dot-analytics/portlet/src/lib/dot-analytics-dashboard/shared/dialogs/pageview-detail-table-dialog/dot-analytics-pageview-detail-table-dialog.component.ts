import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';
import { TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { ANALYTICS_DETAIL_DIALOG_TABLE } from '../../../shared/constants';
import { DotAnalyticsCountPipe } from '../../pipes/dot-analytics-count/dot-analytics-count.pipe';

import type {
    DotAnalyticsPageviewDetailTableDialogData,
    DotAnalyticsPageviewDetailTableRow
} from './dot-analytics-pageview-detail-table-dialog.models';

@Component({
    selector: 'dot-analytics-pageview-detail-table-dialog',
    imports: [DotMessagePipe, DotAnalyticsCountPipe, TableModule],
    templateUrl: './dot-analytics-pageview-detail-table-dialog.component.html',
    styleUrl: './dot-analytics-pageview-detail-table-dialog.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsPageviewDetailTableDialogComponent {
    readonly COLUMN_RATE_KEY = 'analytics.pageview.detail.column.rate';
    readonly COLUMN_VIEWS_KEY = 'analytics.pageview.detail.column.views';
    readonly EMPTY_TABLE_KEY = 'analytics.detail.table.empty';

    /** Exposed for table bindings. */
    readonly DETAIL_TABLE = ANALYTICS_DETAIL_DIALOG_TABLE;

    readonly #dialogConfig = inject(
        DynamicDialogConfig
    ) as DynamicDialogConfig<DotAnalyticsPageviewDetailTableDialogData>;

    readonly #messageService = inject(DotMessageService);

    protected readonly $rows = computed<DotAnalyticsPageviewDetailTableRow[]>(
        () => this.#dialogConfig.data?.rows ?? []
    );

    protected readonly $dimensionHeaderKey = computed(
        () => this.#dialogConfig.data?.firstColumnHeaderKey ?? ''
    );

    protected progressAriaLabel(row: DotAnalyticsPageviewDetailTableRow): string {
        const pctMsg = `${row.percentage}%`;
        const rateLabel = this.#messageService.get(this.COLUMN_RATE_KEY);
        return `${rateLabel}: ${pctMsg}`;
    }
}
