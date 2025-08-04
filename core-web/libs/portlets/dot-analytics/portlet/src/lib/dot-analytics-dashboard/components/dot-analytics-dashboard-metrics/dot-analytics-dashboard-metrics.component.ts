import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe, fadeInContent } from '@dotcms/ui';

import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/**
 * Metric card component for displaying key analytics metrics.
 * Shows a metric name, value, optional subtitle, and icon in a compact card format.
 *
 */
@Component({
    selector: 'dot-analytics-dashboard-metrics',
    imports: [
        CommonModule,
        CardModule,
        SkeletonModule,
        DotMessagePipe,
        DotAnalyticsStateMessageComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-analytics-dashboard-metrics.component.html',
    styleUrl: './dot-analytics-dashboard-metrics.component.scss',
    animations: [fadeInContent]
})
export class DotAnalyticsDashboardMetricsComponent {
    // Inputs
    /** Metric display name (shown in uppercase) */
    readonly $name = input.required<string>({ alias: 'name' });

    /** Metric value (number will be formatted with separators) */
    readonly $value = input.required<number>({ alias: 'value' });

    /** Optional secondary text below the metric value */
    readonly $subtitle = input<string>('', { alias: 'subtitle' });

    /** PrimeIcons icon name (without 'pi-' prefix) displayed in top-right */
    readonly $icon = input<string>('', { alias: 'icon' });

    /** Component status for loading/error states */
    readonly $status = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'status' });

    // Computed properties
    /** Formats numeric values with locale-specific separators */
    protected readonly $formattedValue = computed(() => {
        const val = this.$value();
        if (typeof val === 'number') {
            // Formatear números grandes con separadores de miles
            return val.toLocaleString();
        }

        return val;
    });

    /** Computed signal for complete icon classes */
    protected readonly $iconClasses = computed(() => {
        const iconName = this.$icon();

        return iconName ? `pi ${iconName}` : '';
    });

    /** Check if component is in loading state */
    protected readonly $isLoading = computed(() => {
        const status = this.$status();

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    /** Check if component is in error state */
    protected readonly $isError = computed(() => this.$status() === ComponentStatus.ERROR);

    /** Check if metric data is empty or insufficient - 0 is a VALID metric value */
    protected readonly $isEmpty = computed(() => {
        const value = this.$value();
        const status = this.$status();

        // Don't show empty state if we're loading or have an error
        if (
            status === ComponentStatus.LOADING ||
            status === ComponentStatus.INIT ||
            status === ComponentStatus.ERROR
        ) {
            return false;
        }

        // Show empty state only when we have NO data (null/undefined)
        // 0 is a valid metric value and should NOT be considered empty
        return value === null || value === undefined;
    });
}
