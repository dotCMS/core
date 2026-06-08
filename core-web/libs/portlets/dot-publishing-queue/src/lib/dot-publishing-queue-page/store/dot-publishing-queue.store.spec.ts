import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotHttpErrorManagerService, DotPublishingQueueService } from '@dotcms/data-access';
import {
    BundleAssetView,
    IN_PROGRESS_STATUSES,
    PublishAuditStatus,
    PublishingJobsResponse,
    PublishingJobView,
    READY_STATUSES
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

const READY_RESPONSE: PublishingJobsResponse = {
    entity: [buildJob({ bundleId: 'ready-1' })],
    pagination: { currentPage: 1, perPage: 10, totalEntries: 1 }
};

const PROGRESS_RESPONSE: PublishingJobsResponse = {
    entity: [
        buildJob({ bundleId: 'progress-1', status: PublishAuditStatus.BUNDLING }),
        buildJob({ bundleId: 'progress-2', status: PublishAuditStatus.SENDING_TO_ENDPOINTS })
    ],
    pagination: { currentPage: 1, perPage: 10, totalEntries: 2 }
};

const MOCK_ASSETS: BundleAssetView[] = [
    { id: 'a1', title: 'Asset 1', type: 'contentlet' },
    { id: 'a2', title: 'Asset 2', type: 'template' }
];

describe('DotPublishingQueueStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotPublishingQueueStore>>;
    let store: InstanceType<typeof DotPublishingQueueStore>;
    let service: jest.Mocked<DotPublishingQueueService>;
    let httpErrorManager: jest.Mocked<DotHttpErrorManagerService>;

    const createService = createServiceFactory({
        service: DotPublishingQueueStore,
        providers: [
            mockProvider(DotPublishingQueueService, {
                listPublishingJobs: jest
                    .fn()
                    .mockImplementation(({ statuses }) =>
                        of(statuses === READY_STATUSES ? READY_RESPONSE : PROGRESS_RESPONSE)
                    ),
                getBundleAssets: jest.fn().mockReturnValue(of(MOCK_ASSETS))
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
        // onInit effect kicks off loadReady + loadProgress
        spectator.flushEffects();
    });

    describe('onInit', () => {
        it('loads ready + progress columns on init', () => {
            expect(service.listPublishingJobs).toHaveBeenCalledTimes(2);
            expect(service.listPublishingJobs).toHaveBeenCalledWith({
                statuses: READY_STATUSES,
                page: 1,
                perPage: 10,
                filter: undefined
            });
            expect(service.listPublishingJobs).toHaveBeenCalledWith({
                statuses: IN_PROGRESS_STATUSES,
                page: 1,
                perPage: 10,
                filter: undefined
            });
            expect(store.readyRows()).toEqual(READY_RESPONSE.entity);
            expect(store.readyTotal()).toBe(1);
            expect(store.readyStatus()).toBe('loaded');
            expect(store.progressRows()).toEqual(PROGRESS_RESPONSE.entity);
            expect(store.progressTotal()).toBe(2);
            expect(store.progressStatus()).toBe('loaded');
        });
    });

    describe('setSearch', () => {
        it('updates search, resets both pages to 1, and triggers reload', () => {
            store.setReadyPage(3);
            store.setProgressPage(2);
            spectator.flushEffects();
            (service.listPublishingJobs as jest.Mock).mockClear();

            store.setSearch('bundle-name');
            spectator.flushEffects();

            expect(store.search()).toBe('bundle-name');
            expect(store.readyPage()).toBe(1);
            expect(store.progressPage()).toBe(1);
            expect(service.listPublishingJobs).toHaveBeenCalledWith(
                expect.objectContaining({ filter: 'bundle-name' })
            );
        });
    });

    describe('setReadyPage / setProgressPage', () => {
        it('reloads ready when ready page changes (not progress)', () => {
            (service.listPublishingJobs as jest.Mock).mockClear();

            store.setReadyPage(2);
            spectator.flushEffects();

            // Effect re-runs both because it reads multiple signals;
            // the assertion verifies the new page param was forwarded.
            expect(service.listPublishingJobs).toHaveBeenCalledWith(
                expect.objectContaining({ statuses: READY_STATUSES, page: 2 })
            );
        });

        it('reloads progress when progress page changes', () => {
            (service.listPublishingJobs as jest.Mock).mockClear();

            store.setProgressPage(4);
            spectator.flushEffects();

            expect(service.listPublishingJobs).toHaveBeenCalledWith(
                expect.objectContaining({ statuses: IN_PROGRESS_STATUSES, page: 4 })
            );
        });
    });

    describe('refresh', () => {
        it('re-fires both list calls', () => {
            (service.listPublishingJobs as jest.Mock).mockClear();

            store.refresh();

            expect(service.listPublishingJobs).toHaveBeenCalledTimes(2);
        });
    });

    describe('openAssetList / loadAssets / closeAssetList', () => {
        it('opens, sets selectedBundleId, and loads assets', () => {
            store.openAssetList('bundle-X');

            expect(store.selectedBundleId()).toBe('bundle-X');
            expect(service.getBundleAssets).toHaveBeenCalledWith('bundle-X');
            expect(store.selectedAssets()).toEqual(MOCK_ASSETS);
            expect(store.assetListStatus()).toBe('loaded');
        });

        it('closeAssetList clears state', () => {
            store.openAssetList('bundle-X');
            store.closeAssetList();

            expect(store.selectedBundleId()).toBeNull();
            expect(store.selectedAssets()).toEqual([]);
            expect(store.assetListStatus()).toBe('init');
        });

        it('loadAssets is a no-op when no bundle is selected', () => {
            (service.getBundleAssets as jest.Mock).mockClear();
            store.loadAssets();
            expect(service.getBundleAssets).not.toHaveBeenCalled();
        });
    });

    describe('error handling', () => {
        it('loadReady error → httpErrorManager.handle called, status = error', () => {
            const error = new Error('boom');
            (service.listPublishingJobs as jest.Mock).mockReturnValueOnce(throwError(() => error));

            store.loadReady();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.readyStatus()).toBe('error');
        });

        it('loadProgress error → httpErrorManager.handle called, status = error', () => {
            const error = new Error('boom');
            (service.listPublishingJobs as jest.Mock).mockReturnValueOnce(throwError(() => error));

            store.loadProgress();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.progressStatus()).toBe('error');
        });

        it('loadAssets error → httpErrorManager.handle called, status = loaded', () => {
            const error = new Error('boom');
            (service.getBundleAssets as jest.Mock).mockReturnValueOnce(throwError(() => error));

            store.openAssetList('bundle-Y');

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.assetListStatus()).toBe('loaded');
        });
    });
});
