import {
    patchState,
    signalStoreFeature,
    type,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DotActiveImport, DotEventsSocket, DotSystemEventType } from '@dotcms/data-access';
import { DotImportCompletedEvent } from '@dotcms/dotcms-models';

import {
    OUTCOME_KIND,
    DotContentDriveActionExecutionResult,
    DotContentDriveRun
} from '../../../shared/models';

/**
 * How many completions of untracked imports are held for a later claim. Other tabs' imports keep
 * arriving and are never claimed here, so the oldest are dropped past this bound.
 */
const MAX_UNCLAIMED_IMPORT_COMPLETIONS = 20;

interface WithImportRunsState {
    /**
     * CSV imports this store started or restored, by job id (#37896): the run each belongs to and
     * the name the outcome is reported under. The completion reaches every tab of the submitting
     * user, and only ids in here are acted on.
     */
    importJobs: Record<string, { runId: string; fileName: string }>;
    /**
     * CSV imports already reported, by job id. A failed job is announced twice (failed, then
     * permanently failed), so this is what keeps the outcome from being shown twice.
     */
    settledImportJobs: string[];
    /**
     * Completions of CSV imports this page doesn't track yet, oldest first (PR #38054 review).
     *
     * A completion can arrive before its run is tracked, both after a reload (the job ends while
     * the running imports are being read) and on a fresh import (the event outruns the HTTP
     * response that hands the dialog the job id). Held here, tracking that job settles it at once
     * instead of starting a status nothing would end. Bounded: other tabs' imports also land here
     * and are never claimed.
     */
    unclaimedImportCompletions: DotImportCompletedEvent[];
}

/**
 * The status of CSV imports in the Drive's status toast (#37896): a run per import, started by the
 * import dialog or restored after a reload, ended by the import's completion event with its
 * created, updated and failed counts.
 *
 * Composed inside {@link withActionExecution}, whose public run methods it uses: the runs and the
 * results it reports are the ones every other operation shares, so the shell presents an import's
 * outcome like any other.
 */
export function withImportRuns() {
    return signalStoreFeature(
        {
            methods: type<{
                startExternalRun: (run: Omit<DotContentDriveRun, 'runId'>) => string;
                endExternalRun: (runId: string) => void;
                reportExternalResult: (result: DotContentDriveActionExecutionResult) => void;
            }>()
        },
        withState<WithImportRunsState>({
            importJobs: {},
            settledImportJobs: [],
            unclaimedImportCompletions: []
        }),
        withMethods((store) => {
            /**
             * Ends a tracked import's run with its created, updated and failed counts, or the
             * server's reason when it failed outright, and remembers it as reported.
             */
            const settleImportRun = (event: DotImportCompletedEvent): void => {
                const tracked = store.importJobs()[event.jobId];
                const remaining = { ...store.importJobs() };
                delete remaining[event.jobId];
                patchState(store, {
                    importJobs: remaining,
                    settledImportJobs: [...store.settledImportJobs(), event.jobId]
                });

                const created = event.created ?? 0;
                const updated = event.updated ?? 0;

                store.endExternalRun(tracked.runId);
                store.reportExternalResult({
                    actionName: tracked.fileName,
                    outcomeKind: OUTCOME_KIND.IMPORT,
                    successCount: created + updated,
                    createdCount: created,
                    updatedCount: updated,
                    failedCount: event.failed ?? 0,
                    skippedCount: 0,
                    message: event.message,
                    backgrounded: true
                });
            };

            /** Takes the held completion of a job out of the hold, if one arrived early. */
            const claimEarlyCompletion = (jobId: string): DotImportCompletedEvent | undefined => {
                const held = store.unclaimedImportCompletions();
                const early = held.find((event) => event.jobId === jobId);
                if (early) {
                    patchState(store, {
                        unclaimedImportCompletions: held.filter((event) => event !== early)
                    });
                }

                return early;
            };

            /**
             * Shows a CSV import on the status indicator and remembers it, so its completion event
             * can end the run and name the file. An import already tracked or already reported is
             * left alone: a reload can list a job that this page started. When its completion
             * already arrived, the run is ended right away with it.
             */
            const trackImportRun = (jobId: string, fileName: string): void => {
                if (store.importJobs()[jobId] || store.settledImportJobs().includes(jobId)) {
                    return;
                }

                const runId = store.startExternalRun({
                    operation: `import:${jobId}`,
                    total: 1,
                    targets: [],
                    labelKey: 'content-drive.import.indicator',
                    backgrounded: true
                });
                patchState(store, {
                    importJobs: { ...store.importJobs(), [jobId]: { runId, fileName } }
                });

                const early = claimEarlyCompletion(jobId);
                if (early) {
                    settleImportRun(early);
                }
            };

            return {
                /**
                 * Shows a CSV import the import dialog just started on the status indicator until
                 * its completion arrives.
                 */
                startImportRun: (jobId: string, fileName: string): void =>
                    trackImportRun(jobId, fileName),

                /** Puts back the status of CSV imports still running after a reload. */
                restoreImportRuns: (runs: DotActiveImport[]): void =>
                    runs.forEach((run) => trackImportRun(run.id, run.fileName ?? '')),

                /**
                 * Ends an import's run with its outcome. Previews and a job already reported are
                 * ignored: a failed job is announced twice (failed, then permanently failed). An
                 * import this page doesn't track yet is held, so tracking it later settles it.
                 */
                reportImportCompleted: (event: DotImportCompletedEvent): void => {
                    if (
                        event.cmd !== 'publish' ||
                        store.settledImportJobs().includes(event.jobId)
                    ) {
                        return;
                    }

                    if (!store.importJobs()[event.jobId]) {
                        // Another tab's import, or one of this page's whose completion outran
                        // its tracking. Held, newest last, within the bound.
                        patchState(store, {
                            unclaimedImportCompletions: [
                                ...store
                                    .unclaimedImportCompletions()
                                    .filter((held) => held.jobId !== event.jobId),
                                event
                            ].slice(-MAX_UNCLAIMED_IMPORT_COMPLETIONS)
                        });

                        return;
                    }

                    settleImportRun(event);
                }
            };
        }),
        withHooks({
            onInit(store) {
                const eventsSocket = inject(DotEventsSocket);
                const destroyRef = inject(DestroyRef);

                // Preview events are ignored: the dialog previews through the synchronous
                // `/_preview`, which emits none, and a preview queued through `/_validate` has
                // nothing here waiting on it.
                eventsSocket
                    .on<DotImportCompletedEvent>(DotSystemEventType.IMPORT_CONTENTLETS_COMPLETED)
                    .pipe(takeUntilDestroyed(destroyRef))
                    .subscribe((event) => store.reportImportCompleted(event));
            }
        })
    );
}
