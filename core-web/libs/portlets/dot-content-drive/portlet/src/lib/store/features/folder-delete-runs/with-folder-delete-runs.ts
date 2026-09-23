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
import {
    DotContentDriveItem,
    DotFolderBulkDeleteCompletedEvent,
    DotFolderDeleteAnnouncementEvent
} from '@dotcms/dotcms-models';

import { SYSTEM_HOST, SYSTEM_HOST_PATH } from '../../../shared/constants';
import { DotContentDriveState } from '../../../shared/models';
import { isFolder, normalizeFolderRef, toFolderRef } from '../../../utils/functions';

/**
 * The site a listed row belongs to, as a hostname.
 *
 * Chosen the way {@link browsedFolderRef} chooses it, and for the same reason: System Host belongs
 * to no site, so pairing its reserved location value with whatever hostname the switcher happens to
 * show names a place that does not exist. Every row in a listing is on the browsed site, so one
 * hostname answers for all of them.
 */
const rowHostnameOf = (
    location: string | undefined,
    siteHostname: string | undefined
): string | undefined => (location === SYSTEM_HOST_PATH ? SYSTEM_HOST.hostname : siteHostname);

/**
 * Resolves in-flight folder references to the keys the listing and the tree mark by.
 *
 * **The two sides do not speak the same path**, which is the whole reason this goes through
 * {@link toFolderRef}. A run names its folders the way the server does — site-qualified
 * `//demo.dotcms.com/old-a/` — while a listed row carries a bare path from the site root, `/old-a/`.
 * Compared as given they never match, so nothing is ever marked and the feature silently does
 * nothing. `toFolderRef` also lower-cases and drops the trailing slash, because dotCMS resolves
 * asset paths through a unique index over the lower-cased full path per host: two spellings of one
 * folder really are one folder, and comparing them literally would miss half of them.
 *
 * Against the rows **currently shown**, not the whole in-flight set: a folder nothing is rendering
 * needs no marking, and this keeps the work bounded by the page rather than by how much is being
 * deleted across the instance.
 *
 * Each match contributes **both** `inode` and `identifier`, because the search service only
 * backfills one from the other when the API returned none.
 */
const resolveRowKeys = (
    refs: Set<string>,
    items: DotContentDriveItem[],
    hostname: string | undefined
): string[] =>
    refs.size
        ? items
              .filter((item) => isFolder(item) && refs.has(toFolderRef(hostname, item.path)))
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
        withComputed((store) => {
            /** Every folder any in-flight run is working on, as canonical refs. */
            const inFlightRefs = computed<Set<string>>(
                () => new Set(Object.values(store.folderDeleteRuns()).flat())
            );

            /**
             * Those refs resolved to the keys the listing and the tree mark by.
             *
             * Named once and reused by `allBusyRows` below: a second derivation of the same thing
             * is a second chance to drift, and the drift reads as a folder inert in one surface and
             * usable in the other.
             */
            const inFlightFolderKeys = computed<string[]>(() =>
                resolveRowKeys(
                    inFlightRefs(),
                    store.items(),
                    rowHostnameOf(store.path(), store.currentSite()?.hostname)
                )
            );

            return {
                /** Every folder any in-flight run is working on, deduplicated. */
                inFlightFolderPaths: computed<string[]>(() => [...inFlightRefs()]),
                inFlightFolderKeys,
                /**
                 * Every row key that should render as busy, from **both** sources.
                 *
                 * `busyRows` covers runs this client fired, of any kind; the server-derived half
                 * covers folder deletes the server knows about, this client's and other authors'
                 * alike.
                 *
                 * Merged here, once, so the listing and the sidebar tree read the same answer. Two
                 * derivations would drift, and the drift reads as a folder inert in one surface and
                 * usable in the other — worse than marking neither, because it teaches the author
                 * that the marking cannot be trusted (FR-014).
                 */
                allBusyRows: computed<string[]>(() => [
                    ...new Set([...store.busyRows(), ...inFlightFolderKeys()])
                ])
            };
        }),
        withMethods(
            (
                store,
                folderBulkDeleteService = inject(DotFolderBulkDeleteService),
                destroyRef = inject(DestroyRef)
            ) => {
                /**
                 * Adds a folder under the run responsible for it, without duplicating it.
                 *
                 * Stored as a canonical ref rather than as the server spelled it, so everything
                 * downstream — the row match, and the removal below — compares like with like. The
                 * listing and the announcements are two separate server messages about the same
                 * folder, and nothing guarantees they agree on case or a trailing slash.
                 */
                const markInFlight = (jobId: string, path: string): void => {
                    const runs = store.folderDeleteRuns();
                    const paths = runs[jobId] ?? [];
                    const ref = normalizeFolderRef(path);

                    if (paths.includes(ref)) {
                        return;
                    }

                    patchState(store, {
                        folderDeleteRuns: { ...runs, [jobId]: [...paths, ref] }
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
                                            acc[run.id] = run.paths.map(normalizeFolderRef);

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

                        const ref = normalizeFolderRef(event.path);
                        const remaining = paths.filter((path) => path !== ref);
                        const next = { ...runs };

                        if (remaining.length) {
                            next[event.jobId] = remaining;
                        } else {
                            delete next[event.jobId];
                        }

                        patchState(store, { folderDeleteRuns: next });
                    },

                    /**
                     * Drops every folder a run was covering, when the run itself ends.
                     *
                     * The **submitter's** only way out of the server-derived marking. Both
                     * per-folder announcements are pushed with `EXCLUDE_OWNER`, so whoever started
                     * the run never hears their own folders leave it. The completion event they
                     * *do* receive is handled by `reportFolderDeleteCompleted`, which publishes the
                     * outcome and ends any local run — but it knows nothing about
                     * `folderDeleteRuns`, which lives in this feature and composes after it.
                     * Without this, a folder marked from the load-time listing stays marked until
                     * the next reload, which is precisely the inert folder FR-021 forbids.
                     *
                     * Keyed by job rather than by path because a completion names the run, not the
                     * folders: by the time it arrives a successful delete has left no folder to name.
                     */
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

                // The third signal, and the one that closes the submitter's gap. Unlike the two
                // above it is scoped to whoever submitted the run, which is exactly the audience
                // `EXCLUDE_OWNER` denies the announcements to.
                //
                // Acted on unconditionally, for the same reason `reportFolderDeleteCompleted`
                // reports unconditionally: the server pushes this with `Visibility.USER` addressed
                // to the submitter, so every one that arrives names a run this author started —
                // whichever of their tabs happens to be listening. The run it names has ended
                // whatever this page remembers, and a job id that marks nothing here removes
                // nothing.
                socket
                    .on<DotFolderBulkDeleteCompletedEvent>(
                        DotSystemEventType.BULK_FOLDER_DELETE_COMPLETED
                    )
                    .pipe(takeUntilDestroyed(destroyRef))
                    .subscribe((event) => {
                        if (event?.jobId) {
                            store.clearRun(event.jobId);
                        }
                    });
            }
        })
    );
}
