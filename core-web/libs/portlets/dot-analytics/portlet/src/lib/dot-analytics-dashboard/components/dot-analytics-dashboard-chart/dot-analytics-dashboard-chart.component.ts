import { Chart, ChartDataset, ChartTypeRegistry, TooltipItem } from 'chart.js';

import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';

import { map } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import {
    CHART_ANIMATION,
    CHART_TRANSITIONS,
    createAnimationState,
    createLineDrawAnimationPlugin,
    LINE_DRAW_ANIMATION_DURATION,
    resetAnimationState,
    TOOLTIP_STYLE
} from '../../plugins';
import {
    ChartData,
    ChartOptions,
    ChartType,
    ComboChartDataset,
    getAnalyticsChartColors
} from '../../types';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/**
 * Chart height constants based on chart type
 */
const CHART_TYPE_HEIGHTS = {
    line: '21.875rem',
    pie: '23.125rem',
    doughnut: '23.125rem'
} as const;

/**
 * Reusable chart component for analytics dashboard.
 * Supports line, pie, doughnut, bar, and combo chart types with loading states.
 *
 * Auto-detects combo charts when:
 * - Datasets have different `type` values (e.g., 'bar' + 'line')
 * - Any dataset has a `yAxisID` property (dual Y-axes)
 *
 * Usage examples:
 * - Single dataset: Pass 1 dataset → renders as simple chart
 * - Multiple same-type: Pass 2+ datasets with same type → renders with legend
 * - Combo chart: Pass 2+ datasets with different types → renders as combo with dual axes
 */
