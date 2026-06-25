import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { DestroyRef, effect, inject, untracked } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotPublishingQueueService,
    PublishingSortDirection,
    PublishingSortField,
    RetryBundlesPayload
} from '@dotcms/data-access';
import {
    BundleAssetView,
    PublishAuditStatus,
    PublishingJobDetailView,
    PublishingJobView
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
    /** Asset list shown inside the Bundle Details modal (separate from the standalone
     * Asset List modal's `selectedAssets` so the two modals don't fight over state). */
    detailAssets: BundleAssetView[];
    detailAssetsStatus: LoadStatus;

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
    detailAssets: [],
    detailAssetsStatus: 'init',
    canDownloadBundle: null,
    canDownloadManifest: null
};

export const DotPublishingQueueStore = signalStore(
    withState<DotPublishingQueueState>(initialState),
    withMethods((store) => {
        const service = inject(DotPublishingQueueService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const destroyRef = inject(DestroyRef);

        let pollHandle: ReturnType<typeof setInterval> | null = null;

        function loadBundles() {
            patchState(store, { bundlesStatus: 'loading' });

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
                        httpErrorManager.handle(error);
                        patchState(store, { bundlesStatus: 'error' });

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

        /**
         * Lazy-loads the assets that travelled in a bundle. Backed by the legacy
         * `GET /api/bundle/{bundleId}/assets` (the only endpoint that returns the
         * full list — `/api/v1/publishing/{bundleId}` only carries metadata +
         * endpoints + a 3-item assetPreview).
         */
        function loadDetailAssets() {
            const bundleId = store.detailBundleId();
            if (!bundleId) {
                return;
            }

            patchState(store, { detailAssetsStatus: 'loading', detailAssets: [] });
            service
                .getBundleAssets(bundleId)
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { detailAssetsStatus: 'loaded' });

                        return EMPTY;
                    })
                )
                .subscribe((assets) => {
                    patchState(store, {
                        detailAssets: assets,
                        detailAssetsStatus: 'loaded'
                    });
                });
        }

        function refresh() {
            loadBundles();
        }

        function startPolling() {
            stopPolling();
            pollHandle = setInterval(() => {
                if (document.hidden) {
                    return;
                }
                loadBundles();
            }, POLL_INTERVAL_MS);
        }

        function stopPolling() {
            if (pollHandle !== null) {
                clearInterval(pollHandle);
                pollHandle = null;
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
                    detailAssets: [],
                    detailAssetsStatus: 'init',
                    canDownloadBundle: null,
                    canDownloadManifest: null
                });
                loadDetail();
                loadDetailAssets();
                probeDownloads();
            },

            loadDetailAssets,

            closeDetail() {
                patchState(store, {
                    detailBundleId: null,
                    detail: null,
                    detailStatus: 'init',
                    detailAssets: [],
                    detailAssetsStatus: 'init',
                    canDownloadBundle: null,
                    canDownloadManifest: null
                });
            },

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
                    .subscribe(() => {
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
