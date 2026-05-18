import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
    selector: 'dot-analytics-stacked-bar',
    templateUrl: './dot-analytics-stacked-bar.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block w-full min-w-0' }
})
export class DotAnalyticsStackedBarComponent {
    readonly $engagedSessions = input.required<number>({ alias: 'engagedSessions' });
    readonly $notEngagedSessions = input.required<number>({ alias: 'notEngagedSessions' });
    readonly $totalSessions = input.required<number>({ alias: 'totalSessions' });
    readonly $showLabels = input<boolean>(true, { alias: 'showLabels' });
    readonly $height = input<string>('1rem', { alias: 'height' });

    protected readonly $engagedWidthPct = computed(() => {
        const total = this.$totalSessions();
        return total > 0
            ? (Math.min(Math.max(0, this.$engagedSessions()), total) / total) * 100
            : 0;
    });

    protected readonly $notEngagedWidthPct = computed(() => {
        const total = this.$totalSessions();
        return total > 0 ? (Math.max(0, this.$notEngagedSessions()) / total) * 100 : 0;
    });

    protected formatSegment(value: number): string {
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
}
