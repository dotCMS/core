import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    RequestState,
    TopPerformanceTableEntity,
    transformTopPagesTableData
} from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { TABLE_CONFIG, TOP_PAGES_TABLE_COLUMNS } from '../../constants';
import { TableColumn } from '../../types';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/**
 * Skeleton width mapping for different column types
 */
const SKELETON_WIDTH_MAP = {
    number: '60%',
    link: '70%',
    text: '85%'
} as const;

/**
 * Top pages analytics table component.
 * Displays top performing pages with pageviews, sorting and pagination.
 */
@Component({
    selector: 'dot-analytics-dashboard-table',
    imports: [
        CommonModule,
        CardModule,
        SkeletonModule,
        TableModule,
        DotMessagePipe,
        DotAnalyticsStateMessageComponent
    ],
    templateUrl: './dot-analytics-dashboard-table.component.html',
    styleUrls: ['./dot-analytics-dashboard-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsDashboardTableComponent {
    /** Complete table state from analytics store */
    readonly $tableState = input.required<RequestState<TopPerformanceTableEntity[]>>({
        alias: 'tableState'
    });

    /** Transformed table data ready for display */
    protected readonly $data = computed(() => transformTopPagesTableData(this.$tableState().data));

    /** Static column configuration for top pages table */
    protected readonly columns: TableColumn[] = [...TOP_PAGES_TABLE_COLUMNS];

    /** Table configuration constants */
    protected readonly tableConfig = TABLE_CONFIG;

    /** Check if component is in loading state */
    protected readonly $isLoading = computed(() => {
        const status = this.$tableState().status;

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    /** Check if component is in error state */
    protected readonly $isError = computed(
        () => this.$tableState().status === ComponentStatus.ERROR
    );

    /** Check if table data is empty */
    protected readonly $isEmpty = computed(() => {
        const data = this.$data();

        return !data || data.length === 0;
    });

    /** Skeleton rows for loading state */
    protected readonly skeletonRows = Array.from({ length: 3 }, (_, i) => i);

    /** Pre-computed column configurations with CSS classes and skeleton widths */
    protected readonly $columnConfigs = computed(() => {
        return this.columns.map((column) => ({
            ...column,
            cssClass: `text-${column.alignment || 'left'}`,
            skeletonWidth:
                SKELETON_WIDTH_MAP[column.type as keyof typeof SKELETON_WIDTH_MAP] ||
                SKELETON_WIDTH_MAP.text
        }));
    });

    /**
     * Track function for skeleton rows to improve performance
     */
    protected trackByIndex = (index: number): number => index;

    /**
     * Track function for table columns
     */
    protected trackByField = (index: number, column: TableColumn): string => column.field;
}
