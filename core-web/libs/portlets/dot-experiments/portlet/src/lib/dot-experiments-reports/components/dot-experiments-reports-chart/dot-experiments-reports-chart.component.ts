import { ChartData } from 'chart.js';

import { ChangeDetectionStrategy, Component, effect, input } from '@angular/core';

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
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'w-full'
    }
})
export class DotExperimentsReportsChartComponent {
    isEmpty = input(true);
    isLoading = input(true);
    config = input<{ xAxisLabel: string; yAxisLabel: string }>();
    data = input<ChartData<'line'>>();
    isLinearAxis = input(false);

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    options: any;
    protected chartId = `chart-` + getRandomUUID();
    protected readonly plugins = [htmlLegendPlugin];
    protected readonly emptyChartBackgroundImage = `url("data:image/svg+xml,%3Csvg width='917' height='515' viewBox='0 0 917 515' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cg clip-path='url(%23clip0_3061_22368)'%3E%3Crect width='855' height='515' transform='translate(61.5)' fill='white'/%3E%3Cline x1='136.863' y1='437' x2='136.863' y2='18' stroke='%23F4F4F6' stroke-width='1.27393'/%3E%3Cline x1='315.863' y1='437' x2='315.863' y2='18' stroke='%23F4F4F6' stroke-width='1.27393'/%3E%3Cline x1='482.863' y1='437' x2='482.863' y2='18' stroke='%23F4F4F6' stroke-width='1.27393'/%3E%3Cline x1='649.863' y1='437' x2='649.863' y2='18' stroke='%23F4F4F6' stroke-width='1.27393'/%3E%3Cline x1='816.863' y1='437' x2='816.863' y2='18' stroke='%23F4F4F6' stroke-width='1.27393'/%3E%3Cline x1='62.137' y1='437.05' x2='1397.22' y2='437.049' stroke='%23F4F4F6' stroke-width='1.27393' stroke-linecap='round'/%3E%3Cline x1='61.9612' y1='185.225' x2='1397.39' y2='185.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='17.2254' x2='1397.39' y2='17.2254' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='59.2254' x2='1397.39' y2='59.2254' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='101.225' x2='1397.39' y2='101.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='143.225' x2='1397.39' y2='143.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='227.225' x2='1397.39' y2='227.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='269.225' x2='1397.39' y2='269.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='311.225' x2='1397.39' y2='311.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='353.225' x2='1397.39' y2='353.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='395.225' x2='1397.39' y2='395.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Crect x='100.5' y='458' width='72' height='15.7241' rx='7.86207' fill='%23F4F4F6'/%3E%3Crect x='90.5' y='477.725' width='92' height='16.1758' rx='8.08791' fill='%23F4F4F6'/%3E%3Crect x='270.5' y='458' width='72' height='15.7241' rx='7.86207' fill='%23F4F4F6'/%3E%3Crect x='260.5' y='477.725' width='92' height='16.1758' rx='8.08791' fill='%23F4F4F6'/%3E%3Crect x='440.5' y='458' width='72' height='15.7241' rx='7.86207' fill='%23F4F4F6'/%3E%3Crect x='430.5' y='477.725' width='92' height='16.1758' rx='8.08791' fill='%23F4F4F6'/%3E%3Crect x='610.5' y='458' width='72' height='15.7241' rx='7.86207' fill='%23F4F4F6'/%3E%3Crect x='600.5' y='477.725' width='92' height='16.1758' rx='8.08791' fill='%23F4F4F6'/%3E%3Crect x='780.5' y='458' width='72' height='15.7241' rx='7.86207' fill='%23F4F4F6'/%3E%3Crect x='770.5' y='477.725' width='92' height='16.1758' rx='8.08791' fill='%23F4F4F6'/%3E%3C/g%3E%3Cline x1='61.9375' y1='59.4612' x2='31.5003' y2='59.4612' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='17.4612' x2='31.5003' y2='17.4612' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='101.461' x2='31.5003' y2='101.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='143.461' x2='31.5003' y2='143.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='185.461' x2='31.5003' y2='185.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='227.461' x2='31.5003' y2='227.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='269.461' x2='31.5003' y2='269.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='311.461' x2='31.5003' y2='311.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='353.461' x2='31.5003' y2='353.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='395.461' x2='31.5003' y2='395.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='437.461' x2='31.5003' y2='437.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Crect x='0.5' y='10' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='52' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='93' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='136' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='180' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='220' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='261' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='304' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='346' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='386' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='430' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Cdefs%3E%3CclipPath id='clip0_3061_22368'%3E%3Crect width='855' height='515' fill='white' transform='translate(61.5)'/%3E%3C/clipPath%3E%3C/defs%3E%3C/svg%3E")`;

    constructor() {
        effect(() => {
            const configValue = this.config();
            const dataValue = this.data();
            const isLinearAxisValue = this.isLinearAxis();

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
