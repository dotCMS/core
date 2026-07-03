import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { DestroyRef, effect, inject, untracked } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotPublishingQueueService,
    PublishingSortDirection,
    PublishingSortField,
    RetryBundlesPayload
} from '@dotcms/data-access';
import {
    BundleAssetView,
    DotMessageSeverity,
    DotMessageType,
    PublishAuditStatus,
    PublishingJobDetailView,
    PublishingJobView,
    RetryBundleResultView
} from '@dotcms/dotcms-models';

type LoadStatus = 'init' | 'loading' | 'loaded' | 'error';

/** Statuses targeted by the dialog's "SUCCESS" scope — matches legacy
 * `BundleResource#deleteAllSuccess`. */
export const PURGE_SUCCESS_STATUSES: readonly PublishAuditStatus[] = [
    PublishAuditStatus.SUCCESS,
    PublishAuditStatus.SUCCESS_WITH_WARNINGS
];

/** Statuses targeted by the dialog's "FAILED" scope — exact 5 statuses from
 * legacy `BundleResource#deleteAllFail` (does NOT include the newer
 * FAILED_INTEGRITY_CHECK / INVALID_TOKEN / LICENSE_REQUIRED to match the JSP). */
export const PURGE_FAILED_STATUSES: readonly PublishAuditStatus[] = [
    PublishAuditStatus.FAILED_TO_SEND_TO_ALL_GROUPS,
    PublishAuditStatus.FAILED_TO_SEND_TO_SOME_GROUPS,
    PublishAuditStatus.FAILED_TO_BUNDLE,
    PublishAuditStatus.FAILED_TO_SENT,
    PublishAuditStatus.FAILED_TO_PUBLISH
];

const POLL_INTERVAL_MS = 15000;

interface DotPublishingQueueState {
    bundlesRows: PublishingJobView[];
    bundlesPage: number;
    bundlesTotal: number;
    bundlesStatus: LoadStatus;
    bundlesSort: PublishingSortField | null;
    bundlesSortDirection: PublishingSortDirection;
    bundlesSelectedIds: string[];

    rowsPerPage: number;
    search: string;
    /** Status chips checked in the toolbar filter. Empty = no filter (all statuses). */
    statusFilter: PublishAuditStatus[];

    selectedBundleId: string | null;
    selectedAssets: BundleAssetView[];
    assetListStatus: LoadStatus;

    detailBundleId: string | null;
    detail: PublishingJobDetailView | null;
    detailStatus: LoadStatus;

    /** File-on-disk probes for the detail dialog's two download buttons.
     *
     * `null` = unknown (probe in flight or not yet started — buttons stay hidden).
     * `true`/`false` = HEAD probe completed; the button shows when `true`.
     *
     * We need these as state instead of computing from `detail` because the
     * existence of the `.tar.gz` (and a manifest entry inside it) is not
     * exposed on the detail response — see
     * `DotPublishingQueueService.probeBundleDownload` /
     * `probeBundleManifest` for the full rationale. */
    canDownloadBundle: boolean | null;
    canDownloadManifest: boolean | null;
}

const initialState: DotPublishingQueueState = {
    bundlesRows: [],
    bundlesPage: 1,
    bundlesTotal: 0,
    bundlesStatus: 'init',
    bundlesSort: null,
    bundlesSortDirection: 'desc',
    bundlesSelectedIds: [],

    rowsPerPage: 20,
    search: '',
    statusFilter: [],

    selectedBundleId: null,
    selectedAssets: [],
    assetListStatus: 'init',

    detailBundleId: null,
    detail: null,
    detailStatus: 'init',
    canDownloadBundle: null,
    canDownloadManifest: null
};

