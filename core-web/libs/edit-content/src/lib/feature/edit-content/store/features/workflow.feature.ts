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
import { forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { MessageService, SelectItem } from 'primeng/api';

import { switchMap, tap } from 'rxjs/operators';

import {
    DotFireActionOptions,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSWorkflow,
    DotCMSWorkflowAction,
    WorkflowStep,
    WorkflowTask
} from '@dotcms/dotcms-models';

import { ContentState } from './content.feature';

import {
    getWorkflowActions,
    shouldShowWorkflowActions,
    shouldShowWorkflowWarning
} from '../../../../utils/functions.util';
import { EditContentRootState } from '../edit-content.store';

export interface WorkflowState {
    /** Current workflow scheme id */
    currentSchemeId: string | null;

    /** Actions available for the current content */
    currentContentActions: DotCMSWorkflowAction[];

    /** Current workflow step */
    currentStep: WorkflowStep | null;

    /** Last workflow task of the contentlet */
    lastTask: WorkflowTask | null;

    // Nested workflow status
    workflow: {
        status: ComponentStatus;
        error: string | null;
    };
}

export const workflowInitialState: WorkflowState = {
    currentSchemeId: null,
    currentContentActions: [],
    currentStep: null,
    lastTask: null,
    workflow: {
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
        { state: type<EditContentRootState & ContentState>() },
        withState(workflowInitialState),
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
            }),

            /**
             * Computed property that determines if workflow action buttons should be shown.
             */
            showWorkflowActions: computed(() =>
                shouldShowWorkflowActions(
                    store.schemes(),
                    store.contentlet(),
                    store.currentSchemeId()
                )
            ),

            /**
             * Computed property that determines if the workflow selection warning should be shown.
             * Shows warning when content is new AND no workflow scheme has been selected yet.
             *
             * @returns {boolean} True if warning should be shown, false otherwise
             */
            showSelectWorkflowWarning: computed(() =>
                shouldShowWorkflowWarning(
                    store.schemes(),
                    store.contentlet(),
                    store.currentSchemeId()
                )
            ),

            /**
             * Computed property that retrieves the actions for the current workflow scheme.
             *
             * @returns {DotCMSWorkflowAction[]} The actions for the current workflow scheme.
             */
            getActions: computed(() =>
                getWorkflowActions(
                    store.schemes(),
                    store.contentlet(),
                    store.currentSchemeId(),
                    store.currentContentActions()
                )
            ),

            /**
             * Computed property that transforms the workflow schemes into dropdown options
             * @returns Array of options with value (scheme id) and label (scheme name)
             */
            workflowSchemeOptions: computed<SelectItem[]>(() =>
                Object.entries(store.schemes()).map(([id, data]) => ({
                    value: id,
                    label: data.scheme.name
                }))
            ),

            /**
             * Computed property that retrieves the first step of the current workflow scheme.
             *
             * @returns {WorkflowStep} The first step of the current workflow scheme.
             */
            getFirstStep: computed(() => {
                const schemes = store.schemes();
                const currentSchemeId = store.currentSchemeId();

                return schemes[currentSchemeId]?.firstStep;
            })
        })),
        withMethods(
            (
                store,
                dotWorkflowService = inject(DotWorkflowService),
                workflowActionService = inject(DotWorkflowsActionsService),
                workflowActionsFireService = inject(DotWorkflowActionsFireService),
                dotHttpErrorManagerService = inject(DotHttpErrorManagerService),
                messageService = inject(MessageService),
                dotMessageService = inject(DotMessageService),
                router = inject(Router)
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
                                                error: 'Error getting workflow status'
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
                 * Sets the selected workflow scheme ID in the store.
                 *
                 * @param {string} schemeId - The ID of the workflow scheme to be selected.
                 */
                setSelectedWorkflow: (schemeId: string) => {
                    patchState(store, {
                        currentSchemeId: schemeId
                    });
                },

                /**
                 * Fires a workflow action and updates the component state accordingly.
                 *
                 * This method triggers a sequence of events to fire a workflow action
                 * and handles the response or error. If the action is successful,
                 * it navigates to the content view with the updated contentlet and actions.
                 * In case of an error, it updates the state with an error message.
                 *
                 * @param options The options required to fire the workflow action.
                 */
                fireWorkflowAction: rxMethod<
                    DotFireActionOptions<{ [key: string]: string | object }>
                >(
                    pipe(
                        tap(() => patchState(store, { state: ComponentStatus.SAVING })),
                        switchMap((options) => {
                            return workflowActionsFireService.fireTo(options).pipe(
                                tap((contentlet) => {
                                    if (!contentlet.inode) {
                                        router.navigate(['/c/content']);
                                    }
                                }),
                                switchMap((contentlet) => {
                                    return forkJoin({
                                        currentContentActions: workflowActionService.getByInode(
                                            contentlet.inode,
                                            DotRenderMode.EDITING
                                        ),
                                        contentlet: of(contentlet)
                                    });
                                }),
                                tapResponse({
                                    next: ({ contentlet, currentContentActions }) => {
                                        router.navigate(['/content', contentlet.inode], {
                                            replaceUrl: true,
                                            queryParamsHandling: 'preserve'
                                        });

                                        patchState(store, {
                                            contentlet,
                                            currentContentActions,
                                            state: ComponentStatus.LOADED,
                                            error: null
                                        });

                                        messageService.add({
                                            severity: 'success',
                                            summary: dotMessageService.get('success'),
                                            detail: dotMessageService.get(
                                                'edit.content.success.workflow.message'
                                            )
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        patchState(store, {
                                            state: ComponentStatus.LOADED,
                                            error: 'Error firing workflow action'
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
