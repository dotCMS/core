import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { ChartData } from 'chart.js';
import { forkJoin, Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { MessageService } from 'primeng/api';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    BayesianNoWinnerStatus,
    BayesianStatusResponse,
    ComponentStatus,
    DEFAULT_VARIANT_ID,
    DotExperiment,
    DotExperimentResults,
    DotExperimentVariantDetail,
    DotResultGoal,
    DotResultVariant,
    ExperimentLineChartDatasetDefaultProperties,
    MINIMUM_SESSIONS_TO_SHOW_CHART,
    MonthsOfTheYear,
    ReportSummaryLegendByBayesianStatus,
    SummaryLegend,
    Variant
} from '@dotcms/dotcms-models';

import {
    getBayesianDatasets,
    getBayesianVariantResult,
    getConversionRate,
    getConversionRateRage,
    getParsedChartData,
    getPreviousDay,
    getProbabilityToBeBest,
    getPropertyColors,
    getSuggestedWinner,
    isPromotedVariant,
    orderVariants
} from '../../shared/dot-experiment.utils';

export interface DotExperimentsReportsState {
    experiment: DotExperiment | null;
    status: ComponentStatus;
    results: DotExperimentResults | null;
}

const initialState: DotExperimentsReportsState = {
    experiment: null,
    status: ComponentStatus.INIT,
    results: null
};

interface DotChart {
    hasEnoughData: boolean;
    chartData: ChartData<'line'> | null;
}
// ViewModel Interfaces
export interface VmReportExperiment {
    experiment: DotExperiment;
    results: DotExperimentResults;
    dailyChart: DotChart | null;
    bayesianChart: DotChart | null;
    detailData: DotExperimentVariantDetail[];
    isLoading: boolean;
    status: ComponentStatus;
    winnerLegendSummary: SummaryLegend;
    suggestedWinner: DotResultVariant | null;
    promotedVariant: Variant | null;
}

const NOT_ENOUGH_DATA_LABEL = 'experiments.reports.not.enough.data';
const CONVERSION_RATE_RANGE_SEPARATOR_LABEL = 'to';

@Injectable()
export class DotExperimentsReportsStore extends ComponentStore<DotExperimentsReportsState> {
    readonly isLoading$: Observable<boolean> = this.select(
        ({ status }) => status === ComponentStatus.LOADING
    );

    readonly summaryWinnerLegend$: Observable<{ icon: string; legend: string }> = this.select(
        ({ experiment, results }) => {
            if (experiment != null && results != null) {
                return getSuggestedWinner(experiment, results);
            }

            return { ...ReportSummaryLegendByBayesianStatus.NO_ENOUGH_SESSIONS };
        }
    );

    readonly getSuggestedWinner$: Observable<DotResultVariant | null> = this.select(
        ({ results }) =>
            BayesianNoWinnerStatus.includes(results?.bayesianResult?.suggestedWinner)
                ? null
                : results?.goals.primary.variants[results?.bayesianResult?.suggestedWinner]
    );

    readonly getPromotedVariant$: Observable<Variant | null> = this.select(({ experiment }) =>
        experiment?.trafficProportion?.variants.find(({ promoted }) => promoted)
    );

    // Bayesian Chart
    readonly getBayesianChartData$ = this.select(({ results }) =>
        results ? { datasets: getBayesianDatasets(results) } : null
    );

    readonly hasEnoughDataForShowBayesianChart$: Observable<boolean> = this.select(
        this.state$,
        this.getBayesianChartData$,
        ({ results }, chartData) => {
            if (results && chartData) {
                const { datasets } = chartData;

                return (
                    results.bayesianResult?.suggestedWinner != BayesianStatusResponse.NONE &&
                    datasets.every((dataset) => dataset.data.length > 0)
                );
            }

            return false;
        }
    );

    readonly bayesianChart$: Observable<DotChart> = this.select(
        this.getBayesianChartData$,
        this.hasEnoughDataForShowBayesianChart$,
        (chartData, hasEnoughData) => {
            return { hasEnoughData, chartData };
        }
    );

    // Daily Chart
    readonly hasEnoughSessions$: Observable<boolean> = this.select(
        ({ results }) => results && results.sessions.total >= MINIMUM_SESSIONS_TO_SHOW_CHART
    );

    readonly getDailyChartData$: Observable<ChartData<'line'>> = this.select(({ results }) =>
        results
            ? {
                  labels: this.getDailyChartLabels(results.goals.primary.variants),
                  datasets: this.getDailyChartDatasets(results.goals.primary.variants)
              }
            : null
    );

    readonly dailyChart$: Observable<DotChart> = this.select(
        this.getDailyChartData$,
        this.hasEnoughSessions$,
        (chartData, hasEnoughData) => {
            return { hasEnoughData, chartData };
        }
    );

    readonly getDetailData$: Observable<DotExperimentVariantDetail[]> = this.select(
        ({ experiment, results }) => {
            const noDataLabel = this.dotMessageService.get(NOT_ENOUGH_DATA_LABEL);
            const separatorLabel = this.dotMessageService.get(
                CONVERSION_RATE_RANGE_SEPARATOR_LABEL
            );

            return results && results.bayesianResult
                ? Object.values(results.goals.primary.variants).map((variant) => {
                      return this.getDotExperimentVariantDetail(
                          experiment,
                          results,
                          variant,
                          noDataLabel,
                          separatorLabel
                      );
                  })
                : [];
        }
    );

    // Updaters
    readonly setComponentStatus = this.updater(
        (state: DotExperimentsReportsState, status: ComponentStatus) => ({
            ...state,
            status
        })
    );

    readonly setExperiment = this.updater(
        (state: DotExperimentsReportsState, experiment: DotExperiment) => ({
            ...state,
            experiment: {
                ...state.experiment,
                ...experiment
            }
        })
    );

