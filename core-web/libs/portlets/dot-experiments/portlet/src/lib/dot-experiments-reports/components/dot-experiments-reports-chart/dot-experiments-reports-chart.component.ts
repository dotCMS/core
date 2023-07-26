import { ChartData } from 'chart.js';

import { NgIf } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnChanges,
    SimpleChanges,
    ViewChild
} from '@angular/core';

import { ChartModule, UIChart } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessagePipe } from '@dotcms/ui';

import { generateDotExperimentLineChartJsOptions } from './chartjs/options/dotExperiments-chartjs.options';
import { htmlLegendPlugin } from './chartjs/plugins/dotHtmlLegend-chartjs.plugin';

import { getRandomUUID } from '../../../shared/dot-experiment.utils';

@Component({
    standalone: true,
    selector: 'dot-experiments-reports-chart',
    imports: [ChartModule, NgIf, SkeletonModule, DotMessagePipe],
    templateUrl: './dot-experiments-reports-chart.component.html',
    styleUrls: ['./dot-experiments-reports-chart.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsReportsChartComponent implements OnChanges {
    @ViewChild('chart') chart: UIChart;
    options;
    @Input()
    isEmpty = true;
    @Input()
    isLoading = true;
    @Input()
    config: { xAxisLabel: string; yAxisLabel: string };
    @Input()
    data: ChartData<'line'>;

    protected chartId = `chart-` + getRandomUUID();

    protected readonly plugins = [htmlLegendPlugin];

    ngOnChanges(changes: SimpleChanges): void {
        const { config, data } = changes;
        if (config?.currentValue && data.currentValue) {
            this.options = generateDotExperimentLineChartJsOptions({
                xAxisLabel: config.currentValue.xAxisLabel,
                yAxisLabel: config.currentValue.yAxisLabel
            });
        }
    }
}
