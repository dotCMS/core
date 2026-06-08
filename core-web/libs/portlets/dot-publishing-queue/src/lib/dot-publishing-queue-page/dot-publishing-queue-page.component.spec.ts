import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueuePageComponent } from './dot-publishing-queue-page.component';
import { DotPublishingQueueStore } from './store/dot-publishing-queue.store';

const buildJob = (overrides: Partial<PublishingJobView> = {}): PublishingJobView => ({
    bundleId: 'b1',
    bundleName: 'Bundle 1',
    status: PublishAuditStatus.WAITING_FOR_PUBLISHING,
    filterName: null,
    filterKey: null,
    assetCount: 3,
    assetPreview: [],
    environmentCount: 1,
    createDate: '2026-06-08T10:00:00Z',
    statusUpdated: null,
    numTries: 0,
    ...overrides
});

describe('DotPublishingQueuePageComponent', () => {
    let spectator: Spectator<DotPublishingQueuePageComponent>;
    let store: InstanceType<typeof DotPublishingQueueStore>;

    const readyRows = signal<PublishingJobView[]>([buildJob()]);
    const progressRows = signal<PublishingJobView[]>([
        buildJob({ bundleId: 'p1', status: PublishAuditStatus.FAILED_TO_PUBLISH })
    ]);

    const createComponent = createComponentFactory({
        component: DotPublishingQueuePageComponent,
        componentProviders: [
            mockProvider(DotPublishingQueueStore, {
                readyRows,
                progressRows,
                readyStatus: jest.fn().mockReturnValue('loaded'),
                progressStatus: jest.fn().mockReturnValue('loaded'),
                readyTotal: jest.fn().mockReturnValue(1),
                progressTotal: jest.fn().mockReturnValue(1),
                readyPage: jest.fn().mockReturnValue(1),
                progressPage: jest.fn().mockReturnValue(1),
                rowsPerPage: jest.fn().mockReturnValue(10),
                openAssetList: jest.fn(),
                openDetail: jest.fn(),
                openConfigureSend: jest.fn(),
                retryBundles: jest.fn(),
                deleteBundle: jest.fn(),
                generateBundle: jest.fn(),
                setReadyPage: jest.fn(),
                setProgressPage: jest.fn()
            }),
            ConfirmationService
        ],
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService({}) }]
    });

    beforeEach(() => {
        readyRows.set([buildJob()]);
        progressRows.set([
            buildJob({ bundleId: 'p1', status: PublishAuditStatus.FAILED_TO_PUBLISH })
        ]);
        spectator = createComponent();
        store = spectator.inject(DotPublishingQueueStore, true);
        jest.clearAllMocks();
    });

    it('renders both ready and progress list slots', () => {
        expect(spectator.query(byTestId('pq-ready-list'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-progress-list'))).toBeTruthy();
    });

    it('ready row click opens the asset list', () => {
        spectator.component.onRowClick(buildJob({ bundleId: 'B-X' }), 'ready');
        expect(store.openAssetList).toHaveBeenCalledWith('B-X');
    });

    it('progress row click opens the detail dialog', () => {
        spectator.component.onRowClick(buildJob({ bundleId: 'B-Y' }), 'progress');
        expect(store.openDetail).toHaveBeenCalledWith('B-Y');
    });

    it('Send opens Configure & send for the bundle', () => {
        const job = buildJob({ bundleId: 'B-Z' });
        spectator.component.onSend(job);
        expect(store.openConfigureSend).toHaveBeenCalledWith(job);
    });

    it('Retry calls retryBundles with the single bundle id', () => {
        const job = buildJob({ bundleId: 'B-R', status: PublishAuditStatus.FAILED_TO_PUBLISH });
        spectator.component.onRetry(job);
        expect(store.retryBundles).toHaveBeenCalledWith({ bundleIds: ['B-R'] });
    });

    it('builds 4 kebab items for a READY row (configure, generate, sep, remove)', () => {
        const items = spectator.component.readyKebabFor(buildJob());
        expect(items.length).toBe(4);
        expect(items[2].separator).toBe(true);
        expect(items[3].styleClass).toContain('danger');
    });
});
