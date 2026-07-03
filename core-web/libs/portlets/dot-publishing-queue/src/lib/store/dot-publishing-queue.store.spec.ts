import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import {
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotPublishingQueueService
} from '@dotcms/data-access';
import {
    BundleAssetView,
    DotMessageSeverity,
    DotMessageType,
    PublishAuditStatus,
    PublishingJobDetailView,
    PublishingJobsResponse,
    PublishingJobView,
    RetryBundleResultView
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

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
    numTries: 1,
    scheduledPublishDate: null
};

describe('DotPublishingQueueStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotPublishingQueueStore>>;
    let store: InstanceType<typeof DotPublishingQueueStore>;
    let service: jest.Mocked<DotPublishingQueueService>;
    let httpErrorManager: jest.Mocked<DotHttpErrorManagerService>;
    let messageDisplay: jest.Mocked<DotMessageDisplayService>;

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
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotMessageDisplayService, {
                push: jest.fn()
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'publishing-queue.retry.success.plural': '{0} bundles queued for retry.',
                    'publishing-queue.retry.failed.plural': 'Could not retry {0} bundles: {1}',
                    'publishing-queue.retry.partial':
                        '{0} of {1} bundles queued for retry. Failed: {2}',
                    'publishing-queue.retry.more': 'and {0} more'
                })
            }
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
        messageDisplay = spectator.inject(
            DotMessageDisplayService
        ) as jest.Mocked<DotMessageDisplayService>;
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
        it('loads details when opened (assets fetched lazily by view-contents dialog)', () => {
            (service.getBundleAssets as jest.Mock).mockClear();
            store.openDetail('B-Y');
            expect(service.getPublishingJobDetails).toHaveBeenCalledWith('B-Y');
            // Details dialog only renders metadata + endpoints + download links.
            // Asset list lives in the separate View Contents dialog, so openDetail
            // must NOT eagerly fetch assets.
            expect(service.getBundleAssets).not.toHaveBeenCalled();
            expect(store.detail()).toEqual(MOCK_DETAIL);
            expect(store.detailStatus()).toBe('loaded');
        });

        it('closeDetail clears detail state', () => {
            store.openDetail('B-Y');
            store.closeDetail();
            expect(store.detailBundleId()).toBeNull();
            expect(store.detail()).toBeNull();
            expect(store.detailStatus()).toBe('init');
        });
    });

    describe('retryBundles / deleteBundle / deleteBundlesBulk / purgeBundles', () => {
        const retryResult = (
            overrides: Partial<RetryBundleResultView> = {}
        ): RetryBundleResultView => ({
            bundleId: 'x',
            success: true,
            message: 'ok',
            forcePush: false,
            operation: 'PUBLISH',
            deliveryStrategy: 'ALL_ENDPOINTS',
            assetCount: 1,
            ...overrides
        });

        const expectToast = (severity: DotMessageSeverity, message: string) =>
            expect.objectContaining({
                severity,
                message,
                type: DotMessageType.SIMPLE_MESSAGE
            });

        const successCalls = () =>
            (messageDisplay.push as jest.Mock).mock.calls.filter(
                ([m]) => m.severity === DotMessageSeverity.SUCCESS
            );

        const errorCalls = () =>
            (messageDisplay.push as jest.Mock).mock.calls.filter(
                ([m]) => m.severity === DotMessageSeverity.ERROR
            );

        beforeEach(() => {
            // Toast mock accumulates across the retry cases below; reset it so
            // each `not.toHaveBeenCalled` assertion sees a clean slate.
            (messageDisplay.push as jest.Mock).mockClear();
        });

        it('retryBundles calls service and refreshes', () => {
            const onDone = jest.fn();
            store.retryBundles({ bundleIds: ['x'] }, onDone);
            expect(service.retryBundles).toHaveBeenCalledWith({ bundleIds: ['x'] });
            expect(onDone).toHaveBeenCalled();
        });

        it('retryBundles passes the BE success message through verbatim for a single bundle', () => {
            (service.retryBundles as jest.Mock).mockReturnValueOnce(
                of([retryResult({ message: 'Bundle successfully re-queued for publishing' })])
            );
            store.retryBundles({ bundleIds: ['x'] });
            expect(messageDisplay.push).toHaveBeenCalledWith(
                expectToast(
                    DotMessageSeverity.SUCCESS,
                    'Bundle successfully re-queued for publishing'
                )
            );
            expect(errorCalls()).toHaveLength(0);
        });

        it('retryBundles summarizes with a count when N bundles all succeed', () => {
            // Individual BE messages are identical canned strings for every bundle
            // in an all-success batch — listing them would just be repetition.
            (service.retryBundles as jest.Mock).mockReturnValueOnce(
                of([
                    retryResult({ bundleId: 'a' }),
                    retryResult({ bundleId: 'b' }),
                    retryResult({ bundleId: 'c' })
                ])
            );
            store.retryBundles({ bundleIds: ['a', 'b', 'c'] });
            expect(messageDisplay.push).toHaveBeenCalledWith(
                expectToast(DotMessageSeverity.SUCCESS, '3 bundles queued for retry.')
            );
        });

        it('retryBundles surfaces the BE message verbatim when a single bundle fails', () => {
            // The BE returns HTTP 200 with success:false when a bundle is already
            // queued — the classic "silent failure" this notification prevents.
            (service.retryBundles as jest.Mock).mockReturnValueOnce(
                of([
                    retryResult({
                        success: false,
                        message: 'Bundle already in queue - cannot retry while publishing: x'
                    })
                ])
            );
            store.retryBundles({ bundleIds: ['x'] });
            expect(messageDisplay.push).toHaveBeenCalledWith(
                expectToast(
                    DotMessageSeverity.ERROR,
                    'Bundle already in queue - cannot retry while publishing: x'
                )
            );
            expect(successCalls()).toHaveLength(0);
        });

        it('retryBundles lists per-bundle names + BE messages when several fail', () => {
            // Bundle names come from the store's already-loaded rows (bundle-A,
            // bundle-B are the seed fixtures from BUNDLES_RESPONSE).
            (service.retryBundles as jest.Mock).mockReturnValueOnce(
                of([
                    retryResult({
                        bundleId: 'bundle-A',
                        success: false,
                        message: 'Bundle already in queue'
                    }),
                    retryResult({
                        bundleId: 'bundle-B',
                        success: false,
                        message: 'Permission denied'
                    })
                ])
            );
            store.retryBundles({ bundleIds: ['bundle-A', 'bundle-B'] });
            expect(messageDisplay.push).toHaveBeenCalledWith(
                expectToast(
                    DotMessageSeverity.ERROR,
                    "Could not retry 2 bundles: 'Bundle One' — Bundle already in queue; 'Bundle One' — Permission denied"
                )
            );
        });

        it('retryBundles falls back to the bundle id when the row is not in the store', () => {
            (service.retryBundles as jest.Mock).mockReturnValueOnce(
                of([
                    retryResult({ bundleId: 'zzz', success: false, message: 'boom' }),
                    retryResult({ bundleId: 'yyy', success: false, message: 'bang' })
                ])
            );
            store.retryBundles({ bundleIds: ['zzz', 'yyy'] });
            expect(messageDisplay.push).toHaveBeenCalledWith(
                expectToast(
                    DotMessageSeverity.ERROR,
                    "Could not retry 2 bundles: 'zzz' — boom; 'yyy' — bang"
                )
            );
        });

        it('retryBundles caps the inline failure list at 3 with an "and N more" tail', () => {
            const fails: RetryBundleResultView[] = ['a', 'b', 'c', 'd', 'e'].map((id) =>
                retryResult({ bundleId: id, success: false, message: `msg-${id}` })
            );
            (service.retryBundles as jest.Mock).mockReturnValueOnce(of(fails));
            store.retryBundles({ bundleIds: fails.map((f) => f.bundleId) });
            expect(messageDisplay.push).toHaveBeenCalledWith(
                expectToast(
                    DotMessageSeverity.ERROR,
                    "Could not retry 5 bundles: 'a' — msg-a; 'b' — msg-b; 'c' — msg-c — and 2 more"
                )
            );
        });

        it('retryBundles emits a partial-outcome toast listing only the failures', () => {
            // Success detail is a count; failure detail is a per-item list.
            (service.retryBundles as jest.Mock).mockReturnValueOnce(
                of([
                    retryResult({ bundleId: 'bundle-A' }),
                    retryResult({ bundleId: 'bundle-B' }),
                    retryResult({ bundleId: 'zzz', success: false, message: 'nope' })
                ])
            );
            store.retryBundles({ bundleIds: ['bundle-A', 'bundle-B', 'zzz'] });
            expect(messageDisplay.push).toHaveBeenCalledWith(
                expectToast(
                    DotMessageSeverity.ERROR,
                    "2 of 3 bundles queued for retry. Failed: 'zzz' — nope"
                )
            );
            expect(successCalls()).toHaveLength(0);
        });

        it('retryBundles does NOT emit any toast when the response is empty', () => {
            (service.retryBundles as jest.Mock).mockReturnValueOnce(of([]));
            store.retryBundles({ bundleIds: [] });
            expect(messageDisplay.push).not.toHaveBeenCalled();
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

        it('fires a silent refresh when the tab becomes visible again', () => {
            // `onInit` already started polling — reset the counter and simulate
            // the tab going hidden then visible.
            (service.listPublishingJobs as jest.Mock).mockClear();
            Object.defineProperty(document, 'hidden', {
                configurable: true,
                get: () => false
            });

            document.dispatchEvent(new Event('visibilitychange'));

            expect(service.listPublishingJobs).toHaveBeenCalledTimes(1);
        });

        it('does NOT fetch on visibilitychange while the tab is still hidden', () => {
            (service.listPublishingJobs as jest.Mock).mockClear();
            Object.defineProperty(document, 'hidden', {
                configurable: true,
                get: () => true
            });

            document.dispatchEvent(new Event('visibilitychange'));

            expect(service.listPublishingJobs).not.toHaveBeenCalled();
        });

        it('unbinds the visibilitychange listener when stopPolling is called', () => {
            (service.listPublishingJobs as jest.Mock).mockClear();
            store.stopPolling();
            Object.defineProperty(document, 'hidden', {
                configurable: true,
                get: () => false
            });

            document.dispatchEvent(new Event('visibilitychange'));

            expect(service.listPublishingJobs).not.toHaveBeenCalled();
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
