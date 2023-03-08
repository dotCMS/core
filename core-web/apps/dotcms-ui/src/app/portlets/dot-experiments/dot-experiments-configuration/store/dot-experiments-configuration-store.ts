import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { MessageService } from 'primeng/api';

import { switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotExperiment,
    DotExperimentStatusList,
    ExperimentSteps,
    Goals,
    GoalsLevels,
    RangeOfDateAndTime,
    StepStatus,
    TrafficProportion,
    Variant
} from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

export interface DotExperimentsConfigurationState {
    experiment: DotExperiment;
    status: ComponentStatus;
    stepStatusSidebar: StepStatus;
}

const initialState: DotExperimentsConfigurationState = {
    experiment: undefined,
    status: ComponentStatus.LOADING,
    stepStatusSidebar: {
        status: ComponentStatus.IDLE,
        isOpen: false,
        experimentStep: null
    }
};

export interface ConfigurationViewModel {
    experiment: DotExperiment;
    stepStatusSidebar: StepStatus;
    isLoading: boolean;
    isExperimentADraft: boolean;
    disabledStartExperiment: boolean;
    showExperimentSummary: boolean;
    experimentStatus: DotExperimentStatusList;
    isSaving: boolean;
}

@Injectable()
export class DotExperimentsConfigurationStore extends ComponentStore<DotExperimentsConfigurationState> {
    // Selectors
    readonly isLoading$: Observable<boolean> = this.select(
        ({ status }) => status === ComponentStatus.LOADING
    );
    readonly isSaving$: Observable<boolean> = this.select(
        ({ status }) => status === ComponentStatus.SAVING
    );
    readonly getExperimentId$: Observable<string> = this.select(({ experiment }) => experiment.id);

    readonly isExperimentADraft$: Observable<boolean> = this.select(
        ({ experiment }) => experiment?.status === DotExperimentStatusList.DRAFT
    );
    readonly disabledStartExperiment$: Observable<boolean> = this.select(
        ({ experiment }) => experiment?.trafficProportion.variants.length < 2 || !experiment?.goals
    );

    readonly getExperimentStatus$: Observable<DotExperimentStatusList> = this.select(
        ({ experiment }) => experiment?.status
    );

    readonly showExperimentSummary$: Observable<boolean> = this.select(({ experiment }) =>
        Object.values([
            DotExperimentStatusList.ENDED,
            DotExperimentStatusList.RUNNING,
            DotExperimentStatusList.ARCHIVED
        ]).includes(experiment?.status)
    );

    // Variants Step //
    readonly variantsStatus$ = this.select(this.state$, ({ stepStatusSidebar }) =>
        stepStatusSidebar.experimentStep === ExperimentSteps.VARIANTS ? stepStatusSidebar : null
    );

    // Goals Step //
    readonly goals$: Observable<Goals> = this.select(({ experiment }) =>
        experiment.goals ? experiment.goals : null
    );
    readonly goalsStatus$ = this.select(this.state$, ({ stepStatusSidebar }) =>
        stepStatusSidebar.experimentStep === ExperimentSteps.GOAL ? stepStatusSidebar : null
    );

    // Scheduling Step //
    readonly scheduling$: Observable<RangeOfDateAndTime> = this.select(({ experiment }) =>
        experiment.scheduling ? experiment.scheduling : null
    );
    readonly schedulingStatus$ = this.select(this.state$, ({ stepStatusSidebar }) =>
        stepStatusSidebar.experimentStep === ExperimentSteps.SCHEDULING ? stepStatusSidebar : null
    );

    //Traffic Step
    readonly trafficProportion$: Observable<TrafficProportion> = this.select(({ experiment }) =>
        experiment.trafficProportion ? experiment.trafficProportion : null
    );
    readonly trafficAllocation$: Observable<number> = this.select(({ experiment }) =>
        experiment.trafficAllocation ? experiment.trafficAllocation : null
    );

    readonly trafficStatus$ = this.select(this.state$, ({ stepStatusSidebar }) =>
        stepStatusSidebar.experimentStep === ExperimentSteps.TRAFFIC ? stepStatusSidebar : null
    );

    // Updaters
    readonly setExperiment = this.updater((state, experiment: DotExperiment) => ({
        ...state,
        experiment
    }));

    readonly setComponentStatus = this.updater((state, status: ComponentStatus) => ({
        ...state,
        status
    }));

    readonly setSidebarStatus = this.updater((state, stepStatusSidebar: Partial<StepStatus>) => ({
        ...state,
        stepStatusSidebar: { ...state.stepStatusSidebar, ...stepStatusSidebar }
    }));

