import { Component, computed, inject, signal } from '@angular/core';

import { CardModule } from 'primeng/card';
import { TabsModule } from 'primeng/tabs';

import { DotMessageService } from '@dotcms/data-access';
import { GOALS_METADATA_MAP } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotExperimentsReportsChartComponent } from '../../../shared/ui/dot-experiments-reports-chart/dot-experiments-reports-chart.component';
import { DotExperimentsResultsStore } from '../../../store/dot-experiments-results.store';

/** The two reports the Results screen charts, one per tab. */
type ResultsChartTab = 'daily' | 'bayesian';

/** Axis labels the chart needs as plain strings, so they are resolved once per chart. */
interface ChartAxisLabels {
    xAxisLabel: string;
    yAxisLabel: string;
}

/**
 * The charts half of the Results screen: Daily results and Bayesian results behind two tabs.
 *
 * Only the selected tab is rendered, and the tabs are `p-tabs` for the tab strip alone: the chart
 * stays behind an `@if` rather than in a `p-tabpanel`, because an inactive panel stays in the DOM
 * (`hidden`) and `lazy` only defers the first render — `hasBeenRendered` latches, so after visiting
 * both tabs both charts would be mounted. Each chart mints its own `chartId`, so two live at once
 * is not a correctness problem, but it is two canvases and two Chart.js instances kept in step for
 * one that is ever on screen.
 *
 * Both charts are read-only views of the store; whatever shaping the series need happens there.
 */
@Component({
    selector: 'dot-experiments-results-charts',
    imports: [CardModule, TabsModule, DotExperimentsReportsChartComponent, DotMessagePipe],
    templateUrl: './dot-experiments-results-charts.component.html',
    host: {
        class: 'block w-full'
    }
})
export class DotExperimentsResultsChartsComponent {
    readonly #dotMessageService = inject(DotMessageService);

    protected readonly store = inject(DotExperimentsResultsStore);

    protected readonly $activeTab = signal<ResultsChartTab>('daily');

    protected readonly tabs: readonly { id: ResultsChartTab; labelKey: string }[] = [
        { id: 'daily', labelKey: 'experiments.reports.chart.title' },
        { id: 'bayesian', labelKey: 'experiments.bayesian.reports.chart.title' }
    ];

    protected readonly dailyAxisLabels: ChartAxisLabels = {
        xAxisLabel: this.#dotMessageService.get('experiments.chart.xAxisLabel'),
        yAxisLabel: this.#dotMessageService.get('experiments.chart.yAxisLabel')
    };

    protected readonly bayesianAxisLabels: ChartAxisLabels = {
        xAxisLabel: this.#dotMessageService.get('experiments.chart.xAxisLabel.bayesian'),
        yAxisLabel: this.#dotMessageService.get('experiments.chart.yAxisLabel.bayesian')
    };

    /**
     * A chart with too few sessions, or with no payload at all — DRAFT and SCHEDULED included,
     * where no results are ever fetched — hands the empty state to the chart component rather than
     * drawing an axis nothing sits on.
     */
    protected readonly $isDailyEmpty = computed<boolean>(
        () => !this.store.$hasEnoughSessionsForDailyChart() || !this.store.$dailyChartData()
    );

    protected readonly $isBayesianEmpty = computed<boolean>(
        () => !this.store.$hasEnoughDataForBayesianChart() || !this.store.$bayesianChartData()
    );

    /**
     * What the Daily chart is measuring, under its title: the goal's own name, then the metric
     * behind it — `Checkout reached · Reach Page`. The two repeat when the goal still carries the
     * name we default to, which is the same name the metric would produce; that is the default
     * doing its job, not a duplicated field.
     */
    protected readonly $dailyChartSubtitle = computed<string>(() => {
        const goal = this.store.experiment()?.goals?.primary;

        if (!goal?.type) {
            return '';
        }

        const metric = this.#dotMessageService.get(GOALS_METADATA_MAP[goal.type].label);

        return goal.name ? `${goal.name} · ${metric}` : metric;
    });

    /**
     * `p-tabs` reports its value as `string | number`, and the template below branches on a closed
     * union, so anything that is not one of the two tabs is ignored rather than cast into it.
     */
    protected selectTab(tab: string | number): void {
        if (tab === 'daily' || tab === 'bayesian') {
            this.$activeTab.set(tab);
        }
    }
}
