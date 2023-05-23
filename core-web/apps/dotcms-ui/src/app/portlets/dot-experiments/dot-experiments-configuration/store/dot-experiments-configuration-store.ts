import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';

import { switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    ConditionDefaultByTypeOfGoal,
    DotExperiment,
    DotExperimentStatusList,
    ExperimentSteps,
    Goal,
    Goals,
    GoalsLevels,
    PROP_NOT_FOUND,
    RangeOfDateAndTime,
    StepStatus,
    TIME_14_DAYS,
    TIME_90_DAYS,
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
    runExperimentBtnLabel: string;
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

    readonly getRunExperimentBtnLabel$: Observable<string> = this.select(({ experiment }) => {
        const { scheduling } = experiment ? experiment : { scheduling: null };

        return scheduling === null || Object.values(experiment.scheduling).includes(null)
            ? this.dotMessageService.get('experiments.action.start-experiment')
            : this.dotMessageService.get('experiments.action.schedule-experiment');
    });

    readonly showExperimentSummary$: Observable<boolean> = this.select(({ experiment }) =>
        Object.values([
            DotExperimentStatusList.ENDED,
            DotExperimentStatusList.RUNNING,
            DotExperimentStatusList.ARCHIVED
        ]).includes(experiment?.status)
    );

    readonly variantsStatus$ = this.select(this.state$, ({ stepStatusSidebar }) =>
        stepStatusSidebar.experimentStep === ExperimentSteps.VARIANTS ? stepStatusSidebar : null
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
        this.state$,
        () => this.processConfigProps(this.route.snapshot.data?.config)
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
                                    response.status === DotExperimentStatusList.RUNNING
                                        ? 'experiments.action.start.confirm-title'
                                        : 'experiments.action.scheduled.confirm-title'
                                ),
                                detail: this.dotMessageService.get(
                                    response.status === DotExperimentStatusList.RUNNING
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
                                    message: error.message.split('.')[0]
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
        this.getRunExperimentBtnLabel$,
        this.isLoading$,
        this.disabledStartExperiment$,
        this.showExperimentSummary$,
        this.isSaving$,
        this.getExperimentStatus$,
        (
            { experiment, stepStatusSidebar },
            isExperimentADraft,
            runExperimentBtnLabel,
            isLoading,
            disabledStartExperiment,
            showExperimentSummary,
            isSaving,
            experimentStatus
        ) => ({
            experiment,
            stepStatusSidebar,
            isExperimentADraft,
            runExperimentBtnLabel,
            isLoading,
            disabledStartExperiment,
            showExperimentSummary,
            isSaving,
            experimentStatus
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
        private readonly route: ActivatedRoute
    ) {
        super(initialState);
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

    /**
     * Process the config properties that comes form the BE as days,
     * return the object with the values in milliseconds
     * @param configProps
     *
     * @private
     */
    private processConfigProps(configProps: Record<string, string>): Record<string, number> {
        const config: Record<string, number> = {};

        config.EXPERIMENTS_MIN_DURATION =
            configProps.EXPERIMENTS_MIN_DURATION === PROP_NOT_FOUND
                ? TIME_14_DAYS
                : this.daysToMilliseconds(+configProps.EXPERIMENTS_MIN_DURATION);
        config.EXPERIMENTS_MAX_DURATION =
            configProps.EXPERIMENTS_MAX_DURATION === PROP_NOT_FOUND
                ? TIME_90_DAYS
                : this.daysToMilliseconds(+configProps.EXPERIMENTS_MAX_DURATION);

        return config;
    }

    private daysToMilliseconds(days: number): number {
        return days * 24 * 60 * 60 * 1000;
    }
}
