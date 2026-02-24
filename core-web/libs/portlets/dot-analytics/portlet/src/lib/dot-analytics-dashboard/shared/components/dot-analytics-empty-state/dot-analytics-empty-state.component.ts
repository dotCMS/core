import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

/**
 * Standardized empty state for all analytics components.
 * Centers icon + message vertically and horizontally, filling the parent container.
 */
@Component({
    selector: 'dot-analytics-empty-state',
    imports: [DotMessagePipe],
    template: `
        <div class="flex flex-col flex-1 items-center justify-center h-full gap-3 text-center">
            <i class="pi {{ icon() }} text-4xl text-gray-400"></i>
            <p class="text-sm font-medium text-gray-500 m-0">{{ message() | dm }}</p>
        </div>
    `,
    styles: `
        :host {
            display: flex;
            flex: 1;
            height: 100%;
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsEmptyStateComponent {
    readonly message = input('analytics.charts.empty.description');
    readonly icon = input('pi-info-circle');
}
