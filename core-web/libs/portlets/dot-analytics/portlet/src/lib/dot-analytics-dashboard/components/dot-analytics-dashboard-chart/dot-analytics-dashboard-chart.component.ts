import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';

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
    /** Returns the chart data for PrimeNG Chart component */
    protected readonly $chartData = computed(() => {
        return this.$data();
    });

    /** Merges default options with custom options for chart configuration */
    protected readonly $chartOptions = computed(() => {
        const defaultOptions: ChartOptions = {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'bottom'
                }
            }
        };

        // Merge with custom options
        const customOptions = this.$options() || {};

        return { ...defaultOptions, ...customOptions };
    });
}
