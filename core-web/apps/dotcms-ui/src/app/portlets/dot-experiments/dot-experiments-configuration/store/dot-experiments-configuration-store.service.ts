import { Injectable } from '@angular/core';
import { ComponentStore, OnStoreInit, tapResponse } from '@ngrx/component-store';
import { LoadingState } from '@portlets/shared/models/shared-models';
import { ActivatedRoute } from '@angular/router';
import { DotExperiment } from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { Observable, pipe, throwError } from 'rxjs';
import { switchMap, tap, withLatestFrom } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { Title } from '@angular/platform-browser';

export interface DotExperimentsConfigurationState {
    pageId: string;
    experimentId: string;
    experiment: DotExperiment | null;
    status: LoadingState;
}

const initialState: DotExperimentsConfigurationState = {
    pageId: '',
    experimentId: '',
    experiment: null,
    status: LoadingState.LOADING
};

// Vm Interfaces
export interface VmConfigurationExperiments {
    pageId: string;
    experimentId: string;
    experiment: DotExperiment;
    isLoading: boolean;
}

@Injectable()
export class DotExperimentsConfigurationStore
    extends ComponentStore<DotExperimentsConfigurationState>
    implements OnStoreInit
{
    // Selectors
    readonly isLoading$ = this.select(({ status }) => status === LoadingState.LOADING);

    // Updaters
    readonly setComponentStatus = this.updater((state, status: LoadingState) => ({
        ...state,
        status
    }));
    readonly setExperiment = this.updater((state, experiment: DotExperiment) => ({
        ...state,
        status: LoadingState.LOADED,
        experiment: experiment
    }));

    // Effects
    readonly loadExperiment = this.effect<void>(
        pipe(
            tap(() => this.setComponentStatus(LoadingState.LOADING)),
            withLatestFrom(this.state$),
            switchMap(([, { experimentId }]) =>
                this.dotExperimentsService.getById(experimentId).pipe(
                    tapResponse(
                        (experiments) => {
                            this.setExperiment(experiments);
                            this.updateTitles(experiments);
                        },
                        (error: HttpErrorResponse) => throwError(error),
                        () => this.setComponentStatus(LoadingState.LOADED)
                    )
                )
            )
        )
    );

    readonly vm$: Observable<VmConfigurationExperiments> = this.select(
        this.state$,
        this.isLoading$,
        ({ pageId, experiment, experimentId }, isLoading) => ({
            pageId,
            experiment,
            experimentId,
            isLoading
        })
    );

    constructor(
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly route: ActivatedRoute,
        private readonly title: Title
    ) {
        super({
            ...initialState,
            experimentId: route.snapshot.params.experimentId,
            pageId: route.snapshot.params.pageId
        });
    }

    ngrxOnStoreInit() {
        this.loadExperiment();
    }

    private updateTitles(experiment: DotExperiment) {
        this.title.setTitle(`${experiment.name} - ${this.title.getTitle()}`);
    }
}
