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
import { DotAnalyticsCountPipe } from '../../pipes/dot-analytics-count/dot-analytics-count.pipe';
import { formatAnalyticsCount } from '../../utils/format-analytics-count.util';
import { DotAnalyticsEmptyStateComponent } from '../dot-analytics-empty-state/dot-analytics-empty-state.component';
import { DotAnalyticsStackedBarComponent } from '../dot-analytics-stacked-bar/dot-analytics-stacked-bar.component';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/** View model for one stacked bar row (tooltip and aria-label precomputed). */
export interface DotAnalyticsBarEngagementRowVm {
    name: string;
    engagedSessions: number;
    notEngagedSessions: number;
    totalSessions: number;
    ariaLabel: string;
    sessionsTotalsTitle: string;
}

function buildSessionsTotalsTitle(messageService: DotMessageService, total: number): string {
    if (!Number.isFinite(total) || total <= 0) {
        return '';
    }

    const formatted = formatAnalyticsCount(total, 'full');
    if (Math.round(total) === 1) {
        return messageService.get('analytics.engagement.charts.one-session-tooltip');
    }

    return messageService.get('analytics.engagement.charts.multi-sessions-tooltip', formatted);
}

function buildRowAriaLabel(name: string, sessionsPart: string): string {
    return sessionsPart ? `${name}, ${sessionsPart}` : name;
}

@Component({
    selector: 'dot-analytics-bar-engagement-chart',
    imports: [
        ButtonModule,
        CardModule,
        DynamicDialogModule,
        SkeletonModule,
        DotMessagePipe,
        DotAnalyticsCountPipe,
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
            const sessionsTotalsTitle = buildSessionsTotalsTitle(this.#messageService, total);
            const ariaLabel = buildRowAriaLabel(d.name, sessionsTotalsTitle);

            return {
                name: d.name,
                engagedSessions: engaged,
                notEngagedSessions: notEngaged,
                totalSessions: total,
                sessionsTotalsTitle,
                ariaLabel
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
