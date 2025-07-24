import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { ChartData, ChartOptions, ChartType } from '../../types';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/**
 * Reusable chart component for analytics dashboard.
 * Supports line, pie, doughnut, and bar chart types with loading states.
 */
@Component({
    selector: 'dot-analytics-dashboard-chart',
    standalone: true,
    imports: [
        CommonModule,
        CardModule,
        ChartModule,
        SkeletonModule,
        DotMessagePipe,
        DotAnalyticsStateMessageComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-analytics-dashboard-chart.component.html',
    styleUrl: './dot-analytics-dashboard-chart.component.scss'
})
export class DotAnalyticsDashboardChartComponent {
    private readonly messageService = inject(DotMessageService);

    // Required inputs
    /** Chart type (line, pie, doughnut, bar, etc.) */
    readonly $type = input.required<ChartType>({ alias: 'type' });

    /** Chart data with labels and datasets */
    readonly $data = input.required<ChartData>({ alias: 'data' });

    // Optional inputs
    /** Chart title displayed in header */
    readonly $title = input<string>('', { alias: 'title' });

    /** Chart width as CSS value */
    readonly $width = input<string>('100%', { alias: 'width' });

    /** Chart height as CSS value */
    readonly $height = input<string>('300px', { alias: 'height' });

    /** Custom chart options to merge with defaults */
    readonly $options = input<Partial<ChartOptions>>({}, { alias: 'options' });

    /** Component status for loading/error states */
    readonly $status = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'status' });

    // Computed properties
    /** Complete chart configuration merging defaults with custom options */
    protected readonly $chartOptions = computed((): ChartOptions => {
        const chartType = this.$type();
        const customOptions = this.$options();

        const defaultOptions: ChartOptions = {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'bottom',
                    // Use smaller filled circular point style for all chart types
                    labels: {
                        usePointStyle: true,
                        pointStyle: 'circle',
                        boxWidth: 10,
                        boxHeight: 10,
                        padding: 20,
                        font: {
                            size: 12
                        },
                        // Custom legend generation for line charts to ensure solid points
                        ...(chartType === 'line' && {
                            // eslint-disable-next-line @typescript-eslint/no-explicit-any
                            generateLabels: (chart: any) => {
                                const datasets = chart.data.datasets;

                                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                return datasets.map((dataset: any, i: number) => ({
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
                        })
                    }
                },
                tooltip: {
                    // Custom tooltip callbacks to ensure translation
                    callbacks: {
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        label: (context: any) => {
                            const chartType = this.$type();
                            const dataset = context.dataset;
                            const parsedValue = context.parsed;

                            if (chartType === 'pie' || chartType === 'doughnut') {
                                // For pie/doughnut charts, parsed value is a number directly
                                const value = parsedValue;
                                const label = context.label
                                    ? this.messageService.get(context.label)
                                    : '';
                                const total = dataset.data.reduce(
                                    (sum: number, val: number) => sum + val,
                                    0
                                );
                                const percentage = ((value / total) * 100).toFixed(1);

                                return `${label}: ${value} (${percentage}%)`;
                            } else {
                                // For line/bar charts, parsed value is an object with x, y properties
                                const value = parsedValue.y ?? parsedValue;
                                const datasetLabel = dataset.label
                                    ? this.messageService.get(dataset.label)
                                    : '';

                                return `${datasetLabel}: ${value}`;
                            }
                        },
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        title: (context: any) => {
                            const chartType = this.$type();
                            if (chartType === 'pie' || chartType === 'doughnut') {
                                // For pie charts, translate the title if it's a translation key
                                const title = context[0]?.label;

                                return title ? this.messageService.get(title) : title;
                            }

                            // For other charts, return the label as is (usually dates/times)
                            return context[0]?.label || '';
                        }
                    }
                }
            }
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

        // Check if all datasets are empty or have no data
        return (
            data.datasets.length === 0 ||
            data.datasets.every(
                (dataset) =>
                    !dataset.data ||
                    dataset.data.length === 0 ||
                    dataset.data.every(
                        (value) => value === 0 || value === null || value === undefined
                    )
            )
        );
    });
}
