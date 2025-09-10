import { Chart, ChartDataset, ChartTypeRegistry, TooltipItem } from 'chart.js';

import { Breakpoints, BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';

import { map } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    RequestState,
    transformDeviceBrowsersData,
    transformPageViewTimeLineData
} from '@dotcms/portlets/dot-analytics/data-access';

import { ChartData, ChartOptions, ChartType } from '../../types';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/**
 * Chart height constants based on chart type
 */
const CHART_TYPE_HEIGHTS = {
    line: '21.875rem',
    pie: '23.125rem'
} as const;

/**
 * Union type for chart raw data
 */
type ChartRawData =
    | RequestState<PageViewTimeLineEntity[]> // For line charts
    | RequestState<PageViewDeviceBrowsersEntity[]>; // For pie charts

/**
 * Reusable chart component for analytics dashboard.
 * Supports line, pie, doughnut, and bar chart types with loading states.
 * Now receives raw data and transforms it internally based on chart type.
 */
@Component({
    selector: 'dot-analytics-dashboard-chart',
    imports: [CardModule, ChartModule, SkeletonModule, DotAnalyticsStateMessageComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-analytics-dashboard-chart.component.html',
    styleUrl: './dot-analytics-dashboard-chart.component.scss'
})
export class DotAnalyticsDashboardChartComponent {
    private readonly messageService = inject(DotMessageService);
    private readonly breakpointObserver = inject(BreakpointObserver);

    private readonly isMobile$ = this.breakpointObserver
        .observe([Breakpoints.XSmall, Breakpoints.Small, Breakpoints.Tablet])
        .pipe(map((result) => result.matches));

    /** Signal to track if we're on mobile/small screen */
    protected readonly $isMobile = toSignal(this.isMobile$, { initialValue: false });

    // Required inputs
    /** Chart type (line, pie, doughnut, bar, etc.) */
    readonly $type = input.required<ChartType>({ alias: 'type' });

    /** Complete chart state from analytics store */
    readonly $chartState = input.required<ChartRawData>({ alias: 'chartState' });

    /** Chart title displayed in header */
    readonly $title = input.required<string>({ alias: 'title' });

    /** Custom chart options to merge with defaults */
    readonly $options = input<Partial<ChartOptions>>({}, { alias: 'options' });

    /** Transformed chart data ready for display */
    protected readonly $data = computed((): ChartData => {
        const type = this.$type();
        const rawData = this.$chartState().data;

        // Transform data based on chart type
        if (type === 'line') {
            return transformPageViewTimeLineData(rawData as PageViewTimeLineEntity[] | null);
        } else if (type === 'pie' || type === 'doughnut') {
            return transformDeviceBrowsersData(rawData as PageViewDeviceBrowsersEntity[] | null);
        }

        // Fallback for unsupported types
        return { labels: [], datasets: [] };
    });

    /** Chart height determined automatically by chart type */
    protected readonly $height = computed(() => {
        const type = this.$type();

        return (
            CHART_TYPE_HEIGHTS[type as keyof typeof CHART_TYPE_HEIGHTS] || CHART_TYPE_HEIGHTS.line
        );
    });

    // Computed properties
    /** Complete chart configuration merging defaults with custom options */
    protected readonly $chartOptions = computed<ChartOptions>(() => {
        const chartType = this.$type();
        const customOptions = this.$options();
        const isMobile = this.$isMobile();

        // For mobile pie/doughnut charts, position legend to the right for better space usage
        const shouldUseSideLegend = isMobile && (chartType === 'pie' || chartType === 'doughnut');
        const legendPosition = shouldUseSideLegend ? 'right' : 'bottom';

        const defaultOptions: ChartOptions = {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index' as const,
                intersect: false,
                axis: 'x' as const
            },
            plugins: {
                legend: {
                    display: chartType !== 'line', // Hide legend for line charts
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
                        ...this.getChartTypeSpecificLegendOptions(chartType)
                    }
                },
                tooltip: {
                    mode: 'index' as const,
                    intersect: false,
                    callbacks: {
                        label: (context: TooltipItem<keyof ChartTypeRegistry>) =>
                            this.getTooltipLabel(context),
                        title: (context: TooltipItem<keyof ChartTypeRegistry>[]) =>
                            this.getTooltipTitle(context)
                    }
                }
            },
            scales:
                chartType === 'line'
                    ? {
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
                      }
                    : undefined
        };

        // Merge with custom options
        return { ...defaultOptions, ...customOptions };
    });

    /** Combined chart data ready for Chart.js with translated labels */
    protected readonly $chartData = computed(() => {
        const originalData = this.$data();

        // Clone the data and translate any translation keys in dataset labels AND main labels
        const translatedData: ChartData = {
            ...originalData,
            // Translate main chart labels (used in pie chart segments and tooltips)
            labels: originalData.labels?.map((label) =>
                typeof label === 'string' ? this.messageService.get(label) : label
            ),
            datasets: originalData.datasets.map((dataset) => ({
                ...dataset,
                label: dataset.label ? this.messageService.get(dataset.label) : dataset.label
            }))
        };

        return translatedData;
    });

    /** Check if component is in loading state */
    protected readonly $isLoading = computed(() => {
        const status = this.$chartState().status;

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    /** Check if component is in error state */
    protected readonly $isError = computed(
        () => this.$chartState().status === ComponentStatus.ERROR
    );

    /** Check if chart data is empty */
    protected readonly $isEmpty = computed(() => {
        const data = this.$data();

        if (!data || !data.datasets) {
            return true;
        }

        // Check if all datasets are empty or have no data
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
    private getChartTypeSpecificLegendOptions(chartType: ChartType) {
        if (chartType === 'line') {
            return {
                generateLabels: (chart: Chart) => {
                    const datasets = chart.data.datasets;

                    return datasets.map((dataset: ChartDataset, i: number) => ({
                        text: dataset.label,
                        fillStyle: dataset.borderColor, // Use borderColor for solid legend
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
    private getTooltipLabel(context: TooltipItem<keyof ChartTypeRegistry>): string {
        const chartType = this.$type();

        if (chartType === 'pie' || chartType === 'doughnut') {
            return this.getPieTooltipLabel(context);
        } else {
            return this.getLineTooltipLabel(context);
        }
    }

    /**
     * Get tooltip title based on chart type
     */
    private getTooltipTitle(context: TooltipItem<keyof ChartTypeRegistry>[]): string {
        const chartType = this.$type();

        if (chartType === 'pie' || chartType === 'doughnut') {
            // For pie charts, translate the title if it's a translation key
            const title = context[0]?.label;

            return title ? this.messageService.get(title) : title;
        }

        // For other charts, return the label as is (usually dates/times)
        return context[0]?.label || '';
    }

    /**
     * Get tooltip label for pie/doughnut charts
     */
    private getPieTooltipLabel(context: TooltipItem<keyof ChartTypeRegistry>): string {
        const dataset = context.dataset;
        const parsedValue = context.parsed;
        const value = parsedValue;
        const label = context.label ? this.messageService.get(context.label) : '';
        const total = dataset.data
            .filter((val): val is number => typeof val === 'number')
            .reduce((sum, val) => sum + val, 0);
        const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0';

        return `${label}: ${value} (${percentage}%)`;
    }

    /**
     * Get tooltip label for line/bar charts
     */
    private getLineTooltipLabel(context: TooltipItem<keyof ChartTypeRegistry>): string {
        const dataset = context.dataset;
        const parsedValue = context.parsed;
        const value = parsedValue.y ?? parsedValue;
        const datasetLabel = dataset.label ? this.messageService.get(dataset.label) : '';

        return `${datasetLabel}: ${value}`;
    }
}
