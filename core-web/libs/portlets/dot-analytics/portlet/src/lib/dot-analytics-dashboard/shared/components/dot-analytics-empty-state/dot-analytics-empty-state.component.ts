import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

/**
 * Standardized empty state for all analytics components.
 * Centers icon + message vertically and horizontally, filling the parent container.
 */
@Component({
    selector: 'dot-analytics-empty-state',
    imports: [DotMessagePipe],
    templateUrl: './dot-analytics-empty-state.component.html',
    styleUrl: './dot-analytics-empty-state.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsEmptyStateComponent {
    readonly $message = input('analytics.charts.empty.description', { alias: 'message' });
    readonly $icon = input('pi-info-circle', { alias: 'icon' });
}
