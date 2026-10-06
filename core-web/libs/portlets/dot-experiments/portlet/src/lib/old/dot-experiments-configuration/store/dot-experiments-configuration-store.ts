import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';

import { switchMap, tap } from 'rxjs/operators';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    AllowedConditionOperatorsByTypeOfGoal,
    ComponentStatus,
    CONFIGURATION_CONFIRM_DIALOG_KEY,
    DotEnvironment,
    DotExperiment,
    DotExperimentStatus,
    DotPageRenderState,
    DotPageState,
    EXP_CONFIG_ERROR_LABEL_CANT_EDIT,
    EXP_CONFIG_ERROR_LABEL_PAGE_BLOCKED,
    ExperimentSteps,
    Goal,
    Goals,
    GoalsLevels,
    RangeOfDateAndTime,
    StepStatus,
    TrafficProportion,
    Variant
} from '@dotcms/dotcms-models';

import {
    checkIfExperimentDescriptionIsSaving,
    processExperimentConfigProps
} from '../../shared/dot-experiment.utils';

export interface DotExperimentsConfigurationState {
    /** Undefined until `loadExperiment` resolves; `status` is `LOADING` for that window. */
    experiment: DotExperiment | undefined;
    status: ComponentStatus;
    stepStatusSidebar: StepStatus;
    /** Null until the config props are fetched. */
    configProps: Record<string, string | boolean> | null;
    hasEnterpriseLicense: boolean;
    /** Null except while the Add-to-Bundle dialog is open for a specific experiment. */
    addToBundleContentId: string | null;
    /** Null until the route snapshot supplies them, and on a non-Enterprise licence. */
    pushPublishEnvironments: DotEnvironment[] | null;
    /** Null until resolved off the parent route. */
    dotPageRenderState: DotPageRenderState | null;
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
    pushPublishEnvironments: null,
    dotPageRenderState: null
};

export interface ConfigurationViewModel {
    /** Undefined while the experiment is still loading; `isLoading` covers that window. */
    experiment: DotExperiment | undefined;
    stepStatusSidebar: StepStatus;
    isLoading: boolean;
    isExperimentADraft: boolean;
    disabledStartExperiment: boolean;
    showExperimentSummary: boolean;
    /** Undefined while the experiment is still loading. */
    experimentStatus: DotExperimentStatus | undefined;
    isSaving: boolean;
    isDescriptionSaving: boolean;
    menuItems: MenuItem[];
    addToBundleContentId: string | null;
    disabledTooltipLabel: string | null;
}

export interface ConfigurationVariantStepViewModel {
    experimentId: string;
    /** Null before the experiment resolves — see `trafficProportion$`. */
    trafficProportion: TrafficProportion | null;
    status: StepStatus | null;
    isExperimentADraft: boolean;
    /** Undefined until the page render state is resolved off the parent route. */
    canLockPage: boolean | undefined;
    pageSate: DotPageState | undefined;
    disabledTooltipLabel: string | null;
}

export interface ConfigurationTrafficStepViewModel {
    experimentId: string;
    trafficProportion: TrafficProportion | null;
    trafficAllocation: number | null;
    status: StepStatus | null;
    isExperimentADraft: boolean;
    disabledTooltipLabel: string | null;
}

