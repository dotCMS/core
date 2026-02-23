import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, linkedSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MultiSelectModule } from 'primeng/multiselect';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { ContentConversionRow } from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsStateMessageComponent } from '../../../shared/components/dot-analytics-state-message/dot-analytics-state-message.component';

/**
 * Content Conversions Table Component
 *
 * Displays a table of content present in conversions with type, identifier, title, count, conversions, and conversion rate.
 */
@Component({
    selector: 'dot-analytics-content-conversions-table',
    imports: [
        CommonModule,
        FormsModule,
        MultiSelectModule,
        TableModule,
        TagModule,
        DotAnalyticsStateMessageComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-content-conversions-table.component.html',
    styleUrl: './dot-analytics-content-conversions-table.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export default class DotAnalyticsContentConversionsTableComponent {
    /** Content conversion rows to display in the table */
    readonly $data = input.required<ContentConversionRow[]>({ alias: 'data' });
    /** Component status controlling loading, error, and empty states */
    readonly $status = input.required<ComponentStatus>({ alias: 'status' });

    /** Extract unique event types from data for filter options */
    protected readonly $eventTypeOptions = linkedSignal(() => {
        const data = this.$data();
        if (!data || data.length === 0) {
            return [];
        }

        const uniqueTypes = [...new Set(data.map((row) => row.eventType))];

        return uniqueTypes.map((type) => ({ label: type, value: type }));
    });

    /** Whether the component is in a loading or init state */
    protected readonly $isLoading = computed(() => {
        const status = this.$status();

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    /** Whether the component is in an error state */
    protected readonly $isError = computed(() => this.$status() === ComponentStatus.ERROR);

    /** Whether the data loaded successfully but returned no rows */
    protected readonly $isEmpty = computed(() => {
        const data = this.$data();
        const status = this.$status();

        return status === ComponentStatus.LOADED && (!data || data.length === 0);
    });
}
