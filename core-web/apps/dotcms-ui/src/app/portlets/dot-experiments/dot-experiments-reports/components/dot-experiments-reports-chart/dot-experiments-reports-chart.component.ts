import { ChartData } from 'chart.js';

import { JsonPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';

import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { SpinnerModule } from 'primeng/spinner';

import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { getDotExperimentLineChartJsOptions } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-reports-chart/chartjs/options/dotExperiments-chartjs.options';
import { htmlLegendPlugin } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-reports-chart/chartjs/plugins/dotHtmlLegend-chartjs.plugin';

@Component({
    standalone: true,
    selector: 'dot-experiments-reports-chart',
    imports: [ChartModule, SpinnerModule, NgIf, SkeletonModule, JsonPipe, DotMessagePipeModule],
    templateUrl: './dot-experiments-reports-chart.component.html',
    styleUrls: ['./dot-experiments-reports-chart.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsReportsChartComponent implements OnChanges {
    options;
    isEmpty = true;

    @Input()
    loading = true;

    @Input()
    config: { xAxisLabel: string; yAxisLabel: string; title: string };

    @Input()
    data: ChartData<'line'>;

    protected readonly plugins = [htmlLegendPlugin];

    ngOnChanges(changes: SimpleChanges): void {
        const { config, data } = changes;
        if (config.currentValue && data.currentValue) {
            this.options = getDotExperimentLineChartJsOptions({
                xAxisLabel: config.currentValue.xAxisLabel,
                yAxisLabel: config.currentValue.yAxisLabel
            });

            this.isEmpty = this.isEmptyDatasets(data.currentValue);
        }
    }

    private isEmptyDatasets(data: ChartData<'line'>): boolean {
        return data.datasets.find((dataset) => dataset.data.length > 0) === undefined;
    }
}
