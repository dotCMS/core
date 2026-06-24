import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotHttpErrorManagerService, DotPublishingQueueService } from '@dotcms/data-access';
import {
    BundleAssetView,
    PublishAuditStatus,
    PublishingJobDetailView,
    PublishingJobsResponse,
    PublishingJobView
} from '@dotcms/dotcms-models';

import { DotPublishingQueueStore } from './dot-publishing-queue.store';

const buildJob = (overrides: Partial<PublishingJobView> = {}): PublishingJobView => ({
    bundleId: 'bundle-1',
    bundleName: 'Bundle One',
    status: PublishAuditStatus.WAITING_FOR_PUBLISHING,
    filterName: null,
    filterKey: null,
    assetCount: 5,
    assetPreview: [],
    environmentCount: 1,
    createDate: '2026-06-08T10:00:00Z',
    statusUpdated: null,
    numTries: 0,
    ...overrides
});

const BUNDLES_RESPONSE: PublishingJobsResponse = {
    entity: [
        buildJob({ bundleId: 'bundle-A', status: PublishAuditStatus.BUNDLING }),
        buildJob({ bundleId: 'bundle-B', status: PublishAuditStatus.SUCCESS })
    ],
    pagination: { currentPage: 1, perPage: 10, totalEntries: 2 }
};

const MOCK_ASSETS: BundleAssetView[] = [
    { asset: 'a1', title: 'Asset 1', type: 'contentlet' },
    { asset: 'a2', title: 'Asset 2', type: 'template' }
];

const MOCK_DETAIL: PublishingJobDetailView = {
    bundleId: 'bundle-A',
    bundleName: 'Bundle One',
    status: PublishAuditStatus.SUCCESS,
    filterName: null,
    filterKey: null,
    assetCount: 1,
    environments: [],
    timestamps: {
        bundleStart: null,
        bundleEnd: null,
        publishStart: null,
        publishEnd: null,
        createDate: '2026-06-08T10:00:00Z',
        statusUpdated: null
    },
    numTries: 1
};

