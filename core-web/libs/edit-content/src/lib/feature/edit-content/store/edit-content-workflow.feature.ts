import { tapResponse } from '@ngrx/component-store';
import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotWorkflowService } from '@dotcms/data-access';
import { ComponentStatus, DotCMSWorkflow, WorkflowStep, WorkflowTask } from '@dotcms/dotcms-models';

import { EditContentState } from './edit-content.store';

interface WorkflowState {
    workflow: {
        scheme: DotCMSWorkflow | null;
        step: WorkflowStep | null;
        task: WorkflowTask | null;
        status: ComponentStatus;
        error: string | null;
    };
}

const initialState: WorkflowState = {
    workflow: {
        scheme: null,
        step: null,
        task: null,
        status: ComponentStatus.INIT,
        error: null
    }
};

/**
 * Signal store feature that manages the workflow component state in the edit content sidebar
 * Handles loading states, error handling, and workflow status for the current contentlet
 *
 * @returns
 */
export function withWorkflow() {
    return signalStoreFeature(
        {
            state: type<EditContentState>()
        },
        withState(initialState),
        withComputed(({ workflow }) => ({
            isLoadingWorkflow: computed(() => workflow.status() === ComponentStatus.LOADING)
        })),
        withMethods(
            (
                store,
                dotWorkflowService = inject(DotWorkflowService),
                dotHttpErrorManagerService = inject(DotHttpErrorManagerService)
            ) => ({
                /**
                 * Get workflow status for a new contentlet
                 * we use the content type id to get the workflow scheme
                 */
                getNewContentStatus: rxMethod<string>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                workflow: {
                                    ...store.workflow(),
                                    status: ComponentStatus.LOADING,
                                    error: null
                                }
                            })
                        ),
                        switchMap((contentTypeId: string) => {
                            return dotWorkflowService.getSchemaContentType(contentTypeId).pipe(
                                tapResponse({
                                    next: ({ contentTypeSchemes }) => {
                                        patchState(store, {
                                            workflow: {
                                                ...store.workflow(),
                                                status: ComponentStatus.LOADED,
                                                scheme: contentTypeSchemes[0]
                                            }
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        patchState(store, {
                                            workflow: {
                                                ...store.workflow(),
                                                status: ComponentStatus.ERROR,
                                                error: error.message
                                            }
                                        });
                                        dotHttpErrorManagerService.handle(error);
                                    }
                                })
                            );
                        })
                    )
                ),

                /**
                 * Get workflow status for an existing contentlet
                 * we use the inode to get the workflow status
                 */
                getWorkflowStatus: rxMethod<string>(
                    pipe(
                        tap(() =>
                            patchState(store, {
                                workflow: {
                                    ...store.workflow(),
                                    status: ComponentStatus.LOADING,
                                    error: null
                                }
                            })
                        ),
                        switchMap((inode: string) => {
                            return dotWorkflowService.getWorkflowStatus(inode).pipe(
                                tapResponse({
                                    next: (response) => {
                                        const { scheme, step, task } = response;
                                        patchState(store, {
                                            workflow: {
                                                ...store.workflow(),
                                                scheme,
                                                step,
                                                task,
                                                status: ComponentStatus.LOADED
                                            }
                                        });
                                    },

                                    error: (error: HttpErrorResponse) => {
                                        patchState(store, {
                                            workflow: {
                                                ...store.workflow(),
                                                status: ComponentStatus.ERROR,
                                                error: error.message
                                            }
                                        });
                                        dotHttpErrorManagerService.handle(error);
                                    }
                                })
                            );
                        })
                    )
                )
            })
        )
    );
}