@Injectable()
export class DotExperimentsConfigurationStore extends ComponentStore<DotExperimentsConfigurationState> {
    private readonly dotExperimentsService = inject(DotExperimentsService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly messageService = inject(MessageService);
    private readonly title = inject(Title);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotPushPublishDialogService = inject(DotPushPublishDialogService);

    // Selectors
    readonly isLoading$: Observable<boolean> = this.select(
        ({ status }) => status === ComponentStatus.LOADING
    );
    readonly isSaving$: Observable<boolean> = this.select(
        ({ status }) => status === ComponentStatus.SAVING
    );
    /** Empty string before the experiment resolves — `isExperimentADraft$` below already reads it
     * with `?.` for the same reason. */
    readonly getExperimentId$: Observable<string> = this.select(
        ({ experiment }) => experiment?.id ?? ''
    );

    readonly isExperimentADraft$: Observable<boolean> = this.select(
        ({ experiment }) => experiment?.status === DotExperimentStatus.DRAFT
    );
    readonly disabledStartExperiment$: Observable<boolean> = this.select(({ experiment }) =>
        this.disableStartExperiment(experiment)
    );

    readonly getExperimentStatus$: Observable<DotExperimentStatus | undefined> = this.select(
        ({ experiment }) => experiment?.status
    );

    readonly showExperimentSummary$: Observable<boolean> = this.select(({ experiment }) =>
        Object.values([
            DotExperimentStatus.ENDED,
            DotExperimentStatus.RUNNING,
            DotExperimentStatus.ARCHIVED
            // `as` on the array, not on the value: an unloaded experiment has no status and simply
            // is not one of the three, which is the answer `includes` already gave.
        ] as (DotExperimentStatus | undefined)[]).includes(experiment?.status)
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

    readonly disabledTooltipLabel$: Observable<string | null> = this.select(
        this.state$,
        ({ experiment, dotPageRenderState }) =>
            this.getDisabledTooltipLabel(experiment, dotPageRenderState)
    );

    // Goals Step //
    readonly goals$: Observable<Goals | null> = this.select(({ experiment }) => {
        return experiment?.goals
            ? {
                  ...experiment.goals,
                  primary: {
                      ...experiment.goals.primary,
                      ...this.filterConditionsByGoal(experiment.goals.primary)
                  }
              }
            : null;
    });
    readonly goalsStatus$ = this.select(this.state$, ({ stepStatusSidebar }) =>
        stepStatusSidebar.experimentStep === ExperimentSteps.GOAL ? stepStatusSidebar : null
    );

    // Scheduling Step //
    readonly scheduling$: Observable<RangeOfDateAndTime | null> = this.select(({ experiment }) =>
        experiment?.scheduling ? experiment.scheduling : null
    );
    readonly schedulingStatus$ = this.select(this.state$, ({ stepStatusSidebar }) =>
        stepStatusSidebar.experimentStep === ExperimentSteps.SCHEDULING ? stepStatusSidebar : null
    );

    readonly schedulingBoundaries$: Observable<Record<string, number>> = this.select(
        ({ configProps }) => processExperimentConfigProps(configProps)
    );

    //Traffic Step
    readonly trafficProportion$: Observable<TrafficProportion | null> = this.select(
        ({ experiment }) => (experiment?.trafficProportion ? experiment.trafficProportion : null)
    );
    readonly trafficAllocation$: Observable<number | null> = this.select(({ experiment }) =>
        experiment?.trafficAllocation ? experiment.trafficAllocation : null
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
        // Guarded rather than spread blind: `{ ...undefined }` would build a partial
        // `DotExperiment`. Every caller runs from an effect that has just loaded one.
        experiment: state.experiment ? { ...state.experiment, trafficProportion } : state.experiment
    }));

    readonly setGoals = this.updater((state, goals: Goals | null) => ({
        ...state,
        // Guarded rather than spread blind: `{ ...undefined }` would build a partial
        // `DotExperiment`. Every caller runs from an effect that has just loaded one.
        experiment: state.experiment ? { ...state.experiment, goals } : state.experiment
    }));

    readonly setScheduling = this.updater((state, scheduling: RangeOfDateAndTime | null) => ({
        ...state,
        // Guarded rather than spread blind: `{ ...undefined }` would build a partial
        // `DotExperiment`. Every caller runs from an effect that has just loaded one.
        experiment: state.experiment ? { ...state.experiment, scheduling } : state.experiment
    }));