describe('DotPublishingQueueStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotPublishingQueueStore>>;
    let store: InstanceType<typeof DotPublishingQueueStore>;
    let service: jest.Mocked<DotPublishingQueueService>;
    let httpErrorManager: jest.Mocked<DotHttpErrorManagerService>;

    const createService = createServiceFactory({
        service: DotPublishingQueueStore,
        providers: [
            mockProvider(DotPublishingQueueService, {
                listPublishingJobs: jest.fn().mockReturnValue(of(BUNDLES_RESPONSE)),
                getBundleAssets: jest.fn().mockReturnValue(of(MOCK_ASSETS)),
                getPublishingJobDetails: jest.fn().mockReturnValue(of(MOCK_DETAIL)),
                probeBundleDownload: jest.fn().mockReturnValue(of(true)),
                probeBundleManifest: jest.fn().mockReturnValue(of(true)),
                removeAssetsFromBundle: jest
                    .fn()
                    .mockReturnValue(of([{ assetId: 'a1', success: true, message: 'ok' }])),
                retryBundles: jest.fn().mockReturnValue(of([])),
                deleteBundle: jest.fn().mockReturnValue(of({ message: 'ok' })),
                deleteBundles: jest.fn().mockReturnValue(of({ entity: 'ok' })),
                purgeBundles: jest.fn().mockReturnValue(of({ entity: { message: 'ok' } }))
            }),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        service = spectator.inject(
            DotPublishingQueueService
        ) as jest.Mocked<DotPublishingQueueService>;
        httpErrorManager = spectator.inject(
            DotHttpErrorManagerService
        ) as jest.Mocked<DotHttpErrorManagerService>;
        spectator.flushEffects();
    });

    afterEach(() => {
        store.stopPolling();
    });

    describe('onInit', () => {
        it('loads bundles once on init with no status filter (BE returns all)', () => {
            expect(service.listPublishingJobs).toHaveBeenCalledTimes(1);
            expect(service.listPublishingJobs).toHaveBeenCalledWith(
                expect.objectContaining({ statuses: undefined })
            );
            expect(store.bundlesStatus()).toBe('loaded');
            expect(store.bundlesRows().length).toBe(2);
        });
    });

    describe('setSearch', () => {
        it('resets page and selection, then refetches with the search filter', () => {
            store.setBundlesPage(3);
            store.setBundlesSelection(['x']);
            spectator.flushEffects();
            (service.listPublishingJobs as jest.Mock).mockClear();

            store.setSearch('term');
            spectator.flushEffects();

            expect(store.search()).toBe('term');
            expect(store.bundlesPage()).toBe(1);
            expect(store.bundlesSelectedIds()).toEqual([]);
            expect(service.listPublishingJobs).toHaveBeenCalledWith(
                expect.objectContaining({ filter: 'term' })
            );
        });
    });

    describe('setStatusFilter', () => {
        it('forwards only the chosen statuses on the next list call', () => {
            const filter = [PublishAuditStatus.BUNDLING, PublishAuditStatus.WAITING_FOR_PUBLISHING];
            (service.listPublishingJobs as jest.Mock).mockClear();
            store.setStatusFilter(filter);
            spectator.flushEffects();

            expect(service.listPublishingJobs).toHaveBeenCalledWith(
                expect.objectContaining({ statuses: filter })
            );
            expect(store.bundlesPage()).toBe(1);
            expect(store.bundlesSelectedIds()).toEqual([]);
        });

        it('omits the statuses param when the filter is empty so BE returns every status', () => {
            store.setStatusFilter([PublishAuditStatus.SUCCESS]);
            spectator.flushEffects();
            (service.listPublishingJobs as jest.Mock).mockClear();

            store.setStatusFilter([]);
            spectator.flushEffects();

            expect(service.listPublishingJobs).toHaveBeenCalledWith(
                expect.objectContaining({ statuses: undefined })
            );
        });
    });

    describe('cycleBundlesSort', () => {
        it('cycles asc → desc → off for the same field', () => {
            store.cycleBundlesSort('bundle_name');
            expect(store.bundlesSort()).toBe('bundle_name');
            expect(store.bundlesSortDirection()).toBe('asc');

            store.cycleBundlesSort('bundle_name');
            expect(store.bundlesSortDirection()).toBe('desc');

            store.cycleBundlesSort('bundle_name');
            expect(store.bundlesSort()).toBeNull();
        });

        it('switching field starts asc again', () => {
            store.cycleBundlesSort('bundle_name');
            store.cycleBundlesSort('status');
            expect(store.bundlesSort()).toBe('status');
            expect(store.bundlesSortDirection()).toBe('asc');
        });
    });

    describe('refresh', () => {
        it('reloads bundles', () => {
            (service.listPublishingJobs as jest.Mock).mockClear();
            store.refresh();
            expect(service.listPublishingJobs).toHaveBeenCalledTimes(1);
        });
    });

    describe('openAssetList / closeAssetList', () => {
        it('opens and loads assets', () => {
            store.openAssetList('B-X');
            expect(store.selectedBundleId()).toBe('B-X');
            expect(service.getBundleAssets).toHaveBeenCalledWith('B-X');
            expect(store.selectedAssets()).toEqual(MOCK_ASSETS);
            expect(store.assetListStatus()).toBe('loaded');
        });

        it('closes clears state', () => {
            store.openAssetList('B-X');
            store.closeAssetList();
            expect(store.selectedBundleId()).toBeNull();
            expect(store.selectedAssets()).toEqual([]);
        });
    });

    describe('removeBundleAsset', () => {
        beforeEach(() => {
            (service.removeAssetsFromBundle as jest.Mock).mockClear();
        });

        it('calls service.removeAssetsFromBundle with [assetId] and refetches assets', () => {
            store.openAssetList('B-X');
            (service.getBundleAssets as jest.Mock).mockClear();
            store.removeBundleAsset('a1');
            expect(service.removeAssetsFromBundle).toHaveBeenCalledWith('B-X', ['a1']);
            expect(service.getBundleAssets).toHaveBeenCalledWith('B-X');
        });

        it('is a no-op when no bundle is currently selected', () => {
            store.removeBundleAsset('a1');
            expect(service.removeAssetsFromBundle).not.toHaveBeenCalled();
        });

        it('error path → httpErrorManager.handle called', () => {
            const error = new Error('boom');
            (service.removeAssetsFromBundle as jest.Mock).mockReturnValueOnce(
                throwError(() => error)
            );
            store.openAssetList('B-X');
            store.removeBundleAsset('a1');
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('openDetail / loadDetail / closeDetail', () => {
        it('loads details + assets when opened', () => {
            store.openDetail('B-Y');
            expect(service.getPublishingJobDetails).toHaveBeenCalledWith('B-Y');
            expect(service.getBundleAssets).toHaveBeenCalledWith('B-Y');
            expect(store.detail()).toEqual(MOCK_DETAIL);
            expect(store.detailStatus()).toBe('loaded');
            expect(store.detailAssets()).toEqual(MOCK_ASSETS);
            expect(store.detailAssetsStatus()).toBe('loaded');
        });

        it('closeDetail clears state including detail assets', () => {
            store.openDetail('B-Y');
            store.closeDetail();
            expect(store.detailBundleId()).toBeNull();
            expect(store.detail()).toBeNull();
            expect(store.detailAssets()).toEqual([]);
            expect(store.detailAssetsStatus()).toBe('init');
        });

        it('loadDetailAssets error → handle + status reset to loaded', () => {
            const error = new Error('boom');
            (service.getBundleAssets as jest.Mock).mockReturnValueOnce(throwError(() => error));
            store.openDetail('B-Z');
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.detailAssetsStatus()).toBe('loaded');
        });
    });

    describe('retryBundles / deleteBundle / deleteBundlesBulk / purgeBundles', () => {
        it('retryBundles calls service and refreshes', () => {
            const onDone = jest.fn();
            store.retryBundles({ bundleIds: ['x'] }, onDone);
            expect(service.retryBundles).toHaveBeenCalledWith({ bundleIds: ['x'] });
            expect(onDone).toHaveBeenCalled();
        });

        it('deleteBundle calls service', () => {
            store.deleteBundle('x');
            expect(service.deleteBundle).toHaveBeenCalledWith('x');
        });

        it('deleteBundlesBulk hits the bulk service in one call and clears selection', () => {
            store.setBundlesSelection(['a', 'b']);
            store.deleteBundlesBulk(['a', 'b']);
            expect(service.deleteBundles).toHaveBeenCalledTimes(1);
            expect(service.deleteBundles).toHaveBeenCalledWith(['a', 'b']);
            expect(store.bundlesSelectedIds()).toEqual([]);
        });

        it('deleteBundlesBulk is a no-op when given an empty list', () => {
            jest.clearAllMocks();
            const onDone = jest.fn();
            store.deleteBundlesBulk([], onDone);
            expect(service.deleteBundles).not.toHaveBeenCalled();
            expect(onDone).toHaveBeenCalled();
        });

        it('purgeBundles forwards the status list and clears selection', () => {
            store.setBundlesSelection(['a']);
            const statuses = [PublishAuditStatus.SUCCESS, PublishAuditStatus.SUCCESS_WITH_WARNINGS];
            store.purgeBundles(statuses);
            expect(service.purgeBundles).toHaveBeenCalledWith(statuses);
            expect(store.bundlesSelectedIds()).toEqual([]);
        });

        it('purgeBundles calls service without statuses for the "ALL" scope', () => {
            const onDone = jest.fn();
            store.purgeBundles(undefined, onDone);
            expect(service.purgeBundles).toHaveBeenCalledWith(undefined);
            expect(onDone).toHaveBeenCalled();
        });
    });

    describe('polling', () => {
        it('startPolling / stopPolling do not throw', () => {
            store.stopPolling();
            store.startPolling();
            store.stopPolling();
        });
    });

    describe('error handling', () => {
        it('loadBundles error → handle + status=error', () => {
            const error = new Error('boom');
            (service.listPublishingJobs as jest.Mock).mockReturnValueOnce(throwError(() => error));
            store.loadBundles();
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.bundlesStatus()).toBe('error');
        });

        it('loadDetail error → handle + status=error', () => {
            const error = new Error('boom');
            (service.getPublishingJobDetails as jest.Mock).mockReturnValueOnce(
                throwError(() => error)
            );
            store.openDetail('y');
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.detailStatus()).toBe('error');
        });
    });
});
