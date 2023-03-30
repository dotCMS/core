import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { forkJoin, Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { switchMap, tap } from 'rxjs/operators';

import {
    ComponentStatus,
    DotExperiment,
    DotExperimentResults,
    DotExperimentStatusList,
    DotResultSimpleVariant,
    DotResultVariant
} from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

export interface DotExperimentsReportsState {
    experiment: DotExperiment | null;
    status: ComponentStatus;
    results: DotExperimentResults | null;
    variantResults: DotResultSimpleVariant[] | null;
    chartResults: unknown | null;
}

const initialState: DotExperimentsReportsState = {
    experiment: null,
    status: ComponentStatus.INIT,
    results: null,
    variantResults: null,
    chartResults: null
};

// ViewModel Interfaces
export interface VmReportExperiment {
    isLoading: boolean;
    experiment: DotExperiment;
    status: ComponentStatus;
    showSummary: boolean;
    results: DotExperimentResults;
    variantResults: DotResultSimpleVariant[] | null;
    chartResults: unknown | null;
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

    readonly loadExperiment = this.effect((experimentId$: Observable<string>) => {
        return experimentId$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.LOADING)),
            switchMap((experimentId) =>
                forkJoin({
                    experiment: this.dotExperimentsService.getById(experimentId),
                    results: this.dotExperimentsService.getResults(experimentId)
                }).pipe(
                    tapResponse(
                        (data: { experiment: DotExperiment; results: DotExperimentResults }) => {
                            this.patchState({
                                experiment: data.experiment,
                                results: data.results,
                                variantResults: this.reduceVariantsData(
                                    data.results.goals.primary.variants
                                )
                            });
                            this.updateTabTitle(data.experiment);
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
                        (experiment) => {
                            this.patchState({
                                experiment: experiment
                            });
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
        (
            { experiment, status, results, variantResults, chartResults },
            isLoading,
            showSummary
        ) => ({
            experiment,
            status,
            isLoading,
            showSummary,
            results,
            variantResults,
            chartResults
        })
    );

    constructor(
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotHttpErrorManagerService: DotHttpErrorManagerService,
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
     * @returns {DotResultSimpleVariant[]}
     * @memberof DotExperimentsReportsStore
     */
    private reduceVariantsData(
        variants: Record<string, DotResultVariant>
    ): DotResultSimpleVariant[] {
        return Object.values(variants).map(({ variantName, uniqueBySession }) => ({
            variantName,
            uniqueBySession
        }));
    }
}
