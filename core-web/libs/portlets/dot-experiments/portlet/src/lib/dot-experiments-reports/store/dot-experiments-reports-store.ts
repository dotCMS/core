import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { ChartData } from 'chart.js';
import { forkJoin, Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { MessageService } from 'primeng/api';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    BayesianNoWinnerStatus,
    BayesianStatusResponse,
    ComponentStatus,
    daysOfTheWeek,
    DEFAULT_VARIANT_ID,
    DotExperiment,
    DotExperimentResults,
    DotExperimentStatusList,
    DotExperimentVariantDetail,
    DotResultGoal,
    DotResultVariant,
    ExperimentLineChartDatasetDefaultProperties,
    ReportSummaryLegendByBayesianStatus,
    SummaryLegend,
    Variant
} from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import {
    getBayesianVariantResult,
    getConversionRate,
    getConversionRateRage,
    getParsedChartData,
    getProbabilityToBeBest,
    getPropertyColors,
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

// ViewModel Interfaces
export interface VmReportExperiment {
    experiment: DotExperiment;
    results: DotExperimentResults;
    chartData: ChartData<'line'> | null;
    detailData: DotExperimentVariantDetail[];
    isLoading: boolean;
    hasEnoughSessions: boolean;
    status: ComponentStatus;
    showSummary: boolean;
    winnerLegendSummary: SummaryLegend;
    suggestedWinner: DotResultVariant | null;
    promotedVariant: Variant | null;
}

const NOT_ENOUGH_DATA_LABEL = 'Not enough data';

@Injectable()
export class DotExperimentsReportsStore extends ComponentStore<DotExperimentsReportsState> {
    readonly isLoading$: Observable<boolean> = this.select(
        ({ status }) => status === ComponentStatus.LOADING
    );

    readonly summaryWinnerLegend$: Observable<{ icon: string; legend: string }> = this.select(
        ({ experiment, results }) => {
            if (experiment != null && results != null) {
                return this.getSuggestedWinner(experiment, results);
            }

            return { ...ReportSummaryLegendByBayesianStatus.NO_ENOUGH_SESSIONS };
        }
    );

    readonly getSuggestedWinner$: Observable<DotResultVariant | null> = this.select(({ results }) =>
        BayesianNoWinnerStatus.includes(results?.bayesianResult.suggestedWinner)
            ? null
            : results?.goals.primary.variants[results?.bayesianResult.suggestedWinner]
    );

    readonly getPromotedVariant$: Observable<Variant | null> = this.select(({ experiment }) =>
        experiment?.trafficProportion?.variants.find(({ promoted }) => promoted)
    );

    readonly hasEnoughSessions$: Observable<boolean> = this.select(
        ({ results }) => results != null && results.sessions.total > 0
    );

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

    readonly showExperimentSummary$: Observable<boolean> = this.select(({ experiment }) =>
        Object.values([
            DotExperimentStatusList.ENDED,
            DotExperimentStatusList.RUNNING,
            DotExperimentStatusList.ARCHIVED
        ]).includes(experiment?.status)
    );

    readonly getChartData$: Observable<ChartData<'line'>> = this.select(({ results }) =>
        results
            ? {
                  labels: this.getChartLabels(results.goals.primary.variants),
                  datasets: this.getChartDatasets(results.goals.primary.variants)
              }
            : null
    );

    readonly getDetailData$: Observable<DotExperimentVariantDetail[]> = this.select(
        ({ experiment, results }) => {
            const noData = this.dotMessageService.get(NOT_ENOUGH_DATA_LABEL);

            return results
                ? Object.values(results.goals.primary.variants).map((variant) => {
                      return this.getDotExperimentVariantDetail(
                          experiment,
                          results,
                          variant,
                          noData
                      );
                  })
                : [];
        }
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
        (variant$: Observable<{ experimentId: string; variant: DotExperimentVariantDetail }>) => {
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
        this.hasEnoughSessions$,
        this.showExperimentSummary$,
        this.getChartData$,
        this.summaryWinnerLegend$,
        this.getSuggestedWinner$,
        this.getDetailData$,
        this.getPromotedVariant$,
        (
            { experiment, status, results },
            isLoading,
            hasEnoughSessions,
            showSummary,
            chartData,
            winnerLegendSummary,
            suggestedWinner,
            detailData,
            promotedVariant
        ) => ({
            experiment,
            status,
            results,
            isLoading,
            hasEnoughSessions,
            showSummary,
            chartData,
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

    private getChartDatasets(result: DotResultGoal['variants']): ChartData<'line'>['datasets'] {
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

    private getChartLabels(variants: DotResultGoal['variants']) {
        return variants[DEFAULT_VARIANT_ID].details
            ? this.addWeekdayToDateLabels(Object.keys(variants[DEFAULT_VARIANT_ID].details))
            : [];
    }

    private addWeekdayToDateLabels(labels: Array<string>): string[][] {
        return labels.map((item) => {
            const date = new Date(item).getDay();

            return [this.dotMessageService.get(daysOfTheWeek[date]), item];
        });
    }

    private getSuggestedWinner(
        experiment: DotExperiment,
        results: DotExperimentResults
    ): SummaryLegend {
        const { bayesianResult, sessions } = results;

        const hasSessions = sessions.total > 0;
        const isATieBayesianSuggestionWinner =
            bayesianResult.suggestedWinner === BayesianStatusResponse.TIE;
        const isNoneBayesianSuggestionWinner =
            bayesianResult.suggestedWinner === BayesianStatusResponse.NONE;

        if (!hasSessions || isNoneBayesianSuggestionWinner) {
            return experiment.status === DotExperimentStatusList.ENDED
                ? ReportSummaryLegendByBayesianStatus.NO_WINNER_FOUND
                : ReportSummaryLegendByBayesianStatus.NO_ENOUGH_SESSIONS;
        }

        if (isATieBayesianSuggestionWinner) {
            return { ...ReportSummaryLegendByBayesianStatus.NO_WINNER_FOUND };
        }

        return experiment.status === DotExperimentStatusList.ENDED
            ? { ...ReportSummaryLegendByBayesianStatus.WINNER }
            : { ...ReportSummaryLegendByBayesianStatus.PRELIMINARY_WINNER };
    }

    private getDotExperimentVariantDetail(
        experiment: DotExperiment,
        results: DotExperimentResults,
        variant: DotResultVariant,
        noDataLabel: string
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
                noDataLabel
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
