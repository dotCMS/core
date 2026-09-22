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
    DotFolderBulkDeleteRefusal,
    DotFolderBulkDeleteService,
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
    DotFolderBulkDeleteCompletedEvent,
    DotBulkUploadCompletedEvent,
    DotBundle,
    DotWorkflowPushPublishValue
} from '@dotcms/dotcms-models';

import {
    DotContentDriveActionExecution,
    DotContentDriveActionExecutionResult,
    DotContentDriveRun,
    DotContentDriveUploadJob,
    DotContentDriveState
} from '../../../shared/models';
import { browsedFolderRef, normalizeFolderRef } from '../../../utils/functions';

/**
 * The operation key a bulk folder delete run is registered under.
 *
 * Paired with the run's targets it forms the repeat guard — *this operation over these folders* —
 * so a delete running for minutes never blocks an unrelated action, nor a delete of different
 * folders (FR-018). Kept here rather than imported from the quick-action registry: the store's
 * guard key is its own concern, and tying it to a UI constant would make a rename of one silently
 * change the other.
 */
const DELETE_FOLDER_OPERATION = 'DELETE_FOLDER';

interface WithActionExecutionState {
    /**
     * Every run currently in flight, keyed by its client-allocated id.
     *
     * Was a single slot, which made one long operation block every other one. An upload runs for
     * minutes, so "one at a time" stopped being a reasonable guard and became a freeze (FR-015).
     */
    runs: Record<string, DotContentDriveRun>;
    /**
     * Outcomes awaiting presentation, oldest first. The shell consumes the head and calls
     * {@link clearActionExecutionResult}; the store never shows the toast itself.
     *
     * A queue rather than a slot because several runs are legitimate at once and the shell drains
     * from an `effect()`, which flushes on the next change-detection pass rather than synchronously.
     * Two outcomes landing in the same tick therefore met one slot and the first was overwritten
     * before anything read it — reported nowhere, which is the silent shortfall this feature exists
     * to remove.
     */
    actionExecutionResults: DotContentDriveActionExecutionResult[];
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
    /**
     * Batches this store submitted, and where each one landed.
     *
     * Same reasoning as {@link refreshJobIds} — the completion event is scoped to the submitting
     * user, so another tab's upload reaches this store too and only ids in here are acted on. The
     * folders come along because the outcome decides whether the listing can show what changed, and
     * by the time the event lands the author may be looking somewhere else entirely.
     */
    uploadJobs: Record<string, DotContentDriveUploadJob>;
    /**
     * Bulk folder deletes this store submitted, by job id, valued by the run they belong to.
     *
     * Same reasoning as {@link uploadJobs}: the completion is scoped to the submitting *user*, so a
     * run fired from another tab or a Login-As session reaches this store too, and only ids in here
     * are reported. Not persisted — a reload loses them and that run settles silently, which the
     * durable record still covers.
     */
    folderDeleteJobs: Record<string, string>;
    /**
     * Bulk folder deletes this page has already reported, by job id.
     *
     * Kept because {@link folderDeleteJobs} no longer answers "have we settled this?" on its own.
     * A run submitted before a reload is reported from an empty map, so removal from that map
     * cannot be what makes reporting idempotent any more — and the completion is a pushed event,
     * which a socket reconnect can deliver again.
     */
    settledFolderDeleteJobs: string[];
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
            actionExecutionResults: [],
            refreshJobIds: [],
            uploadJobs: {},
            folderDeleteJobs: {},
            settledFolderDeleteJobs: []
        }),
        withComputed(({ runs, actionExecutionResults }) => ({
            /**
             * The outcome waiting to be presented, or `undefined` when none is.
             *
             * The head of the queue, not a slot of its own: every consumer wants "the next thing to
             * show", and draining is what advances it. Keeping the singular name means the shell and
             * the dialog read exactly what they always did, while the queue behind it stops a second
             * outcome landing in the same tick from overwriting this one.
             */
            actionExecutionResult: computed(() => actionExecutionResults()[0]),
            /** Runs in flight, in insertion order. */
            activeRuns: computed(() => Object.values(runs())),
            /**
             * Runs the toolbar indicator speaks for: the ones with nothing to mark.
             *
             * A run over rows is already reported by those rows dimming, so a toolbar line saying
             * the same thing is the duplication this feature set out to remove. A run with no
             * targets has no other surface at all — an upload's content does not exist until the run
             * creates it — so the indicator is the only place it can be seen.
             */
            unmarkedRuns: computed(() =>
                Object.values(runs()).filter((run) => run.targets.length === 0)
            ),
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
            /** How many runs are in flight, of any kind. */
            activeRunCount: computed(() => Object.keys(runs()).length),
            /**
             * The run the toolbar names, when exactly one has nothing to mark.
             *
             * Separate from `actionExecution` on purpose: that is the general "is something
             * running" signal, read by the Action Center to gate itself. This one is presentation —
             * which runs the *indicator* should speak for — and the answer is only those the rows
             * cannot speak for themselves.
             */
            toolbarRun: computed<DotContentDriveActionExecution | undefined>(() => {
                const unmarked = Object.values(runs()).filter((run) => run.targets.length === 0);

                return unmarked.length === 1 ? unmarked[0] : undefined;
            }),
            /** How many runs the indicator speaks for. */
            toolbarRunCount: computed(
                () => Object.values(runs()).filter((run) => run.targets.length === 0).length
            ),
            /**
             * Every inode any in-flight run is acting on.
             *
             * Keyed by inode, not identifier: the language filter is multi-select, so one identifier
             * can legitimately occupy several rows and marking by identifier would mark siblings
             * that nothing is happening to.
             */
            busyRows: computed(() => Object.values(runs()).flatMap((run) => run.targets))
        })),
        withMethods(
            (
                store,
                workflowActionsFireService = inject(DotWorkflowActionsFireService),
                httpErrorManagerService = inject(DotHttpErrorManagerService),
                addToBundleService = inject(AddToBundleService),
                pushPublishService = inject(PushPublishService),
                bulkRefreshService = inject(DotBulkRefreshService),
                folderBulkDeleteService = inject(DotFolderBulkDeleteService),
                destroyRef = inject(DestroyRef)
            ) => {
                /**
                 * The key a run is stored under: what it is, and what it is about.
                 *
                 * Natural rather than generated. It has to be unique, and this already is: a second
                 * run with the same key is exactly what {@link isRunning} refuses, so a collision
                 * cannot arise. It also makes the guard a single lookup instead of a scan over every
                 * live run intersecting target arrays, and leaves callers holding something readable
                 * rather than an opaque token.
                 *
                 * Caveat worth knowing: a target containing the separator could in principle collide.
                 * The colliding case is "same operation, same items", which the guard refuses anyway,
                 * so it fails safe.
                 */
                const runKey = (operation: string, targets: string[]): string =>
                    `${operation}:${targets.join(',')}`;

                /**
                 * Registers a run and returns its key.
                 *
                 * Also clears any pending outcome, so a stale result cannot sit next to a new run.
                 */
                const startRun = (run: Omit<DotContentDriveRun, 'runId'>): string => {
                    const runId = runKey(run.operation, run.targets);

                    // The queue is deliberately left alone. This used to clear it so a stale result
                    // could not sit beside a new run, but with several runs in flight that discards
                    // an outcome nobody has seen yet — and starting one run while another settles is
                    // ordinary now. Presentation drains the queue; starting work does not.
                    patchState(store, {
                        runs: { ...store.runs(), [runId]: { ...run, runId } }
                    });

                    return runId;
                };

                /** Removes one run. Safe for a key already gone. */
                const endRun = (runId: string): void => {
                    const remaining = { ...store.runs() };
                    delete remaining[runId];

                    patchState(store, { runs: remaining });
                };

                /**
                 * Whether this exact operation is already running over these items.
                 *
                 * Scoped to the operation *and* its targets (FR-016): firing Publish twice on the
                 * same row is refused, locking a row while an upload runs is not.
                 */
                const isRunning = (operation: string, targets: string[]): boolean => {
                    const active = Object.values(store.runs());

                    // Two checks, because the natural key alone is not enough. Publish on [a,b] and
                    // Publish on [a] are *different* keys, so a key match would let the second
                    // through and act on row `a` twice at once. Busy rows are non-interactive in the
                    // UI, but a disabled control is an affordance, not a lock on the store.
                    //
                    // The overlap check is also stronger than the operation-scoped one it replaces:
                    // it refuses *any* run over an item another run is already touching, which is
                    // what the row marks already tell the author. The key check is what still covers
                    // a run with no item targets at all, such as an upload.
                    return (
                        runKey(operation, targets) in store.runs() ||
                        active.some((run) => run.targets.some((target) => targets.includes(target)))
                    );
                };

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
                const onSettled = (
                    runId: string,
                    result: DotContentDriveActionExecutionResult
                ): void => {
                    endRun(runId);
                    patchState(store, {
                        actionExecutionResults: [...store.actionExecutionResults(), result]
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
                    noResultMessage: string,
                    rowTargets: string[] = []
                ): void => {
                    // Falls back to the identifiers when a caller has no rows to name. Defaulting
                    // to an empty list instead would key every run of this action to the same
                    // string, so bundling one asset would refuse to bundle a different one — the
                    // exact repeat-fire collision the key exists to make impossible.
                    const targets = rowTargets.length ? rowTargets : identifiers;

                    if (!identifiers.length || isRunning(actionName, targets)) {
                        return;
                    }

                    const runId = startRun({
                        operation: actionName,
                        // Counted in identifiers, because that is what the server queues: language
                        // versions of one contentlet are one asset.
                        total: identifiers.length,
                        // Targeted by inode, because that is what a *row* is. These two actions are
                        // the only ones whose request vocabulary differs from the listing's, and
                        // registering the run under identifiers meant it marked no row and could
                        // not overlap with an inode-keyed run over the same content.
                        targets
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
                                failedCount: result.errors,
                                // Both consumers of this path — Add to Bundle and Push Publish —
                                // change nothing in the listing, so their success has to be said out
                                // loud or the author gets no sign at all. The row-based operations
                                // stay silent precisely because their rows *do* change.
                                confirmSuccess: true
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
                                    failedCount: summary.failCount
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
                            actionExecutionResults: [
                                ...store.actionExecutionResults(),
                                {
                                    actionName,
                                    successCount: event.successCount ?? 0,
                                    skippedCount: event.skippedCount ?? 0,
                                    failedCount: event.failedCount ?? 0,
                                    partialDetailKey:
                                        'content-drive.action-center.toast.refreshed-partial',
                                    backgrounded: true
                                }
                            ]
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

                        // A move changes two folders: the one the rows leave and the one they
                        // arrive in. Every other workflow action changes rows where they already
                        // are, so the browsed folder is the only one affected.
                        const browsedFolder = browsedFolderRef(
                            store.currentSite()?.hostname,
                            store.path()
                        );
                        const affectedFolders = inputs?.pathToMove
                            ? [browsedFolder, normalizeFolderRef(inputs.pathToMove)]
                            : [browsedFolder];

                        const runId = startRun({
                            operation: workflowActionId,
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
                                    failedCount: result?.fails?.length ?? 0,
                                    affectedFolders
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
                        identifiers: string[],
                        inodes: string[] = []
                    ): void =>
                        fireLegacyServletBulk(
                            actionName,
                            identifiers,
                            // Comma-joined: the servlet splits `assetIdentifier` on "," and has
                            // always accepted several ids that way, so bulk needs no new endpoint.
                            () => addToBundleService.addToBundle(identifiers.join(','), bundle),
                            'Adding to the bundle returned no result',
                            inodes
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
                        settings: DotWorkflowPushPublishValue,
                        inodes: string[] = []
                    ): void =>
                        fireLegacyServletBulk(
                            actionName,
                            identifiers,
                            () =>
                                pushPublishService.pushPublishAssets(
                                    identifiers.join(','),
                                    settings
                                ),
                            'The push publish returned no result',
                            inodes
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
                     * Remembers a batch this store submitted, so its completion can be told from
                     * another tab's.
                     *
                     * @param affectedFolders where the batch landed, as `//hostname/path` refs
                     */
                    /**
                     * Submits a bulk folder delete and marks the folders it accepted (#37063 US1).
                     *
                     * Two separate identities, deliberately:
                     *
                     * - `assetPaths` is what the **server** works in — site-qualified folder paths,
                     *   the same form the shipped single delete accepts. The caller builds them,
                     *   because it is the one that knows the site.
                     * - `targets` is what the **listing** marks by, and must carry **both** a
                     *   folder's `inode` and its `identifier`: the search service only backfills
                     *   `inode` from `identifier` when the API returned none, so neither is reliably
                     *   the key the row actually carries.
                     *
                     * The guard is the shared one — this operation over these folders — so a delete
                     * running for minutes never blocks an unrelated action, or a delete of different
                     * folders (FR-018).
                     *
                     * NOT YET IMPLEMENTED — stub so the specs compile and fail on behaviour (T019).
                     */
                    executeFolderBulkDelete: (
                        actionName: string,
                        assetPaths: string[],
                        targets: string[]
                    ): void => {
                        if (!assetPaths.length || isRunning(DELETE_FOLDER_OPERATION, targets)) {
                            return;
                        }

                        // No resolved label travels with the run any more: a run now names itself
                        // through `operation` and an optional `labelKey`, and the toolbar only ever
                        // surfaces *unmarked* runs (`targets.length === 0`). This one is marked by
                        // construction, so a label on it could never reach a screen. `actionName`
                        // stays on the signature because the outcome toast still needs it, via
                        // `reportFolderDeleteCompleted`.
                        const runId = startRun({
                            operation: DELETE_FOLDER_OPERATION,
                            total: assetPaths.length,
                            targets
                        });

                        folderBulkDeleteService
                            .submit(assetPaths)
                            .pipe(
                                take(1),
                                catchError((refusal: DotFolderBulkDeleteRefusal) => {
                                    // A refusal means no run exists server-side, so nothing will
                                    // ever arrive to settle this one.
                                    endRun(runId);
                                    httpErrorManagerService.handle(
                                        refusal?.response ??
                                            new HttpErrorResponse({ error: refusal })
                                    );

                                    return EMPTY;
                                })
                            )
                            .subscribe((handle) => {
                                // The server's count, not the caller's. The two disagree whenever a
                                // duplicate or a nested path is dropped, and the first screen has to
                                // agree with the last (CR-03). Left as submitted when the instance
                                // is older than the field, which is the honest fallback.
                                const run = store.runs()[runId];

                                patchState(store, {
                                    // Remembered so the pushed completion can find its run. The
                                    // event is scoped to the submitting *user*, so another tab's
                                    // run reaches this store too and only ids in here are reported.
                                    folderDeleteJobs: {
                                        ...store.folderDeleteJobs(),
                                        [handle.jobId]: runId
                                    },
                                    ...(run && handle.submitted !== undefined
                                        ? {
                                              runs: {
                                                  ...store.runs(),
                                                  [runId]: { ...run, total: handle.submitted }
                                              }
                                          }
                                        : {})
                                });
                            });
                    },

                    /**
                     * Publishes a finished delete's outcome, or reports that it cannot be trusted.
                     *
                     * Mirrors {@link reportUploadCompleted} deliberately: same correlation, same
                     * refusal to invent numbers. What differs is only the vocabulary of the
                     * failures it carries.
                     */
                    reportFolderDeleteCompleted: (
                        actionName: string,
                        event: DotFolderBulkDeleteCompletedEvent
                    ): void => {
                        if (!event.jobId) {
                            return;
                        }

                        // Already reported here. The only reason to see one twice is redelivery.
                        if (store.settledFolderDeleteJobs().includes(event.jobId)) {
                            return;
                        }

                        const tracked = store.folderDeleteJobs();

                        // **Ownership is the server's answer, not this map's.** The completion is
                        // pushed with `Visibility.USER` addressed to the submitter, and
                        // `UserVerifier` delivers it only to sessions whose user matches — so every
                        // completion that arrives here belongs to this author by construction.
                        //
                        // What the map answers is narrower: whether *this page* submitted the run.
                        // Requiring that was why a delete started before a reload settled in
                        // silence — the map is store state and the reload emptied it, so the event
                        // arrived about a run nothing here remembered. The author was left with a
                        // folder gone from the listing, still sitting in the sidebar tree, and no
                        // word that their delete had finished (FR-024, FR-036).
                        //
                        // `hasOwnProperty`, not `in`: the latter walks the prototype chain, so a
                        // jobId of `constructor` would read as tracked.
                        const isLocalRun = Object.prototype.hasOwnProperty.call(
                            tracked,
                            event.jobId
                        );
                        const runId = isLocalRun ? tracked[event.jobId] : undefined;

                        const remaining = { ...tracked };
                        delete remaining[event.jobId];
                        patchState(store, {
                            folderDeleteJobs: remaining,
                            settledFolderDeleteJobs: [
                                ...store.settledFolderDeleteJobs(),
                                event.jobId
                            ]
                        });

                        // Only when this page has a run to end. A reload left none, and the
                        // indicator it would have quietened went with it.
                        //
                        // Ended before the outcome is published, so the indicator is already quiet
                        // when the message about it appears.
                        if (undefined !== runId) {
                            endRun(runId);
                        }

                        // The state first, because the counters cannot answer this. An abandoned
                        // run still records the counters it reached, and publishing them would tell
                        // the author their delete finished when it did not. A cancellation IS worth
                        // reporting: the author did it, and its counts say how far it got.
                        if ('SUCCESS' !== event.state && 'CANCELED' !== event.state) {
                            httpErrorManagerService.handle(
                                new HttpErrorResponse({
                                    status: 500,
                                    statusText: `The delete did not report a usable outcome (state: ${event.state})`
                                })
                            );

                            return;
                        }

                        const closes =
                            undefined !== event.total &&
                            (event.successCount ?? 0) +
                                (event.failedCount ?? 0) +
                                (event.skippedCount ?? 0) ===
                                event.total;

                        if (!closes) {
                            // Either no counters at all, or counters that do not account for every
                            // folder. Both are unusable: trusting the zeros would report a run over
                            // nothing, and substituting the number submitted would claim every
                            // folder was deleted.
                            httpErrorManagerService.handle(
                                new HttpErrorResponse({
                                    status: 500,
                                    statusText:
                                        'The delete did not report an outcome for every folder'
                                })
                            );

                            return;
                        }

                        patchState(store, {
                            actionExecutionResults: [
                                ...store.actionExecutionResults(),
                                {
                                    actionName,
                                    successCount: event.successCount ?? 0,
                                    failedCount: event.failedCount ?? 0,
                                    skippedCount: event.skippedCount ?? 0,
                                    // Counts alone tell an author a folder failed and nothing they
                                    // can act on. The names and reasons are the point of a partial
                                    // outcome (FR-026).
                                    failures: (event.results ?? []).filter(
                                        (item) => 'SUCCESS' !== item.status
                                    ),
                                    outcomeKind: 'folderDelete',
                                    // Arrived unprompted, possibly minutes after the author moved
                                    // on, so nothing on screen reflects it — the notification is
                                    // the only way they learn (FR-024).
                                    backgrounded: true
                                }
                            ]
                        });
                    },

                    trackUploadJob: (
                        jobId: string,
                        affectedFolders: string[] = [],
                        runId?: string,
                        baseType?: string
                    ): void => {
                        patchState(store, {
                            uploadJobs: {
                                ...store.uploadJobs(),
                                [jobId]: { affectedFolders, runId, baseType }
                            }
                        });
                    },

                    /**
                     * Publishes a finished batch's outcome, or reports that it cannot be trusted.
                     *
                     * Mirrors {@link reportRefreshCompleted} deliberately: same correlation, same
                     * refusal to invent numbers. What differs is that an upload's outcome carries
                     * the folders it changed, so the shell can decide whether the listing it is
                     * showing can display the result at all.
                     */
                    reportUploadCompleted: (
                        actionName: string,
                        event: DotBulkUploadCompletedEvent
                    ): void => {
                        const tracked = store.uploadJobs();

                        // `hasOwnProperty`, not `in`: the latter walks the prototype chain, so a
                        // jobId of `constructor` or `toString` would read as tracked and destructure
                        // an inherited member. Server ids are UUIDs so it is unreachable today, and
                        // this is the shape the rest of the codebase already uses for a lookup keyed
                        // by a value that did not come from here.
                        if (
                            !event.jobId ||
                            !Object.prototype.hasOwnProperty.call(tracked, event.jobId)
                        ) {
                            // Not ours: another tab's batch, or one already settled. Silent by
                            // design — an error here would blame this author for someone else's.
                            return;
                        }

                        const { affectedFolders, runId, baseType } = tracked[event.jobId];
                        const remaining = { ...tracked };
                        delete remaining[event.jobId];
                        patchState(store, { uploadJobs: remaining });

                        // The run reporting the server phase outlives the request that started it,
                        // so this event is the only thing left that knows the batch is over. Ended
                        // before the outcome is published, so the indicator is already quiet when
                        // the message about it appears.
                        if (runId) {
                            endRun(runId);
                        }

                        // The state first, because the counters cannot answer this. A run that
                        // gave up still records the counters it reached, and those can close over
                        // `total` perfectly well — publishing them would tell the author their
                        // batch finished when it was abandoned. Only SUCCESS and CANCELED are
                        // outcomes worth reporting; a cancellation is something the author did, and
                        // its counts say how far it got before they stopped it.
                        if ('SUCCESS' !== event.state && 'CANCELED' !== event.state) {
                            httpErrorManagerService.handle(
                                new HttpErrorResponse({
                                    status: 500,
                                    statusText: `The upload did not report a usable outcome (state: ${event.state})`
                                })
                            );

                            return;
                        }

                        const closes =
                            undefined !== event.total &&
                            (event.successCount ?? 0) +
                                (event.failedCount ?? 0) +
                                (event.skippedCount ?? 0) ===
                                event.total;

                        if (!closes) {
                            // Either no counters at all, or counters that do not account for every
                            // file. Both are unusable: trusting the zeros would report a run over
                            // nothing, and the author would believe their files were never sent.
                            httpErrorManagerService.handle(
                                new HttpErrorResponse({
                                    status: 500,
                                    statusText:
                                        'The upload did not report an outcome for every file'
                                })
                            );

                            return;
                        }

                        patchState(store, {
                            actionExecutionResults: [
                                ...store.actionExecutionResults(),
                                {
                                    actionName,
                                    successCount: event.successCount ?? 0,
                                    skippedCount: event.skippedCount ?? 0,
                                    failedCount: event.failedCount ?? 0,
                                    affectedFolders,
                                    // An upload's shortfall needs its own sentence. The default is the
                                    // workflow one, which explains failures as missing permissions or
                                    // content locked by another user, and skips as the action not being
                                    // on the item's workflow step — none of which an upload can mean.
                                    partialDetailKey: 'content-drive.upload.toast.partial',
                                    // Carried whole rather than summarised here: turning results into
                                    // copy is the shell's business, and the store has no message
                                    // service to do it with.
                                    failures: event.results,
                                    duplicateSubmission: event.duplicateSubmission,
                                    // Carried because the flag alone does not say what happened to the
                                    // folder: see FR-040b.
                                    baseType,
                                    // It arrives unprompted, long after the click, so it announces
                                    // itself and must not interrupt whatever is happening now.
                                    backgrounded: true
                                }
                            ]
                        });
                    },

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
                    reportExternalResult: (result: DotContentDriveActionExecutionResult): void => {
                        patchState(store, {
                            actionExecutionResults: [...store.actionExecutionResults(), result]
                        });
                    },

                    /** Called by the shell once the result has been presented. */
                    clearActionExecutionResult: (): void => {
                        // Shifts one, rather than emptying: anything queued behind it has not been
                        // presented yet and is the next thing the shell will read.
                        patchState(store, {
                            actionExecutionResults: store.actionExecutionResults().slice(1)
                        });
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

                // Same seam for the upload: the run reports itself when it settles, which is what
                // lets the author walk away. Nothing here polls.
                eventsSocket
                    .on<DotBulkUploadCompletedEvent>(DotSystemEventType.BULK_UPLOAD_COMPLETED)
                    .pipe(takeUntilDestroyed(destroyRef))
                    .subscribe((event) => {
                        store.reportUploadCompleted(
                            dotMessageService.get('content-drive.upload'),
                            event
                        );
                    });

                // And the same again for a bulk folder delete. Three operations, one seam: the run
                // reports itself when it settles, so walking away never loses the outcome.
                eventsSocket
                    .on<DotFolderBulkDeleteCompletedEvent>(
                        DotSystemEventType.BULK_FOLDER_DELETE_COMPLETED
                    )
                    .pipe(takeUntilDestroyed(destroyRef))
                    .subscribe((event) => {
                        store.reportFolderDeleteCompleted(
                            dotMessageService.get('content-drive.context-menu.delete-folder'),
                            event
                        );
                    });
            }
        })
    );
}
