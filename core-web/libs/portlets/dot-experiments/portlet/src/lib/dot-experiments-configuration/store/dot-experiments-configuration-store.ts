import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';

import { switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    ComponentStatus,
    ConditionDefaultByTypeOfGoal,
    CONFIGURATION_CONFIRM_DIALOG_KEY,
    DotExperiment,
    DotExperimentStatus,
    ExperimentSteps,
    Goal,
    Goals,
    GoalsLevels,
    RangeOfDateAndTime,
    StepStatus,
    TrafficProportion,
    Variant
} from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';
import { DotEnvironment } from '@models/dot-environment/dot-environment';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import {
    checkIfExperimentDescriptionIsSaving,
    processExperimentConfigProps
} from '../../shared/dot-experiment.utils';

export interface DotExperimentsConfigurationState {
    experiment: DotExperiment;
    status: ComponentStatus;
    stepStatusSidebar: StepStatus;
    configProps: Record<string, string>;
    hasEnterpriseLicense: boolean;
    addToBundleContentId: string;
    pushPublishEnvironments: DotEnvironment[];
}

const initialState: DotExperimentsConfigurationState = {
    experiment: undefined,
    status: ComponentStatus.LOADING,
    stepStatusSidebar: {
        status: ComponentStatus.IDLE,
        isOpen: false,
        experimentStep: null
    },
    configProps: null,
    hasEnterpriseLicense: false,
    addToBundleContentId: null,
    pushPublishEnvironments: null
};

