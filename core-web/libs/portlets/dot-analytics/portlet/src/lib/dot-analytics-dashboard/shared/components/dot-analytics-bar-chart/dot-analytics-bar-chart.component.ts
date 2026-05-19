import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    viewChildren
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { SkeletonModule } from 'primeng/skeleton';
import { Tooltip, TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsPageviewDetailTableDialogComponent } from '../../dialogs/pageview-detail-table-dialog/dot-analytics-pageview-detail-table-dialog.component';
import { buildPageviewDetailTableRows } from '../../dialogs/pageview-detail-table-dialog/dot-analytics-pageview-detail-table-dialog.models';
import { formatAnalyticsCount } from '../../utils/format-analytics-count.util';
import { DotAnalyticsEmptyStateComponent } from '../dot-analytics-empty-state/dot-analytics-empty-state.component';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

@Component({
    selector: 'dot-analytics-bar-chart',
    imports: [
        ButtonModule,
        CardModule,
        DynamicDialogModule,
        SkeletonModule,
        TooltipModule,
        DotMessagePipe,
        DotAnalyticsEmptyStateComponent,
        DotAnalyticsStateMessageComponent
    ],
    templateUrl: './dot-analytics-bar-chart.component.html',
    styleUrl: './dot-analytics-bar-chart.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DialogService],
    host: { class: 'flex flex-col flex-1 w-full min-w-0 min-h-0' }
})
export class DotAnalyticsBarChartComponent {
    readonly #messageService = inject(DotMessageService);
    readonly #dialogService = inject(DialogService);
    protected readonly $barFillTooltips = viewChildren(Tooltip);

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

    /** PrimeNG tooltip text with absolute view count for a bar row. */
    protected viewsTooltip(item: EngagementPlatformMetrics): string {
        const views = item.views;
        if (!Number.isFinite(views) || views <= 0) {
            return '';
        }

        if (Math.round(views) === 1) {
            return this.#messageService.get('analytics.pageview.charts.one-view-tooltip');
        }

        return this.#messageService.get(
            'analytics.pageview.charts.multi-views-tooltip',
            formatAnalyticsCount(views, 'full')
        );
    }

    /** Accessible label for a data row (name + views phrase). */
    protected rowAriaLabel(item: EngagementPlatformMetrics): string {
        const viewsPart = this.viewsTooltip(item);
        return viewsPart ? `${item.name}, ${viewsPart}` : item.name;
    }

    /** Row hover shows tooltip anchored to the bar fill (center of the blue segment). */
    protected onRowMouseEnter(index: number, item: EngagementPlatformMetrics): void {
        if (!this.viewsTooltip(item)) {
            return;
        }

        this.$barFillTooltips()[index]?.activate();
    }

    protected onRowMouseLeave(index: number): void {
        this.$barFillTooltips()[index]?.deactivate();
    }

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
