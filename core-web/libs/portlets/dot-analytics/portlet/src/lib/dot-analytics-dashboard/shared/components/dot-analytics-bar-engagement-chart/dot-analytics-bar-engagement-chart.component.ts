import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsEmptyStateComponent } from '../dot-analytics-empty-state/dot-analytics-empty-state.component';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/** View model for one stacked bar row (segments are shares of that row's totalSessions — full pill width each row). */
export interface DotAnalyticsBarEngagementRowVm {
    name: string;
    engagedSessions: number;
    notEngagedSessions: number;
    totalSessions: number;
    engagedBarWidthPct: number;
    notEngagedBarWidthPct: number;
    engagementPercent: number;
}

@Component({
    selector: 'dot-analytics-bar-engagement-chart',
    imports: [
        CardModule,
        SkeletonModule,
        DotMessagePipe,
        DotAnalyticsEmptyStateComponent,
        DotAnalyticsStateMessageComponent
    ],
    templateUrl: './dot-analytics-bar-engagement-chart.component.html',
    styleUrl: './dot-analytics-bar-engagement-chart.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsBarEngagementChartComponent {
    readonly #messageService = inject(DotMessageService);

    readonly $data = input.required<EngagementPlatformMetrics[]>({ alias: 'data' });
    readonly $status = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'status' });
    readonly $title = input<string>('', { alias: 'title' });
    readonly $maxItems = input<number>(5, { alias: 'maxItems' });

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
            const engagedBarWidthPct = total > 0 ? (engaged / total) * 100 : 0;
            const notEngagedBarWidthPct = total > 0 ? (notEngaged / total) * 100 : 0;

            return {
                name: d.name,
                engagedSessions: engaged,
                notEngagedSessions: notEngaged,
                totalSessions: total,
                engagedBarWidthPct,
                notEngagedBarWidthPct,
                engagementPercent: Number.isFinite(d.percentage) ? d.percentage : 0
            };
        });
    });

    protected readonly $isLoading = computed(() => {
        const status = this.$status();
        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    protected readonly $isError = computed(() => this.$status() === ComponentStatus.ERROR);

    protected readonly $isEmpty = computed(() => this.$displayRows().length === 0);

    /** Grouped digits for totals column (e.g. 6454 → 6,454 in en-US). */
    protected formatSessionsCount(total: number): string {
        if (!Number.isFinite(total)) return '0';
        return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(
            Math.round(total)
        );
    }

    /**
     * Shorter numeric label inside thin bar segments; uses compact notation when in the thousands+.
     */
    protected formatSegmentSessions(value: number): string {
        if (!Number.isFinite(value) || value <= 0) return '';
        if (value < 1000) return String(Math.round(value));
        try {
            return new Intl.NumberFormat(undefined, {
                notation: 'compact',
                compactDisplay: 'short',
                maximumFractionDigits: 1
            }).format(Math.round(value));
        } catch {
            const rounded = Math.round(value);
            if (rounded >= 1_000_000) return `${(rounded / 1_000_000).toFixed(1)}M`;
            if (rounded >= 1_000) return `${(rounded / 1_000).toFixed(1)}K`;
            return String(rounded);
        }
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
}