    readonly closeSidebar = this.updater((state) => ({
        ...state,
        stepStatusSidebar: {
            status: ComponentStatus.IDLE,
            isOpen: false,
            experimentStep: null,
            error: ''
        },
        variantIdSelected: ''
    }));

    readonly openSidebar = this.updater((state, experimentStep: ExperimentSteps) => ({
        ...state,
        stepStatusSidebar: {
            status: ComponentStatus.IDLE,
            isOpen: true,
            experimentStep,
            error: ''
        }
    }));

    readonly setTrafficProportion = this.updater((state, trafficProportion: TrafficProportion) => ({
        ...state,
        experiment: { ...state.experiment, trafficProportion }
    }));

    readonly setGoals = this.updater((state, goals: Goals) => ({
        ...state,
        experiment: { ...state.experiment, goals }
    }));

    readonly setScheduling = this.updater((state, scheduling: RangeOfDateAndTime) => ({
        ...state,
        experiment: { ...state.experiment, scheduling }
    }));

    readonly setTrafficAllocation = this.updater((state, trafficAllocation: number) => ({
        ...state,
        experiment: { ...state.experiment, trafficAllocation }
    }));

    // Effects
    readonly loadExperiment = this.effect((experimentId$: Observable<string>) => {
        return experimentId$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.LOADING)),
            switchMap((experimentId) =>
                this.dotExperimentsService.getById(experimentId).pipe(
                    tapResponse(
                        (experiment) => {
                            this.patchState({
                                experiment: experiment
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
    readonly startExperiment = this.effect((experiment$: Observable<DotExperiment>) => {
        return experiment$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.SAVING)),
            switchMap((experiment) =>
                this.dotExperimentsService.start(experiment.id).pipe(
                    tapResponse(
                        (response) => {
                            this.messageService.add({
                                severity: 'info',
                                summary: this.dotMessageService.get(
                                    'experiments.action.start.confirm-title'
                                ),
                                detail: this.dotMessageService.get(
                                    'experiments.action.start.confirm-message',
                                    experiment.name
                                )
                            });
                            this.setExperiment(response);
                        },
                        (error) => throwError(error),
                        () => this.setComponentStatus(ComponentStatus.IDLE)
                    )
                )
            )
        );
    });

    // Variants
    readonly addVariant = this.effect(
        (variant$: Observable<{ experimentId: string; name: string }>) => {
            return variant$.pipe(
                tap(() =>
                    this.setSidebarStatus({
                        status: ComponentStatus.SAVING,
                        experimentStep: ExperimentSteps.VARIANTS
                    })
                ),
                switchMap((variant) =>
                    this.dotExperimentsService.addVariant(variant.experimentId, variant.name).pipe(
                        tapResponse(
                            (experiment) => {
                                this.messageService.add({
                                    severity: 'info',
                                    summary: this.dotMessageService.get(
                                        'experiments.configure.variant.add.confirm-title'
                                    ),
                                    detail: this.dotMessageService.get(
                                        'experiments.configure.variant.add.confirm-message',
                                        variant.name
                                    )
                                });

                                this.setTrafficProportion(experiment.trafficProportion);
                                this.closeSidebar();
                            },
                            (error: HttpErrorResponse) => {
                                this.setSidebarStatus({
                                    status: ComponentStatus.IDLE
                                });
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
                tap(() =>
                    this.setSidebarStatus({
                        status: ComponentStatus.SAVING,
                        experimentStep: ExperimentSteps.VARIANTS
                    })
                ),
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
                                            variant.data.name
                                        )
                                    });

                                    this.setTrafficProportion(experiment.trafficProportion);
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE,
                                        experimentStep: null
                                    });
                                },
                                (error: HttpErrorResponse) => {
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE
                                    });
                                    throwError(error);
                                }
                            )
                        )
                )
            );
        }
    );

    readonly deleteVariant = this.effect(
        (variant$: Observable<{ experimentId: string; variant: Variant }>) => {
            return variant$.pipe(
                switchMap((selected) =>
                    this.dotExperimentsService
                        .removeVariant(selected.experimentId, selected.variant.id)
                        .pipe(
                            tapResponse(
                                (experiment) => {
                                    this.messageService.add({
                                        severity: 'info',
                                        summary: this.dotMessageService.get(
                                            'experiments.configure.variant.delete.confirm-title'
                                        ),
                                        detail: this.dotMessageService.get(
                                            'experiments.configure.variant.delete.confirm-message',
                                            selected.variant.name
                                        )
                                    });

                                    this.setTrafficProportion(experiment.trafficProportion);
                                },
                                (error: HttpErrorResponse) => throwError(error)
                            )
                        )
                )
            );
        }
    );

    // Goals
    readonly setSelectedGoal = this.effect(
        (selectedGoal$: Observable<{ experimentId: string; goals: Goals }>) => {
            return selectedGoal$.pipe(
                tap(() =>
                    this.setSidebarStatus({
                        status: ComponentStatus.SAVING,
                        experimentStep: ExperimentSteps.GOAL
                    })
                ),
                switchMap((selected) =>
                    this.dotExperimentsService.setGoal(selected.experimentId, selected.goals).pipe(
                        tapResponse(
                            (experiment) => {
                                this.messageService.add({
                                    severity: 'info',
                                    summary: this.dotMessageService.get(
                                        'experiments.configure.goals.select.confirm-title'
                                    ),
                                    detail: this.dotMessageService.get(
                                        'experiments.configure.goals.select.confirm-message'
                                    )
                                });

                                this.setGoals(experiment.goals);
                                this.setSidebarStatus({
                                    status: ComponentStatus.IDLE,
                                    experimentStep: ExperimentSteps.GOAL,
                                    isOpen: false
                                });
                            },
                            (error: HttpErrorResponse) => throwError(error)
                        )
                    )
                )
            );
        }
    );

    readonly deleteGoal = this.effect(
        (goalLevel$: Observable<{ goalLevel: GoalsLevels; experimentId: string }>) => {
            return goalLevel$.pipe(
                switchMap((selected) =>
                    this.dotExperimentsService
                        .deleteGoal(selected.experimentId, selected.goalLevel)
                        .pipe(
                            tapResponse(
                                (experiment) => {
                                    this.messageService.add({
                                        severity: 'info',
                                        summary: this.dotMessageService.get(
                                            'experiments.configure.goals.delete.confirm-title'
                                        ),
                                        detail: this.dotMessageService.get(
                                            'experiments.configure.goals.delete.confirm-message'
                                        )
                                    });

                                    this.setGoals(experiment.goals);
                                },
                                (error: HttpErrorResponse) => throwError(error)
                            )
                        )
                )
            );
        }
    );

    readonly setSelectedScheduling = this.effect(
        (setScheduling$: Observable<{ scheduling: RangeOfDateAndTime; experimentId: string }>) => {
            return setScheduling$.pipe(
                tap(() => {
                    this.setSidebarStatus({
                        status: ComponentStatus.SAVING,
                        experimentStep: ExperimentSteps.SCHEDULING
                    });
                }),
                switchMap((data) => {
                    return this.dotExperimentsService
                        .setScheduling(data.experimentId, data.scheduling)
                        .pipe(
                            tapResponse(
                                (experiment) => {
                                    this.setScheduling(experiment.scheduling);
                                    this.messageService.add({
                                        severity: 'info',
                                        summary: this.dotMessageService.get(
                                            'experiments.configure.scheduling.add.confirm.title'
                                        ),
                                        detail: this.dotMessageService.get(
                                            'experiments.configure.scheduling.add.confirm.message'
                                        )
                                    });
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE,
                                        experimentStep: ExperimentSteps.SCHEDULING,
                                        isOpen: false
                                    });
                                },
                                (response: HttpErrorResponse) => {
                                    this.dotHttpErrorManagerService.handle(response);
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE,
                                        experimentStep: ExperimentSteps.SCHEDULING
                                    });
                                }
                            )
                        );
                })
            );
        }
    );

    readonly setSelectedAllocation = this.effect(
        (trafficAllocation$: Observable<{ trafficAllocation: number; experimentId: string }>) => {
            return trafficAllocation$.pipe(
                tap(() => {
                    this.setSidebarStatus({
                        status: ComponentStatus.SAVING,
                        experimentStep: ExperimentSteps.TRAFFIC
                    });
                }),
                switchMap((data) => {
                    return this.dotExperimentsService
                        .setTrafficAllocation(data.experimentId, data.trafficAllocation)
                        .pipe(
                            tapResponse(
                                (experiment) => {
                                    this.setTrafficAllocation(experiment.trafficAllocation);
                                    this.messageService.add({
                                        severity: 'info',
                                        summary: this.dotMessageService.get(
                                            'experiments.configure.traffic.allocation.add.confirm.title'
                                        ),
                                        detail: this.dotMessageService.get(
                                            'experiments.configure.traffic.allocation.add.confirm.message'
                                        )
                                    });
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE,
                                        experimentStep: ExperimentSteps.TRAFFIC,
                                        isOpen: false
                                    });
                                },
                                (response: HttpErrorResponse) => {
                                    this.dotHttpErrorManagerService.handle(response);
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE,
                                        experimentStep: ExperimentSteps.TRAFFIC
                                    });
                                }
                            )
                        );
                })
            );
        }
    );

    readonly setSelectedTrafficProportion = this.effect(
        (
            trafficProportion$: Observable<{
                trafficProportion: TrafficProportion;
                experimentId: string;
            }>
        ) => {
            return trafficProportion$.pipe(
                tap(() => {
                    this.setSidebarStatus({
                        status: ComponentStatus.SAVING,
                        experimentStep: ExperimentSteps.TRAFFIC
                    });
                }),
                switchMap((data) => {
                    return this.dotExperimentsService
                        .setTrafficProportion(data.experimentId, data.trafficProportion)
                        .pipe(
                            tapResponse(
                                (experiment) => {
                                    this.setTrafficProportion(experiment.trafficProportion);
                                    this.messageService.add({
                                        severity: 'info',
                                        summary: this.dotMessageService.get(
                                            'experiments.configure.traffic.split.add.confirm.title'
                                        ),
                                        detail: this.dotMessageService.get(
                                            'experiments.configure.traffic.split.add.confirm.message'
                                        )
                                    });
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE,
                                        experimentStep: ExperimentSteps.TRAFFIC,
                                        isOpen: false
                                    });
                                },
                                (response: HttpErrorResponse) => {
                                    this.dotHttpErrorManagerService.handle(response);
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE,
                                        experimentStep: ExperimentSteps.TRAFFIC
                                    });
                                }
                            )
                        );
                })
            );
        }
    );

    readonly vm$: Observable<ConfigurationViewModel> = this.select(
        this.state$,
        this.isExperimentADraft$,
        this.isLoading$,
        this.disabledStartExperiment$,
        this.showExperimentSummary$,
        this.isSaving$,
        this.getExperimentStatus$,
        (
            { experiment, stepStatusSidebar },
            isExperimentADraft,
            isLoading,
            disabledStartExperiment,
            showExperimentSummary,
            isSaving,
            experimentStatus
        ) => ({
            experiment,
            stepStatusSidebar,
            isExperimentADraft,
            isLoading,
            disabledStartExperiment,
            showExperimentSummary,
            isSaving,
            experimentStatus
        })
    );

    readonly variantsStepVm$: Observable<{
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.select(
        this.variantsStatus$,
        this.isExperimentADraft$,
        (status, isExperimentADraft) => ({
            status,
            isExperimentADraft
        })
    );

    readonly goalsStepVm$: Observable<{
        experimentId: string;
        goals: Goals;
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.select(
        this.getExperimentId$,
        this.goals$,
        this.goalsStatus$,
        this.isExperimentADraft$,
        (experimentId, goals, status, isExperimentADraft) => ({
            experimentId,
            goals,
            status,
            isExperimentADraft
        })
    );

    readonly schedulingStepVm$: Observable<{
        experimentId: string;
        scheduling: RangeOfDateAndTime;
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.select(
        this.getExperimentId$,
        this.scheduling$,
        this.schedulingStatus$,
        this.isExperimentADraft$,
        (experimentId, scheduling, status, isExperimentADraft) => ({
            experimentId,
            scheduling,
            status,
            isExperimentADraft
        })
    );

    readonly targetStepVm$: Observable<{
        experimentId: string;
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.select(
        this.getExperimentId$,
        this.trafficStatus$,
        this.isExperimentADraft$,
        (experimentId, status, isExperimentADraft) => ({
            experimentId,
            status,
            isExperimentADraft
        })
    );

    readonly trafficStepVm$: Observable<{
        experimentId: string;
        trafficProportion: TrafficProportion;
        trafficAllocation: number;
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.select(
        this.getExperimentId$,
        this.trafficProportion$,
        this.trafficAllocation$,
        this.trafficStatus$,
        this.isExperimentADraft$,
        (experimentId, trafficProportion, trafficAllocation, status, isExperimentADraft) => ({
            experimentId,
            trafficProportion,
            trafficAllocation,
            status,
            isExperimentADraft
        })
    );

    constructor(
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotMessageService: DotMessageService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private readonly messageService: MessageService,
        private readonly title: Title
    ) {
        super(initialState);
    }

    private updateTabTitle(experiment: DotExperiment) {
        this.title.setTitle(`${experiment.name} - ${this.title.getTitle()}`);
    }
}
