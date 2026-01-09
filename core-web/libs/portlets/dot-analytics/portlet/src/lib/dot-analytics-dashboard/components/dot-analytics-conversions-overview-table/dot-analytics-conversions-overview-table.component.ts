import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { ConversionsOverviewEntity } from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/**
 * Conversions Overview Table Component
 *
 * Displays a table of conversions overview with conversion name, total, conversion rate, and top attributed content.
 */
@Component({
    selector: 'dot-analytics-conversions-overview-table',
    imports: [
        CommonModule,
        CardModule,
        TableModule,
        TagModule,
        DotAnalyticsStateMessageComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-conversions-overview-table.component.html',
    styleUrl: './dot-analytics-conversions-overview-table.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export default class DotAnalyticsConversionsOverviewTableComponent {
    readonly $data = input.required<ConversionsOverviewEntity[]>({ alias: 'data' });
    readonly $status = input.required<ComponentStatus>({ alias: 'status' });

    // Computed states based on status and data
    protected readonly $isLoading = computed(() => {
        const status = this.$status();

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    protected readonly $isError = computed(() => this.$status() === ComponentStatus.ERROR);

    protected readonly $isEmpty = computed(() => {
        const data = this.$data();
        const status = this.$status();

        return status === ComponentStatus.LOADED && (!data || data.length === 0);
    });
}
