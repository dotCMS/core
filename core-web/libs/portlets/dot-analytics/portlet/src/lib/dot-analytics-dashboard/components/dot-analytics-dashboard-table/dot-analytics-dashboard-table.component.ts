import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';

import { DotMessagePipe } from '@dotcms/ui';

import { TableColumn } from '../../types';

/**
 * Reusable data table component for analytics dashboard.
 * Displays tabular data with configurable columns and data types.
 *
 */
@Component({
    selector: 'dot-analytics-dashboard-table',
    standalone: true,
    imports: [CommonModule, CardModule, TableModule, DotMessagePipe],
    templateUrl: './dot-analytics-dashboard-table.component.html',
    styleUrls: ['./dot-analytics-dashboard-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsDashboardTableComponent {
    /** Table title displayed in card header */
    readonly $title = input.required<string>({ alias: 'title' });

    /** Array of data objects to display in table rows */
    readonly $data = input.required<unknown[]>({ alias: 'data' });

    /** Column configuration defining fields, headers, and formatting */
    readonly $columns = input.required<TableColumn[]>({ alias: 'columns' });

    /** Auto-generated test ID based on table title */
    readonly $testId = computed(
        () => 'analytics-table-' + this.$title().toLowerCase().replace(/\s+/g, '-')
    );
}
