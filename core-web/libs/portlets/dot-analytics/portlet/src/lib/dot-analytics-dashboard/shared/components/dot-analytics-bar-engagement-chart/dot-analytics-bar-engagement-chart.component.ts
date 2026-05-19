import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsEngagementDetailTableDialogComponent } from '../../dialogs/engagement-detail-table-dialog/dot-analytics-engagement-detail-table-dialog.component';
import { buildEngagementDetailTableRows } from '../../dialogs/engagement-detail-table-dialog/dot-analytics-engagement-detail-table-dialog.models';
import { DotAnalyticsEmptyStateComponent } from '../dot-analytics-empty-state/dot-analytics-empty-state.component';
import { DotAnalyticsStackedBarComponent } from '../dot-analytics-stacked-bar/dot-analytics-stacked-bar.component';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/** View model for one stacked bar row. */
export interface DotAnalyticsBarEngagementRowVm {
    name: string;
    engagedSessions: number;
    notEngagedSessions: number;
    totalSessions: number;
}

@Component({
    selector: 'dot-analytics-bar-engagement-chart',
    imports: [
        ButtonModule,
        CardModule,
        DynamicDialogModule,
        SkeletonModule,
        DotMessagePipe,
        DotAnalyticsEmptyStateComponent,
        DotAnalyticsStackedBarComponent,
        DotAnalyticsStateMessageComponent
    ],
    templateUrl: './dot-analytics-bar-engagement-chart.component.html',
    styleUrl: './dot-analytics-bar-engagement-chart.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DialogService],
    host: { class: 'flex flex-col flex-1 w-full max-w-full min-w-0 min-h-0' }
})
export class DotAnalyticsBarEngagementChartComponent {
    readonly #dialogService = inject(DialogService);
    readonly #messageService = inject(DotMessageService);

    readonly $data = input.required<EngagementPlatformMetrics[]>({ alias: 'data' });
    readonly $status = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'status' });
    readonly $title = input<string>('', { alias: 'title' });
    readonly $maxItems = input<number>(5, { alias: 'maxItems' });
    readonly $detailsEnabled = input(false, { alias: 'detailsEnabled' });
    /** Message key for the first column heading in the details modal table. */
    readonly $detailsDimensionHeaderKey = input('', { alias: 'detailsDimensionHeaderKey' });

    protected readonly $resolvedCardHeader = computed(() => {
        const title = this.$title();
        if (!title?.trim()) {
            return undefined;
        }

        return this.#messageService.get(title);
    });

    protected readonly $displayRows = computed<DotAnalyticsBarEngagementRowVm[]>(() => {
        const data = this.$data();
        const maxItems = this.$maxItems();
        const sorted = [...data]
            .filter((d) => Number.isFinite(d.totalSessions) && d.totalSessions > 0)
            .sort((a, b) => b.totalSessions - a.totalSessions)
            .slice(0, maxItems);

        return sorted.map((d) => {
            const total = d.totalSessions;
            const engaged = Math.min(Math.max(0, d.views), total);
            const notEngaged = Math.max(0, total - engaged);

            return {
                name: d.name,
                engagedSessions: engaged,
                notEngagedSessions: notEngaged,
                totalSessions: total
            };
        });
    });

    protected readonly $isLoading = computed(() => {
        const status = this.$status();
        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    protected readonly $isError = computed(() => this.$status() === ComponentStatus.ERROR);

    protected readonly $isEmpty = computed(() => this.$displayRows().length === 0);

    /** Footer link visibility: enabled, dimension key present, and chart rows exist. */
    protected readonly $showDetailsFooter = computed(
        () =>
            this.$detailsEnabled() &&
            !!this.$detailsDimensionHeaderKey().trim() &&
            this.$displayRows().length > 0
    );

    /** Grouped digits for totals column (e.g. 6454 → 6,454 in en-US). */
    protected formatSessionsCount(total: number): string {
        if (!Number.isFinite(total)) return '0';
        return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(
            Math.round(total)
        );
    }

    /** Accessible label for a data row (name + total sessions phrase). */
    protected rowAriaLabel(row: DotAnalyticsBarEngagementRowVm): string {
        const sessionsPart = this.sessionsTotalsTitle(row.totalSessions);
        return sessionsPart ? `${row.name}, ${sessionsPart}` : row.name;
    }

    /** Accessible description for the totals cell (full words, shown as native tooltip too). */
    protected sessionsTotalsTitle(total: number): string {
        if (!Number.isFinite(total) || total <= 0) return '';
        const formatted = this.formatSessionsCount(total);
        if (Math.round(total) === 1) {
            return this.#messageService.get('analytics.engagement.charts.one-session-tooltip');
        }
        return this.#messageService.get(
            'analytics.engagement.charts.multi-sessions-tooltip',
            formatted
        );
    }

    protected openEngagementDetailDialog(): void {
        const firstColumnHeaderKey = this.$detailsDimensionHeaderKey().trim();
        if (!firstColumnHeaderKey) {
            return;
        }

        this.#dialogService.open(DotAnalyticsEngagementDetailTableDialogComponent, {
            header: this.$resolvedCardHeader() ?? '',
            width: 'min(92vw, 42rem)',
            closable: true,
            closeOnEscape: true,
            data: {
                rows: buildEngagementDetailTableRows(this.$data()),
                firstColumnHeaderKey: firstColumnHeaderKey
            }
        });
    }
}