export const DotPublishingQueueStore = signalStore(
    withState<DotPublishingQueueState>(initialState),
    withMethods((store) => {
        const service = inject(DotPublishingQueueService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const messageDisplay = inject(DotMessageDisplayService);
        const dotMessageService = inject(DotMessageService);
        const destroyRef = inject(DestroyRef);

        let pollHandle: ReturnType<typeof setInterval> | null = null;

        /**
         * Fetches the bundles list.
         *
         * `silent` controls the visible loading state:
         * - **false** (default) — user-initiated reload: flips `bundlesStatus`
         *   to `'loading'` so the table renders skeleton rows. Used for the
         *   first fetch, search/filter/sort/page changes, and post-action
         *   refresh — anywhere the user is waiting and needs feedback.
         * - **true** — background poll: leaves the existing rows on screen
         *   and only patches in the new data when the response arrives.
         *   Prevents the every-15s skeleton flash the polling otherwise
         *   causes.
         */
        function loadBundles(silent = false) {
            if (!silent) {
                patchState(store, { bundlesStatus: 'loading' });
            }

            const filter = store.statusFilter();

            service
                .listPublishingJobs({
                    // Omit `statuses` entirely when nothing is selected so the BE returns
                    // every status. Forward-compatible with new server-side statuses (e.g.
                    // SCHEDULED, see #36267) without an FE update.
                    statuses: filter.length > 0 ? filter : undefined,
                    page: store.bundlesPage(),
                    perPage: store.rowsPerPage(),
                    filter: store.search() || undefined,
                    sort: store.bundlesSort() ?? undefined,
                    sortDirection: store.bundlesSortDirection()
                })
                .pipe(
                    take(1),
                    catchError((error) => {
                        // Silent polls swallow transient errors: keep showing the
                        // existing rows, let the next tick retry. Surfacing an
                        // error toast / red-state on a background blink would be
                        // worse UX than nothing.
                        if (!silent) {
                            httpErrorManager.handle(error);
                            patchState(store, { bundlesStatus: 'error' });
                        }

                        return EMPTY;
                    })
                )
                .subscribe((response) => {
                    patchState(store, {
                        bundlesRows: response.entity,
                        bundlesTotal: response.pagination?.totalEntries ?? 0,
                        bundlesStatus: 'loaded'
                    });
                });
        }

        function loadAssets() {
            const bundleId = store.selectedBundleId();
            if (!bundleId) {
                return;
            }

            patchState(store, { assetListStatus: 'loading', selectedAssets: [] });
            service
                .getBundleAssets(bundleId)
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { assetListStatus: 'loaded' });

                        return EMPTY;
                    })
                )
                .subscribe((assets) => {
                    patchState(store, {
                        selectedAssets: assets,
                        assetListStatus: 'loaded'
                    });
                });
        }

        function loadDetail() {
            const bundleId = store.detailBundleId();
            if (!bundleId) {
                return;
            }

            patchState(store, { detailStatus: 'loading', detail: null });
            service
                .getPublishingJobDetails(bundleId)
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { detailStatus: 'error' });

                        return EMPTY;
                    })
                )
                .subscribe((detail) => {
                    patchState(store, { detail, detailStatus: 'loaded' });
                });
        }

        /**
         * Fires two parallel HEAD probes so the detail dialog knows whether to
         * render each of its download buttons. We have to do this client-side
         * because `GET /api/v1/publishing/{bundleId}` does not expose either:
         *   - whether the `.tar.gz` is still on disk (it may have been purged
         *     via `DELETE /api/bundle/olderthan/{N}`), or
         *   - whether the archive contains a manifest entry (older bundles
         *     pre-dating the manifest feature don't).
         *
         * Two extra requests per dialog open is the price of mirroring the
         * legacy JSP's file-existence gates without a backend change. When the
         * detail response grows `hasBundle` / `hasManifest` flags, delete this
         * function and read directly from `detail`.
         */
        function probeDownloads() {
            const bundleId = store.detailBundleId();
            if (!bundleId) {
                return;
            }

            service
                .probeBundleDownload(bundleId)
                .pipe(take(1))
                .subscribe((canDownload) => {
                    patchState(store, { canDownloadBundle: canDownload });
                });

            service
                .probeBundleManifest(bundleId)
                .pipe(take(1))
                .subscribe((canDownload) => {
                    patchState(store, { canDownloadManifest: canDownload });
                });
        }

        function refresh() {
            loadBundles();
        }

        // Fires a silent refresh as soon as the tab comes back into focus so
        // the user doesn't stare at stale rows waiting for the next interval.
        // Assigned in startPolling / cleared in stopPolling so the listener
        // matches the interval's lifetime exactly.
        let onVisibilityChange: (() => void) | null = null;

        function startPolling() {
            stopPolling();
            pollHandle = setInterval(() => {
                if (document.hidden) {
                    return;
                }
                // Silent: keep existing rows on screen, only swap when the
                // response arrives. No skeleton flash every 15 s.
                loadBundles(true);
            }, POLL_INTERVAL_MS);

            onVisibilityChange = () => {
                if (!document.hidden) {
                    loadBundles(true);
                }
            };
            document.addEventListener('visibilitychange', onVisibilityChange);
        }

        function stopPolling() {
            if (pollHandle !== null) {
                clearInterval(pollHandle);
                pollHandle = null;
            }
            if (onVisibilityChange !== null) {
                document.removeEventListener('visibilitychange', onVisibilityChange);
                onVisibilityChange = null;
            }
        }

        destroyRef.onDestroy(() => stopPolling());

        return {
            loadBundles,
            loadAssets,
            loadDetail,
            refresh,
            startPolling,
            stopPolling,

            setSearch(search: string) {
                patchState(store, {
                    search,
                    bundlesPage: 1,
                    bundlesSelectedIds: []
                });
            },

            setStatusFilter(statuses: PublishAuditStatus[]) {
                patchState(store, {
                    statusFilter: statuses,
                    bundlesPage: 1,
                    bundlesSelectedIds: []
                });
            },

            setBundlesPage(page: number) {
                patchState(store, { bundlesPage: page });
            },

            /** Updates the rows-per-page size and snaps back to page 1 so the
             * user doesn't land on an out-of-range page when shrinking the
             * window. The `withHooks` effect tracks `rowsPerPage` and refetches
             * automatically. */
            setRowsPerPage(rows: number) {
                if (rows === store.rowsPerPage()) {
                    return;
                }
                patchState(store, { rowsPerPage: rows, bundlesPage: 1 });
            },

            cycleBundlesSort(field: PublishingSortField) {
                const current = store.bundlesSort();
                const dir = store.bundlesSortDirection();
                if (current !== field) {
                    patchState(store, {
                        bundlesSort: field,
                        bundlesSortDirection: 'asc',
                        bundlesPage: 1
                    });
                    return;
                }
                if (dir === 'asc') {
                    patchState(store, { bundlesSortDirection: 'desc', bundlesPage: 1 });
                    return;
                }
                patchState(store, {
                    bundlesSort: null,
                    bundlesSortDirection: 'desc',
                    bundlesPage: 1
                });
            },

            setBundlesSelection(ids: string[]) {
                patchState(store, { bundlesSelectedIds: ids });
            },

            clearBundlesSelection() {
                patchState(store, { bundlesSelectedIds: [] });
            },

            openAssetList(bundleId: string) {
                patchState(store, {
                    selectedBundleId: bundleId,
                    selectedAssets: [],
                    assetListStatus: 'init'
                });
                loadAssets();
            },

            closeAssetList() {
                patchState(store, {
                    selectedBundleId: null,
                    selectedAssets: [],
                    assetListStatus: 'init'
                });
            },

            /**
             * Removes a single asset from the currently-open bundle and refetches
             * the asset list so the row disappears. Backend returns 409 if the
             * bundle is already in progress — surfaced via httpErrorManager toast.
             */
            removeBundleAsset(assetId: string, onDone?: () => void) {
                const bundleId = store.selectedBundleId();
                if (!bundleId) {
                    return;
                }
                service
                    .removeAssetsFromBundle(bundleId, [assetId])
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe(() => {
                        loadAssets();
                        refresh();
                        onDone?.();
                    });
            },

            openDetail(bundleId: string) {
                patchState(store, {
                    detailBundleId: bundleId,
                    detail: null,
                    detailStatus: 'init',
                    canDownloadBundle: null,
                    canDownloadManifest: null
                });
                loadDetail();
                probeDownloads();
            },

            closeDetail() {
                patchState(store, {
                    detailBundleId: null,
                    detail: null,
                    detailStatus: 'init',
                    canDownloadBundle: null,
                    canDownloadManifest: null
                });
            },

            /**
             * Retries one or more bundles.
             *
             * The BE always responds with HTTP 200; per-bundle success/failure is
             * carried in `entity[].success`. HTTP-level failures (auth, network)
             * are handled by httpErrorManager as usual. Business outcomes are
             * surfaced via `DotMessageDisplayService` toasts so the user always
             * knows what happened — a common failure is "Bundle already in queue"
             * which returns 200 + success:false and would otherwise be silent.
             */
            retryBundles(payload: RetryBundlesPayload, onDone?: () => void) {
                service
                    .retryBundles(payload)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe((results) => {
                        const resolveName = (bundleId: string): string => {
                            const row = store.bundlesRows().find((r) => r.bundleId === bundleId);
                            return row?.bundleName ?? bundleId;
                        };
                        notifyRetryOutcome(results, resolveName, messageDisplay, dotMessageService);
                        refresh();
                        onDone?.();
                    });
            },

            deleteBundle(bundleId: string, onDone?: () => void) {
                service
                    .deleteBundle(bundleId)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe(() => {
                        refresh();
                        onDone?.();
                    });
            },

            /**
             * Fire-and-forget bulk delete. The BE acks immediately and runs the
             * delete on a background thread (WebSocket-notified on completion), so
             * `refresh()` here only reflects the immediate state — the user may need
             * to refresh again after the system message arrives.
             */
            deleteBundlesBulk(bundleIds: string[], onDone?: () => void) {
                if (bundleIds.length === 0) {
                    onDone?.();
                    return;
                }
                service
                    .deleteBundles(bundleIds)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe(() => {
                        patchState(store, { bundlesSelectedIds: [] });
                        refresh();
                        onDone?.();
                    });
            },

            /**
             * Fire-and-forget bulk purge by status. Omit `statuses` to use the BE's
             * safe defaults (all terminal/queued — equivalent of the JSP "ALL" button).
             */
            purgeBundles(statuses?: readonly PublishAuditStatus[], onDone?: () => void) {
                service
                    .purgeBundles(statuses)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe(() => {
                        patchState(store, { bundlesSelectedIds: [] });
                        refresh();
                        onDone?.();
                    });
            }
        };
    }),
    withHooks((store) => {
        return {
            onInit() {
                effect(() => {
                    store.search();
                    store.statusFilter();
                    store.bundlesPage();
                    store.bundlesSort();
                    store.bundlesSortDirection();
                    store.rowsPerPage();
                    untracked(() => store.loadBundles());
                });

                store.startPolling();
            }
        };
    })
);

