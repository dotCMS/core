import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { ConversionsOverviewEntity } from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsEmptyStateComponent } from '../../../shared/components/dot-analytics-empty-state/dot-analytics-empty-state.component';
import { DotAnalyticsStateMessageComponent } from '../../../shared/components/dot-analytics-state-message/dot-analytics-state-message.component';

/**
 * Conversions Overview Table Component
 *
 * Displays a table of conversions overview with conversion name, total, conversion rate, and top attributed content.
 */
@Component({
    selector: 'dot-analytics-conversions-overview-table',
    imports: [
        CommonModule,
        SkeletonModule,
        TableModule,
        TagModule,
        DotAnalyticsEmptyStateComponent,
        DotAnalyticsStateMessageComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-conversions-overview-table.component.html',
    styleUrl: './dot-analytics-conversions-overview-table.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export default class DotAnalyticsConversionsOverviewTableComponent {
    /** Conversions overview rows to display in the table */
    readonly $data = input.required<ConversionsOverviewEntity[]>({ alias: 'data' });
    /** Component status controlling loading, error, and empty states */
    readonly $status = input.required<ComponentStatus>({ alias: 'status' });

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
