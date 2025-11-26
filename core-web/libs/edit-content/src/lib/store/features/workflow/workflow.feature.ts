import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, effect, inject, untracked } from '@angular/core';
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
import { ComponentStatus, DotCMSWorkflow, DotContentletDepths } from '@dotcms/dotcms-models';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { parseCurrentActions } from '../../../utils/workflows.utils';
import { EditContentState } from '../../edit-content.store';

/**
 * Signal store feature that manages the workflow component state in the edit content sidebar
 * Handles loading states, error handling, and workflow status for the current contentlet
 *
 * @returns
 */
export function withWorkflow() {
    return signalStoreFeature(
        { state: type<EditContentState>() },
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
            showWorkflowActions: computed(() => {
                const currentSchemeId = store.currentSchemeId();
                const currentActions = store.currentContentActions()[currentSchemeId] || [];

                return currentActions.length > 0;
            }),

            /**
             * Computed property that determines if the reset action should be shown.
             *
             * @returns {boolean} True if the reset action should be shown, false otherwise.
             */
            resetActionState: computed(() => !store.currentStep()),

            /**
             * Computed property that determines if the workflow selection warning should be shown.
             * Shows warning when content is new AND no workflow scheme has been selected yet.
             *
             * @returns {boolean} True if warning should be shown, false otherwise
             */
            showSelectWorkflowWarning: computed(() => {
                const currentSchemeId = store.currentSchemeId();

                return !currentSchemeId;
            }),

            /**
             * Gets the first workflow action that has reset capability and is shown on EDITING.
             *
             * @returns {DotCMSWorkflowAction | undefined} First workflow action with reset capability shown on EDITING
             */
            getResetWorkflowAction: computed(() => {
                const currentActions = store.currentContentActions()[store.currentSchemeId()] || [];

                return (
                    currentActions.find(
                        (action) =>
                            action.hasResetActionlet &&
                            action.showOn?.includes(DotRenderMode.EDITING)
                    ) || undefined
                );
            }),

            /**
             * Computed property that retrieves the actions for the current workflow scheme.
             *
             * @returns {DotCMSWorkflowAction[]} The actions for the current workflow scheme.
             */
            getActions: computed(() => {
                const currentSchemeId = store.currentSchemeId();
                const currentContentActions = store.currentContentActions();

                return currentSchemeId ? currentContentActions[currentSchemeId] : [];
            }),

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
             * Computed property that retrieves the current step of the workflow.
             * For new content, returns the first step of the selected scheme.
             * For existing content, returns the current step from the workflow status.
             *
             * @returns {WorkflowStep} The current workflow step
             */
            getCurrentStep: computed(() => {
                const contentlet = store.contentlet();

                if (!contentlet?.inode) {
                    // New content - get first step of selected scheme
                    const schemes = store.schemes();
                    const currentSchemeId = store.currentSchemeId();

                    return schemes[currentSchemeId]?.firstStep;
                }

                // Existing content - get current step from workflow status
                return store.currentStep();
            })
        })),
        withMethods(
            (
                store,
                dotEditContentService = inject(DotEditContentService),
                workflowActionService = inject(DotWorkflowsActionsService),
                workflowActionsFireService = inject(DotWorkflowActionsFireService),
                dotHttpErrorManagerService = inject(DotHttpErrorManagerService),
                messageService = inject(MessageService),
                dotMessageService = inject(DotMessageService),
                dotWorkflowService = inject(DotWorkflowService),
                router = inject(Router)
            ) => ({
                /**
                 * Sets the selected workflow scheme ID and updates related state in the store.
                 * For new content, it sets the current scheme ID, parses and sets the workflow actions,
                 * and sets the first step of the selected scheme.
                 * For existing content, it only updates the current scheme ID and first step.
                 *
                 * @param {string} currentSchemeId - The ID of the workflow scheme to be selected
                 */
                setSelectedWorkflow: (currentSchemeId: string) => {
                    const schemes = store.schemes();
                    const currentScheme = schemes[currentSchemeId];
                    const actions = currentScheme.actions;
                    const isNew = !store.contentlet()?.inode;

                    if (isNew) {
                        patchState(store, {
                            currentSchemeId,
                            currentContentActions: parseCurrentActions(actions),
                            currentStep: currentScheme.firstStep
                        });
                    } else {
                        // Existing content
                        patchState(store, {
                            currentSchemeId,
                            currentStep: currentScheme.firstStep
                        });
                    }
                },

                /**
                 * Fires a workflow action and updates the component state accordingly.
                 *
                 * This method triggers a sequence of events to fire a workflow action
                 * and handles the response or error. If the action is successful,
                 * it updates the store with the new contentlet and actions. In route mode,
                 * it also navigates to the content view with the updated contentlet.
                 * In dialog mode, it only updates the store without navigation.
                 * In case of an error, it updates the state with an error message.
                 *
                 * @param options The options required to fire the workflow action.
                 */
                fireWorkflowAction: rxMethod<
                    DotFireActionOptions<{ [key: string]: string | object | string[] }>
                >(
                    pipe(
                        tap(() => {
                            patchState(store, { state: ComponentStatus.SAVING });
                            messageService.clear();
                            messageService.add({
                                severity: 'info',
                                icon: 'pi pi-spin pi-spinner',
                                summary: dotMessageService.get(
                                    'edit.content.processing.workflow.message.title'
                                ),
                                detail: dotMessageService.get(
                                    'edit.content.processing.workflow.message'
                                )
                            });
                        }),
                        switchMap((options) => {
                            const currentContentlet = store.contentlet();

                            return workflowActionsFireService.fireTo(options).pipe(
                                switchMap((updatedContentlet) => {
                                    // Use current contentlet if response is empty (reset action)
                                    // otherwise use the updated contentlet from response
                                    const contentlet =
                                        Object.keys(updatedContentlet).length === 0
                                            ? currentContentlet
                                            : updatedContentlet;

                                    const inode = contentlet.inode;

                                    // A reset action will return an empty object
                                    const isReset = Object.keys(updatedContentlet).length === 0;

                                    return forkJoin({
                                        currentContentActions: workflowActionService.getByInode(
                                            inode,
                                            DotRenderMode.EDITING
                                        ),
                                        contentlet: dotEditContentService.getContentById({
                                            id: inode,
                                            depth: DotContentletDepths.TWO
                                        }),
                                        isReset: of(isReset),
                                        // Workflow status for this inode
                                        workflowStatus: dotWorkflowService.getWorkflowStatus(inode)
                                    });
                                }),
                                tapResponse({
                                    next: ({
                                        contentlet,
                                        currentContentActions,
                                        isReset,
                                        workflowStatus
                                    }) => {
                                        // Only navigate if NOT in dialog mode and the inode has changed
                                        const isDialogMode = store.isDialogMode();
                                        if (
                                            !isDialogMode &&
                                            contentlet.inode !== currentContentlet?.inode
                                        ) {
                                            router.navigate(['/content', contentlet.inode], {
                                                replaceUrl: true,
                                                queryParamsHandling: 'preserve'
                                            });
                                        }

                                        const parsedCurrentActions =
                                            parseCurrentActions(currentContentActions);

                                        const { step } = workflowStatus;

                                        if (isReset) {
                                            patchState(store, {
                                                contentlet,
                                                currentContentActions: parsedCurrentActions,
                                                currentSchemeId:
                                                    Object.keys(store.schemes()).length > 1
                                                        ? null
                                                        : store.currentSchemeId(),
                                                initialContentletState: 'reset',
                                                state: ComponentStatus.LOADED,
                                                currentStep: null,
                                                error: null,
                                                workflowActionSuccess: contentlet
                                            });
                                        } else {
                                            patchState(store, {
                                                contentlet,
                                                currentContentActions: parsedCurrentActions,
                                                currentSchemeId: store.currentSchemeId(),
                                                state: ComponentStatus.LOADED,
                                                currentStep: step,
                                                error: null,
                                                workflowActionSuccess: contentlet
                                            });
                                        }

                                        messageService.clear();
                                        messageService.add({
                                            severity: 'success',
                                            summary: dotMessageService.get(
                                                'edit.content.success.workflow.title'
                                            ),
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
                ),

                /**
                 * Updates the current content actions by fetching the latest allowed actions for the content
                 * This is used to refresh the available workflow actions for the current contentlet
                 */
                updateCurrentContentActions: rxMethod<void>(
                    pipe(
                        switchMap(() => {
                            const contentlet = store.contentlet();
                            if (!contentlet?.inode) {
                                return [];
                            }

                            return workflowActionService
                                .getByInode(contentlet.inode, DotRenderMode.EDITING)
                                .pipe(
                                    tapResponse({
                                        next: (actions) => {
                                            const parsedCurrentActions =
                                                parseCurrentActions(actions);

                                            patchState(store, {
                                                currentContentActions: parsedCurrentActions
                                            });
                                        },
                                        error: (error: HttpErrorResponse) => {
                                            dotHttpErrorManagerService.handle(error);
                                        }
                                    })
                                );
                        })
                    )
                ),

                /**
                 * Clears the workflow action success signal.
                 * Used to reset the signal after it has been processed to prevent duplicate emissions.
                 */
                clearWorkflowActionSuccess: () => {
                    patchState(store, {
                        workflowActionSuccess: null
                    });
                }
            })
        ),
        withHooks({
            onInit(store) {
                /**
                 * Effect that updates the allowed workflow actions whenever the content changes
                 * This ensures that the available actions are always in sync with the current state of the contentlet
                 */
                effect(() => {
                    const contentlet = store.contentlet();

                    // Use untracked to prevent circular dependencies in the effect
                    untracked(() => {
                        // Only update actions if we have a contentlet with an inode
                        if (contentlet?.inode) {
                            store.updateCurrentContentActions();
                        }
                    });
                });
            }
        })
    );
}
