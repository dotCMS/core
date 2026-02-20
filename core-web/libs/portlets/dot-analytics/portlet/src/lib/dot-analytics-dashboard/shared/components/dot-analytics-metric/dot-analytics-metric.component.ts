import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe, fadeInContent } from '@dotcms/ui';

import { DotCountUpDirective } from '../../directives';
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
        DotCountUpDirective,
        DotAnalyticsStateMessageComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-analytics-dashboard-metrics.component.html',
    animations: [fadeInContent],
    host: { class: 'block w-full' }
})
export class DotAnalyticsDashboardMetricsComponent {
    // Inputs
    /** Metric display name (shown in uppercase) */
    readonly $name = input.required<string>({ alias: 'name' });

    /** Metric value (number will be formatted with separators, or string for special formats like "2/3") */
    readonly $value = input.required<number | string>({ alias: 'value' });

    /** Optional secondary text below the metric value */
    readonly $subtitle = input<string>('', { alias: 'subtitle' });

    /** PrimeIcons icon name (without 'pi-' prefix) displayed in top-right */
    readonly $icon = input<string>('', { alias: 'icon' });

    /** Trend value (percentage) for the metric */
    readonly $trend = input<number | undefined>(undefined, { alias: 'trend' });

    /** Component status for loading/error states */
    readonly $status = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'status' });

    /** Whether to animate numeric values with count-up effect */
    readonly $animated = input<boolean>(true, { alias: 'animated' });

    /** Duration of count-up animation in milliseconds */
    readonly $animationDuration = input<number>(350, { alias: 'animationDuration' });

    // Computed properties
    /** Computed signal for trend display data */
    protected readonly $trendData = computed(() => {
        const trend = this.$trend();
        if (trend === undefined) {
            return null;
        }

        const isPositive = trend >= 0;
        return {
            value: trend,
            isPositive,
            prefix: isPositive ? '+' : '',
            class: isPositive
                ? 'inline-flex items-center gap-1 text-base text-green-600'
                : 'inline-flex items-center gap-1 text-base text-red-600'
        };
    });

    /** Check if value is a fraction format (e.g., "2/3") */
    protected readonly $isFraction = computed(() => {
        const val = this.$value();

        return typeof val === 'string' && val.includes('/');
    });

    /** Extract and format numerator and denominator from fraction string */
    protected readonly $fractionParts = computed(() => {
        const val = this.$value();
        if (typeof val === 'string' && val.includes('/')) {
            const [num, den] = val.split('/').map((s) => s.trim());
            const numerator = parseInt(num, 10);
            const denominator = parseInt(den, 10);

            return {
                numerator: numerator.toLocaleString(),
                denominator: denominator.toLocaleString(),
                // Raw values for count-up animation
                numeratorValue: numerator,
                denominatorValue: denominator
            };
        }

        return null;
    });

    /** Formats numeric values with locale-specific separators */
    protected readonly $formattedValue = computed(() => {
        const val = this.$value();
        if (typeof val === 'number') {
            // Formatear números grandes con separadores de miles
            return val.toLocaleString();
        }

        return val;
    });

    /** Returns numeric value for count-up animation, or null if not a number */
    protected readonly $numericValue = computed(() => {
        const val = this.$value();

        if (typeof val === 'number') {
            return { value: val, suffix: '', format: 'number' as const };
        }

        if (typeof val === 'string') {
            // Check for time format "Xm Ys" or "Xm" or "Ys"
            const timeMatch = val.match(/^(?:(\d+)m)?\s*(?:(\d+)s)?$/);
            if (timeMatch && (timeMatch[1] || timeMatch[2])) {
                const minutes = parseInt(timeMatch[1] || '0', 10);
                const seconds = parseInt(timeMatch[2] || '0', 10);
                const totalSeconds = minutes * 60 + seconds;

                return { value: totalSeconds, suffix: '', format: 'time' as const };
            }

            // Check for numeric strings with suffix (e.g., "45%", "5.2%")
            const numMatch = val.match(/^([\d.]+)(.*)$/);
            if (numMatch) {
                const num = parseFloat(numMatch[1]);
                if (!isNaN(num)) {
                    return { value: num, suffix: numMatch[2], format: 'number' as const };
                }
            }
        }

        return null;
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
