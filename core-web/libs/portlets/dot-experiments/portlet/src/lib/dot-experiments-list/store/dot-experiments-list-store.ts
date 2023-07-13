import { ComponentStore, OnStateInit, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    AllowedActionsByExperimentStatus,
    ComponentStatus,
    DotExperiment,
    DotExperimentStatus,
    DotExperimentsWithActions,
    GroupedExperimentByStatus,
    SidebarStatus
} from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';
import { DotEnvironment } from '@models/dot-environment/dot-environment';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsStore } from '../../dot-experiments-shell/store/dot-experiments.store';

export interface DotExperimentsState {
    experiments: DotExperiment[];
    filterStatus: Array<string>;
    status: ComponentStatus;
    sidebar: SidebarStatus;
    hasEnterpriseLicense: boolean;
    addToBundleContentId: string;
    pushPublishEnvironments: DotEnvironment[];
}

const initialState: DotExperimentsState = {
    experiments: [],
    filterStatus: [
        DotExperimentStatus.RUNNING,
        DotExperimentStatus.SCHEDULED,
        DotExperimentStatus.DRAFT,
        DotExperimentStatus.ENDED,
        DotExperimentStatus.ARCHIVED
    ],
    status: ComponentStatus.INIT,
    sidebar: {
        status: ComponentStatus.IDLE,
        isOpen: false
    },
    hasEnterpriseLicense: false,
    addToBundleContentId: null,
    pushPublishEnvironments: null
};

// Vm Interfaces
export interface VmListExperiments {
    experiments: DotExperiment[];
    isLoading: boolean;
    experimentsFiltered: GroupedExperimentByStatus[];
    filterStatus: Array<string>;
    sidebar: SidebarStatus;
    pageId: string;
    pageTitle: string;
    addToBundleContentId: string;
}

