import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { formatSecondsToTime, MetricFormat } from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCountUpDirective } from '../../directives';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/**
 * Metric card component for displaying key analytics metrics.
 * Shows a metric name, value, optional subtitle, and icon in a compact card format.
 */
@Component({
    selector: 'dot-analytics-metric',
    imports: [
        CardModule,
        SkeletonModule,
        DotMessagePipe,
        DotCountUpDirective,
        DotAnalyticsStateMessageComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-analytics-metric.component.html',
    styleUrl: './dot-analytics-metric.component.scss',
    host: {
        class: 'flex flex-col gap-2 w-full'
    }
})
export class DotAnalyticsMetricComponent {
    // Inputs
    /** Optional title displayed above the card */
    readonly $title = input<string>('', { alias: 'title' });

    /** Metric value. Prefer number + format; strings are supported for backward compat but won't animate. */
    readonly $value = input.required<number | string | null>({ alias: 'value' });

    /** Display format: 'number' (default), 'time' (seconds → Xm Ys), 'percentage' (appends %) */
    readonly $format = input<MetricFormat>('number', { alias: 'format' });

    /** Optional secondary text below the metric value */
    readonly $subtitle = input<string>('', { alias: 'subtitle' });

    /** PrimeIcons icon name (without 'pi-' prefix) displayed in top-right */
    readonly $icon = input<string>('', { alias: 'icon' });

    /** Trend value (percentage) for the metric */
    readonly $trend = input<number | undefined>(undefined, { alias: 'trend' });

    /** Comparison label displayed next to the trend (e.g., "from previous 7 days") */
    readonly $comparisonLabel = input<string>('', { alias: 'comparisonLabel' });

    /** Component status for loading/error states */
    readonly $status = input<keyof typeof ComponentStatus>(ComponentStatus.INIT, {
        alias: 'status'
    });

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

        const isPositive = trend > 0;
        const isNegative = trend < 0;
        return {
            value: trend,
            isPositive,
            isNegative,
            isNeutral: trend === 0,
            prefix: isPositive ? '+' : '',
            colorClasses: isPositive
                ? 'text-green-700 bg-green-50'
                : isNegative
                  ? 'text-red-600 bg-red-50'
                  : 'text-gray-500 bg-gray-100'
        };
    });

    /** Formats the value for static display based on format (number) or as-is (string) */
    protected readonly $formattedValue = computed(() => {
        const val = this.$value();
        if (val === null || val === undefined) {
            return null;
        }

        // String values display as-is (legacy path, e.g. "150/1000")
        if (typeof val === 'string') {
            return val;
        }

        const format = this.$format();
        switch (format) {
            case 'time':
                return formatSecondsToTime(val);
            case 'percentage':
                return `${val}%`;
            default:
                return val.toLocaleString();
        }
    });

    /** Returns value info for count-up animation (only for numeric values) */
    protected readonly $numericValue = computed(() => {
        const val = this.$value();
        if (val === null || val === undefined || typeof val !== 'number') {
            return null;
        }

        const format = this.$format();
        switch (format) {
            case 'time':
                return { value: val, suffix: '', format: 'time' as const };
            case 'percentage':
                return { value: val, suffix: '%', format: 'number' as const };
            default:
                return { value: val, suffix: '', format: 'number' as const };
        }
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

        // Show empty state only when we have NO data (null/undefined/empty string)
        // 0 is a valid metric value and should NOT be considered empty
        return value === null || value === undefined || value === '';
    });
}
