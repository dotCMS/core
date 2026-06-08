import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { effect, inject, untracked } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotPublishingQueueService } from '@dotcms/data-access';
import {
    BundleAssetView,
    IN_PROGRESS_STATUSES,
    PublishingJobView,
    READY_STATUSES
} from '@dotcms/dotcms-models';

type LoadStatus = 'init' | 'loading' | 'loaded' | 'error';

interface DotPublishingQueueState {
    readyRows: PublishingJobView[];
    readyPage: number;
    readyTotal: number;
    readyStatus: LoadStatus;

    progressRows: PublishingJobView[];
    progressPage: number;
    progressTotal: number;
    progressStatus: LoadStatus;

    rowsPerPage: number;
    search: string;

    selectedBundleId: string | null;
    selectedAssets: BundleAssetView[];
    assetListStatus: LoadStatus;
}

const initialState: DotPublishingQueueState = {
    readyRows: [],
    readyPage: 1,
    readyTotal: 0,
    readyStatus: 'init',

    progressRows: [],
    progressPage: 1,
    progressTotal: 0,
    progressStatus: 'init',

    rowsPerPage: 10,
    search: '',

    selectedBundleId: null,
    selectedAssets: [],
    assetListStatus: 'init'
};

export const DotPublishingQueueStore = signalStore(
    withState<DotPublishingQueueState>(initialState),
    withMethods((store) => {
        const service = inject(DotPublishingQueueService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

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

        return {
            loadReady,
            loadProgress,
            loadAssets,

            setSearch(search: string) {
                patchState(store, { search, readyPage: 1, progressPage: 1 });
            },

            setReadyPage(page: number) {
                patchState(store, { readyPage: page });
            },

            setProgressPage(page: number) {
                patchState(store, { progressPage: page });
            },

            refresh() {
                loadReady();
                loadProgress();
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
            }
        };
    }),
    withHooks((store) => {
        return {
            onInit() {
                effect(() => {
                    store.search();
                    store.readyPage();
                    store.progressPage();

                    untracked(() => {
                        store.loadReady();
                        store.loadProgress();
                    });
                });
            }
        };
    })
);
