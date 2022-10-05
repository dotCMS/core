import { Injectable } from '@angular/core';
import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { LoadingState } from '@portlets/shared/models/shared-models';
import {
    DotExperiment,
    DotExperimentStatusList,
    GroupedExperimentByStatus,
    RenderedPageExperiments
} from '../models/dot-experiments.model';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { catchError, switchMap, tap, withLatestFrom } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { EMPTY, Observable, throwError } from 'rxjs';

export interface DotExperimentsState {
    pageId: string;
    pageTitle: string;
    experiments: DotExperiment[];
    experimentsFilterStatus: Array<string>;
    status: LoadingState;
}

const initialState: DotExperimentsState = {
    pageId: '',
    pageTitle: '',
    experiments: [],
    experimentsFilterStatus: [
        DotExperimentStatusList.DRAFT,
        DotExperimentStatusList.ENDED,
        DotExperimentStatusList.RUNNING,
        DotExperimentStatusList.SCHEDULED,
        DotExperimentStatusList.ARCHIVED
    ],
    status: LoadingState.INIT
};

interface VmListExperiments {
    isLoading: boolean;
    experiments: DotExperiment[];
    experimentsFiltered: { [key: string]: DotExperiment[] };
    experimentsFilterStatus: Array<string>;
}

@Injectable({ providedIn: 'root' })
export class DotExperimentsStore extends ComponentStore<DotExperimentsState> {
    // Selectors
    readonly getStatus$ = this.select((state) => state.status);
    readonly getSelectedPageTitle$ = this.select((state) => state.pageTitle);
    readonly getSelectedPageId$ = this.select((state) => state.pageId);
    readonly getExperiments$ = this.select((state) => state.experiments);
    readonly getExperimentsFilterStatusList$ = this.select(
        (state) => state.experimentsFilterStatus
    );
    readonly getExperimentsFilteredAndGroupedByStatus$ = this.select(
        ({ experiments, experimentsFilterStatus }) =>
            experiments
                .filter((experiment) => experimentsFilterStatus.includes(experiment.status))
                .reduce<GroupedExperimentByStatus>((group, experiment) => {
                    group[experiment.status] = group[experiment.status] ?? [];
                    group[experiment.status].push(experiment);

                    return group;
                }, <GroupedExperimentByStatus>{})
    );
    readonly isLoading$: Observable<boolean> = this.select(
        (state) => state.status === LoadingState.LOADING || state.status === LoadingState.INIT
    );

    //Updater
    readonly initStore = this.updater((state, renderedPage: RenderedPageExperiments) => ({
        ...state,
        pageId: renderedPage.identifier,
        pageTitle: renderedPage.title,
        status: LoadingState.INIT
    }));
    readonly setComponentStatus = this.updater((state, status: LoadingState) => ({
        ...state,
        status
    }));
    readonly setExperiments = this.updater((state, experiments: DotExperiment[]) => ({
        ...state,
        status: LoadingState.LOADED,
        experiments
    }));
    readonly setExperimentsFilterStatus = this.updater(
        (state, experimentsFilterStatus: Array<string>) => ({
            ...state,
            experimentsFilterStatus
        })
    );
    readonly deleteExperimentById = this.updater((state, experimentId: string) => ({
        ...state,
        experiments: state.experiments.filter((exp) => exp.id != experimentId)
    }));
    readonly archiveExperimentById = this.updater((state, experimentId: string) => ({
        ...state,
        experiments: state.experiments.map((exp) =>
            experimentId === exp.id ? { ...exp, status: DotExperimentStatusList.ARCHIVED } : exp
        )
    }));

    // Effects
    readonly loadExperiments = this.effect((trigger$) =>
        trigger$.pipe(
            withLatestFrom(this.getSelectedPageId$),
            switchMap(([, pageId]) => {
                return this.dotExperimentsService.get(pageId).pipe(
                    tap(() => this.setComponentStatus(LoadingState.LOADING)),
                    tapResponse(
                        (experiments) => this.setExperiments(experiments),
                        (error: HttpErrorResponse) => throwError(error),
                        () => this.setComponentStatus(LoadingState.LOADED)
                    )
                );
            })
        )
    );
    readonly deleteExperiment = this.effect((experimentId$: Observable<string>) => {
        return experimentId$.pipe(
            tap(() => this.setComponentStatus(LoadingState.LOADING)),
            switchMap((id) =>
                this.dotExperimentsService.delete(id).pipe(
                    tapResponse(
                        () => this.deleteExperimentById(id),
                        (error) => throwError(error),
                        () => this.setComponentStatus(LoadingState.LOADED)
                    ),
                    catchError(() => EMPTY)
                )
            )
        );
    });
    readonly archiveExperiment = this.effect((experimentId$: Observable<string>) => {
        return experimentId$.pipe(
            tap(() => this.setComponentStatus(LoadingState.LOADING)),
            switchMap((id) =>
                this.dotExperimentsService.archive(id).pipe(
                    tapResponse(
                        () => this.archiveExperimentById(id),
                        (error) => throwError(error),
                        () => this.setComponentStatus(LoadingState.LOADED)
                    ),
                    catchError(() => EMPTY)
                )
            )
        );
    });

    // VM's
    readonly vmShell$ = this.select(
        this.getStatus$,
        this.getSelectedPageTitle$,
        (status, currentTitlePage) => ({
            status,
            currentTitlePage
        })
    );

    readonly vmExperimentList$: Observable<VmListExperiments> = this.select(
        this.isLoading$,
        this.getExperiments$,
        this.getExperimentsFilteredAndGroupedByStatus$,
        this.getExperimentsFilterStatusList$,
        (isLoading, experiments, experimentsFiltered, experimentsFilterStatus) => ({
            isLoading,
            experiments,
            experimentsFiltered,
            experimentsFilterStatus
        })
    );

    constructor(private readonly dotExperimentsService: DotExperimentsService) {
        super(initialState);
    }
}
