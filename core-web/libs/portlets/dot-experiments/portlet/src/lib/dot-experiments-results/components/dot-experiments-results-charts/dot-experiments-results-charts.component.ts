import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
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
 * Only the selected tab is rendered. The chart's legend is drawn by a Chart.js plugin that walks
 * *up* from the canvas until it finds a `.legend-wrapper`, and that walk also inspects siblings —
 * with both charts mounted at once, one could claim the other's wrapper and a legend would silently
 * go missing. `@if` keeps exactly one canvas in the tree, so each chart can only ever find its own.
 *
 * For the same reason the chart component is composed as a plain child: nothing here wraps or
 * re-projects its internals.
 *
 * Both charts are read-only views of the store — the Bayesian curves arrive already computed from
 * the backend, and nothing is derived from them here.
 */
@Component({
    selector: 'dot-experiments-results-charts',
    imports: [DotExperimentsReportsChartComponent, DotMessagePipe],
    templateUrl: './dot-experiments-results-charts.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
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

    protected selectTab(tab: ResultsChartTab): void {
        this.$activeTab.set(tab);
    }
}
