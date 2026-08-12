import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import {
    AddToBundleService,
    DotHttpErrorManagerService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotActionBulkRequestOptions, DotBundle } from '@dotcms/dotcms-models';

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
                httpErrorManagerService = inject(DotHttpErrorManagerService),
                addToBundleService = inject(AddToBundleService)
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

                /**
                 * Abandons a run whose outcome cannot be known, routing it through the same error
                 * path as a transport failure.
                 *
                 * The multi-contentlet endpoint streams `results` and then `summary`, and the writer
                 * swallows an `IOException` mid-stream — so a 200 whose body has no `summary` is
                 * reachable. There is no honest count to report in that case: substituting
                 * `inodes.length` claims every item succeeded, and substituting `0` claims a total
                 * failure. Both invent a number the server never sent, and the first does it in the
                 * reassuring direction. Publishing no result at all leaves the user with an error
                 * rather than a fabricated success.
                 */
                const onUnknownOutcome = (): void => {
                    patchState(store, { actionExecution: undefined });
                    httpErrorManagerService.handle(
                        new HttpErrorResponse({
                            status: 500,
                            statusText: 'The action response carried no summary'
                        })
                    );
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
                            .subscribe((result) => {
                                const summary = result?.summary;

                                if (!summary) {
                                    onUnknownOutcome();

                                    return;
                                }

                                onSettled({
                                    actionName,
                                    successCount: summary.successCount,
                                    skippedCount: 0,
                                    failCount: summary.failCount
                                });
                            });
                    },

                    /**
                     * Fires the selected workflow action over the given contentlet inodes.
                     *
                     * Contentlets whose scheme does not own the action are skipped server-side and
                     * reported in `skippedCount`, so a mixed-type selection partially skips by
                     * design — the result carries that through to the toast.
                     *
                     * `inputs` carries whatever the action declared it needs — a move destination in the
                     * `//hostname/path` form the actionlet reads, an assignee and comment, push publish
                     * settings. Each is ignored by an action that did not ask for it, which is why they
                     * stay optional on one method rather than becoming three: the request shape is
                     * identical either way, and only the filled-in parts are read server-side.
                     */
                    executeWorkflowAction: (
                        workflowActionId: string,
                        actionName: string,
                        contentletIds: string[],
                        inputs?: {
                            pathToMove?: string;
                            assignComment?: { assign: string; comment: string };
                            pushPublish?: DotActionBulkRequestOptions['additionalParams']['pushPublish'];
                        }
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
                                assignComment: inputs?.assignComment ?? {
                                    assign: '',
                                    comment: ''
                                },
                                pushPublish: inputs?.pushPublish ?? {},
                                additionalParamsMap: {
                                    _path_to_move: inputs?.pathToMove ?? ''
                                }
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

                    /**
                     * Adds the given contentlets to a bundle.
                     *
                     * Separate from the other two because it is not a workflow action at all: no
                     * actionlet, no step transition, and it posts form-encoded to the legacy
                     * `/DotAjaxDirector/…/addToBundle` servlet rather than a workflow endpoint. It
                     * shares the execution *state* so the toolbar indicator, the "one at a time" guard
                     * and the result toast all behave identically.
                     *
                     * Takes **identifiers**, not inodes — the one action here that does. A bundle holds
                     * one entry per identifier, so language versions of a contentlet are one asset.
                     *
                     * `total` is the server's count of assets actually queued, already deduped and with
                     * anything already in the bundle removed. It is reported rather than
                     * `identifiers.length` so the toast cannot claim more than was added.
                     */
                    executeAddToBundle: (
                        actionName: string,
                        bundle: DotBundle,
                        identifiers: string[]
                    ): void => {
                        if (!identifiers.length || store.actionExecution()) {
                            return;
                        }

                        patchState(store, {
                            actionExecution: { actionName, total: identifiers.length },
                            actionExecutionResult: undefined
                        });

                        addToBundleService
                            // Comma-joined: the servlet splits `assetIdentifier` on "," and has
                            // always accepted several ids that way, so bulk needs no new endpoint.
                            .addToBundle(identifiers.join(','), bundle)
                            .pipe(
                                take(1),
                                catchError((error) => {
                                    patchState(store, { actionExecution: undefined });
                                    httpErrorManagerService.handle(error);

                                    return EMPTY;
                                })
                            )
                            .subscribe((result) => {
                                const failCount = result?.errors ?? 0;

                                onSettled({
                                    actionName,
                                    // `total` counts everything queued, failures included, so the
                                    // successes are what is left after removing them.
                                    successCount: Math.max((result?.total ?? 0) - failCount, 0),
                                    skippedCount: 0,
                                    failCount
                                });
                            });
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
