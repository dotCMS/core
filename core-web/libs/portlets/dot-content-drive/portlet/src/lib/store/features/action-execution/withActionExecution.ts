import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY, Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { catchError, take } from 'rxjs/operators';

import {
    AddToBundleService,
    DotBulkRefreshService,
    DotEventsSocket,
    DotHttpErrorManagerService,
    DotMessageService,
    DotSystemEventType,
    DotWorkflowActionsFireService,
    PushPublishService
} from '@dotcms/data-access';
import {
    DotActionBulkRequestOptions,
    DotAjaxActionResponseView,
    DotBulkRefreshCompletedEvent,
    DotBundle,
    DotWorkflowPushPublishValue
} from '@dotcms/dotcms-models';

import {
    DotContentDriveActionExecution,
    DotContentDriveActionExecutionResult,
    DotContentDriveRun,
    DotContentDriveState
} from '../../../shared/models';

interface WithActionExecutionState {
    /**
     * Every run currently in flight, keyed by its client-allocated id.
     *
     * Was a single slot, which made one long operation block every other one. An upload runs for
     * minutes, so "one at a time" stopped being a reasonable guard and became a freeze (FR-015).
     */
    runs: Record<string, DotContentDriveRun>;
    /**
     * Outcome of the last finished execution, awaiting presentation. The shell consumes this and
     * calls {@link clearActionExecutionResult}; the store never shows the toast itself.
     */
    actionExecutionResult?: DotContentDriveActionExecutionResult;
    /**
     * Ids of the reindex jobs this store submitted and has not yet settled.
     *
     * `BULK_REFRESH_COMPLETED` is scoped to the submitting *user*, so this store also receives runs
     * fired from another tab, another window, or a Login-As session. Reporting those would toast
     * counts for content this grid never selected. Only ids in here are acted on.
     *
     * Not persisted: a page reload loses them, and a run submitted before the reload settles silently.
     * The notification bell still records it, and that is the better trade against reacting to
     * somebody else's run.
     */
    refreshJobIds: string[];
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
            runs: {},
            actionExecutionResult: undefined,
            refreshJobIds: []
        }),
        withComputed(({ runs }) => ({
            /** Runs in flight, in insertion order. */
            activeRuns: computed(() => Object.values(runs())),
            /**
             * The run the indicator names when there is exactly one.
             *
             * Kept as a single value so every existing consumer reads unchanged; with several runs
             * it is `undefined` and the indicator falls back to a count (FR-017). Naming one of
             * several arbitrarily would be worse than naming none.
             */
            actionExecution: computed<DotContentDriveActionExecution | undefined>(() => {
                const active = Object.values(runs());

                return active.length === 1 ? active[0] : undefined;
            }),
            /** How many runs are in flight; what the indicator shows once naming one is not enough. */
            activeRunCount: computed(() => Object.keys(runs()).length),
            /**
             * Every inode any in-flight run is acting on.
             *
             * Keyed by inode, not identifier: the language filter is multi-select, so one identifier
             * can legitimately occupy several rows and marking by identifier would mark siblings
             * that nothing is happening to.
             */
            busyRows: computed(() =>
                Object.values(runs()).flatMap((run) => run.targets)
            )
        })),
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
                /**
                 * Registers a run and returns its id.
                 *
                 * The id is allocated here rather than taken from a server handle because the window
                 * a double-click has to fire twice is precisely the window before any handle exists.
                 */
                const startRun = (run: Omit<DotContentDriveRun, 'runId'>): string => {
                    const runId = `${run.operation}-${Date.now()}-${Math.random()
                        .toString(36)
                        .slice(2, 8)}`;

                    patchState(store, {
                        runs: { ...store.runs(), [runId]: { ...run, runId } },
                        actionExecutionResult: undefined
                    });

                    return runId;
                };

                /** Removes a run from the registry. Safe to call for an id already gone. */
                const endRun = (runId: string): void => {
                    const remaining = { ...store.runs() };
                    delete remaining[runId];

                    patchState(store, { runs: remaining });
                };

                /**
                 * Whether this operation is already running over any of these items.
                 *
                 * Scoped to the operation *and* its targets (FR-016). Firing Publish twice on the
                 * same row is still refused; firing Lock while an upload runs is not, which is the
                 * whole point of the change.
                 */
                const isRunning = (operation: string, targets: string[]): boolean =>
                    Object.values(store.runs()).some(
                        (run) =>
                            run.operation === operation &&
                            run.targets.some((target) => targets.includes(target))
                    );

                const onSettled = (
                    runId: string,
                    result: DotContentDriveActionExecutionResult
                ): void => {
                    endRun(runId);
                    patchState(store, { actionExecutionResult: result });
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
                const onUnknownOutcome = (runId: string): void => {
                    endRun(runId);
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
                    if (!identifiers.length || isRunning(actionName, identifiers)) {
                        return;
                    }

                    const runId = startRun({
                        operation: actionName,
                        actionName,
                        total: identifiers.length,
                        targets: identifiers
                    });

                    request()
                        .pipe(
                            take(1),
                            catchError((error) => {
                                endRun(runId);
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
                                endRun(runId);
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

                            onSettled(runId, {
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
                        if (!inodes.length || isRunning(actionId, inodes)) {
                            return;
                        }

                        const runId = startRun({
                            operation: actionId,
                            actionName,
                            total: inodes.length,
                            targets: inodes
                        });

                        workflowActionsFireService
                            .fireDefaultAction({ action: actionId, inodes })
                            .pipe(
                                take(1),
                                catchError((error) => {
                                    endRun(runId);
                                    httpErrorManagerService.handle(error);

                                    return EMPTY;
                                })
                            )
                            .subscribe((result) => {
                                const summary = result?.summary;

                                if (!summary) {
                                    onUnknownOutcome(runId);

                                    return;
                                }

                                onSettled(runId, {
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
                     * Submit-and-forget: the endpoint answers 202 and nothing here waits or guards. A
                     * second reindex is allowed to be fired — firing clears the selection, so it takes a
                     * deliberate re-selection, and reindexing the same rows again is wasteful rather
                     * than wrong.
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
                        if (!inodes.length) {
                            return;
                        }

                        // Note what is NOT set: actionExecution. That field shows an "Applying …"
                        // indicator and locks the Action Center, and neither fits a job that runs for
                        // minutes and cannot report progress. The user is told at trigger that this is
                        // backgrounded, and told again when it finishes.
                        //
                        // Note also what is not started: a completion deadline. Nothing on screen is
                        // waiting, so there is nothing for one to unblock - and a client that gave up
                        // after N minutes would be reporting a failure it has no evidence of, over a
                        // run the server records in the notification bell either way.

                        // Submit and stop. The endpoint answers 202 and the reindex continues in the
                        // background; the outcome arrives on the socket subscription below rather than
                        // by asking for it. Nothing here waits.
                        bulkRefreshService
                            .refresh(inodes)
                            .pipe(
                                take(1),
                                catchError((error) => {
                                    // The only reindex failure a client sees directly: no job was
                                    // created, so no completion event is coming for it either.
                                    httpErrorManagerService.handle(error);

                                    return EMPTY;
                                }),
                                takeUntilDestroyed(destroyRef)
                            )
                            .subscribe((response) => {
                                if (!response?.jobId) {
                                    return;
                                }

                                // Remember the run so its completion event can be told apart from
                                // every other one this user's session receives.
                                patchState(store, {
                                    refreshJobIds: [...store.refreshJobIds(), response.jobId]
                                });
                            });
                    },

                    /**
                     * Reports a finished bulk refresh, from the pushed completion event.
                     *
                     * Feeds the same {@link onSettled} path as every other action, so the toast copy,
                     * severity, grid reload and selection clear all behave identically — with its own
                     * partial-outcome wording, because a reindex falls short for different reasons than a
                     * workflow fire.
                     *
                     * Three ways a run can arrive with nothing honest to report, all of which would
                     * otherwise render as a green success toast:
                     *
                     * 1. No counters at all.
                     * 2. A state whose counters describe only how far the job got before dying — a
                     *    permanently failed job still carries the counters it had reached, so an all-zero
                     *    result is indistinguishable from a clean run over nothing unless state is checked.
                     * 3. Counters that do not close over `total`, meaning the run did not account for
                     *    every item and the shortfall is unexplained.
                     */
                    reportRefreshCompleted: (
                        actionName: string,
                        event: DotBulkRefreshCompletedEvent
                    ): void => {
                        if (!event.jobId || !store.refreshJobIds().includes(event.jobId)) {
                            // Not ours: another tab's run, or one already settled. Silent by design -
                            // an error toast here would blame the user for somebody else's event.
                            return;
                        }

                        patchState(store, {
                            refreshJobIds: store.refreshJobIds().filter((id) => id !== event.jobId)
                        });

                        const closes =
                            undefined !== event.total &&
                            (event.successCount ?? 0) +
                                (event.failedCount ?? 0) +
                                (event.skippedCount ?? 0) ===
                                event.total;

                        if ('SUCCESS' !== event.state && 'CANCELED' !== event.state) {
                            // Note what is not touched: actionExecution. It may belong to a different
                            // action that is still running - a reindex no longer locks the dialog, so
                            // that is an ordinary situation, and clearing it here would un-gate that
                            // action early.
                            httpErrorManagerService.handle(
                                new HttpErrorResponse({
                                    status: 500,
                                    statusText: `The reindex did not report a usable outcome (state: ${event.state})`
                                })
                            );

                            return;
                        }

                        if (!closes) {
                            httpErrorManagerService.handle(
                                new HttpErrorResponse({
                                    status: 500,
                                    statusText:
                                        'The reindex counters did not account for every item'
                                })
                            );

                            return;
                        }

                        // Still not onSettled, but for a smaller reason now: a reindex never
                        // registered a run, so there is nothing to settle. Before the registry this
                        // also had to avoid wiping a *different* action's slot; keying runs by id
                        // removed that hazard.
                        patchState(store, {
                            actionExecutionResult: {
                                actionName,
                                successCount: event.successCount ?? 0,
                                skippedCount: event.skippedCount ?? 0,
                                failCount: event.failedCount ?? 0,
                                partialDetailKey:
                                    'content-drive.action-center.toast.refreshed-partial',
                                backgrounded: true
                            }
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
                        if (!contentletIds.length || isRunning(workflowActionId, contentletIds)) {
                            return;
                        }

                        const runId = startRun({
                            operation: workflowActionId,
                            actionName,
                            total: contentletIds.length,
                            targets: contentletIds
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
                                    endRun(runId);
                                    httpErrorManagerService.handle(error);

                                    return EMPTY;
                                })
                            )
                            .subscribe((result) =>
                                onSettled(runId, {
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

                    /**
                     * Registers a run this store did not fire itself, returning its id.
                     *
                     * The context menu and the drag-and-drop move own their own service calls and
                     * present their own outcomes, but the *in-flight* half belongs on the shared
                     * indicator like every other operation (FR-007). Without this the context menu
                     * had one way to say "working": blanking the whole listing.
                     */
                    startExternalRun: (run: Omit<DotContentDriveRun, 'runId'>): string =>
                        startRun(run),

                    /** Settles a run registered with {@link startExternalRun}. */
                    endExternalRun: (runId: string): void => endRun(runId),

                    /**
                     * Publishes an outcome for a run this store did not fire itself.
                     *
                     * Add to Bundle and Push Publish from the row context menu hand off to shared
                     * dialogs that own their own request. Fired from the Workflow Center the same
                     * two operations settle through `onSettled` and are reported by the shell with
                     * one wording; fired from the context menu they used to report nothing at all.
                     *
                     * Rather than give the context menu its own copy, it publishes here and the
                     * shell's existing effect renders it — so the same operation reads the same way
                     * whichever surface started it, and the reload behaviour matches too.
                     */
                    reportExternalResult: (
                        result: DotContentDriveActionExecutionResult
                    ): void => {
                        patchState(store, { actionExecutionResult: result });
                    },

                    /** Called by the shell once the result has been presented. */
                    clearActionExecutionResult: (): void => {
                        patchState(store, { actionExecutionResult: undefined });
                    }
                };
            }
        ),
        withHooks({
            onInit(store) {
                const eventsSocket = inject(DotEventsSocket);
                const dotMessageService = inject(DotMessageService);
                const destroyRef = inject(DestroyRef);

                // The socket is already open app-wide, so subscribing costs nothing. This is what
                // replaced polling: the run reports itself when it settles instead of being asked.
                eventsSocket
                    .on<DotBulkRefreshCompletedEvent>(DotSystemEventType.BULK_REFRESH_COMPLETED)
                    .pipe(takeUntilDestroyed(destroyRef))
                    .subscribe((event) => {
                        // Resolve the label here rather than server-side: the backend should not be
                        // composing user-facing copy, and this keeps the wording with the rest of the
                        // Action Center's i18n.
                        store.reportRefreshCompleted(dotMessageService.get('Refresh'), event);
                    });
            }
        })
    );
}
