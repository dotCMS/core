import { ChangeDetectionStrategy, Component, computed, inject, input, NgZone } from '@angular/core';

import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';

import { ComponentStatus } from '@dotcms/dotcms-models';

import {
    createAnimationState,
    createGradientFillPlugin,
    createInteractionConfig,
    createLineDrawAnimationPlugin,
    createTooltipConfig,
    SPARKLINE_ANIMATION_DURATION
} from '../../plugins';
import { AnalyticsChartColors, ChartData } from '../../types';
import { hexToRgba } from '../../utils/dot-analytics.utils';

/** Data point for sparkline with date and value */
export interface SparklineDataPoint {
    date: string;
    value: number;
}

/**
 * Sparkline component for displaying mini trend charts within metric cards.
 * A lightweight chart with gradient fill and optional interactions.
 *
 * @example
 * ```html
 * <dot-analytics-sparkline [data]="[{ date: 'Oct 1', value: 10 }, { date: 'Oct 2', value: 25 }]" />
 * ```
 */
@Component({
    selector: 'dot-analytics-sparkline',
    imports: [ChartModule, SkeletonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        @if ($isLoading()) {
            <div class="sparkline-skeleton" [style.height]="$height()">
                <p-skeleton width="100%" height="100%" />
            </div>
        } @else {
            <p-chart
                type="line"
                [data]="$chartData()"
                [options]="$chartOptions()"
                [height]="$height()"
                [plugins]="chartPlugins" />
        }
    `,
    styles: `
        :host {
            display: block;
            width: 100%;
        }

        .sparkline-skeleton {
            width: 100%;
            display: flex;
            align-items: center;

            ::ng-deep .p-skeleton {
                border-radius: 0.5rem;
            }
        }
    `
})
export class DotAnalyticsSparklineComponent {
    readonly #ngZone = inject(NgZone);

    /** Data points for the sparkline (array of date/value objects) */
    readonly $data = input.required<SparklineDataPoint[]>({ alias: 'data' });

    /** Label for the value in tooltip (e.g., "Rate", "Sessions") */
    readonly $valueLabel = input<string>('Value', { alias: 'valueLabel' });

    /** Unit suffix for the value (e.g., "%", "ms") */
    readonly $valueSuffix = input<string>('', { alias: 'valueSuffix' });

    /** Line color (defaults to primary indigo) */
    readonly $color = input<string>(AnalyticsChartColors.primary.line, { alias: 'color' });

    /** Height of the sparkline */
    readonly $height = input<string>('4.5rem', { alias: 'height' });

    /** Whether to show the filled area below the line */
    readonly $filled = input<boolean>(true, { alias: 'filled' });

    /** Whether to enable interactions (hover points and tooltips) */
    readonly $interactive = input<boolean>(true, { alias: 'interactive' });

    /** Whether to animate the line drawing on load */
    readonly $animated = input<boolean>(true, { alias: 'animated' });

    /** Animation duration in milliseconds */
    readonly $animationDuration = input<number>(SPARKLINE_ANIMATION_DURATION, {
        alias: 'animationDuration'
    });

    /** Component status for loading state */
    readonly $status = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'status' });

    /** Whether the component is in loading state */
    protected readonly $isLoading = computed(() => {
        const status = this.$status();
        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    /** Animation state for line drawing effect */
    #animationState = createAnimationState();

    /**
     * Plugins for gradient fill and line drawing animation.
     * Animation runs outside Angular's zone to avoid triggering change detection.
     */
    readonly chartPlugins = [
        // Gradient fill plugin (reusable)
        createGradientFillPlugin(
            () => ({
                enabled: this.$filled(),
                color: this.$color()
            }),
            hexToRgba
        ),
        // Line drawing animation plugin (reusable, runs outside zone)
        createLineDrawAnimationPlugin(
            () => ({
                enabled: this.$animated(),
                duration: this.$animationDuration()
            }),
            this.#animationState,
            this.#ngZone
        )
    ];

    /** Chart data formatted for Chart.js */
    protected readonly $chartData = computed<ChartData>(() => {
        const dataPoints = this.$data();
        const color = this.$color();
        const filled = this.$filled();

        return {
            labels: dataPoints.map((point) => point.date),
            datasets: [
                {
                    data: dataPoints.map((point) => point.value),
                    borderColor: color,
                    borderWidth: 2,
                    backgroundColor: hexToRgba(color, 0.15), // Fallback color
                    fill: filled
                }
            ]
        };
    });

    /** Chart options optimized for sparkline display */
    protected readonly $chartOptions = computed(() => {
        const interactive = this.$interactive();
        const valueLabel = this.$valueLabel();
        const valueSuffix = this.$valueSuffix();

        return {
            responsive: true,
            maintainAspectRatio: false,
            // Smooth interaction animations (centralized config)
            ...createInteractionConfig(),
            plugins: {
                legend: { display: false },
                tooltip: createTooltipConfig({
                    enabled: interactive,
                    labelCallback: (context) => `${valueLabel}: ${context.parsed.y}${valueSuffix}`
                })
            },
            scales: {
                x: { display: false },
                y: { display: false }
            },
            elements: {
                line: {
                    tension: 0.4
                },
                point: {
                    radius: 0,
                    hoverRadius: interactive ? 6 : 0,
                    hoverBackgroundColor: AnalyticsChartColors.primary.line,
                    hoverBorderColor: 'white',
                    hoverBorderWidth: 2
                }
            },
            interaction: {
                intersect: false,
                mode: 'index'
            }
        };
    });
}
