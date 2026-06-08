import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY, forkJoin } from 'rxjs';

import { DestroyRef, effect, inject, untracked } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotPublishingQueueService,
    PublishingSortDirection,
    PublishingSortField,
    PushBundlePayload,
    RetryBundlesPayload
} from '@dotcms/data-access';
import {
    BundleAssetView,
    DotEnvironment,
    IN_PROGRESS_STATUSES,
    PublishAuditStatus,
    PublishingJobDetailView,
    PublishingJobView,
    READY_STATUSES
} from '@dotcms/dotcms-models';

type LoadStatus = 'init' | 'loading' | 'loaded' | 'error';

const HISTORY_STATUSES: readonly PublishAuditStatus[] = [
    PublishAuditStatus.SUCCESS,
    PublishAuditStatus.SUCCESS_WITH_WARNINGS,
    PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY,
    PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY,
    PublishAuditStatus.FAILED_TO_SEND_TO_ALL_GROUPS,
    PublishAuditStatus.FAILED_TO_SEND_TO_SOME_GROUPS,
    PublishAuditStatus.FAILED_TO_BUNDLE,
    PublishAuditStatus.FAILED_TO_SENT,
    PublishAuditStatus.FAILED_TO_PUBLISH,
    PublishAuditStatus.FAILED_INTEGRITY_CHECK,
    PublishAuditStatus.INVALID_TOKEN,
    PublishAuditStatus.LICENSE_REQUIRED
];

const POLL_INTERVAL_MS = 15000;

export type ActiveTab = 'queue' | 'history';

interface DotPublishingQueueState {
    activeTab: ActiveTab;

    readyRows: PublishingJobView[];
    readyPage: number;
    readyTotal: number;
    readyStatus: LoadStatus;

    progressRows: PublishingJobView[];
    progressPage: number;
    progressTotal: number;
    progressStatus: LoadStatus;

    historyRows: PublishingJobView[];
    historyPage: number;
    historyTotal: number;
    historyStatus: LoadStatus;
    historySort: PublishingSortField | null;
    historySortDirection: PublishingSortDirection;
    historySelectedIds: string[];

    rowsPerPage: number;
    search: string;
    siteId: string | null;

    selectedBundleId: string | null;
    selectedAssets: BundleAssetView[];
    assetListStatus: LoadStatus;

    detailBundleId: string | null;
    detail: PublishingJobDetailView | null;
    detailStatus: LoadStatus;

    environments: DotEnvironment[];
    environmentsStatus: LoadStatus;

    pushBundleTarget: PublishingJobView | null;
    pushInFlight: boolean;

    uploadInFlight: boolean;
    uploadProgress: number;
}

const initialState: DotPublishingQueueState = {
    activeTab: 'queue',

    readyRows: [],
    readyPage: 1,
    readyTotal: 0,
    readyStatus: 'init',

    progressRows: [],
    progressPage: 1,
    progressTotal: 0,
    progressStatus: 'init',

    historyRows: [],
    historyPage: 1,
    historyTotal: 0,
    historyStatus: 'init',
    historySort: null,
    historySortDirection: 'desc',
    historySelectedIds: [],

    rowsPerPage: 10,
    search: '',
    siteId: null,

    selectedBundleId: null,
    selectedAssets: [],
    assetListStatus: 'init',

    detailBundleId: null,
    detail: null,
    detailStatus: 'init',

    environments: [],
    environmentsStatus: 'init',

    pushBundleTarget: null,
    pushInFlight: false,

    uploadInFlight: false,
    uploadProgress: 0
};

