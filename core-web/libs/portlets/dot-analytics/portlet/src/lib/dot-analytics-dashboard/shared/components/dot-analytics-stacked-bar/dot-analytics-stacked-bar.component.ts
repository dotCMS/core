import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { DotAnalyticsCountPipe } from '../../pipes/dot-analytics-count/dot-analytics-count.pipe';

@Component({
    selector: 'dot-analytics-stacked-bar',
    imports: [DotAnalyticsCountPipe],
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
}
