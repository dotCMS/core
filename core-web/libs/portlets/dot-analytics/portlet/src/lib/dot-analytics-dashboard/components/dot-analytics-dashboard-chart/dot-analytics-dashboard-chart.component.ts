import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';

import { DotMessageService } from '@dotcms/data-access';

import { ChartData, ChartOptions, ChartType } from '../../types';

/**
 * Reusable chart component for analytics dashboard.
 * Supports multiple chart types (line, pie, bar, doughnut) with configurable options.
 *
 */
@Component({
    selector: 'dot-analytics-dashboard-chart',
    standalone: true,
    imports: [CommonModule, CardModule, ChartModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dot-analytics-dashboard-chart.component.html',
    styleUrl: './dot-analytics-dashboard-chart.component.scss'
})
export class DotAnalyticsDashboardChartComponent {

    private readonly dotMessageService = inject(DotMessageService);

    // Inputs
    /** Optional chart title displayed in card header */
    readonly $title = input<string>('', { alias: 'title' });

    /** Chart type (line, pie, bar, doughnut) */
    readonly $type = input.required<ChartType>({ alias: 'type' });

    /** Chart data including labels and datasets */
    readonly $data = input.required<ChartData>({ alias: 'data' });

    /** Custom chart options to override defaults */
    readonly $options = input<ChartOptions>({}, { alias: 'options' });

    /** Chart width as CSS value */
    readonly $width = input<string>('100%', { alias: 'width' });

    /** Chart height as CSS value */
    readonly $height = input<string>('300px', { alias: 'height' });

    // Computed properties
    /** Returns the chart data for PrimeNG Chart component with translated labels */
    protected readonly $chartData = computed(() => {
        const originalData = this.$data();

        // Clone the data and translate any translation keys in dataset labels only
        const translatedData: ChartData = {
            ...originalData,
            datasets: originalData.datasets.map(dataset => ({
                ...dataset,
                label: dataset.label ? this.dotMessageService.get(dataset.label) : dataset.label
            }))
        };

        return translatedData;
    });

    /** Merges default options with custom options for chart configuration */
    protected readonly $chartOptions = computed(() => {
        const chartType = this.$type();

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
                }
            }
        };

        // Merge with custom options
        const customOptions = this.$options() || {};

        return { ...defaultOptions, ...customOptions };
    });
}