export const DotPublishingQueueStore = signalStore(
    withState<DotPublishingQueueState>(initialState),
    withMethods((store) => {
        const service = inject(DotPublishingQueueService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const destroyRef = inject(DestroyRef);

        let pollHandle: ReturnType<typeof setInterval> | null = null;

        function loadReady() {
            patchState(store, { readyStatus: 'loading' });
            service
                .listPublishingJobs({
                    statuses: READY_STATUSES,
                    page: store.readyPage(),
                    perPage: store.rowsPerPage(),
                    filter: store.search() || undefined
                })
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { readyStatus: 'error' });

                        return EMPTY;
                    })
                )
                .subscribe((response) => {
                    patchState(store, {
                        readyRows: response.entity,
                        readyTotal: response.pagination?.totalEntries ?? 0,
                        readyStatus: 'loaded'
                    });
                });
        }

        function loadProgress() {
            patchState(store, { progressStatus: 'loading' });
            service
                .listPublishingJobs({
                    statuses: IN_PROGRESS_STATUSES,
                    page: store.progressPage(),
                    perPage: store.rowsPerPage(),
                    filter: store.search() || undefined
                })
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { progressStatus: 'error' });

                        return EMPTY;
                    })
                )
                .subscribe((response) => {
                    patchState(store, {
                        progressRows: response.entity,
                        progressTotal: response.pagination?.totalEntries ?? 0,
                        progressStatus: 'loaded'
                    });
                });
        }

        function loadHistory() {
            patchState(store, { historyStatus: 'loading' });
            service
                .listPublishingJobs({
                    statuses: HISTORY_STATUSES,
                    page: store.historyPage(),
                    perPage: store.rowsPerPage(),
                    filter: store.search() || undefined,
                    sort: store.historySort() ?? undefined,
                    sortDirection: store.historySortDirection()
                })
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { historyStatus: 'error' });

                        return EMPTY;
                    })
                )
                .subscribe((response) => {
                    patchState(store, {
                        historyRows: response.entity,
                        historyTotal: response.pagination?.totalEntries ?? 0,
                        historyStatus: 'loaded'
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

        function loadEnvironments() {
            if (
                store.environmentsStatus() === 'loading' ||
                store.environmentsStatus() === 'loaded'
            ) {
                return;
            }
            patchState(store, { environmentsStatus: 'loading' });
            service
                .getEnvironments()
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { environmentsStatus: 'error' });

                        return EMPTY;
                    })
                )
                .subscribe((environments) => {
                    patchState(store, { environments, environmentsStatus: 'loaded' });
                });
        }

        function refresh() {
            const tab = store.activeTab();
            if (tab === 'queue') {
                loadReady();
                loadProgress();
            } else {
                loadHistory();
            }
        }

        function refreshProgressOnly() {
            if (store.activeTab() === 'queue') {
                loadProgress();
            }
        }

        function startPolling() {
            stopPolling();
            pollHandle = setInterval(() => {
                if (document.hidden) {
                    return;
                }
                refreshProgressOnly();
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
            loadReady,
            loadProgress,
            loadHistory,
            loadAssets,
            loadDetail,
            loadEnvironments,
            refresh,
            startPolling,
            stopPolling,

            setActiveTab(tab: ActiveTab) {
                patchState(store, { activeTab: tab });
            },

            setSearch(search: string) {
                patchState(store, {
                    search,
                    readyPage: 1,
                    progressPage: 1,
                    historyPage: 1,
                    historySelectedIds: []
                });
            },

            setSiteId(siteId: string | null) {
                patchState(store, {
                    siteId,
                    readyPage: 1,
                    progressPage: 1,
                    historyPage: 1
                });
            },

            setReadyPage(page: number) {
                patchState(store, { readyPage: page });
            },

            setProgressPage(page: number) {
                patchState(store, { progressPage: page });
            },

            setHistoryPage(page: number) {
                patchState(store, { historyPage: page });
            },

            cycleHistorySort(field: PublishingSortField) {
                const current = store.historySort();
                const dir = store.historySortDirection();
                if (current !== field) {
                    patchState(store, {
                        historySort: field,
                        historySortDirection: 'asc',
                        historyPage: 1
                    });
                    return;
                }
                if (dir === 'asc') {
                    patchState(store, { historySortDirection: 'desc', historyPage: 1 });
                    return;
                }
                patchState(store, {
                    historySort: null,
                    historySortDirection: 'desc',
                    historyPage: 1
                });
            },

            setHistorySelection(ids: string[]) {
                patchState(store, { historySelectedIds: ids });
            },

            clearHistorySelection() {
                patchState(store, { historySelectedIds: [] });
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

            openDetail(bundleId: string) {
                patchState(store, {
                    detailBundleId: bundleId,
                    detail: null,
                    detailStatus: 'init'
                });
                loadDetail();
            },

            closeDetail() {
                patchState(store, {
                    detailBundleId: null,
                    detail: null,
                    detailStatus: 'init'
                });
            },

            openConfigureSend(bundle: PublishingJobView) {
                patchState(store, { pushBundleTarget: bundle });
                loadEnvironments();
            },

            closeConfigureSend() {
                patchState(store, { pushBundleTarget: null });
            },

            submitPush(bundleId: string, payload: PushBundlePayload, onDone: () => void) {
                patchState(store, { pushInFlight: true });
                service
                    .pushBundle(bundleId, payload)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { pushInFlight: false });
                            return EMPTY;
                        })
                    )
                    .subscribe(() => {
                        patchState(store, { pushInFlight: false, pushBundleTarget: null });
                        refresh();
                        onDone();
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

            deleteBundlesBulk(bundleIds: string[], onDone?: () => void) {
                // Fans out per-id until BE adds the bulk DELETE in #36046.
                // Uses forkJoin so we refresh once after every id resolves (success or skip).
                forkJoin(
                    bundleIds.map((id) =>
                        service.deleteBundle(id).pipe(
                            take(1),
                            catchError((error) => {
                                httpErrorManager.handle(error);
                                return EMPTY;
                            })
                        )
                    )
                )
                    .pipe(take(1))
                    .subscribe(() => {
                        patchState(store, { historySelectedIds: [] });
                        refresh();
                        onDone?.();
                    });
            },

            generateBundle(bundleId: string, filterKey: string, onDone?: () => void) {
                service
                    .generateBundle(bundleId, filterKey)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe(() => onDone?.());
            },

            uploadBundle(file: File, onDone?: () => void) {
                patchState(store, { uploadInFlight: true, uploadProgress: 0 });
                service
                    .uploadBundle(file)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { uploadInFlight: false, uploadProgress: 0 });
                            return EMPTY;
                        })
                    )
                    .subscribe(() => {
                        patchState(store, { uploadInFlight: false, uploadProgress: 100 });
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
                    const tab = store.activeTab();
                    store.search();
                    if (tab === 'queue') {
                        store.readyPage();
                        store.progressPage();
                        untracked(() => {
                            store.loadReady();
                            store.loadProgress();
                        });
                    } else {
                        store.historyPage();
                        store.historySort();
                        store.historySortDirection();
                        untracked(() => store.loadHistory());
                    }
                });

                store.startPolling();
            }
        };
    })
);
