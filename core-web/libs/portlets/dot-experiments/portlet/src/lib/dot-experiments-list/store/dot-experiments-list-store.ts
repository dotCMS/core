import { ComponentStore, OnStateInit, tapResponse } from '@ngrx/component-store';
import { EMPTY, Observable, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotExperiment,
    DotExperimentStatusList,
    GroupedExperimentByStatus,
    SidebarStatus
} from '@dotcms/dotcms-models';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsService } from '../../../../../data-access/src/lib/services/dot-experiments.service';
import { DotExperimentsStore } from '../../dot-experiments-shell/store/dot-experiments.store';

export interface DotExperimentsState {
    experiments: DotExperiment[];
    filterStatus: Array<string>;
    status: ComponentStatus;
    sidebar: SidebarStatus;
}

const initialState: DotExperimentsState = {
    experiments: [],
    filterStatus: [
        DotExperimentStatusList.RUNNING,
        DotExperimentStatusList.SCHEDULED,
        DotExperimentStatusList.DRAFT,
        DotExperimentStatusList.ENDED,
        DotExperimentStatusList.ARCHIVED
    ],
    status: ComponentStatus.INIT,
    sidebar: {
        status: ComponentStatus.IDLE,
        isOpen: false
    }
};

// Vm Interfaces
export interface VmListExperiments {
    isLoading: boolean;
    experiments: DotExperiment[];
    experimentsFiltered: GroupedExperimentByStatus[];
    filterStatus: Array<string>;
    sidebar: SidebarStatus;
    pageId: string;
    pageTitle: string;
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
    // Selectors
    readonly getExperimentsFilteredAndGroupedByStatus$: Observable<GroupedExperimentByStatus[]> =
        this.select(({ experiments, filterStatus }) => {
            const grouped: GroupedExperimentByStatus[] = [];

            if (experiments.length) {
                Object.keys(DotExperimentStatusList).forEach((key) =>
                    grouped.push({ status: DotExperimentStatusList[key], experiments: [] })
                );

                experiments
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
        });

    readonly isLoading$: Observable<boolean> = this.select(
        (state) => state.status === ComponentStatus.LOADING || state.status === ComponentStatus.INIT
    );
    readonly isSidebarSaving$: Observable<boolean> = this.select(
        (state) =>
            state.sidebar.status === ComponentStatus.SAVING ||
            state.sidebar.status === ComponentStatus.INIT
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
            experimentId === exp.id ? { ...exp, status: DotExperimentStatusList.ARCHIVED } : exp
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
        (
            { experiments, filterStatus, sidebar },
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
            pageTitle
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
        private readonly dotHttpErrorManagerService: DotHttpErrorManagerService
    ) {
        super({
            ...initialState
        });
    }

    ngrxOnStateInit(): void {
        const pageId = this.route.snapshot.params['pageId'];
        this.loadExperiments(pageId);
    }
}
