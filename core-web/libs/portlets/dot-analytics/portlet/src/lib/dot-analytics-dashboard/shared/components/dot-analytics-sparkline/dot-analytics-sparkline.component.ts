import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    input,
    NgZone,
    signal
} from '@angular/core';

import { ChartModule, UIChart } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';

import { ComponentStatus } from '@dotcms/dotcms-models';
import type { SparklineDataPoint } from '@dotcms/portlets/dot-analytics/data-access';

import {
    createAnimationState,
    createGradientFillPlugin,
    createInteractionConfig,
    createLineDrawAnimationPlugin,
    createSparklineCrosshairPlugin,
    SPARKLINE_ANIMATION_DURATION
} from '../../plugins';
import { AnalyticsChartColors, ChartData } from '../../types';
import { hexToRgba } from '../../utils/dot-analytics.utils';

/** Context object passed to the Chart.js external tooltip callback */
interface SparklineTooltipContext {
    chart: { width: number };
    tooltip: {
        opacity: number;
        caretX: number;
        caretY: number;
        title?: string[];
        dataPoints?: { datasetIndex: number; parsed: { y: number } }[];
    };
}

/** Single series for sparkline (e.g. current or previous period) */
export interface SparklineDataset {
    /** Display label for tooltip/legend */
    label?: string;
    /** Data points (date + value) */
    data: SparklineDataPoint[];
    /** Line color (hex or rgb) */
    color?: string;
    /** Whether to draw line as dashed */
    dashed?: boolean;
    /** Line width in pixels (default 2) */
    borderWidth?: number;
    /** Gradient fill opacity multiplier (0-1, default 1). Lower values produce a more subtle gradient. */
    fillOpacity?: number;
}

/**
 * Sparkline component for displaying mini trend charts within metric cards.
 * Supports multiple series (e.g. current vs previous period) with optional fill and dashed lines.
 *
 * @example
 * ```html
 * <dot-analytics-sparkline
 *   [datasets]="[
 *     { data: currentPoints, label: 'This period', color: '#1243e3' },
 *     { data: previousPoints, label: 'Previous period', color: '#E5E7EB', dashed: true }
 *   ]"
 *   valueSuffix="%" />
 * ```
 */
