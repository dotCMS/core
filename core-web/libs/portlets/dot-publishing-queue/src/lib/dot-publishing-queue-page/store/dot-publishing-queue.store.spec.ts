import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import {
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotPublishingQueueService
} from '@dotcms/data-access';
import {
    BundleAssetView,
    IN_PROGRESS_STATUSES,
    PublishAuditStatus,
    PublishingJobDetailView,
    PublishingJobsResponse,
    PublishingJobView,
    UnsentBundlesResponse
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

const UNSENT_RESPONSE: UnsentBundlesResponse = {
    identifier: 'id',
    label: 'name',
    items: [{ id: 'ready-1', name: 'Ready Bundle' }],
    numRows: 1
};

const PROGRESS_RESPONSE: PublishingJobsResponse = {
    entity: [
        buildJob({ bundleId: 'progress-1', status: PublishAuditStatus.BUNDLING }),
        buildJob({ bundleId: 'progress-2', status: PublishAuditStatus.SENDING_TO_ENDPOINTS })
    ],
    pagination: { currentPage: 1, perPage: 10, totalEntries: 2 }
};

const HISTORY_RESPONSE: PublishingJobsResponse = {
    entity: [buildJob({ bundleId: 'hist-1', status: PublishAuditStatus.SUCCESS })],
    pagination: { currentPage: 1, perPage: 10, totalEntries: 1 }
};

const MOCK_ASSETS: BundleAssetView[] = [
    { id: 'a1', title: 'Asset 1', type: 'contentlet' },
    { id: 'a2', title: 'Asset 2', type: 'template' }
];

const MOCK_DETAIL: PublishingJobDetailView = {
    bundleId: 'ready-1',
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
                listPublishingJobs: jest.fn().mockImplementation(({ statuses }) => {
                    if (statuses === IN_PROGRESS_STATUSES) return of(PROGRESS_RESPONSE);
                    return of(HISTORY_RESPONSE);
                }),
                getUnsendBundles: jest.fn().mockReturnValue(of(UNSENT_RESPONSE)),
                getBundleAssets: jest.fn().mockReturnValue(of(MOCK_ASSETS)),
                getPublishingJobDetails: jest.fn().mockReturnValue(of(MOCK_DETAIL)),
                getEnvironments: jest.fn().mockReturnValue(
                    of([
                        { id: 'env-1', name: 'Prod' },
                        { id: 'env-2', name: 'Stage' }
                    ])
                ),
                pushBundle: jest.fn().mockReturnValue(
                    of({
                        bundleId: 'b',
                        operation: 'publish',
                        environments: [],
                        filterKey: 'k'
                    })
                ),
                retryBundles: jest.fn().mockReturnValue(of([])),
                deleteBundle: jest.fn().mockReturnValue(of({ message: 'ok' })),
                deleteBundles: jest.fn().mockReturnValue(of({ message: 'ok', deleted: [] })),
                generateBundle: jest.fn().mockReturnValue(of({})),
                uploadBundle: jest
                    .fn()
                    .mockReturnValue(of({ bundleName: 'b', status: 'BUNDLE_REQUESTED' }))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest
                    .fn()
                    .mockReturnValue(of({ userId: 'dotcms.org.1', email: 'admin@dotcms.com' }))
            })
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
        it('defaults activeTab to history and loads only the history list', () => {
            expect(store.activeTab()).toBe('history');
            // History uses listPublishingJobs with HISTORY_STATUSES;
            // Queue columns stay idle until the user switches tabs.
            expect(service.listPublishingJobs).toHaveBeenCalledTimes(1);
            expect(service.getUnsendBundles).not.toHaveBeenCalled();
            expect(store.historyStatus()).toBe('loaded');
        });

        it('loads queue columns when switching to the Queue tab', () => {
            store.setActiveTab('queue');
            spectator.flushEffects();

            expect(service.getUnsendBundles).toHaveBeenCalledTimes(1);
            expect(store.readyStatus()).toBe('loaded');
            expect(store.progressStatus()).toBe('loaded');
        });

        it('maps unsent bundle items to PublishingJobView rows with status=null', () => {
            store.setActiveTab('queue');
            spectator.flushEffects();

            const row = store.readyRows()[0];
            expect(row.bundleId).toBe('ready-1');
            expect(row.bundleName).toBe('Ready Bundle');
            expect(row.status).toBeNull();
            expect(row.assetCount).toBe(0);
            expect(row.environmentCount).toBe(0);
        });

        it('caches userId from DotCurrentUserService once Queue tab is opened', () => {
            store.setActiveTab('queue');
            spectator.flushEffects();
            expect(store.userId()).toBe('dotcms.org.1');
        });
    });

    describe('setSearch', () => {
        it('resets pages and clears history selection', () => {
            store.setReadyPage(3);
            store.setProgressPage(2);
            store.setHistoryPage(4);
            store.setHistorySelection(['x']);
            spectator.flushEffects();
            (service.listPublishingJobs as jest.Mock).mockClear();
            (service.getUnsendBundles as jest.Mock).mockClear();

            store.setSearch('term');
            spectator.flushEffects();

            expect(store.search()).toBe('term');
            expect(store.readyPage()).toBe(1);
            expect(store.progressPage()).toBe(1);
            expect(store.historyPage()).toBe(1);
            expect(store.historySelectedIds()).toEqual([]);
        });

        it('wildcards the search term when reloading READY via getUnsendBundles', () => {
            store.setActiveTab('queue');
            spectator.flushEffects();
            (service.getUnsendBundles as jest.Mock).mockClear();

            store.setSearch('term');
            spectator.flushEffects();

            expect(service.getUnsendBundles).toHaveBeenCalledWith('dotcms.org.1', '*term*', 0, 10);
        });
    });

    describe('setActiveTab', () => {
        it('switching to queue triggers loadReady + loadProgress', () => {
            (service.listPublishingJobs as jest.Mock).mockClear();
            (service.getUnsendBundles as jest.Mock).mockClear();
            store.setActiveTab('queue');
            spectator.flushEffects();

            expect(service.getUnsendBundles).toHaveBeenCalledTimes(1);
            expect(service.listPublishingJobs).toHaveBeenCalledWith(
                expect.objectContaining({ statuses: IN_PROGRESS_STATUSES })
            );
        });
    });

    describe('cycleHistorySort', () => {
        it('cycles asc → desc → off for the same field', () => {
            store.setActiveTab('history');
            store.cycleHistorySort('bundle_name');
            expect(store.historySort()).toBe('bundle_name');
            expect(store.historySortDirection()).toBe('asc');

            store.cycleHistorySort('bundle_name');
            expect(store.historySortDirection()).toBe('desc');

            store.cycleHistorySort('bundle_name');
            expect(store.historySort()).toBeNull();
        });

        it('switching field starts asc again', () => {
            store.setActiveTab('history');
            store.cycleHistorySort('bundle_name');
            store.cycleHistorySort('status');
            expect(store.historySort()).toBe('status');
            expect(store.historySortDirection()).toBe('asc');
        });
    });

    describe('refresh', () => {
        it('reloads queue when active tab is queue (ready via getUnsendBundles, progress via listPublishingJobs)', () => {
            store.setActiveTab('queue');
            spectator.flushEffects();
            (service.listPublishingJobs as jest.Mock).mockClear();
            (service.getUnsendBundles as jest.Mock).mockClear();
            store.refresh();
            expect(service.getUnsendBundles).toHaveBeenCalledTimes(1);
            expect(service.listPublishingJobs).toHaveBeenCalledTimes(1);
        });

        it('reloads history when active tab is history', () => {
            store.setActiveTab('history');
            spectator.flushEffects();
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

    describe('openDetail / loadDetail / closeDetail', () => {
        it('loads details when opened', () => {
            store.openDetail('B-Y');
            expect(service.getPublishingJobDetails).toHaveBeenCalledWith('B-Y');
            expect(store.detail()).toEqual(MOCK_DETAIL);
            expect(store.detailStatus()).toBe('loaded');
        });

        it('closeDetail clears state', () => {
            store.openDetail('B-Y');
            store.closeDetail();
            expect(store.detailBundleId()).toBeNull();
            expect(store.detail()).toBeNull();
        });
    });

    describe('openConfigureSend / submitPush / closeConfigureSend', () => {
        it('loads environments + sets target on open', () => {
            store.openConfigureSend(buildJob({ bundleId: 'B-Z' }));
            expect(store.pushBundleTarget()?.bundleId).toBe('B-Z');
            expect(service.getEnvironments).toHaveBeenCalled();
            expect(store.environments().length).toBeGreaterThan(0);
        });

        it('submitPush calls the service and clears the target on success', () => {
            const onDone = jest.fn();
            store.openConfigureSend(buildJob({ bundleId: 'B-Z' }));
            store.submitPush(
                'B-Z',
                {
                    operation: 'publish',
                    environments: ['env-1'],
                    filterKey: 'k'
                },
                onDone
            );
            expect(service.pushBundle).toHaveBeenCalled();
            expect(store.pushBundleTarget()).toBeNull();
            expect(onDone).toHaveBeenCalled();
        });

        it('closeConfigureSend clears the target', () => {
            store.openConfigureSend(buildJob());
            store.closeConfigureSend();
            expect(store.pushBundleTarget()).toBeNull();
        });
    });

    describe('retryBundles / deleteBundle / deleteBundlesBulk / generateBundle', () => {
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

        it('deleteBundlesBulk loops per-id (until #36046 lands) and clears selection', () => {
            store.setHistorySelection(['a', 'b']);
            store.deleteBundlesBulk(['a', 'b']);
            expect(service.deleteBundle).toHaveBeenCalledWith('a');
            expect(service.deleteBundle).toHaveBeenCalledWith('b');
            expect(store.historySelectedIds()).toEqual([]);
        });

        it('generateBundle calls service with bundleId + filterKey', () => {
            const onDone = jest.fn();
            store.generateBundle('x', 'force.yml', onDone);
            expect(service.generateBundle).toHaveBeenCalledWith('x', 'force.yml');
            expect(onDone).toHaveBeenCalled();
        });
    });

    describe('uploadBundle', () => {
        it('toggles uploadInFlight and refreshes on success', () => {
            const onDone = jest.fn();
            const file = new File(['x'], 'b.tar.gz');
            store.uploadBundle(file, onDone);
            expect(service.uploadBundle).toHaveBeenCalledWith(file);
            expect(store.uploadInFlight()).toBe(false);
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
        it('loadReady error → handle + status=error', () => {
            const error = new Error('boom');
            (service.getUnsendBundles as jest.Mock).mockReturnValueOnce(throwError(() => error));
            store.loadReady();
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.readyStatus()).toBe('error');
        });

        it('loadHistory error → handle + status=error', () => {
            const error = new Error('boom');
            (service.listPublishingJobs as jest.Mock).mockReturnValueOnce(throwError(() => error));
            store.loadHistory();
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.historyStatus()).toBe('error');
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

        it('uploadBundle error → handle + uploadInFlight reset', () => {
            const error = new Error('boom');
            (service.uploadBundle as jest.Mock).mockReturnValueOnce(throwError(() => error));
            store.uploadBundle(new File(['x'], 'b.tar.gz'));
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.uploadInFlight()).toBe(false);
        });
    });
});
