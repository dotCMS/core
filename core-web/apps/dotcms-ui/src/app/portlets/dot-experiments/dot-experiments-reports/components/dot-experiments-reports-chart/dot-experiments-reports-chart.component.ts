import { ChartData, ChartDataset } from 'chart.js';

import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ChartModule } from 'primeng/chart';

import { DotMessageService } from '@dotcms/data-access';
import { ChartColors, daysOfTheWeek } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { dotExperimentChartJsOptions } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-reports-chart/chartjs/options/dotExperiments-chartjs.options';
import { htmlLegendPlugin } from '@portlets/dot-experiments/dot-experiments-reports/components/dot-experiments-reports-chart/chartjs/plugins/dotHtmlLegend-chartjs.plugin';

const DefaultExperimentChartDatasetOption: Partial<ChartDataset<'line'>> = {
    type: 'line',
    pointRadius: 4,
    pointHoverRadius: 6,
    fill: true,
    cubicInterpolationMode: 'monotone',
    borderWidth: 1.5
};

const DefaultExperimentChartDatasetColors: Record<
    'DEFAULT' | 'VARIANT1' | 'VARIANT2',
    { borderColor: string; backgroundColor: string; pointBackgroundColor: string }
> = {
    DEFAULT: {
        borderColor: ChartColors.primary.rgb,
        pointBackgroundColor: ChartColors.primary.rgb,
        backgroundColor: ChartColors.primary.rgba_10
    },
    VARIANT1: {
        borderColor: ChartColors.secondary.rgb,
        pointBackgroundColor: ChartColors.secondary.rgb,
        backgroundColor: ChartColors.secondary.rgba_10
    },
    VARIANT2: {
        borderColor: ChartColors.accent.rgb,
        pointBackgroundColor: ChartColors.accent.rgb,
        backgroundColor: ChartColors.accent.rgba_10
    }
};

@Component({
    standalone: true,
    selector: 'dot-experiments-reports-chart',
    imports: [ChartModule, DotMessagePipeModule],
    templateUrl: './dot-experiments-reports-chart.component.html',
    styleUrls: ['./dot-experiments-reports-chart.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsReportsChartComponent {
    readonly customChartPlugins = [htmlLegendPlugin];
    readonly dotExperimentsChartJSOptions = dotExperimentChartJsOptions({
        xAxisLabel: this.dotMessageService.get('dot.experimental.chart.xAxisLabel'),
        yAxisLabel: this.dotMessageService.get('dot.experimental.chart.yAxisLabel')
    });

    labels = this.addWeekdayToDateLabels([
        '03/15/2023',
        '03/16/2023',
        '03/17/2023',
        '03/18/2023',
        '03/19/2023',
        '03/20/2023',
        '03/21/2023',
        '03/22/2023',
        '03/23/2023',
        '03/24/2023',
        '03/25/2023',
        '03/26/2023',
        '03/27/2023',
        '03/28/2023',
        '03/29/2023'
    ]);

    data: ChartData<'line'>;

    constructor(private readonly dotMessageService: DotMessageService) {
        this.data = {
            labels: [...this.labels],
            datasets: [
                {
                    label: 'Original',
                    data: [...this.getRandomDataMock(5)],
                    ...DefaultExperimentChartDatasetColors.DEFAULT,
                    ...DefaultExperimentChartDatasetOption
                },
                {
                    label: 'Variant A',
                    data: [...this.getRandomDataMock(25)],
                    ...DefaultExperimentChartDatasetColors.VARIANT1,
                    ...DefaultExperimentChartDatasetOption
                },
                {
                    label: 'Variant B',
                    data: [...this.getRandomDataMock(70)],
                    ...DefaultExperimentChartDatasetColors.VARIANT2,
                    ...DefaultExperimentChartDatasetOption
                }
            ]
        };
    }

    // TODO: remove after get the real data
    private getRandomDataMock(between = 50, qty = 15) {
        return Array.from({ length: qty }, () => Math.floor(Math.random() * between));
    }

    private addWeekdayToDateLabels(labels: Array<string>) {
        return labels.map((item) => {
            const date = new Date(item).getDay();

            return [this.dotMessageService.get(daysOfTheWeek[date]), item];
        });
    }
}