@Component({
    selector: 'dot-analytics-sparkline',
    imports: [ChartModule, UIChart, SkeletonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        @if ($isLoading()) {
            <div class="sparkline-skeleton">
                <p-skeleton width="100%" height="100%" />
            </div>
        } @else if ($isEmpty()) {
            <div class="sparkline-empty">
                <div class="sparkline-empty__line"></div>
            </div>
        } @else {
            <div class="sparkline-container">
                <p-chart
                    type="line"
                    [data]="$chartData()"
                    [options]="$chartOptions()"
                    height="100%"
                    [plugins]="chartPlugins" />
                @if ($hoverInfo(); as info) {
                    <div
                        class="sparkline-tooltip"
                        [class.sparkline-tooltip--left]="info.alignLeft"
                        [style.left.px]="info.left"
                        [style.top.px]="info.top">
                        <div class="sparkline-tooltip__date">{{ info.date }}</div>
                        @for (item of info.items; track item.label) {
                            <div class="sparkline-tooltip__row">
                                <span
                                    class="sparkline-tooltip__dot"
                                    [style.background]="item.color"></span>
                                <span>{{ item.label }}: {{ item.value }}</span>
                            </div>
                        }
                    </div>
                }
            </div>
        }
    `,
    styles: `
        :host {
            display: block;
            width: 100%;
            height: var(--sparkline-height, 6rem);
        }

        .sparkline-skeleton {
            width: 100%;
            height: 100%;

            ::ng-deep .p-skeleton {
                border-radius: 0.5rem;
            }
        }

        .sparkline-empty {
            width: 100%;
            height: 100%;
            display: flex;
            align-items: center;
        }

        .sparkline-empty__line {
            width: 100%;
            border-top: 2px dashed var(--p-gray-300, #d1d5db);
        }

        .sparkline-container {
            position: relative;
            width: 100%;
            height: 100%;
            overflow: visible;
        }

        .sparkline-tooltip {
            position: absolute;
            z-index: 10;
            transform: translateY(-50%);
            background: rgba(30, 30, 30, 0.92);
            color: white;
            border-radius: 6px;
            padding: 5px 8px;
            font-size: 0.625rem;
            line-height: 1.4;
            white-space: nowrap;
            pointer-events: none;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
        }

        .sparkline-tooltip--left {
            transform: translate(-100%, -50%);
        }

        .sparkline-tooltip__row {
            display: flex;
            align-items: center;
            gap: 4px;
        }

        .sparkline-tooltip__date {
            opacity: 0.7;
            margin-bottom: 2px;
        }

        .sparkline-tooltip__dot {
            display: inline-block;
            width: 6px;
            height: 6px;
            border-radius: 50%;
            flex-shrink: 0;
        }
    `
})
export class DotAnalyticsSparklineComponent {
    readonly #ngZone = inject(NgZone);
    readonly #el = inject(ElementRef);

    /** Series to display (each with data, optional label, color, dashed). Labels from first series used for X axis. */
    readonly $datasets = input.required<SparklineDataset[]>({ alias: 'datasets' });

    /** Label for the value in tooltip (e.g., "Rate", "Sessions") */
    readonly $valueLabel = input<string>('Value', { alias: 'valueLabel' });

    /** Unit suffix for the value (e.g., "%", "ms") */
    readonly $valueSuffix = input<string>('', { alias: 'valueSuffix' });

    /** Line color for first series when not set per dataset (defaults to primary indigo) */
    readonly $color = input<string>(AnalyticsChartColors.primary.line, { alias: 'color' });

    /** Height of the sparkline (sets CSS --sparkline-height variable on the host) */
    readonly $height = input<string>('6rem', { alias: 'height' });

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

    /** Hover info populated by Chart.js external tooltip callback */
    protected readonly $hoverInfo = signal<{
        date: string;
        items: { label: string; value: string; color: string }[];
        left: number;
        top: number;
        alignLeft: boolean;
    } | null>(null);

    /** Whether the component is in loading state */
    protected readonly $isLoading = computed(() => {
        const status = this.$status();
        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    /** Whether there is no data to display */
    protected readonly $isEmpty = computed(() => {
        if (this.$isLoading()) return false;
        const datasets = this.$datasets();
        if (!datasets?.length) return true;
        return datasets.every((ds) => !ds.data?.length);
    });

    constructor() {
        effect(() => {
            this.#el.nativeElement.style.setProperty('--sparkline-height', this.$height());
        });
    }

    /** Animation state for line drawing effect */
    #animationState = createAnimationState();

    /**
     * Plugins for gradient fill and line drawing animation.
     * Animation runs outside Angular's zone to avoid triggering change detection.
     */
    readonly chartPlugins = [
        // Gradient fill plugin (applies to all filled datasets using each dataset's borderColor)
        createGradientFillPlugin(
            () => ({
                enabled: this.$filled() && this.$datasets().length > 0,
                color: this.$color()
            }),
            hexToRgba
        ),
        // Vertical crosshair when hovering (tooltip active)
        createSparklineCrosshairPlugin(),
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
        const datasets = this.$datasets();
        const filled = this.$filled();
        if (!datasets?.length) {
            return { labels: [], datasets: [] };
        }
        const first = datasets[0];
        const labels = first.data?.map((p) => p.date) ?? [];

        const chartDatasets = datasets.map((ds) => {
            const color = ds.color ?? this.$color();
            const values = ds.data?.map((p) => p.value) ?? [];
            return {
                data: values,
                borderColor: color,
                borderWidth: ds.borderWidth ?? 2,
                borderDash: ds.dashed ? [4, 2] : undefined,
                backgroundColor: hexToRgba(color, 0.15),
                fill: filled,
                clip: false as const,
                _fillOpacity: ds.fillOpacity ?? 1
            };
        });

        return { labels, datasets: chartDatasets };
    });

    /** Chart options optimized for sparkline display */
    protected readonly $chartOptions = computed(() => {
        const interactive = this.$interactive();
        const datasets = this.$datasets();
        const valueSuffix = this.$valueSuffix();

        const pointSpace = interactive ? 4 : 0;

        return {
            responsive: true,
            maintainAspectRatio: false,
            layout: {
                padding: {
                    top: pointSpace,
                    bottom: pointSpace,
                    left: pointSpace,
                    right: pointSpace
                }
            },
            ...createInteractionConfig(),
            plugins: {
                legend: { display: false },
                tooltip: {
                    enabled: false,
                    external: ({ chart, tooltip }: SparklineTooltipContext) => {
                        if (!tooltip.opacity || !tooltip.dataPoints?.length) {
                            this.#ngZone.run(() => this.$hoverInfo.set(null));
                            return;
                        }

                        const gap = 14;
                        const alignLeft = tooltip.caretX > chart.width / 2;
                        const left = alignLeft ? tooltip.caretX - gap : tooltip.caretX + gap;

                        const items = tooltip.dataPoints.map((dp) => ({
                            label: datasets[dp.datasetIndex]?.label ?? this.$valueLabel(),
                            value: `${dp.parsed.y}${valueSuffix}`,
                            color: datasets[dp.datasetIndex]?.color ?? this.$color()
                        }));

                        this.#ngZone.run(() =>
                            this.$hoverInfo.set({
                                date: tooltip.title?.[0] ?? '',
                                items,
                                left,
                                top: tooltip.caretY,
                                alignLeft
                            })
                        );
                    }
                }
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
                    hoverRadius: 0
                }
            },
            interaction: {
                intersect: false,
                mode: 'index'
            }
        };
    });
}
