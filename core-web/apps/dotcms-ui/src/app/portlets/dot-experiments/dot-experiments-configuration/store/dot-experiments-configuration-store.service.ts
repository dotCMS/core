import { Injectable } from '@angular/core';
import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { LoadingState, Status } from '@portlets/shared/models/shared-models';
import { ActivatedRoute } from '@angular/router';
import {
    DotExperiment,
    ExperimentSteps,
    StepStatus,
    TrafficProportion,
    Variant
} from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { Observable, throwError } from 'rxjs';
import { switchMap, tap, withLatestFrom } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { Title } from '@angular/platform-browser';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MessageService } from 'primeng/api';

export interface DotExperimentsConfigurationState {
    pageId: string;
    experiment: DotExperiment | null;
    status: LoadingState;
    isSidebarOpen: boolean;
    isSaving: boolean;
    stepStatus: StepStatus;
}

const initialState: DotExperimentsConfigurationState = {
    pageId: '',
    experiment: null,
    status: LoadingState.LOADING,
    isSidebarOpen: false,
    isSaving: false,
    stepStatus: {
        status: Status.IDLE,
        isOpenSidebar: false,
        step: null
    }
};

@Injectable()
export class DotExperimentsConfigurationStore extends ComponentStore<DotExperimentsConfigurationState> {
    experimentId: string;
    // Selectors
    readonly isLoading$ = this.select(({ status }) => status === LoadingState.LOADING);
    readonly getTrafficProportion$ = this.select(({ experiment }) => experiment.trafficProportion);

    // Updaters
    readonly setComponentStatus = this.updater((state, status: LoadingState) => ({
        ...state,
        status
    }));

    readonly savingStepStatus = this.updater((state, step: ExperimentSteps) => ({
        ...state,
        stepStatus: {
            ...state.stepStatus,
            status: Status.SAVING,
            step
        }
    }));
    readonly doneStepStatus = this.updater((state, step: ExperimentSteps) => ({
        ...state,
        stepStatus: {
            ...state.stepStatus,
            status: Status.DONE,
            step
        }
    }));

    readonly closeSidebar = this.updater((state) => ({
        ...state,
        stepStatus: {
            status: Status.DONE,
            isOpenSidebar: false,
            step: null
        }
    }));
    readonly openSidebar = this.updater((state, step: ExperimentSteps) => ({
        ...state,
        stepStatus: {
            ...state.stepStatus,
            isOpenSidebar: true,
            step
        }
    }));

    readonly setTrafficProportion = this.updater((state, trafficProportion: TrafficProportion) => ({
        ...state,
        experiment: { ...state.experiment, trafficProportion }
    }));

    // Effects
    readonly loadExperiment = this.effect((experimentId$: Observable<string>) => {
        return experimentId$.pipe(
            tap(() => this.setComponentStatus(LoadingState.LOADING)),
            switchMap((experimentId) =>
                this.dotExperimentsService.getById(experimentId).pipe(
                    tapResponse(
                        (experiment) => {
                            this.patchState({
                                experiment: experiment
                            });
                            this.updateTabTitle(experiment);
                        },
                        (error: HttpErrorResponse) => throwError(error),
                        () => this.setComponentStatus(LoadingState.LOADED)
                    )
                )
            )
        );
    });

    // Variants
    readonly addVariant = this.effect((variant$: Observable<Pick<DotExperiment, 'name'>>) => {
        return variant$.pipe(
            tap(() => this.savingStepStatus(ExperimentSteps.VARIANTS)),
            withLatestFrom(this.state$),
            switchMap(([variant, { experiment }]) =>
                this.dotExperimentsService.addVariant(experiment.id, variant).pipe(
                    tapResponse(
                        (experiment) => {
                            this.messageService.add({
                                severity: 'info',
                                summary: this.dotMessageService.get(
                                    'experiments.configure.variant.add.confirm-title'
                                ),
                                detail: this.dotMessageService.get(
                                    'experiments.configure.variant.add.confirm-message',
                                    experiment.name
                                )
                            });

                            this.setTrafficProportion(experiment.trafficProportion);
                            this.closeSidebar();
                        },
                        (error: HttpErrorResponse) => {
                            this.doneStepStatus(ExperimentSteps.VARIANTS);
                            throwError(error);
                        }
                    )
                )
            )
        );
    });

    readonly deleteVariant = this.effect((variant$: Observable<Variant>) => {
        return variant$.pipe(
            tap(() => this.savingStepStatus(ExperimentSteps.VARIANTS)),
            withLatestFrom(this.state$),
            switchMap(([variant, { experiment }]) =>
                this.dotExperimentsService.removeVariant(experiment.id, variant.id).pipe(
                    tapResponse(
                        (experiment) => {
                            this.messageService.add({
                                severity: 'info',
                                summary: this.dotMessageService.get(
                                    'experiments.configure.variant.delete.confirm-title'
                                ),
                                detail: this.dotMessageService.get(
                                    'experiments.configure.variant.delete.confirm-message',
                                    variant.name
                                )
                            });

                            this.setTrafficProportion(experiment.trafficProportion);
                        },
                        (error: HttpErrorResponse) => throwError(error),
                        () => this.doneStepStatus(ExperimentSteps.VARIANTS)
                    )
                )
            )
        );
    });

    readonly vm$: Observable<{
        pageId: string;
        experiment: DotExperiment;
        stepStatus: StepStatus;
        isLoading: boolean;
    }> = this.select(
        this.state$,
        this.isLoading$,
        ({ pageId, experiment, stepStatus }, isLoading) => ({
            pageId,
            experiment,
            stepStatus,
            isLoading
        })
    );

    constructor(
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotMessageService: DotMessageService,
        private readonly messageService: MessageService,
        private readonly route: ActivatedRoute,
        private readonly title: Title
    ) {
        super({ ...initialState, pageId: route.snapshot.params.pageId });
        this.loadExperiment(route.snapshot.params.experimentId);
    }

    private updateTabTitle(experiment: DotExperiment) {
        this.title.setTitle(`${experiment.name} - ${this.title.getTitle()}`);
    }
}