    readonly loadExperimentAndResults = this.effect((experimentId$: Observable<string>) =>
        experimentId$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.LOADING)),
            switchMap((experimentId) =>
                forkJoin([
                    this.dotExperimentsService.getById(experimentId),
                    this.dotExperimentsService.getResults(experimentId).pipe(
                        catchError((response) => {
                            const { error } = response;
                            this.dotHttpErrorManagerService.handle({
                                ...response,
                                error: {
                                    ...error,
                                    header: error.header
                                        ? error.header
                                        : this.dotMessageService.get(
                                              'dot.common.http.error.400.experiment.analytics-app-not-configured.header'
                                          ),
                                    message: error.message
                                }
                            });

                            return of(null);
                        })
                    )
                ]).pipe(
                    map(([experiment, results]) => {
                        this.patchState({
                            experiment: experiment,
                            results: results,
                            status: ComponentStatus.IDLE
                        });
                        this.updateTabTitle(experiment);
                        this.setComponentStatus(ComponentStatus.IDLE);
                    }),
                    catchError((err) => {
                        this.setComponentStatus(ComponentStatus.IDLE);

                        return this.dotHttpErrorManagerService.handle(err);
                    })
                )
            )
        )
    );

    readonly promoteVariant = this.effect(
        (
            variant$: Observable<{
                experimentId: string;
                variant: DotExperimentVariantDetail;
            }>
        ) => {
            return variant$.pipe(
                switchMap((variantToPromote) => {
                    const { experimentId, variant } = variantToPromote;

                    return this.dotExperimentsService.promoteVariant(experimentId, variant.id).pipe(
                        tapResponse(
                            (experiment) => {
                                this.messageService.add({
                                    severity: 'info',
                                    summary: this.dotMessageService.get(
                                        'experiments.action.promote.variant.confirm-title'
                                    ),
                                    detail: this.dotMessageService.get(
                                        'experiments.action.promote.variant.confirm-message',
                                        variantToPromote.variant.name
                                    )
                                });
                                this.setExperiment(experiment);
                            },
                            (error: HttpErrorResponse) =>
                                this.dotHttpErrorManagerService.handle(error)
                        )
                    );
                })
            );
        }
    );

    readonly vm$: Observable<VmReportExperiment> = this.select(
        this.state$,
        this.isLoading$,
        this.bayesianChart$,
        this.dailyChart$,
        this.summaryWinnerLegend$,
        this.getSuggestedWinner$,
        this.getDetailData$,
        this.getPromotedVariant$,
        (
            { experiment, status, results },
            isLoading,

            bayesianChart,
            dailyChart,
            winnerLegendSummary,
            suggestedWinner,
            detailData,
            promotedVariant
        ) => ({
            experiment,
            status,
            results,
            isLoading,
            bayesianChart,
            dailyChart,
            winnerLegendSummary: {
                ...winnerLegendSummary,
                legend: this.dotMessageService.get(
                    winnerLegendSummary?.legend,
                    suggestedWinner?.variantDescription
                )
            },
            suggestedWinner,
            detailData,
            promotedVariant
        })
    );

    constructor(
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotHttpErrorManagerService: DotHttpErrorManagerService,
        private readonly dotMessageService: DotMessageService,
        private readonly messageService: MessageService,
        private readonly title: Title
    ) {
        super(initialState);
    }

    private updateTabTitle(experiment: DotExperiment) {
        this.title.setTitle(`${experiment.name} - ${this.title.getTitle()}`);
    }

    private getDailyChartDatasets(
        result: DotResultGoal['variants']
    ): ChartData<'line'>['datasets'] {
        const variantsOrdered = orderVariants(Object.keys(result));

        let colorIndex = 0;

        return variantsOrdered.map((variantName) => {
            const { details } = result[variantName];

            return {
                label: result[variantName].variantDescription,
                data: getParsedChartData(details),
                ...getPropertyColors(colorIndex++),
                ...ExperimentLineChartDatasetDefaultProperties
            };
        });
    }

    private getDailyChartLabels(variants: DotResultGoal['variants']) {
        return variants[DEFAULT_VARIANT_ID].details
            ? this.parseDaysLabels(Object.keys(variants[DEFAULT_VARIANT_ID].details))
            : [];
    }

    private parseDaysLabels(labels: Array<string>): string[] {
        return [getPreviousDay(labels[0]), ...labels].map((item) => {
            const [, month, day] = item.split('-').map(Number);
            const monthTranslated = this.dotMessageService.get(MonthsOfTheYear[month - 1]);

            return `${monthTranslated}-${day}`;
        });
    }

    private getDotExperimentVariantDetail(
        experiment: DotExperiment,
        results: DotExperimentResults,
        variant: DotResultVariant,
        noDataLabel: string,
        separatorLabel: string
    ): DotExperimentVariantDetail {
        const variantBayesianResult = getBayesianVariantResult(
            variant.variantName,
            results.bayesianResult.results
        );

        return {
            id: variant.variantName,
            name: variant.variantDescription,
            conversions: variant.uniqueBySession.count,
            conversionRate: getConversionRate(
                variant.uniqueBySession.count,
                results.sessions.variants[variant.variantName]
            ),
            conversionRateRange: getConversionRateRage(
                variantBayesianResult?.credibilityInterval,
                noDataLabel,
                separatorLabel
            ),
            sessions: results.sessions.variants[variant.variantName],
            probabilityToBeBest: getProbabilityToBeBest(
                variantBayesianResult?.probability,
                noDataLabel
            ),
            isWinner: results.bayesianResult.suggestedWinner === variant.variantName,
            isPromoted: isPromotedVariant(experiment, variant.variantName)
        };
    }
}
