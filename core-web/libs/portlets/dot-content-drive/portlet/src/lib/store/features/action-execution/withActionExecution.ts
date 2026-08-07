import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotActionBulkRequestOptions } from '@dotcms/dotcms-models';

import {
    DotContentDriveActionExecution,
    DotContentDriveActionExecutionResult,
    DotContentDriveState
} from '../../../shared/models';

interface WithActionExecutionState {
    /** The action currently being applied, or `undefined` when nothing is running. */
    actionExecution?: DotContentDriveActionExecution;
    /**
     * Outcome of the last finished execution, awaiting presentation. The shell consumes this and
     * calls {@link clearActionExecutionResult}; the store never shows the toast itself.
     */
    actionExecutionResult?: DotContentDriveActionExecutionResult;
}

/**
 * Owns the firing of workflow actions over the current selection.
 *
 * **Why this lives in the store and not in the Action Center dialog.** The dialog is rendered inside
 * the shell's `@switch`, so closing it destroys the component. An execution owned by that component
 * would either be cancelled on close (if correctly tied to its lifecycle) or leak (if not) — and it
 * previously leaked, which made "closing the dialog does not abort the action" true only by accident.
 * Holding the subscription here makes surviving the close a deliberate property: the store outlives
 * every dialog, so `takeUntilDestroyed` in the dialog stays correct and nothing silently aborts.
 *
 * It also gives a reopened dialog a truthful state. Because {@link actionExecution} is store state
 * rather than a component signal, reopening mid-flight still reports the run as in progress, which is
 * what stops the same action being fired twice over the same rows.
 */
export function withActionExecution() {
    return signalStoreFeature(
        {
            state: type<DotContentDriveState>()
        },
        withState<WithActionExecutionState>({
            actionExecution: undefined,
            actionExecutionResult: undefined
        }),
        withMethods(
            (
                store,
                workflowActionsFireService = inject(DotWorkflowActionsFireService),
                httpErrorManagerService = inject(DotHttpErrorManagerService)
            ) => {
                /**
                 * Settles a finished run by publishing its result for the shell to present.
                 *
                 * Refreshing the grid is deliberately *not* done here. `loadItems` belongs to the base
                 * store's own `withMethods`, and a feature cannot reach it: the accumulated methods
                 * type at this point in the composition widens to `MethodsDictionary`, so declaring it
                 * via `methods: type<...>()` does not compile. It would also be redundant — `loadItems`
                 * already sets `LOADING` and clears the selection itself. The shell reloads when it
                 * consumes the result, which is where the rest of the post-run UI work already lives.
                 */
                const onSettled = (result: DotContentDriveActionExecutionResult): void => {
                    patchState(store, {
                        actionExecution: undefined,
                        actionExecutionResult: result
                    });
                };

                return {
                    /**
                     * Fires a quick action (publish, unpublish, archive, …) over the given inodes.
                     *
                     * Counts come from the response rather than `inodes.length`: the endpoint answers
                     * 200 with per-item failures inside, so a lock held by another user or a
                     * permission the row state could not see would otherwise read as a success.
                     */
                    executeQuickAction: (
                        actionId: string,
                        actionName: string,
                        inodes: string[]
                    ): void => {
                        if (!inodes.length || store.actionExecution()) {
                            return;
                        }

                        patchState(store, {
                            actionExecution: { actionName, total: inodes.length },
                            actionExecutionResult: undefined
                        });

                        workflowActionsFireService
                            .fireDefaultAction({ action: actionId, inodes })
                            .pipe(
                                take(1),
                                catchError((error) => {
                                    patchState(store, { actionExecution: undefined });
                                    httpErrorManagerService.handle(error);

                                    return EMPTY;
                                })
                            )
                            .subscribe((result) =>
                                onSettled({
                                    actionName,
                                    successCount: result?.summary?.successCount ?? inodes.length,
                                    skippedCount: 0,
                                    failCount: result?.summary?.failCount ?? 0
                                })
                            );
                    },

                    /**
                     * Fires the selected workflow action over the given contentlet inodes.
                     *
                     * Contentlets whose scheme does not own the action are skipped server-side and
                     * reported in `skippedCount`, so a mixed-type selection partially skips by
                     * design — the result carries that through to the toast.
                     */
                    executeWorkflowAction: (
                        workflowActionId: string,
                        actionName: string,
                        contentletIds: string[]
                    ): void => {
                        if (!contentletIds.length || store.actionExecution()) {
                            return;
                        }

                        patchState(store, {
                            actionExecution: { actionName, total: contentletIds.length },
                            actionExecutionResult: undefined
                        });

                        const request: DotActionBulkRequestOptions = {
                            workflowActionId,
                            contentletIds,
                            additionalParams: {
                                assignComment: { assign: '', comment: '' },
                                pushPublish: {},
                                additionalParamsMap: { _path_to_move: '' }
                            }
                        };

                        workflowActionsFireService
                            .bulkFire(request)
                            .pipe(
                                take(1),
                                catchError((error) => {
                                    patchState(store, { actionExecution: undefined });
                                    httpErrorManagerService.handle(error);

                                    return EMPTY;
                                })
                            )
                            .subscribe((result) =>
                                onSettled({
                                    actionName,
                                    successCount: result?.successCount ?? 0,
                                    skippedCount: result?.skippedCount ?? 0,
                                    failCount: result?.fails?.length ?? 0
                                })
                            );
                    },

                    /** Called by the shell once the result has been presented. */
                    clearActionExecutionResult: (): void => {
                        patchState(store, { actionExecutionResult: undefined });
                    }
                };
            }
        )
    );
}