export interface ConfigurationViewModel {
    experiment: DotExperiment;
    stepStatusSidebar: StepStatus;
    isLoading: boolean;
    isExperimentADraft: boolean;
    disabledStartExperiment: boolean;
    showExperimentSummary: boolean;
    experimentStatus: DotExperimentStatus;
    isSaving: boolean;
    isDescriptionSaving: boolean;
    menuItems: MenuItem[];
    addToBundleContentId: string;
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
        ({ experiment }) => experiment?.status === DotExperimentStatus.DRAFT
    );
    readonly disabledStartExperiment$: Observable<boolean> = this.select(({ experiment }) =>
        this.disableStartExperiment(experiment)
    );

    readonly getExperimentStatus$: Observable<DotExperimentStatus> = this.select(
        ({ experiment }) => experiment?.status
    );

    readonly showExperimentSummary$: Observable<boolean> = this.select(({ experiment }) =>
        Object.values([
            DotExperimentStatus.ENDED,
            DotExperimentStatus.RUNNING,
            DotExperimentStatus.ARCHIVED
        ]).includes(experiment?.status)
    );

    readonly variantsStatus$ = this.select(this.state$, ({ stepStatusSidebar }) =>
        stepStatusSidebar.experimentStep === ExperimentSteps.VARIANTS ? stepStatusSidebar : null
    );

    readonly getIsDescriptionSaving$: Observable<boolean> = this.select(
        this.state$,
        ({ stepStatusSidebar }) => checkIfExperimentDescriptionIsSaving(stepStatusSidebar)
    );

    readonly getMenuItems$: Observable<MenuItem[]> = this.select(
        this.state$,
        ({ experiment, hasEnterpriseLicense, pushPublishEnvironments }) =>
            this.getMenuItems(experiment, hasEnterpriseLicense, pushPublishEnvironments)
    );

    // Goals Step //
    readonly goals$: Observable<Goals> = this.select(({ experiment }) => {
        return experiment.goals
            ? {
                  ...experiment.goals,
                  primary: {
                      ...experiment.goals.primary,
                      ...this.removeDefaultGoalCondition(experiment.goals.primary)
                  }
              }
            : null;
    });
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

    readonly schedulingBoundaries$: Observable<Record<string, number>> = this.select(
        ({ configProps }) => processExperimentConfigProps(configProps)
    );

    //Traffic Step
    readonly trafficProportion$: Observable<TrafficProportion> = this.select(({ experiment }) =>
        experiment.trafficProportion ? experiment.trafficProportion : null
    );
    readonly trafficAllocation$: Observable<number> = this.select(({ experiment }) =>
        experiment.trafficAllocation ? experiment.trafficAllocation : null
    );

    readonly trafficLoadStatus$ = this.select(this.state$, ({ stepStatusSidebar }) =>
        stepStatusSidebar.experimentStep === ExperimentSteps.TRAFFIC_LOAD ? stepStatusSidebar : null
    );

    readonly trafficSplitStatus$ = this.select(this.state$, ({ stepStatusSidebar }) =>
        stepStatusSidebar.experimentStep === ExperimentSteps.TRAFFICS_SPLIT
            ? stepStatusSidebar
            : null
    );

    // Updaters
    readonly setExperiment = this.updater((state, experiment: DotExperiment) => ({
        ...state,
        experiment,
        status: ComponentStatus.IDLE
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

    readonly showAddToBundle = this.updater((state, addToBundleContentId: string) => ({
        ...state,
        addToBundleContentId
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
                                    response.status === DotExperimentStatus.RUNNING
                                        ? 'experiments.action.start.confirm-title'
                                        : 'experiments.action.scheduled.confirm-title'
                                ),
                                detail: this.dotMessageService.get(
                                    response.status === DotExperimentStatus.RUNNING
                                        ? 'experiments.action.start.confirm-message'
                                        : 'experiments.action.scheduled.confirm-message',
                                    experiment.name
                                )
                            });
                            this.setExperiment(response);
                        },
                        (response: HttpErrorResponse) => {
                            this.setComponentStatus(ComponentStatus.IDLE);
                            const { error } = response;

                            return this.dotHttpErrorManagerService.handle({
                                ...response,
                                error: {
                                    ...error,
                                    header: error.header
                                        ? error.header
                                        : this.dotMessageService.get(
                                              'dot.common.http.error.400.experiment.run-scheduling-error.header'
                                          ),
                                    message: error.message
                                }
                            });
                        }
                    )
                )
            )
        );
    });

    readonly stopExperiment = this.effect((experiment$: Observable<DotExperiment>) => {
        return experiment$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.SAVING)),
            switchMap((experiment) =>
                this.dotExperimentsService.stop(experiment.id).pipe(
                    tapResponse(
                        (response) => {
                            this.messageService.add({
                                severity: 'info',
                                summary: this.dotMessageService.get(
                                    'experiments.action.stop.confirm-title'
                                ),
                                detail: this.dotMessageService.get(
                                    'experiments.action.stop.confirm-message',
                                    experiment.name
                                )
                            });
                            this.setExperiment(response);
                        },
                        (error: HttpErrorResponse) => this.dotHttpErrorManagerService.handle(error),
                        () => this.setComponentStatus(ComponentStatus.IDLE)
                    )
                )
            )
        );
    });

    readonly cancelSchedule = this.effect((experiment$: Observable<DotExperiment>) => {
        return experiment$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.SAVING)),
            switchMap((experiment) =>
                this.dotExperimentsService.cancelSchedule(experiment.id).pipe(
                    tapResponse(
                        (response) => {
                            this.messageService.add({
                                severity: 'info',
                                summary: this.dotMessageService.get(
                                    'experiments.notification.cancel.schedule-title'
                                ),
                                detail: this.dotMessageService.get(
                                    'experiments.notification.cancel.schedule',
                                    experiment.name
                                )
                            });
                            this.setExperiment(response);
                        },
                        (error: HttpErrorResponse) => this.dotHttpErrorManagerService.handle(error),
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

    readonly setDescription = this.effect(
        (
            description$: Observable<{
                experiment: DotExperiment;
                data: Pick<DotExperiment, 'description'>;
            }>
        ) =>
            description$.pipe(
                tap(() =>
                    this.setSidebarStatus({
                        status: ComponentStatus.SAVING,
                        experimentStep: ExperimentSteps.EXPERIMENT_DESCRIPTION
                    })
                ),
                switchMap(({ experiment, data }) =>
                    this.dotExperimentsService.setDescription(experiment.id, data.description).pipe(
                        tapResponse(
                            (response) => {
                                this.messageService.add({
                                    severity: 'info',
                                    summary: this.dotMessageService.get(
                                        'experiments.configure.description.edit.confirm-title'
                                    ),
                                    detail: this.dotMessageService.get(
                                        'experiments.configure.description.edit.confirm-message',
                                        experiment.name
                                    )
                                });

                                this.setExperiment(response);
                                this.setSidebarStatus({
                                    status: ComponentStatus.IDLE
                                });
                            },

                            (error: HttpErrorResponse) => {
                                this.setSidebarStatus({
                                    status: ComponentStatus.IDLE
                                });
                                this.dotHttpErrorManagerService.handle(error);
                            }
                        )
                    )
                )
            )
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
                            (error: HttpErrorResponse) => {
                                this.dotHttpErrorManagerService.handle(error);
                                this.setSidebarStatus({
                                    status: ComponentStatus.IDLE
                                });
                            }
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
                        experimentStep: ExperimentSteps.TRAFFIC_LOAD
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
                                        experimentStep: ExperimentSteps.TRAFFIC_LOAD,
                                        isOpen: false
                                    });
                                },
                                (response: HttpErrorResponse) => {
                                    this.dotHttpErrorManagerService.handle(response);
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE,
                                        experimentStep: ExperimentSteps.TRAFFIC_LOAD
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
                        experimentStep: ExperimentSteps.TRAFFICS_SPLIT
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
                                        experimentStep: ExperimentSteps.TRAFFICS_SPLIT,
                                        isOpen: false
                                    });
                                },
                                (response: HttpErrorResponse) => {
                                    this.dotHttpErrorManagerService.handle(response);
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE,
                                        experimentStep: ExperimentSteps.TRAFFICS_SPLIT
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
        this.getIsDescriptionSaving$,
        this.getMenuItems$,
        (
            { experiment, stepStatusSidebar, addToBundleContentId },
            isExperimentADraft,
            isLoading,
            disabledStartExperiment,
            showExperimentSummary,
            isSaving,
            experimentStatus,
            isDescriptionSaving,
            menuItems
        ) => ({
            experiment,
            stepStatusSidebar,
            addToBundleContentId,
            isExperimentADraft,
            isLoading,
            disabledStartExperiment,
            showExperimentSummary,
            isSaving,
            experimentStatus,
            isDescriptionSaving,
            menuItems
        })
    );

    readonly variantsStepVm$: Observable<{
        experimentId: string;
        trafficProportion: TrafficProportion;
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.select(
        this.getExperimentId$,
        this.trafficProportion$,
        this.variantsStatus$,
        this.isExperimentADraft$,
        (experimentId, trafficProportion, status, isExperimentADraft) => ({
            experimentId,
            trafficProportion,
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
        schedulingBoundaries: Record<string, number>;
        isExperimentADraft: boolean;
    }> = this.select(
        this.getExperimentId$,
        this.scheduling$,
        this.schedulingStatus$,
        this.schedulingBoundaries$,
        this.isExperimentADraft$,
        (experimentId, scheduling, status, schedulingBoundaries, isExperimentADraft) => ({
            experimentId,
            scheduling,
            status,
            schedulingBoundaries,
            isExperimentADraft
        })
    );

    readonly targetStepVm$: Observable<{
        experimentId: string;
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.select(
        this.getExperimentId$,
        this.trafficLoadStatus$,
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
        this.trafficLoadStatus$,
        this.trafficSplitStatus$,
        this.isExperimentADraft$,
        (
            experimentId,
            trafficProportion,
            trafficAllocation,
            statusLoad,
            statusSplit,
            isExperimentADraft
        ) => ({
            experimentId,
            trafficProportion,
            trafficAllocation,
            status: statusSplit ? statusSplit : statusLoad,
            isExperimentADraft
        })
    );

    constructor(
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotMessageService: DotMessageService,
        private readonly dotHttpErrorManagerService: DotHttpErrorManagerService,
        private readonly messageService: MessageService,
        private readonly title: Title,
        private readonly route: ActivatedRoute,
        private readonly confirmationService: ConfirmationService,
        private readonly dotPushPublishDialogService: DotPushPublishDialogService
    ) {
        const configProps = route.snapshot.data['config'];
        const hasEnterpriseLicense = route.parent.snapshot.data['isEnterprise'];
        const pushPublishEnvironments = route.parent.snapshot.data['pushPublishEnvironments'];
        super({ ...initialState, hasEnterpriseLicense, configProps, pushPublishEnvironments });
    }

    private updateTabTitle(experiment: DotExperiment) {
        this.title.setTitle(`${experiment.name} - ${this.title.getTitle()}`);
    }

    private removeDefaultGoalCondition(goal: Goal): Goal {
        const { type, conditions } = goal;

        return {
            ...goal,
            conditions: [
                ...conditions.filter((condition) => {
                    return ConditionDefaultByTypeOfGoal[type] !== condition.parameter;
                })
            ]
        };
    }

    private disableStartExperiment(experiment: DotExperiment): boolean {
        return experiment?.trafficProportion.variants.length < 2 || !experiment?.goals;
    }

    private getMenuItems(
        experiment: DotExperiment,
        hasEnterpriseLicense: boolean,
        pushPublishEnvironments: DotEnvironment[]
    ): MenuItem[] {
        return [
            // Start experiment
            {
                label: this.setStartLabel(experiment),
                visible: experiment?.status === DotExperimentStatus.DRAFT,
                disabled: this.disableStartExperiment(experiment),
                command: () => this.startExperiment(experiment)
            },
            // End experiment
            {
                label: this.dotMessageService.get('experiments.action.end-experiment'),
                visible: experiment?.status === DotExperimentStatus.RUNNING,
                disabled: this.disableStartExperiment(experiment),
                command: () => {
                    this.confirmationService.confirm({
                        key: CONFIGURATION_CONFIRM_DIALOG_KEY,
                        header: this.dotMessageService.get('experiments.action.end-experiment'),
                        message: this.dotMessageService.get(
                            'experiments.action.stop.delete-confirm'
                        ),
                        acceptLabel: this.dotMessageService.get('stop'),
                        rejectLabel: this.dotMessageService.get('dot.common.dialog.reject'),
                        rejectButtonStyleClass: 'p-button-secondary',
                        accept: () => {
                            this.stopExperiment(experiment);
                        }
                    });
                }
            },
            // Schedule experiment
            {
                label: this.dotMessageService.get('experiments.configure.scheduling.cancel'),
                visible: experiment?.status === DotExperimentStatus.SCHEDULED,
                command: () => {
                    this.confirmationService.confirm({
                        key: CONFIGURATION_CONFIRM_DIALOG_KEY,
                        header: this.dotMessageService.get(
                            'experiments.configure.scheduling.cancel'
                        ),
                        message: this.dotMessageService.get(
                            'experiments.action.cancel.schedule-confirm'
                        ),
                        acceptLabel: this.dotMessageService.get('dot.common.dialog.accept'),
                        rejectLabel: this.dotMessageService.get('dot.common.dialog.reject'),
                        rejectButtonStyleClass: 'p-button-secondary',
                        accept: () => {
                            this.cancelSchedule(experiment);
                        }
                    });
                }
            },
            // Push Publish
            {
                label: this.dotMessageService.get('contenttypes.content.push_publish'),
                visible: hasEnterpriseLicense && !!pushPublishEnvironments.length,
                command: () =>
                    this.dotPushPublishDialogService.open({
                        assetIdentifier: experiment.identifier,
                        title: this.dotMessageService.get('contenttypes.content.push_publish')
                    })
            },
            // Add To bundle
            {
                label: this.dotMessageService.get('contenttypes.content.add_to_bundle'),
                visible: hasEnterpriseLicense,
                command: () => this.showAddToBundle(experiment.identifier)
            }
        ];
    }

    private setStartLabel(experiment: DotExperiment): string {
        const { scheduling } = experiment ? experiment : { scheduling: null };
        const schedulingLabel =
            scheduling === null || Object.values(experiment.scheduling).includes(null)
                ? this.dotMessageService.get('experiments.action.start-experiment')
                : this.dotMessageService.get('experiments.action.schedule-experiment');

        return schedulingLabel;
    }
}