/** Max number of per-bundle failure details listed inline in a retry toast
 * before it collapses to "and N more". Keeps the toast readable for large
 * batches while still surfacing enough context for the common 1–3 case. */
const RETRY_FAILURE_INLINE_CAP = 3;

/** How long the retry toast stays on screen. Matches the app-wide default for
 * the PrimeNG p-toast hosted by `<dot-message-display>`. */
const RETRY_TOAST_LIFE_MS = 3000;

/**
 * Picks the right toast for the outcome of a retry batch. The BE always
 * responds HTTP 200; per-bundle success/failure lives in the entity array and
 * the BE `message` field is authoritative for what actually happened. This
 * helper is exported so the store spec can hit it in isolation.
 *
 * Rules for what to surface:
 *  - Single success or single failure → BE `message` verbatim. The BE is the
 *    source of truth and its wording is what the user needs to see (e.g.
 *    "Bundle already in queue — cannot retry while publishing").
 *  - Multi all-success → summarized count. Individual BE messages are the
 *    same canned string per bundle, so listing them would be pure noise.
 *  - Multi all-failed → count header plus per-bundle "'{name}' — {message}"
 *    details, capped at `RETRY_FAILURE_INLINE_CAP` with an "and N more" tail.
 *  - Mixed → success count in the header, then the failure detail list under
 *    the same cap. Users see what succeeded AND what failed in one toast.
 *
 * Toasts are pushed via `DotMessageDisplayService` (rendered app-wide by
 * `<dot-message-display>` in the main shell). `DotGlobalMessageService` is NOT
 * used because it only shows when a portlet explicitly hosts
 * `<dot-global-message>` in its own template — the publishing queue portlet
 * does not, which is why retry outcomes were previously silent.
 *
 * `resolveName` maps a bundle id to its human-readable name (falling back to
 * the id when unresolved) so the toast never leaks raw ULIDs.
 */
