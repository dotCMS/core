import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { ContentConversionRow } from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/**
 * Content Conversions Table Component
 *
 * Displays a table of content present in conversions with type, identifier, title, count, conversions, and conversion rate.
 */
@Component({
    selector: 'dot-analytics-content-conversions-table',
    imports: [
        CommonModule,
        CardModule,
        TableModule,
        TagModule,
        DotAnalyticsStateMessageComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-content-conversions-table.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export default class DotAnalyticsContentConversionsTableComponent {
    readonly $data = input.required<ContentConversionRow[]>({ alias: 'data' });
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
