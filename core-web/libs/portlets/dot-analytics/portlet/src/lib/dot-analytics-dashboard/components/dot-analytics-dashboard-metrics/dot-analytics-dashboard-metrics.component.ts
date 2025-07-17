import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardModule } from 'primeng/card';

import { DotMessagePipe } from '@dotcms/ui';

/**
 * Metric card component for displaying key analytics metrics.
 * Shows a metric name, value, optional subtitle, and icon in a compact card format.
 *
 */
@Component({
    selector: 'dot-analytics-dashboard-metrics',
    standalone: true,
    imports: [CommonModule, CardModule, DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-analytics-dashboard-metrics.component.html',
    styleUrl: './dot-analytics-dashboard-metrics.component.scss'
})
export class DotAnalyticsDashboardMetricsComponent {
    // Inputs
    /** Metric display name (shown in uppercase) */
    readonly $name = input.required<string>({ alias: 'name' });

    /** Metric value (number will be formatted with separators) */
    readonly $value = input.required<string | number>({ alias: 'value' });

    /** Optional secondary text below the metric value */
    readonly $subtitle = input<string>('', { alias: 'subtitle' });

    /** PrimeIcons icon name (without 'pi-' prefix) displayed in top-right */
    readonly $icon = input<string>('', { alias: 'icon' });

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

    /** Generates complete CSS classes for the metric icon */
    protected readonly $iconClasses = computed(() => {
        const iconName = this.$icon();

        return `pi ${iconName} text-xl icon-primary`;
    });
}
