import { ChartData } from 'chart.js';

import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';

import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessagePipe } from '@dotcms/ui';

import { generateDotExperimentLineChartJsOptions } from './chartjs/options/dotExperiments-chartjs.options';
import { htmlLegendPlugin } from './chartjs/plugins/dotHtmlLegend-chartjs.plugin';

import { getRandomUUID } from '../../../shared/dot-experiment.utils';

@Component({
    selector: 'dot-experiments-reports-chart',
    imports: [ChartModule, SkeletonModule, DotMessagePipe],
    templateUrl: './dot-experiments-reports-chart.component.html',
    styleUrls: ['./dot-experiments-reports-chart.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsReportsChartComponent implements OnChanges {
    @Input()
    isEmpty = true;

    @Input()
    isLoading = true;

    @Input()
    config: { xAxisLabel: string; yAxisLabel: string };

    @Input()
    data: ChartData<'line'>;

    @Input()
    isLinearAxis = false;

    options: unknown;
    protected chartId = `chart-` + getRandomUUID();
    protected readonly plugins = [htmlLegendPlugin];

    ngOnChanges(changes: SimpleChanges): void {
        const { config, data } = changes;

        if (config?.currentValue && data.currentValue) {
            this.options = generateDotExperimentLineChartJsOptions({
                xAxisLabel: config.currentValue.xAxisLabel,
                yAxisLabel: config.currentValue.yAxisLabel,
                isLinearAxis: this.isLinearAxis
            });
        }
    }
}
