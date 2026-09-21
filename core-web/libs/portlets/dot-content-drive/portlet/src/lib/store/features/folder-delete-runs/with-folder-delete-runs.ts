import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { computed, DestroyRef, inject, Signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { catchError, take } from 'rxjs/operators';

import {
    DotEventsSocket,
    DotFolderBulkDeleteService,
    DotSystemEventType
} from '@dotcms/data-access';
import { DotContentDriveItem, DotFolderDeleteAnnouncementEvent } from '@dotcms/dotcms-models';

import { DotContentDriveState } from '../../../shared/models';
import { isFolder } from '../../../utils/functions';

/**
 * Resolves folder paths to the keys the listing and the tree mark by.
 *
 * Against the rows **currently shown**, not the whole in-flight set: a folder nothing is rendering
 * needs no marking, and this keeps the work bounded by the page rather than by how much is being
 * deleted across the instance.
 *
 * Each match contributes **both** `inode` and `identifier`, because the search service only
 * backfills one from the other when the API returned none.
 */
const resolveRowKeys = (paths: Set<string>, items: DotContentDriveItem[]): string[] =>
    paths.size
        ? items
              .filter((item) => isFolder(item) && paths.has(item.path))
              .flatMap((item) => [item.inode, item.identifier])
              .filter((key): key is string => !!key)
        : [];

interface WithFolderDeleteRunsState {
    /**
     * Folders a bulk delete is currently working on, by the run responsible for each.
     *
     * **Keyed by run id, valued by path.** Path because that is the identity every server message
     * uses — the submission, the run's recorded parameters, and the per-folder outcome all speak
     * paths. Grouped by run so one run ending clears only its own folders, which matters as soon as
     * two are in flight.
     *
     * Covers runs started by **any** author, not only this one: a folder someone else is deleting
     * must not be presented as usable either (FR-020).
     */
    folderDeleteRuns: Record<string, string[]>;
    /**
     * Whether the load-time read has answered.
     *
     * Only distinguishes "nothing is in flight" from "we have not asked yet". Marking renders from
     * whatever is known either way — the listing never waits on this (FR-022, FR-023).
     */
    folderDeleteRunsEstablished: boolean;
}

/**
 * Server-derived in-flight state for bulk folder delete (#37063 US3).
 *
 * **Why this is not part of `withActionExecution`.** That feature owns runs *this* client fired:
 * keyed by a client-allocated id, started here and ended here. This one holds runs other authors
 * started, which this client can never end, and has to recover from a run that died without
 * announcing anything. Two lifecycles behind one name would make `busyRows` mean two things.
 *
 * Three mechanisms, each doing one job, and none of them polling:
 *
 * 1. **Establish** the set once when Content Drive opens, from the queue's active listing —
 *    filtered on run state, because that listing includes failed and abandoned runs (CR-10).
 * 2. **Keep it current** from the per-folder announcements the server makes as folders enter and
 *    leave a delete (CR-12).
 * 3. **Re-establish** on the next load rather than inheriting what the client was told — a run
 *    whose process dies never announces that it ended, so announcements alone would mark a folder
 *    indefinitely with nothing to correct it (FR-020b, CR-12).
 */
export function withFolderDeleteRuns() {
    return signalStoreFeature(
        {
            state: type<DotContentDriveState>(),
            // A required input rather than something recomputed: `busyRows` belongs to
            // `withActionExecution`, which must be installed before this feature. Reading it here is
            // what lets both sources meet in ONE computed — see `allBusyRows`.
            //
            // `props`, not `computed`: this version of @ngrx/signals names the slot `props`, and the
            // wrong key fails as "busyRows does not exist" rather than as an unknown option.
            props: type<{ busyRows: Signal<string[]> }>()
        },
        withState<WithFolderDeleteRunsState>({
            folderDeleteRuns: {},
            folderDeleteRunsEstablished: false
        }),
        withComputed((store) => ({
            /** Every folder path any in-flight run is working on, deduplicated. */
            inFlightFolderPaths: computed<string[]>(() => [
                ...new Set(Object.values(store.folderDeleteRuns()).flat())
            ]),
            /**
             * Those paths resolved to the keys the listing and the tree mark by.
             *
             * Resolved against the rows **currently shown**, not against the whole in-flight set:
             * a folder nothing is rendering needs no marking, and this keeps the work bounded by
             * the page rather than by how much is being deleted across the instance.
             *
             * Each match contributes **both** `inode` and `identifier`, because the search service
             * only backfills one from the other when the API returned none — so neither is
             * reliably the key a given row carries.
             */
            inFlightFolderKeys: computed<string[]>(() =>
                resolveRowKeys(
                    new Set(Object.values(store.folderDeleteRuns()).flat()),
                    store.items()
                )
            ),
            /**
             * Every row key that should render as busy, from **both** sources.
             *
             * `busyRows` covers runs this client fired, of any kind; the server-derived half covers
             * folder deletes the server knows about, this client's and other authors' alike.
             *
             * Merged here, once, so the listing and the sidebar tree read the same answer. Two
             * derivations would drift, and the drift reads as a folder inert in one surface and
             * usable in the other — worse than marking neither, because it teaches the author that
             * the marking cannot be trusted (FR-014).
             */
            allBusyRows: computed<string[]>(() => [
                ...new Set([
                    ...store.busyRows(),
                    ...resolveRowKeys(
                        new Set(Object.values(store.folderDeleteRuns()).flat()),
                        store.items()
                    )
                ])
            ])
        })),
        withMethods(
            (
                store,
                folderBulkDeleteService = inject(DotFolderBulkDeleteService),
                destroyRef = inject(DestroyRef)
            ) => {
                /** Adds a folder under the run responsible for it, without duplicating it. */
                const markInFlight = (jobId: string, path: string): void => {
                    const runs = store.folderDeleteRuns();
                    const paths = runs[jobId] ?? [];

                    if (paths.includes(path)) {
                        return;
                    }

                    patchState(store, {
                        folderDeleteRuns: { ...runs, [jobId]: [...paths, path] }
                    });
                };

                return {
                    /**
                     * Reads the queue's in-flight runs and **replaces** the set from them.
                     *
                     * Replaces rather than merges: this is what recovers from a run that died
                     * without announcing its end. Inheriting what the client was previously told
                     * would keep such a folder marked forever (FR-020b).
                     */
                    establishInFlightFolders: (): void => {
                        folderBulkDeleteService
                            .readActiveRuns()
                            .pipe(
                                take(1),
                                // The service already answers `[]` on failure; this is the belt to
                                // its braces, so a throw here can never leave the portlet unusable.
                                catchError(() => EMPTY),
                                takeUntilDestroyed(destroyRef)
                            )
                            .subscribe((runs) => {
                                patchState(store, {
                                    folderDeleteRuns: runs.reduce<Record<string, string[]>>(
                                        (acc, run) => {
                                            acc[run.id] = run.paths;

                                            return acc;
                                        },
                                        {}
                                    ),
                                    folderDeleteRunsEstablished: true
                                });
                            });
                    },

                    /** Marks a folder a run has just started working on. */
                    markInFlightFolder: (event: DotFolderDeleteAnnouncementEvent): void => {
                        markInFlight(event.jobId, event.path);
                    },

                    /**
                     * Drops one folder from the set, on the announcement that its run finished with
                     * it — **whether the delete succeeded or failed**.
                     *
                     * A failed delete leaves the folder intact and usable, so keeping it marked
                     * until the framework moves the run on is indistinguishable, to the author,
                     * from a folder nobody can touch (FR-021).
                     */
                    clearInFlightFolder: (event: DotFolderDeleteAnnouncementEvent): void => {
                        const runs = store.folderDeleteRuns();
                        const paths = runs[event.jobId];

                        if (!paths) {
                            return;
                        }

                        const remaining = paths.filter((path) => path !== event.path);
                        const next = { ...runs };

                        if (remaining.length) {
                            next[event.jobId] = remaining;
                        } else {
                            delete next[event.jobId];
                        }

                        patchState(store, { folderDeleteRuns: next });
                    },

                    /** Drops every folder a run was covering, when the run itself ends. */
                    clearRun: (jobId: string): void => {
                        const next = { ...store.folderDeleteRuns() };
                        delete next[jobId];

                        patchState(store, { folderDeleteRuns: next });
                    }
                };
            }
        ),
        withHooks({
            onInit(store, socket = inject(DotEventsSocket), destroyRef = inject(DestroyRef)) {
                // Two streams, two jobs. A folder entering a delete and a folder leaving one are
                // separate announcements precisely so a client can act on each without inferring
                // the other from a state machine it does not own.
                socket
                    .on<DotFolderDeleteAnnouncementEvent>(DotSystemEventType.FOLDER_DELETE_STARTED)
                    .pipe(takeUntilDestroyed(destroyRef))
                    .subscribe((event) => store.markInFlightFolder(event));

                socket
                    .on<DotFolderDeleteAnnouncementEvent>(DotSystemEventType.FOLDER_DELETE_FINISHED)
                    .pipe(takeUntilDestroyed(destroyRef))
                    .subscribe((event) => store.clearInFlightFolder(event));
            }
        })
    );
}
