import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsPageviewDetailTableDialogComponent } from '../../dialogs/pageview-detail-table-dialog/dot-analytics-pageview-detail-table-dialog.component';
import { buildPageviewDetailTableRows } from '../../dialogs/pageview-detail-table-dialog/dot-analytics-pageview-detail-table-dialog.models';
import { DotAnalyticsEmptyStateComponent } from '../dot-analytics-empty-state/dot-analytics-empty-state.component';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

@Component({
    selector: 'dot-analytics-bar-chart',
    imports: [
        ButtonModule,
        CardModule,
        DynamicDialogModule,
        SkeletonModule,
        DotMessagePipe,
        DotAnalyticsEmptyStateComponent,
        DotAnalyticsStateMessageComponent
    ],
    templateUrl: './dot-analytics-bar-chart.component.html',
    styleUrl: './dot-analytics-bar-chart.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DialogService]
})
export class DotAnalyticsBarChartComponent {
    readonly #messageService = inject(DotMessageService);
    readonly #dialogService = inject(DialogService);

    readonly $data = input.required<EngagementPlatformMetrics[]>({ alias: 'data' });
    readonly $status = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'status' });
    readonly $title = input<string>('', { alias: 'title' });
    readonly $maxItems = input<number>(5, { alias: 'maxItems' });
    readonly $detailsEnabled = input(false, { alias: 'detailsEnabled' });
    /** Message key for the first column heading in the details modal table. */
    readonly $detailsDimensionHeaderKey = input('', { alias: 'detailsDimensionHeaderKey' });

    protected readonly $topItems = computed(() => {
        const data = this.$data();
        const max = this.$maxItems();

        return [...data].sort((a, b) => b.percentage - a.percentage).slice(0, max);
    });

    protected readonly $resolvedCardHeader = computed(() => {
        const title = this.$title();
        if (!title?.trim()) {
            return undefined;
        }

        return this.#messageService.get(title);
    });

    protected readonly $isLoading = computed(() => {
        const status = this.$status();

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    protected readonly $isError = computed(() => this.$status() === ComponentStatus.ERROR);

    protected readonly $isEmpty = computed(() => this.$topItems().length === 0);

    /** Footer link visibility: enabled, dimension key present, and chart rows exist. */
    protected readonly $showDetailsFooter = computed(
        () =>
            this.$detailsEnabled() &&
            !!this.$detailsDimensionHeaderKey().trim() &&
            this.$topItems().length > 0
    );

    protected openPageviewDetailDialog(): void {
        const firstColumnHeaderKey = this.$detailsDimensionHeaderKey().trim();
        if (!firstColumnHeaderKey) {
            return;
        }

        this.#dialogService.open(DotAnalyticsPageviewDetailTableDialogComponent, {
            header: this.$resolvedCardHeader() ?? '',
            width: 'min(92vw, 36rem)',
            closable: true,
            closeOnEscape: true,
            data: {
                rows: buildPageviewDetailTableRows(this.$data()),
                firstColumnHeaderKey
            }
        });
    }
}
