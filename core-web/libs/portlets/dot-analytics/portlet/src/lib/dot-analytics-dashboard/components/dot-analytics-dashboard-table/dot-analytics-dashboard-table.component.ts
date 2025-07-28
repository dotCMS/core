import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { TABLE_CONFIG, TOP_PAGES_TABLE_COLUMNS } from '../../constants';
import { TableColumn } from '../../types';

/**
 * Top pages analytics table component.
 * Displays top performing pages with pageviews, sorting and pagination.
 */
@Component({
    selector: 'dot-analytics-dashboard-table',
    imports: [CommonModule, CardModule, SkeletonModule, TableModule, DotMessagePipe],
    templateUrl: './dot-analytics-dashboard-table.component.html',
    styleUrls: ['./dot-analytics-dashboard-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsDashboardTableComponent {
    /** Array of data objects to display in table rows */
    readonly $data = input.required<unknown[]>({ alias: 'data' });

    /** Component status for loading/error states */
    readonly $status = input.required<ComponentStatus>({ alias: 'status' });

    /** Fixed table title */
    protected readonly title = 'analytics.table.title';

    /** Static column configuration for top pages table */
    protected readonly columns: TableColumn[] = [...TOP_PAGES_TABLE_COLUMNS];

    /** Table configuration constants */
    protected readonly tableConfig = {
        ...TABLE_CONFIG,
        ROWS_PER_PAGE_OPTIONS: [...TABLE_CONFIG.ROWS_PER_PAGE_OPTIONS]
    };

    /** Check if component is in loading state */
    protected readonly $isLoading = computed(() => {
        const status = this.$status();

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    /** Skeleton rows for loading state */
    protected readonly skeletonRows = Array.from({ length: 3 }, (_, i) => i);
}
