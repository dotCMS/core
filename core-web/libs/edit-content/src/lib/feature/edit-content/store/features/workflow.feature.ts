import { tapResponse } from '@ngrx/operators';
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
import { ComponentStatus, DotCMSWorkflow, WorkflowTask } from '@dotcms/dotcms-models';

import { EditContentState } from '../edit-content.store';

interface WorkflowState {
    workflow: {
        task: WorkflowTask | null;
        status: ComponentStatus;
        error: string | null;
    };
}

const initialState: WorkflowState = {
    workflow: {
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
        withComputed((store) => ({
            /**
             * Computed property that determines if the workflow component is in a loading state
             *
             * @returns {boolean} True if the workflow component is in a loading state, false otherwise
             */
            isLoadingWorkflow: computed(() => store.workflow().status === ComponentStatus.LOADING),

            /**
             * Gets the workflow scheme for the currently selected scheme ID
             *
             * @returns {DotCMSWorkflow | undefined} The workflow scheme if found, undefined otherwise
             */
            getScheme: computed<DotCMSWorkflow | undefined>(() => {
                const currentSchemeId = store.currentSchemeId();

                return currentSchemeId ? store.schemes()[currentSchemeId]?.scheme : undefined;
            })
        })),
        withMethods(
            (
                store,
                dotWorkflowService = inject(DotWorkflowService),
                dotHttpErrorManagerService = inject(DotHttpErrorManagerService)
            ) => ({
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
                                            currentSchemeId: scheme?.id,
                                            currentStep: step,
                                            lastTask: task,
                                            workflow: {
                                                ...store.workflow(),
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
                ),

                setSelectedWorkflow: (schemeId: string) => {
                    patchState(store, {
                        currentSchemeId: schemeId
                    });
                }
            })
        )
    );
}
