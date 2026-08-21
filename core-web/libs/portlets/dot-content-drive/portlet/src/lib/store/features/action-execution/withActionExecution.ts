import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';
import { EMPTY, Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { catchError, take } from 'rxjs/operators';


import {
    AddToBundleService,
    DotBulkRefreshService,
    DotHttpErrorManagerService,
    DotWorkflowActionsFireService,
    PushPublishService
} from '@dotcms/data-access';
import {
    DOT_BULK_REFRESH_REPORTABLE_STATES,
    DotActionBulkRequestOptions,
    DotAjaxActionResponseView,
    DotBundle,
    DotWorkflowPushPublishValue
} from '@dotcms/dotcms-models';

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
                addToBundleService = inject(AddToBundleService),
                pushPublishService = inject(PushPublishService),
                bulkRefreshService = inject(DotBulkRefreshService),
                destroyRef = inject(DestroyRef)
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

                /**
                 * Runs one of the legacy `RemotePublishAjaxAction` bulk operations.
                 *
                 * Add to Bundle and Push Publish differ only in which servlet command they call and
                 * in what to say when it answers with nothing usable. Everything else — the replay
                 * guard, the in-flight marker, the error routing and the count arithmetic — is the
                 * *servlet's* response contract rather than either action's own logic, so it belongs
                 * in one place. A third command should be a call, not another copy.
                 *
                 * Deliberately not `onUnknownOutcome`, which serves the `bulkFire` path: that
                 * endpoint answers with a `summary` object, so "no honest count" is a different
                 * shape and a different message from the servlet's non-numeric `errors`.
                 *
                 * @param actionName Shown in the toolbar indicator and the result toast
                 * @param identifiers Asset identifiers — the servlet splits `assetIdentifier` on ","
                 * @param request Built lazily, so nothing is posted when the guards refuse the run
                 * @param noResultMessage Reported when the response carries no numeric `errors`
                 */
                const fireLegacyServletBulk = (
                    actionName: string,
                    identifiers: string[],
                    request: () => Observable<DotAjaxActionResponseView>,
                    noResultMessage: string
                ): void => {
                    if (!identifiers.length || store.actionExecution()) {
                        return;
                    }

                    patchState(store, {
                        actionExecution: { actionName, total: identifiers.length },
                        actionExecutionResult: undefined
                    });

                    request()
                        .pipe(
                            take(1),
                            catchError((error) => {
                                patchState(store, { actionExecution: undefined });
                                httpErrorManagerService.handle(error);

                                return EMPTY;
                            })
                        )
                        .subscribe((result) => {
                            // The servlet answers 200 for its own failures too: on a
                            // `DotPublisherException` it writes `{"errors": "<message>"}` with no
                            // `total`, and when the publisher returns nothing it writes no body at
                            // all. Either shape would arrive here as a "success" — the first
                            // producing `NaN` from `total - "<message>"`, the second reporting zero
                            // of everything on what may well have worked. Neither is a result worth
                            // showing, so both go to the error handler instead.
                            if (typeof result?.errors !== 'number') {
                                patchState(store, { actionExecution: undefined });
                                httpErrorManagerService.handle(
                                    new HttpErrorResponse({
                                        status: 500,
                                        statusText:
                                            typeof result?.errors === 'string'
                                                ? result.errors
                                                : noResultMessage
                                    })
                                );

                                return;
                            }

                            onSettled({
                                actionName,
                                // `total` counts everything queued, failures included, so the
                                // successes are what is left after removing them.
                                successCount: Math.max((result.total ?? 0) - result.errors, 0),
                                skippedCount: 0,
                                failCount: result.errors
                            });
                        });
                };

                return {
                    /**
                     * Fires a quick action (lock, unlock) over the given inodes.
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
                     * Reindexes the given contentlet inodes.
                     *
                     * Unlike the other quick actions this one is job-backed: the endpoint answers `202`
                     * with a job id and the service polls until it settles, so the run can outlast the
                     * dialog and still be reported. That is the same property the other actions get from
                     * living in the store, reached a different way.
                     *
                     * No live counters. The status endpoint reports a progress float but nothing
                     * item-wise while the job runs, so the toolbar shows the run as in flight and the
                     * outcome lands once, at the end — which is all the toast needs.
                     *
                     * Reported through the same {@link onSettled} path as everything else, with its own
                     * partial-outcome copy: a failure here is content that could not be read or indexed
                     * and a skip is a cancelled run, neither of which is what the default copy blames.
                     *
                     * Only SUCCESS and CANCELED are reported as outcomes, and only when the counters
                     * close over `total`. A job that died mid-run still carries counters describing how
                     * far it got, and reporting those as a result would turn a failure into a green
                     * toast - the exact misleading success this endpoint exists to remove.
                     */
                    executeRefresh: (actionName: string, inodes: string[]): void => {
                        if (!inodes.length || store.actionExecution()) {
                            return;
                        }

                        patchState(store, {
                            actionExecution: { actionName, total: inodes.length },
                            actionExecutionResult: undefined
                        });

                        bulkRefreshService
                            .refresh(inodes)
                            .pipe(
                                take(1),
                                catchError((error) => {
                                    patchState(store, { actionExecution: undefined });
                                    httpErrorManagerService.handle(error);

                                    return EMPTY;
                                }),
                                // The only action here that needs this. The others are a single
                                // request, so `take(1)` completes them after one round trip. This one
                                // wraps an infinite `timer(0, 1500)` inside the service, and `take(1)`
                                // sits on the *outer* observable, which does not emit until the poll
                                // loop has already finished — so it cannot stop the loop.
                                //
                                // This store is component-scoped (see the shell's `providers`), so it
                                // dies when the user leaves the portlet. Without this the poll keeps
                                // firing at a screen nobody is on, the global error handler can raise a
                                // dialog minutes later about work the user can no longer see, and a
                                // freshly constructed store reports nothing in flight while the orphan
                                // is still running and will eventually patch a dead one.
                                takeUntilDestroyed(destroyRef)
                            )
                            .subscribe((outcome) => {
                                const counts = outcome?.counts;

                                // Three ways a run can end with nothing honest to report, all of
                                // which would otherwise render as a green success toast:
                                //
                                // 1. No counters at all.
                                // 2. A state whose counters describe only how far the job got before
                                //    dying - a FAILED_PERMANENTLY job still carries its metadata, so
                                //    an all-zero result is indistinguishable from a clean run over
                                //    nothing unless the state is checked.
                                // 3. Counters that do not close over `total`, which means the run did
                                //    not account for every item and the shortfall is unexplained.
                                //
                                // Substituting a number in any of these cases - `inodes.length` or
                                // `0` - invents one the server never sent, and the first errs in the
                                // reassuring direction.
                                const reportable =
                                    !!counts &&
                                    !!outcome &&
                                    (
                                        DOT_BULK_REFRESH_REPORTABLE_STATES as readonly string[]
                                    ).includes(outcome.state) &&
                                    counts.successCount +
                                        counts.failedCount +
                                        counts.skippedCount ===
                                        counts.total;

                                if (!reportable) {
                                    patchState(store, { actionExecution: undefined });
                                    httpErrorManagerService.handle(
                                        new HttpErrorResponse({
                                            status: 500,
                                            statusText: `The reindex job did not report a usable outcome (state: ${
                                                outcome?.state ?? 'unknown'
                                            })`
                                        })
                                    );

                                    return;
                                }

                                onSettled({
                                    actionName,
                                    successCount: counts.successCount,
                                    skippedCount: counts.skippedCount,
                                    failCount: counts.failedCount,
                                    partialDetailKey:
                                        'content-drive.action-center.toast.refreshed-partial'
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
                    ): void =>
                        fireLegacyServletBulk(
                            actionName,
                            identifiers,
                            // Comma-joined: the servlet splits `assetIdentifier` on "," and has
                            // always accepted several ids that way, so bulk needs no new endpoint.
                            () => addToBundleService.addToBundle(identifiers.join(','), bundle),
                            'Adding to the bundle returned no result'
                        ),

                    /**
                     * Push publishes the given identifiers to the chosen environments.
                     *
                     * Identifiers, not inodes: push publish sends the *asset*, so every language
                     * version of a contentlet is one entry — the same collapse Add to Bundle makes.
                     *
                     * Shares the legacy servlet's response shape with Add to Bundle, and the same
                     * caveat: it answers 200 for its own failures, so a body without a numeric
                     * `errors` is routed to the error handler rather than reported as a success.
                     *
                     * Known under-report on `publishexpire`. `RemotePublishAjaxAction.publish`
                     * creates one bundle for the publish half and a second for the expire half, and
                     * the second `responseMap` overwrites the first — so the counts that come back
                     * describe the expire half alone and the toast reports fewer items than were
                     * actually queued. Not a regression here: `PushPublishActionlet.doPushPublish`
                     * splits the same way, so the workflow path reports it identically.
                     */
                    executePushPublish: (
                        actionName: string,
                        identifiers: string[],
                        settings: DotWorkflowPushPublishValue
                    ): void =>
                        fireLegacyServletBulk(
                            actionName,
                            identifiers,
                            () =>
                                pushPublishService.pushPublishAssets(
                                    identifiers.join(','),
                                    settings
                                ),
                            'The push publish returned no result'
                        ),

                    /** Called by the shell once the result has been presented. */
                    clearActionExecutionResult: (): void => {
                        patchState(store, { actionExecutionResult: undefined });
                    }
                };
            }
        )
    );
}