    readonly setTrafficAllocation = this.updater((state, trafficAllocation: number) => ({
        ...state,
        // Guarded rather than spread blind: `{ ...undefined }` would build a partial
        // `DotExperiment`. Every caller runs from an effect that has just loaded one.
        experiment: state.experiment ? { ...state.experiment, trafficAllocation } : state.experiment
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
                    tapResponse({
                        next: (experiment) => {
                            this.patchState({
                                experiment: experiment
                            });
                            this.updateTabTitle(experiment);
                        },
                        error: (error: HttpErrorResponse) =>
                            this.dotHttpErrorManagerService.handle(error),
                        complete: () => this.setComponentStatus(ComponentStatus.IDLE)
                    })
                )
            )
        );
    });
    readonly startExperiment = this.effect((experiment$: Observable<DotExperiment>) => {
        return experiment$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.SAVING)),
            switchMap((experiment) =>
                this.dotExperimentsService.start(experiment.id).pipe(
                    tapResponse({
                        next: (response) => {
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
                        error: (response: HttpErrorResponse) => {
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
                    })
                )
            )
        );
    });

    readonly stopExperiment = this.effect((experiment$: Observable<DotExperiment>) => {
        return experiment$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.SAVING)),
            switchMap((experiment) =>
                this.dotExperimentsService.stop(experiment.id).pipe(
                    tapResponse({
                        next: (response) => {
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
                        error: (error: HttpErrorResponse) =>
                            this.dotHttpErrorManagerService.handle(error),
                        complete: () => this.setComponentStatus(ComponentStatus.IDLE)
                    })
                )
            )
        );
    });

    readonly cancelSchedule = this.effect((experiment$: Observable<DotExperiment>) => {
        return experiment$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.SAVING)),
            switchMap((experiment) => {
                return this.dotExperimentsService.cancelSchedule(experiment.id).pipe(
                    tapResponse({
                        next: (response) => {
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
                        error: (error: HttpErrorResponse) =>
                            this.dotHttpErrorManagerService.handle(error),
                        complete: () => this.setComponentStatus(ComponentStatus.IDLE)
                    })
                );
            })
        );
    });

    readonly abortExperiment = this.effect((experiment$: Observable<DotExperiment>) => {
        return experiment$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.SAVING)),
            switchMap((experiment) => {
                return this.dotExperimentsService.cancelSchedule(experiment.id).pipe(
                    tapResponse({
                        next: (response) => {
                            this.messageService.add({
                                severity: 'info',
                                summary: this.dotMessageService.get(
                                    'experiments.notification.abort.title'
                                ),
                                detail: this.dotMessageService.get(
                                    'experiments.notification.abort',
                                    experiment.name
                                )
                            });
                            this.setExperiment(response);
                        },
                        error: (error: HttpErrorResponse) =>
                            this.dotHttpErrorManagerService.handle(error),
                        complete: () => this.setComponentStatus(ComponentStatus.IDLE)
                    })
                );
            })
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
                        tapResponse({
                            next: (experiment) => {
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
                            error: (error: HttpErrorResponse) => {
                                this.setSidebarStatus({
                                    status: ComponentStatus.IDLE
                                });
                                this.dotHttpErrorManagerService.handle(error);
                            }
                        })
                    )
                )
            );
        }
    );

    readonly editVariant = this.effect(
        (
            variant$: Observable<{
                experimentId: string;
                data: Pick<DotExperiment, 'name' | 'id'>;
            }>
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
                            tapResponse({
                                next: (experiment) => {
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
                                error: (error: HttpErrorResponse) => {
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE
                                    });
                                    this.dotHttpErrorManagerService.handle(error);
                                }
                            })
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
                        tapResponse({
                            next: (response) => {
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
                            error: (error: HttpErrorResponse) => {
                                this.setSidebarStatus({
                                    status: ComponentStatus.IDLE
                                });
                                this.dotHttpErrorManagerService.handle(error);
                            }
                        })
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
                            tapResponse({
                                next: (experiment) => {
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
                                error: (error: HttpErrorResponse) =>
                                    this.dotHttpErrorManagerService.handle(error)
                            })
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
                        tapResponse({
                            next: (experiment) => {
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
                            error: (error: HttpErrorResponse) => {
                                this.dotHttpErrorManagerService.handle(error);
                                this.setSidebarStatus({
                                    status: ComponentStatus.IDLE
                                });
                            }
                        })
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
                            tapResponse({
                                next: (experiment) => {
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
                                error: (error: HttpErrorResponse) =>
                                    this.dotHttpErrorManagerService.handle(error)
                            })
                        )
                )
            );
        }
    );

    readonly setSelectedScheduling = this.effect(
        (
            setScheduling$: Observable<{
                scheduling: RangeOfDateAndTime;
                experimentId: string;
            }>
        ) => {
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
                            tapResponse({
                                next: (experiment) => {
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
                                error: (response: HttpErrorResponse) => {
                                    this.dotHttpErrorManagerService.handle(response);
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE,
                                        experimentStep: ExperimentSteps.SCHEDULING
                                    });
                                }
                            })
                        );
                })
            );
        }
    );

    readonly setSelectedAllocation = this.effect(
        (
            trafficAllocation$: Observable<{
                trafficAllocation: number;
                experimentId: string;
            }>
        ) => {
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
                            tapResponse({
                                next: (experiment) => {
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
                                error: (response: HttpErrorResponse) => {
                                    this.dotHttpErrorManagerService.handle(response);
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE,
                                        experimentStep: ExperimentSteps.TRAFFIC_LOAD
                                    });
                                }
                            })
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
                            tapResponse({
                                next: (experiment) => {
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
                                error: (response: HttpErrorResponse) => {
                                    this.dotHttpErrorManagerService.handle(response);
                                    this.setSidebarStatus({
                                        status: ComponentStatus.IDLE,
                                        experimentStep: ExperimentSteps.TRAFFICS_SPLIT
                                    });
                                }
                            })
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
        this.disabledTooltipLabel$,
        (
            { experiment, stepStatusSidebar, addToBundleContentId },
            isExperimentADraft,
            isLoading,
            disabledStartExperiment,
            showExperimentSummary,
            isSaving,
            experimentStatus,
            isDescriptionSaving,
            menuItems,
            disabledTooltipLabel
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
            menuItems,
            disabledTooltipLabel
        })
    );

    readonly variantsStepVm$: Observable<ConfigurationVariantStepViewModel> = this.select(
        this.state$,
        this.getExperimentId$,
        this.trafficProportion$,
        this.variantsStatus$,
        this.isExperimentADraft$,
        this.disabledTooltipLabel$,
        (
            { dotPageRenderState },
            experimentId,
            trafficProportion,
            status,
            isExperimentADraft,
            disabledTooltipLabel
        ) => ({
            experimentId,
            trafficProportion,
            status,
            isExperimentADraft,
            canLockPage: dotPageRenderState?.page?.canLock,
            pageSate: dotPageRenderState?.state,
            disabledTooltipLabel
        })
    );

    readonly goalsStepVm$: Observable<{
        experimentId: string;
        goals: Goals | null;
        status: StepStatus | null;
        isExperimentADraft: boolean;
        disabledTooltipLabel: string | null;
    }> = this.select(
        this.getExperimentId$,
        this.goals$,
        this.goalsStatus$,
        this.isExperimentADraft$,
        this.disabledTooltipLabel$,
        (experimentId, goals, status, isExperimentADraft, disabledTooltipLabel) => ({
            experimentId,
            goals,
            status,
            isExperimentADraft,
            disabledTooltipLabel
        })
    );

    readonly schedulingStepVm$: Observable<{
        experimentId: string;
        scheduling: RangeOfDateAndTime | null;
        status: StepStatus | null;
        schedulingBoundaries: Record<string, number>;
        isExperimentADraft: boolean;
        disabledTooltipLabel: string | null;
    }> = this.select(
        this.getExperimentId$,
        this.scheduling$,
        this.schedulingStatus$,
        this.schedulingBoundaries$,
        this.isExperimentADraft$,
        this.disabledTooltipLabel$,
        (
            experimentId,
            scheduling,
            status,
            schedulingBoundaries,
            isExperimentADraft,
            disabledTooltipLabel
        ) => ({
            experimentId,
            scheduling,
            status,
            schedulingBoundaries,
            isExperimentADraft,
            disabledTooltipLabel
        })
    );

    readonly targetStepVm$: Observable<{
        experimentId: string;
        status: StepStatus | null;
        isExperimentADraft: boolean;
        disabledTooltipLabel: string | null;
    }> = this.select(
        this.getExperimentId$,
        this.trafficLoadStatus$,
        this.isExperimentADraft$,
        this.disabledTooltipLabel$,
        (experimentId, status, isExperimentADraft, disabledTooltipLabel) => ({
            experimentId,
            status,
            isExperimentADraft,
            disabledTooltipLabel
        })
    );

    readonly trafficStepVm$: Observable<ConfigurationTrafficStepViewModel> = this.select(
        this.getExperimentId$,
        this.trafficProportion$,
        this.trafficAllocation$,
        this.trafficLoadStatus$,
        this.trafficSplitStatus$,
        this.isExperimentADraft$,
        this.disabledTooltipLabel$,
        (
            experimentId,
            trafficProportion,
            trafficAllocation,
            statusLoad,
            statusSplit,
            isExperimentADraft,
            disabledTooltipLabel
        ) => ({
            experimentId,
            trafficProportion,
            trafficAllocation,
            status: statusSplit ? statusSplit : statusLoad,
            isExperimentADraft,
            disabledTooltipLabel
        })
    );

    constructor() {
        const route = inject(ActivatedRoute);
        // `parent` is null at the root of a routing tree. These come from ancestors this portlet is
        // always mounted under, so the chain resolves in practice — the `?.` states what the type
        // system can actually see, and the state members below all admit the absent case.
        const dotPageRenderState = route.parent?.parent?.snapshot.data['content'];
        const configProps = route.snapshot.data['config'];
        const hasEnterpriseLicense = route.parent?.snapshot.data['isEnterprise'];
        const pushPublishEnvironments = route.parent?.snapshot.data['pushPublishEnvironments'];

        super({
            ...initialState,
            hasEnterpriseLicense,
            configProps,
            pushPublishEnvironments,
            dotPageRenderState
        });
    }

    private updateTabTitle(experiment: DotExperiment | undefined) {
        this.title.setTitle(`${experiment?.name} - ${this.title.getTitle()}`);
    }

    private filterConditionsByGoal(goal: Goal): Goal {
        const { type, conditions } = goal;

        return {
            ...goal,
            conditions: [
                ...conditions.filter((condition) => {
                    return AllowedConditionOperatorsByTypeOfGoal[type] === condition.parameter;
                })
            ]
        };
    }

    private disableStartExperiment(experiment: DotExperiment | undefined): boolean {
        // The whole chain, not just the first hop: `trafficProportion` is present on a loaded
        // experiment but the `?.` above only guards `experiment` itself. An unloaded experiment
        // cannot be started, which is what the `< 2` comparison against undefined already yielded.
        return (experiment?.trafficProportion?.variants?.length ?? 0) < 2 || !experiment?.goals;
    }

    private getDisabledTooltipLabel(
        experiment: DotExperiment | undefined,
        dotPageRenderState: DotPageRenderState | null
    ): string | null {
        return experiment?.status !== DotExperimentStatus.DRAFT
            ? EXP_CONFIG_ERROR_LABEL_CANT_EDIT
            : dotPageRenderState?.state?.lockedByAnotherUser
              ? EXP_CONFIG_ERROR_LABEL_PAGE_BLOCKED
              : null;
    }

    private getMenuItems(
        experiment: DotExperiment | undefined,
        hasEnterpriseLicense: boolean,
        pushPublishEnvironments: DotEnvironment[] | null
    ): MenuItem[] {
        if (!experiment) {
            // Nothing actionable without an experiment. Every entry below is gated on its status or
            // reads its id, so this is what the `visible: experiment?.status === …` checks already
            // resolved to — except for the two enterprise entries, which were visible and would
            // have thrown on click.
            return [];
        }

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
                    this.sendConfirmation({
                        header: 'experiments.action.end-experiment',
                        message: 'experiments.action.stop.delete-confirm',
                        acceptLabel: 'experiments.action.end',
                        rejectLabel: 'dot.common.dialog.reject',
                        fn: () => this.stopExperiment(experiment)
                    });
                }
            },
            // Abort experiment
            {
                label: this.dotMessageService.get('experiments.action.abort.experiment'),
                visible: experiment?.status === DotExperimentStatus.RUNNING,
                command: () => {
                    this.sendConfirmation({
                        header: 'experiments.action.abort.experiment',
                        message: 'experiments.action.abort.confirm.message',
                        acceptLabel: 'experiments.action.abort.experiment',
                        rejectLabel: 'experiments.action.cancel',
                        fn: () => this.abortExperiment(experiment)
                    });
                }
            },
            // Cancel Schedule
            {
                label: this.dotMessageService.get('experiments.configure.scheduling.cancel'),
                visible: experiment?.status === DotExperimentStatus.SCHEDULED,
                command: () => {
                    this.sendConfirmation({
                        header: 'experiments.configure.scheduling.cancel',
                        message: 'experiments.action.cancel.schedule-confirm',
                        acceptLabel: 'dot.common.dialog.accept',
                        rejectLabel: 'dot.common.dialog.reject',
                        fn: () => this.cancelSchedule(experiment)
                    });
                }
            },
            // Push Publish
            {
                label: this.dotMessageService.get('contenttypes.content.push_publish'),
                visible: hasEnterpriseLicense && !!pushPublishEnvironments?.length,
                command: () =>
                    this.dotPushPublishDialogService.open({
                        assetIdentifier: experiment.id,
                        title: this.dotMessageService.get('contenttypes.content.push_publish')
                    })
            },
            // Add To bundle
            {
                label: this.dotMessageService.get('contenttypes.content.add_to_bundle'),
                visible: hasEnterpriseLicense,
                command: () => this.showAddToBundle(experiment.id)
            }
        ];
    }

    private setStartLabel(experiment: DotExperiment | undefined): string {
        const { scheduling } = experiment ? experiment : { scheduling: null };

        // Reads `scheduling`, not `experiment.scheduling`: the destructure above exists precisely to
        // supply the null fallback, and the second read went around it.
        return scheduling === null || Object.values(scheduling).includes(null)
            ? this.dotMessageService.get('experiments.action.start-experiment')
            : this.dotMessageService.get('experiments.action.schedule-experiment');
    }

    private sendConfirmation(data: {
        header: string;
        message: string;
        acceptLabel: string;
        rejectLabel: string;
        fn: () => void;
    }): void {
        this.confirmationService.confirm({
            key: CONFIGURATION_CONFIRM_DIALOG_KEY,
            header: this.dotMessageService.get(data.header),
            message: this.dotMessageService.get(data.message),
            acceptLabel: this.dotMessageService.get(data.acceptLabel),
            rejectLabel: this.dotMessageService.get(data.rejectLabel),
            rejectButtonStyleClass: 'p-button-outlined',
            accept: data.fn
        });
    }
}
