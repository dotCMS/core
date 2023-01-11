import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { MessageService } from 'primeng/api';

import { switchMap, tap, withLatestFrom } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotExperiment,
    ExperimentSteps,
    LoadingState,
    Status,
    StepStatus,
    TrafficProportion,
    Variant
} from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';




export interface DotExperimentsConfigurationState {
    experiment: DotExperiment | null;
    status: LoadingState;
    stepStatusSidebar: StepStatus;
}

const initialState: DotExperimentsConfigurationState = {
    experiment: null,
    status: LoadingState.LOADING,
    stepStatusSidebar: {
        status: Status.IDLE,
        isOpen: false,
        experimentStep: null
    }
};

export interface ConfigurationViewModel {
    experiment: DotExperiment;
    stepStatusSidebar: StepStatus;
    isLoading: boolean;
}

@Injectable()
export class DotExperimentsConfigurationStore extends ComponentStore<DotExperimentsConfigurationState> {
    // Selectors
    readonly isLoading$ = this.select(({ status }) => status === LoadingState.LOADING);

    // Updaters
    readonly setComponentStatus = this.updater((state, status: LoadingState) => ({
        ...state,
        status
    }));

    readonly setSidebarStatus = this.updater((state, status: Status) => ({
        ...state,
        stepStatusSidebar: {
            ...state.stepStatusSidebar,
            status
        }
    }));

    readonly closeSidebar = this.updater((state) => ({
        ...state,
        stepStatusSidebar: {
            status: Status.DONE,
            isOpen: false,
            experimentStep: null,
            error: ''
        },
        variantIdSelected: ''
    }));

    readonly openSidebar = this.updater((state, experimentStep: ExperimentSteps) => ({
        ...state,
        stepStatusSidebar: {
            status: Status.IDLE,
            isOpen: true,
            experimentStep,
            error: ''
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
    readonly addVariant = this.effect(
        (variant$: Observable<{ experimentId: string; data: Pick<DotExperiment, 'name'> }>) => {
            return variant$.pipe(
                tap(() => this.setSidebarStatus(Status.SAVING)),
                switchMap((variant) =>
                    this.dotExperimentsService.addVariant(variant.experimentId, variant.data).pipe(
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
                                this.setSidebarStatus(Status.IDLE);

                                throwError(error);
                            }
                        )
                    )
                )
            );
        }
    );

    readonly editVariant = this.effect(
        (
            variant$: Observable<{ experimentId: string; data: Pick<DotExperiment, 'name' | 'id'> }>
        ) => {
            return variant$.pipe(
                tap(() => this.setSidebarStatus(Status.SAVING)),
                switchMap((variant) =>
                    this.dotExperimentsService
                        .editVariant(variant.experimentId, variant.data.id, {
                            description: variant.data.name
                        })
                        .pipe(
                            tapResponse(
                                (experiment) => {
                                    this.messageService.add({
                                        severity: 'info',
                                        summary: this.dotMessageService.get(
                                            'experiments.configure.variant.edit.confirm-title'
                                        ),
                                        detail: this.dotMessageService.get(
                                            'experiments.configure.variant.edit.confirm-message',
                                            experiment.name
                                        )
                                    });

                                    this.setTrafficProportion(experiment.trafficProportion);
                                },
                                (error: HttpErrorResponse) => {
                                    throwError(error);
                                }
                            )
                        )
                )
            );
        }
    );

    readonly deleteVariant = this.effect((variant$: Observable<Variant>) => {
        return variant$.pipe(
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
                        (error: HttpErrorResponse) => throwError(error)
                    )
                )
            )
        );
    });

    readonly vm$: Observable<ConfigurationViewModel> = this.select(
        this.state$,
        this.isLoading$,
        ({ experiment, stepStatusSidebar }, isLoading) => ({
            experiment,
            stepStatusSidebar,
            isLoading
        })
    );

    constructor(
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotMessageService: DotMessageService,
        private readonly messageService: MessageService,
        private readonly title: Title
    ) {
        super(initialState);
    }

    private updateTabTitle(experiment: DotExperiment) {
        this.title.setTitle(`${experiment.name} - ${this.title.getTitle()}`);
    }
}