@Component({
    selector: 'dot-analytics-dashboard-chart',
    imports: [CardModule, ChartModule, SkeletonModule, DotAnalyticsStateMessageComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-analytics-dashboard-chart.component.html',
    styleUrl: './dot-analytics-dashboard-chart.component.scss'
})
export class DotAnalyticsDashboardChartComponent {
    readonly #messageService = inject(DotMessageService);
    readonly #breakpointObserver = inject(BreakpointObserver);

    readonly #isMobile$ = this.#breakpointObserver
        .observe([Breakpoints.XSmall, Breakpoints.Small, Breakpoints.Tablet])
        .pipe(map((result) => result.matches));

    /** Signal to track if we're on mobile/small screen */
    protected readonly $isMobile = toSignal(this.#isMobile$, { initialValue: false });

    /** Animation state for line drawing effect */
    #animationState = createAnimationState();

    constructor() {
        // Reset animation when status changes to trigger animation on data load
        effect(() => {
            const status = this.$status();
            if (status === ComponentStatus.LOADED) {
                resetAnimationState(this.#animationState);
            }
        });
    }

    // Required inputs
    /** Chart type (line, pie, doughnut, bar). For combo charts, use 'bar' or 'line' as base. */
    readonly $type = input.required<ChartType>({ alias: 'type' });

    /** Chart data with datasets. Can be 1 dataset (simple) or multiple (combo). */
    readonly $data = input.required<ChartData>({ alias: 'data' });

    /** Chart title displayed in header */
    readonly $title = input.required<string>({ alias: 'title' });

    /** Component status for loading/error states */
    readonly $status = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'status' });

    /** Custom chart options to merge with defaults */
    readonly $options = input<Partial<ChartOptions>>({}, { alias: 'options' });

    /** Whether to animate line charts with drawing effect */
    readonly $animated = input<boolean>(true, { alias: 'animated' });

    /** Custom height for the chart (e.g., '100%', '15rem'). If not provided, uses type-based default. */
    readonly $customHeight = input<string | undefined>(undefined, { alias: 'height' });

    /**
     * Plugin for line drawing animation.
     * Uses clip to progressively reveal the chart from left to right.
     */
    readonly lineDrawPlugin = [
        createLineDrawAnimationPlugin(
            () => ({
                enabled: this.$type() === 'line' && this.$animated(),
                duration: LINE_DRAW_ANIMATION_DURATION
            }),
            this.#animationState
        )
    ];

    /**
     * Auto-detect if this is a combo chart based on datasets.
     * A combo chart has datasets with different types or uses yAxisID.
     */
    protected readonly $isComboChart = computed(() => {
        const datasets = (this.$data()?.datasets as ComboChartDataset[]) || [];
        if (datasets.length < 2) return false;

        // Has yAxisID OR multiple unique types
        return (
            datasets.some((ds) => ds.yAxisID) ||
            new Set(datasets.map((ds) => ds.type).filter(Boolean)).size > 1
        );
    });

    /** Chart height - uses custom height if provided, otherwise falls back to type-based default */
    protected readonly $height = computed(() => {
        const customHeight = this.$customHeight();
        if (customHeight) {
            return customHeight;
        }

        const type = this.$type();

        return (
            CHART_TYPE_HEIGHTS[type as keyof typeof CHART_TYPE_HEIGHTS] || CHART_TYPE_HEIGHTS.line
        );
    });

    /** Complete chart configuration merging defaults with custom options */
    protected readonly $chartOptions = computed<ChartOptions>(() => {
        const chartType = this.$type();
        const customOptions = this.$options();
        const isMobile = this.$isMobile();
        const isCombo = this.$isComboChart();
        const data = this.$data();
        const hasMultipleDatasets = (data?.datasets?.length || 0) > 1;

        // For mobile pie/doughnut charts, position legend to the right for better space usage
        const shouldUseSideLegend = isMobile && (chartType === 'pie' || chartType === 'doughnut');
        const legendPosition = shouldUseSideLegend ? 'right' : 'bottom';

        // Show legend for combo charts, pie/doughnut, or multiple datasets
        const showLegend =
            isCombo || chartType === 'pie' || chartType === 'doughnut' || hasMultipleDatasets;

        // Line charts: disable built-in animation for smooth tooltip transitions
        // (same behavior as sparkline component)
        const isLineChart = chartType === 'line';

        const defaultOptions: ChartOptions = {
            responsive: true,
            maintainAspectRatio: false,
            // Line charts: use CHART_ANIMATION (duration: 0) for smooth tooltip movement
            // Other charts: use default animation
            animation: isLineChart ? CHART_ANIMATION : undefined,
            // Smooth hover transitions (centralized config)
            transitions: CHART_TRANSITIONS,
            interaction: {
                mode: 'index' as const,
                intersect: false,
                axis: 'x' as const
            },
            plugins: {
                legend: {
                    display: showLegend,
                    position: legendPosition,
                    labels: {
                        usePointStyle: true,
                        pointStyle: 'circle',
                        boxWidth: 10,
                        boxHeight: 10,
                        padding: shouldUseSideLegend ? 15 : 20,
                        font: {
                            size: shouldUseSideLegend ? 11 : 12
                        },
                        ...this.#getChartTypeSpecificLegendOptions(chartType)
                    }
                },
                tooltip: {
                    mode: 'index' as const,
                    intersect: false,
                    // Centralized tooltip styling
                    ...TOOLTIP_STYLE,
                    callbacks: {
                        label: (context: TooltipItem<keyof ChartTypeRegistry>) =>
                            this.#getTooltipLabel(context),
                        title: (context: TooltipItem<keyof ChartTypeRegistry>[]) =>
                            this.#getTooltipTitle(context)
                    }
                }
            },
            scales: this.#getScalesConfig(chartType, isCombo, isMobile)
        };

        return { ...defaultOptions, ...customOptions };
    });

    /** Chart data ready for Chart.js with translated labels and default colors */
    protected readonly $chartData = computed(() => {
        const originalData = this.$data();
        const chartType = this.$type();

        // Clone the data, apply default colors, and translate labels
        const translatedData: ChartData = {
            ...originalData,
            labels: originalData.labels?.map((label) =>
                typeof label === 'string' ? this.#messageService.get(label) : label
            ),
            datasets: originalData.datasets.map((dataset, index) => {
                // Determine the effective type for this dataset (for combo charts)
                const datasetType = (dataset as ComboChartDataset).type || chartType;
                const isBarType = datasetType === 'bar';

                // Get default colors for this dataset index
                const defaultColors = getAnalyticsChartColors(index, isBarType ? 'bar' : 'line');

                return {
                    // Apply default colors first (will be overridden if dataset has its own)
                    borderColor: defaultColors.borderColor,
                    backgroundColor: defaultColors.backgroundColor,
                    // Spread the original dataset (overrides defaults if colors are provided)
                    ...dataset,
                    // Translate label
                    label: dataset.label ? this.#messageService.get(dataset.label) : dataset.label
                };
            })
        };

        return translatedData;
    });

    /** Check if component is in loading state */
    protected readonly $isLoading = computed(() => {
        const status = this.$status();

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    /** Check if component is in error state */
    protected readonly $isError = computed(() => this.$status() === ComponentStatus.ERROR);

    /** Check if chart data is empty */
    protected readonly $isEmpty = computed(() => {
        const data = this.$data();

        if (!data || !data.datasets) {
            return true;
        }

        return (
            data.datasets.length === 0 ||
            data.datasets.every(
                (dataset) =>
                    !dataset.data ||
                    dataset.data.length === 0 ||
                    dataset.data.every((value) => value === null || value === undefined)
            )
        );
    });

    /**
     * Get chart type specific legend options
     */
    #getChartTypeSpecificLegendOptions(chartType: ChartType) {
        if (chartType === 'line' || chartType === 'bar') {
            return {
                generateLabels: (chart: Chart) => {
                    const datasets = chart.data.datasets;

                    return datasets.map((dataset: ChartDataset, i: number) => ({
                        text: dataset.label,
                        fillStyle: dataset.borderColor || dataset.backgroundColor,
                        strokeStyle: dataset.borderColor,
                        lineWidth: 0,
                        pointStyle: 'circle',
                        hidden: false,
                        index: i,
                        datasetIndex: i
                    }));
                }
            };
        }

        return {};
    }

    /**
     * Get tooltip label based on chart type
     */
    #getTooltipLabel(context: TooltipItem<keyof ChartTypeRegistry>): string {
        const chartType = this.$type();

        if (chartType === 'pie' || chartType === 'doughnut') {
            return this.#getPieTooltipLabel(context);
        } else {
            return this.#getLineTooltipLabel(context);
        }
    }

    /**
     * Get tooltip title based on chart type
     */
    #getTooltipTitle(context: TooltipItem<keyof ChartTypeRegistry>[]): string {
        const chartType = this.$type();

        if (chartType === 'pie' || chartType === 'doughnut') {
            const title = context[0]?.label;

            return title ? this.#messageService.get(title) : title;
        }

        return context[0]?.label || '';
    }

    /**
     * Get tooltip label for pie/doughnut charts
     */
    #getPieTooltipLabel(context: TooltipItem<keyof ChartTypeRegistry>): string {
        const dataset = context.dataset;
        const parsedValue = context.parsed;
        const value = parsedValue;
        const label = context.label ? this.#messageService.get(context.label) : '';
        const total = dataset.data
            .filter((val): val is number => typeof val === 'number')
            .reduce((sum, val) => sum + val, 0);
        const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0';

        return `${label}: ${value} (${percentage}%)`;
    }

    /**
     * Get tooltip label for line/bar charts
     */
    #getLineTooltipLabel(context: TooltipItem<keyof ChartTypeRegistry>): string {
        const dataset = context.dataset;
        const parsedValue = context.parsed;
        const value = parsedValue.y ?? parsedValue;
        const datasetLabel = dataset.label ? this.#messageService.get(dataset.label) : '';

        return `${datasetLabel}: ${value}`;
    }

    /**
     * Get scales configuration based on chart type.
     * Returns dual Y-axes for combo charts, single Y-axis for line/bar charts.
     */
    #getScalesConfig(
        chartType: ChartType,
        isCombo: boolean,
        isMobile: boolean
    ): Record<string, unknown> | undefined {
        // Combo charts need dual Y-axes
        if (isCombo) {
            return {
                x: {
                    ticks: {
                        maxTicksLimit: isMobile ? 6 : 10,
                        autoSkip: true,
                        maxRotation: 45,
                        minRotation: 0
                    }
                },
                y: {
                    type: 'linear',
                    display: true,
                    position: 'left',
                    beginAtZero: true,
                    title: { display: false }
                },
                y1: {
                    type: 'linear',
                    display: false,
                    position: 'right',
                    beginAtZero: true,
                    grid: { drawOnChartArea: false },
                    title: { display: false }
                }
            };
        }

        // Line/bar charts need single Y-axis
        if (chartType === 'line' || chartType === 'bar') {
            return {
                x: {
                    ticks: {
                        maxTicksLimit: isMobile ? 6 : 10,
                        autoSkip: true,
                        maxRotation: 45,
                        minRotation: 0
                    }
                },
                y: {
                    beginAtZero: true
                }
            };
        }

        // Pie/doughnut charts don't need scales
        return undefined;
    }
}
