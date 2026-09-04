import { ChartData } from 'chart.js';

import { Component, computed, effect, input, signal } from '@angular/core';

import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';

import { DotMessagePipe } from '@dotcms/ui';

import { generateDotExperimentLineChartJsOptions } from './chartjs/options/dotExperiments-chartjs.options';

import { EMPTY_CHART_BACKGROUND_IMAGE } from '../../constants';
import { getRandomUUID } from '../../dot-experiment-results.utils';

/**
 * The swatch colour of a series: its fill, or its line when the fill says nothing.
 *
 * Chart.js allows an array or a callback here, and neither describes a single series, so only a
 * plain colour is taken — anything else leaves the swatch unpainted rather than guessing.
 */
function colorOf(dataset: ChartData<'line'>['datasets'][number]): string | null {
    const { backgroundColor, borderColor } = dataset;

    if (typeof backgroundColor === 'string') {
        return backgroundColor;
    }

    return typeof borderColor === 'string' ? borderColor : null;
}

@Component({
    selector: 'dot-experiments-reports-chart',
    imports: [ChartModule, SkeletonModule, TagModule, DotMessagePipe],
    templateUrl: './dot-experiments-reports-chart.component.html',
    host: {
        class: 'w-full'
    }
})
export class DotExperimentsReportsChartComponent {
    $isEmpty = input(true, { alias: 'isEmpty' });
    $isLoading = input(true, { alias: 'isLoading' });
    $config = input<{ xAxisLabel: string; yAxisLabel: string }>(null, { alias: 'config' });
    $data = input<ChartData<'line'> | null>(null, { alias: 'data' });
    $isLinearAxis = input(false, { alias: 'isLinearAxis' });

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    options: any;
    protected chartId = `chart-` + getRandomUUID();

    protected readonly emptyChartBackgroundImage = EMPTY_CHART_BACKGROUND_IMAGE;

    /** Series the reader has switched off, by dataset index. */
    readonly #hiddenSeries = signal<ReadonlySet<number>>(new Set());

    /**
     * The chart's data with the reader's choices folded in.
     *
     * Toggling a series is expressed as `hidden` on the dataset rather than by reaching for the
     * Chart.js instance: the chart redraws from its input like any other binding, so nothing here
     * has to hold an imperative handle to it.
     */
    protected readonly $chartData = computed<ChartData<'line'> | null>(() => {
        const data = this.$data();

        if (!data) {
            return null;
        }

        const hidden = this.#hiddenSeries();

        return {
            ...data,
            datasets: data.datasets.map((dataset, index) => ({
                ...dataset,
                hidden: hidden.has(index)
            }))
        };
    });

    /**
     * The legend, derived from the datasets themselves.
     *
     * It used to be raw DOM written by a Chart.js plugin, which is why it could not be a `p-tag`
     * and carried its own copy of the pill's shape. Read off the data instead, it is ordinary
     * markup and inherits the theme like every other tag.
     */
    protected readonly $legendItems = computed(() => {
        const hidden = this.#hiddenSeries();

        return (this.$data()?.datasets ?? []).map((dataset, index) => ({
            index,
            label: dataset.label ?? '',
            color: colorOf(dataset),
            hidden: hidden.has(index)
        }));
    });

    /** Switches one series off, or back on. */
    protected toggleSeries(index: number): void {
        this.#hiddenSeries.update((hidden) => {
            const next = new Set(hidden);

            if (!next.delete(index)) {
                next.add(index);
            }

            return next;
        });
    }

    constructor() {
        effect(() => {
            const configValue = this.$config();
            const dataValue = this.$data();
            const isLinearAxisValue = this.$isLinearAxis();

            if (configValue && dataValue) {
                this.options = generateDotExperimentLineChartJsOptions({
                    xAxisLabel: configValue.xAxisLabel,
                    yAxisLabel: configValue.yAxisLabel,
                    isLinearAxis: isLinearAxisValue
                });
            }
        });
    }
}
