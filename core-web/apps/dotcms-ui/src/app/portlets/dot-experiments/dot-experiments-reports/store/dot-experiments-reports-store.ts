import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { ChartData } from 'chart.js';
import { forkJoin, Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    daysOfTheWeek,
    DEFAULT_VARIANT_ID,
    DotExperiment,
    DotExperimentResults,
    DotExperimentStatusList,
    DotResultDate,
    DotResultGoal,
    DotResultSimpleVariant,
    DotResultVariant,
    ExperimentChartDatasetColorsVariants,
    ExperimentLineChartDatasetDefaultProperties,
    LineChartColorsProperties,
    TrafficProportion
} from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

export interface DotExperimentsReportsState {
    experiment: DotExperiment | null;
    status: ComponentStatus;
    results: DotExperimentResults | null;
    variantResults: DotResultSimpleVariant[] | null;
}

const initialState: DotExperimentsReportsState = {
    experiment: null,
    status: ComponentStatus.INIT,
    results: null,
    variantResults: null
};

// ViewModel Interfaces
export interface VmReportExperiment {
    isLoading: boolean;
    experiment: DotExperiment;
    status: ComponentStatus;
    showSummary: boolean;
    results: DotExperimentResults;
    variantResults: DotResultSimpleVariant[] | null;
    chartData: ChartData<'line'> | null;
}

@Injectable()
export class DotExperimentsReportsStore extends ComponentStore<DotExperimentsReportsState> {
    readonly isLoading$: Observable<boolean> = this.select(
        ({ status }) => status === ComponentStatus.LOADING
    );

    readonly setComponentStatus = this.updater(
        (state: DotExperimentsReportsState, status: ComponentStatus) => ({
            ...state,
            status
        })
    );

    readonly showExperimentSummary$: Observable<boolean> = this.select(({ experiment }) =>
        Object.values([
            DotExperimentStatusList.ENDED,
            DotExperimentStatusList.RUNNING,
            DotExperimentStatusList.ARCHIVED
        ]).includes(experiment?.status)
    );

    readonly getChartData$: Observable<ChartData<'line'>> = this.select(({ experiment, results }) =>
        experiment && results
            ? {
                  labels: this.getChartLabels(results.goals.primary.variants),
                  datasets: this.getChartDatasets(results.goals.primary.variants, experiment)
              }
            : null
    );

    readonly loadExperimentAndResults = this.effect((experimentId$: Observable<string>) => {
        return experimentId$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.LOADING)),
            switchMap((experimentId) =>
                forkJoin({
                    experiment: this.dotExperimentsService.getById(experimentId),
                    results: this.dotExperimentsService.getResults(experimentId)
                }).pipe(
                    tapResponse(
                        ({ experiment, results }) => {
                            this.patchState({
                                experiment: experiment,
                                results: results,
                                variantResults: this.reduceVariantsData(
                                    results.goals.primary.variants,
                                    experiment
                                )
                            });
                            this.updateTabTitle(experiment);
                        },
                        (error: HttpErrorResponse) => this.dotHttpErrorManagerService.handle(error),
                        () => this.setComponentStatus(ComponentStatus.IDLE)
                    )
                )
            )
        );
    });

    readonly promoteVariant = this.effect((variant$: Observable<string>) => {
        return variant$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.LOADING)),
            switchMap((variant) =>
                this.dotExperimentsService.promoteVariant(variant).pipe(
                    tapResponse(
                        (_experiment) => {
                            //TODO: Update the experiment & other props in the store
                            // currently the enpoint is not returning the experiment
                        },
                        (error: HttpErrorResponse) => this.dotHttpErrorManagerService.handle(error),
                        () => this.setComponentStatus(ComponentStatus.IDLE)
                    )
                )
            )
        );
    });

    readonly vm$: Observable<VmReportExperiment> = this.select(
        this.state$,
        this.isLoading$,
        this.showExperimentSummary$,
        this.getChartData$,
        ({ experiment, status, results, variantResults }, isLoading, showSummary, chartData) => ({
            experiment,
            status,
            isLoading,
            showSummary,
            results,
            variantResults,
            chartData
        })
    );

    constructor(
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotHttpErrorManagerService: DotHttpErrorManagerService,
        private readonly dotMessageService: DotMessageService,
        private readonly title: Title
    ) {
        super(initialState);
    }

    private updateTabTitle(experiment: DotExperiment) {
        this.title.setTitle(`${experiment.name} - ${this.title.getTitle()}`);
    }

    /**
     * Convert the variant object to array limited with variantName and uniqueBySession
     * @param {Record<string, DotResultVariant>} variants
     * @param experiment
     * @returns {DotResultSimpleVariant[]}
     * @memberof DotExperimentsReportsStore
     */
    private reduceVariantsData(
        variants: Record<string, DotResultVariant>,
        experiment: DotExperiment
    ): DotResultSimpleVariant[] {
        return Object.values(variants).map(({ variantName, uniqueBySession }) => ({
            id: variantName,
            name: experiment.trafficProportion.variants.find((variant) => variant.id == variantName)
                .name,
            uniqueBySession
        }));
    }

    /**
     * Extract the labels from the variant default variant
     * @param variants
     * @private
     * @returns {string[]}
     * @memberof DotExperimentsReportsStore
     */
    private getChartLabels(variants: DotResultGoal['variants']) {
        return variants[DEFAULT_VARIANT_ID].details
            ? this.addWeekdayToDateLabels(Object.keys(variants[DEFAULT_VARIANT_ID].details))
            : [];
    }

    private addWeekdayToDateLabels(labels: Array<string>) {
        return labels.map((item) => {
            const date = new Date(item).getDay();

            return [this.dotMessageService.get(daysOfTheWeek[date]), item];
        });
    }

    /**
     * Generate the chart datasets using results from the experiment
     * @param result
     * @param experiment
     * @private
     * @returns {ChartData<"line">["datasets"]}
     * @memberof DotExperimentsReportsStore
     */
    private getChartDatasets(
        result: DotResultGoal['variants'],
        experiment: DotExperiment
    ): ChartData<'line'>['datasets'] {
        const { trafficProportion } = experiment;

        let colorIndex = 0;

        return Object.values(result).map((value) => {
            const { details, variantName } = value;

            return {
                label: this.getLabelName(trafficProportion, variantName),
                data: this.getParsedChartData(details),
                ...this.getPropertyColors(colorIndex++),
                ...ExperimentLineChartDatasetDefaultProperties
            };
        });
    }

    /**
     * This function returns an array of numbers from multiBySession property
     * @private
     *
     * @param {Record<string, DotResultDate>} data
     * @returns {number[]}
     * @memberof DotExperimentsReportsStore
     */
    private getParsedChartData(data: Record<string, DotResultDate>): number[] {
        return Object.values(data).map((day) => day.multiBySession);
    }

    /**
     * This function returns the color properties of the variant
     * @private
     *
     * @param {number} index
     * @returns {LineChartColorsProperties}
     * @memberof DotExperimentsReportsStore
     */
    private getPropertyColors(index: number): LineChartColorsProperties {
        return ExperimentChartDatasetColorsVariants[index];
    }

    //Todo: Remove this when the endpoint sends the name set by the user
    private getLabelName(trafficProportion: TrafficProportion, variantId: string) {
        return trafficProportion.variants.find((variant) => variant.id == variantId).name;
    }
}