export interface VmCreateExperiments {
    pageId: string;
    sidebar: {
        status: ComponentStatus;
        isOpen: boolean;
    };
    isSaving: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class DotExperimentsListStore
    extends ComponentStore<DotExperimentsState>
    implements OnStateInit
{
    readonly isLoading$: Observable<boolean> = this.select(
        (state) => state.status === ComponentStatus.LOADING || state.status === ComponentStatus.INIT
    );
    readonly isSidebarSaving$: Observable<boolean> = this.select(
        (state) =>
            state.sidebar.status === ComponentStatus.SAVING ||
            state.sidebar.status === ComponentStatus.INIT
    );
    readonly getExperimentsWithActions$: Observable<DotExperimentsWithActions[]> = this.select(
        ({ experiments, hasEnterpriseLicense, pushPublishEnvironments }) => {
            return experiments.map((experiment) => ({
                ...experiment,
                actionsItemsMenu: this.getActionMenuItemsByExperiment(
                    experiment,
                    hasEnterpriseLicense,
                    pushPublishEnvironments
                )
            }));
        }
    );
    readonly getExperimentsFilteredAndGroupedByStatus$: Observable<GroupedExperimentByStatus[]> =
        this.select(
            this.state$,
            this.getExperimentsWithActions$,
            ({ filterStatus }, experimentsWithActions) => {
                const grouped: GroupedExperimentByStatus[] = [];

                if (experimentsWithActions.length) {
                    Object.keys(DotExperimentStatus).forEach((key) =>
                        grouped.push({ status: DotExperimentStatus[key], experiments: [] })
                    );

                    experimentsWithActions
                        .filter((experiment) => filterStatus.includes(experiment.status))
                        .forEach((experiment) => {
                            const statusGroup = grouped.find(
                                (item) => item.status === experiment.status
                            );

                            if (statusGroup) {
                                statusGroup.experiments.push(experiment);
                            } else {
                                grouped.push({
                                    status: experiment.status,
                                    experiments: [experiment]
                                });
                            }
                        });
                }

                return grouped.filter((item) => !!item.experiments.length);
            }
        );
    //Updater
    readonly initStore = this.updater((state) => ({
        ...state,
        status: ComponentStatus.INIT
    }));

    readonly setComponentStatus = this.updater((state, status: ComponentStatus) => ({
        ...state,
        status
    }));
    readonly setSidebarStatus = this.updater((state, sidebarStatus: SidebarStatus) => ({
        ...state,
        sidebar: {
            ...state.sidebar,
            ...sidebarStatus
        }
    }));

    readonly setExperiments = this.updater((state, experiments: DotExperiment[]) => ({
        ...state,
        status: ComponentStatus.LOADED,
        experiments
    }));

    readonly addExperiment = this.updater((state, experiment: DotExperiment) => ({
        ...state,
        experiments: [...state.experiments, experiment],
        sidebar: {
            status: ComponentStatus.IDLE,
            isOpen: false
        }
    }));

    readonly setFilterStatus = this.updater((state, filterStatus: Array<string>) => ({
        ...state,
        filterStatus
    }));

    readonly deleteExperimentById = this.updater((state, experimentId: string) => ({
        ...state,
        experiments: state.experiments.filter((exp) => exp.id != experimentId)
    }));

    readonly archiveExperimentById = this.updater((state, experimentId: string) => ({
        ...state,
        experiments: state.experiments.map((exp) =>
            experimentId === exp.id ? { ...exp, status: DotExperimentStatus.ARCHIVED } : exp
        )
    }));

    readonly openSidebar = this.updater((state) => ({
        ...state,
        sidebar: {
            status: ComponentStatus.IDLE,
            isOpen: true
        }
    }));
    readonly closeSidebar = this.updater((state) => ({
        ...state,
        sidebar: {
            status: ComponentStatus.IDLE,
            isOpen: false
        }
    }));

    readonly showAddToBundle = this.updater((state, addToBundleContentId: string) => ({
        ...state,
        addToBundleContentId
    }));

    readonly loadExperiments = this.effect((pageId$: Observable<string>) => {
        return pageId$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.LOADING)),
            switchMap((pageId) =>
                this.dotExperimentsService.getAll(pageId).pipe(
                    tapResponse(
                        (experiments) => this.setExperiments(experiments),
                        (error: HttpErrorResponse) => throwError(error),
                        () => this.setComponentStatus(ComponentStatus.LOADED)
                    )
                )
            )
        );
    });

    readonly addExperiments = this.effect(
        (experiment$: Observable<Pick<DotExperiment, 'pageId' | 'name' | 'description'>>) => {
            return experiment$.pipe(
                tap(() => this.setSidebarStatus({ status: ComponentStatus.SAVING, isOpen: true })),
                switchMap((experiment) =>
                    this.dotExperimentsService.add(experiment).pipe(
                        tapResponse(
                            (experiment) => {
                                this.messageService.add({
                                    severity: 'info',
                                    summary: this.dotMessageService.get(
                                        'experiments.action.add.confirm-title'
                                    ),
                                    detail: this.dotMessageService.get(
                                        'experiments.action.add.confirm-message',
                                        experiment.name
                                    )
                                });
                                this.addExperiment(experiment);

                                this.router.navigate(
                                    [
                                        '/edit-page/experiments/',
                                        experiment.pageId,
                                        experiment.id,
                                        'configuration'
                                    ],
                                    {
                                        queryParamsHandling: 'preserve'
                                    }
                                );
                            },
                            (error: HttpErrorResponse) => {
                                this.setSidebarStatus({
                                    status: ComponentStatus.IDLE,
                                    isOpen: true
                                });
                                this.dotHttpErrorManagerService.handle(error);
                            }
                        )
                    )
                )
            );
        }
    );

    readonly deleteExperiment = this.effect((experiment$: Observable<DotExperiment>) => {
        return experiment$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.LOADING)),
            switchMap((experiment) =>
                this.dotExperimentsService.delete(experiment.id).pipe(
                    tapResponse(
                        () => {
                            this.messageService.add({
                                severity: 'info',
                                summary: this.dotMessageService.get(
                                    'experiments.action.delete.confirm-title'
                                ),
                                detail: this.dotMessageService.get(
                                    'experiments.action.delete.confirm-message',
                                    experiment.name
                                )
                            });
                            this.deleteExperimentById(experiment.id);
                        },
                        (error) => throwError(error),
                        () => this.setComponentStatus(ComponentStatus.LOADED)
                    ),
                    catchError(() => EMPTY)
                )
            )
        );
    });

    readonly archiveExperiment = this.effect((experiment$: Observable<DotExperiment>) => {
        return experiment$.pipe(
            tap(() => this.setComponentStatus(ComponentStatus.LOADING)),
            switchMap((experiment) =>
                this.dotExperimentsService.archive(experiment.id).pipe(
                    tapResponse(
                        () => {
                            this.messageService.add({
                                severity: 'info',
                                summary: this.dotMessageService.get(
                                    'experiments.action.archive.confirm-title'
                                ),
                                detail: this.dotMessageService.get(
                                    'experiments.action.archive.confirm-message',
                                    experiment.name
                                )
                            });
                            this.archiveExperimentById(experiment.id);
                        },
                        (error) => throwError(error),
                        () => this.setComponentStatus(ComponentStatus.LOADED)
                    ),
                    catchError(() => EMPTY)
                )
            )
        );
    });

    readonly vm$: Observable<VmListExperiments> = this.select(
        this.state$,
        this.isLoading$,
        this.getExperimentsFilteredAndGroupedByStatus$,
        this.dotExperimentsStore.getPageId$,
        this.dotExperimentsStore.getPageTitle$,
        this.getExperimentsWithActions$,
        (
            { experiments, filterStatus, sidebar, addToBundleContentId },
            isLoading,
            experimentsFiltered,
            pageId,
            pageTitle
        ) => ({
            experiments,
            filterStatus,
            sidebar,
            isLoading,
            experimentsFiltered,
            pageId,
            pageTitle,
            addToBundleContentId
        })
    );

    readonly createVm$: Observable<VmCreateExperiments> = this.select(
        this.state$,
        this.isSidebarSaving$,
        this.dotExperimentsStore.getPageId$,
        ({ sidebar }, isSaving, pageId) => ({
            pageId: pageId,
            sidebar,
            isSaving
        })
    );

    constructor(
        private readonly dotExperimentsStore: DotExperimentsStore,
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotMessageService: DotMessageService,
        private readonly messageService: MessageService,
        private readonly route: ActivatedRoute,
        private readonly router: Router,
        private readonly dotHttpErrorManagerService: DotHttpErrorManagerService,
        private readonly confirmationService: ConfirmationService,
        private readonly dotPushPublishDialogService: DotPushPublishDialogService
    ) {
        const hasEnterpriseLicense = route.parent.snapshot.data['isEnterprise'];
        const pushPublishEnvironments = route.parent.snapshot.data['pushPublishEnvironments'];
        super({
            ...initialState,
            hasEnterpriseLicense,
            pushPublishEnvironments
        });
    }

    ngrxOnStateInit(): void {
        const pageId = this.route.snapshot.params['pageId'];
        this.loadExperiments(pageId);
    }

    private getActionMenuItemsByExperiment(
        experiment: DotExperiment,
        hasEnterpriseLicense: boolean,
        pushPublishEnvironments: DotEnvironment[]
    ): MenuItem[] {
        return [
            // Delete Action
            {
                id: 'dot-experiments-delete',
                label: this.dotMessageService.get('experiments.action.delete'),
                visible: AllowedActionsByExperimentStatus['delete'].includes(experiment.status),
                command: () =>
                    this.confirmationService.confirm({
                        header: this.dotMessageService.get('experiments.action.delete'),
                        message: this.dotMessageService.get(
                            'experiments.action.delete.confirm-question',
                            experiment.name
                        ),
                        rejectButtonStyleClass: 'p-button-secondary',
                        rejectLabel: this.dotMessageService.get('experiments.action.cancel'),
                        acceptLabel: this.dotMessageService.get('experiments.action.delete'),
                        accept: () => this.deleteExperiment(experiment),
                        key: 'positionDialog'
                    })
            },
            // Go to Configuration Action
            {
                id: 'dot-experiments-go-to-configuration',
                label: this.dotMessageService.get('experiments.action.configuration'),
                visible: AllowedActionsByExperimentStatus['configuration'].includes(
                    experiment.status
                ),
                routerLink: [
                    '/edit-page/experiments/',
                    experiment.pageId,
                    experiment.id,
                    'configuration'
                ],
                queryParamsHandling: 'merge',
                queryParams: {
                    mode: null,
                    variantName: null,
                    experimentId: null
                }
            },
            // Archive Action
            {
                id: 'dot-experiments-archive',
                label: this.dotMessageService.get('experiments.action.archive'),
                visible: AllowedActionsByExperimentStatus['archive'].includes(experiment.status),
                command: () => this.archiveExperiment(experiment)
            },

            // Push Publish Action
            {
                label: this.dotMessageService.get('contenttypes.content.push_publish'),
                visible: hasEnterpriseLicense && !!pushPublishEnvironments.length,
                command: () =>
                    this.dotPushPublishDialogService.open({
                        assetIdentifier: experiment.identifier,
                        title: this.dotMessageService.get('contenttypes.content.push_publish')
                    })
            },

            // Add To Bundle Action
            {
                id: 'dot-experiments-add-to-bundle',
                label: this.dotMessageService.get('contenttypes.content.add_to_bundle'),
                visible: hasEnterpriseLicense,
                command: () => this.showAddToBundle(experiment.identifier)
            }
        ];
    }
}