export function notifyRetryOutcome(
    results: readonly RetryBundleResultView[],
    resolveName: (bundleId: string) => string,
    messageDisplay: DotMessageDisplayService,
    dotMessageService: DotMessageService
): void {
    if (results.length === 0) {
        return;
    }

    const successes = results.filter((r) => r.success);
    const failures = results.filter((r) => !r.success);

    const push = (severity: DotMessageSeverity, message: string): void => {
        messageDisplay.push({
            life: RETRY_TOAST_LIFE_MS,
            message,
            severity,
            type: DotMessageType.SIMPLE_MESSAGE
        });
    };

    if (failures.length === 0) {
        // BE returns a canned success message per bundle; for a single bundle
        // pass it through, for many we summarize with the count.
        const msg =
            successes.length === 1
                ? successes[0].message
                : dotMessageService.get(
                      'publishing-queue.retry.success.plural',
                      String(successes.length)
                  );
        push(DotMessageSeverity.SUCCESS, msg);
        return;
    }

    // Single bundle — pass the BE message through unchanged (it's the whole
    // point of the toast). Skips name-prefixing because there's no ambiguity
    // about which bundle failed when only one was attempted.
    if (failures.length === 1 && successes.length === 0) {
        push(DotMessageSeverity.ERROR, failures[0].message);
        return;
    }

    const details = buildFailureDetailList(failures, resolveName, dotMessageService);

    if (successes.length === 0) {
        push(
            DotMessageSeverity.ERROR,
            dotMessageService.get(
                'publishing-queue.retry.failed.plural',
                String(failures.length),
                details
            )
        );
        return;
    }

    push(
        DotMessageSeverity.ERROR,
        dotMessageService.get(
            'publishing-queue.retry.partial',
            String(successes.length),
            String(results.length),
            details
        )
    );
}

/** Assembles the "'Bundle A' — msg; 'Bundle B' — msg; ... and N more" detail
 * string used inside the multi-failure and mixed toasts. Capped by
 * `RETRY_FAILURE_INLINE_CAP` so the toast doesn't grow unbounded. */
function buildFailureDetailList(
    failures: readonly RetryBundleResultView[],
    resolveName: (bundleId: string) => string,
    dotMessageService: DotMessageService
): string {
    const shown = failures
        .slice(0, RETRY_FAILURE_INLINE_CAP)
        .map((f) => `'${resolveName(f.bundleId)}' — ${f.message}`)
        .join('; ');
    const remaining = failures.length - RETRY_FAILURE_INLINE_CAP;
    if (remaining <= 0) {
        return shown;
    }
    const overflow = dotMessageService.get('publishing-queue.retry.more', String(remaining));
    return `${shown} — ${overflow}`;
}
