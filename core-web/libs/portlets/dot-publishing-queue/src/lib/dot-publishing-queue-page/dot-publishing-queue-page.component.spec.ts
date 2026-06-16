import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

/* eslint-disable @nx/enforce-module-boundaries */

import { DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

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
    let pushPublishDialog: jest.Mocked<DotPushPublishDialogService>;
    let downloadBundleDialog: jest.Mocked<DotDownloadBundleDialogService>;

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
                retryBundles: jest.fn(),
                deleteBundle: jest.fn(),
                setReadyPage: jest.fn(),
                setProgressPage: jest.fn()
            }),
            ConfirmationService
        ],
        providers: [
            mockProvider(DotPushPublishDialogService, { open: jest.fn() }),
            mockProvider(DotDownloadBundleDialogService, { open: jest.fn() }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ]
    });

    beforeEach(() => {
        readyRows.set([buildJob()]);
        progressRows.set([
            buildJob({ bundleId: 'p1', status: PublishAuditStatus.FAILED_TO_PUBLISH })
        ]);
        spectator = createComponent();
        store = spectator.inject(DotPublishingQueueStore, true);
        pushPublishDialog = spectator.inject(
            DotPushPublishDialogService
        ) as jest.Mocked<DotPushPublishDialogService>;
        downloadBundleDialog = spectator.inject(
            DotDownloadBundleDialogService
        ) as jest.Mocked<DotDownloadBundleDialogService>;
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

    it('Send opens the project-wide push publish dialog with isBundle=true', () => {
        const job = buildJob({ bundleId: 'B-Z', bundleName: 'Bundle Z' });
        spectator.component.onSend(job);
        expect(pushPublishDialog.open).toHaveBeenCalledWith({
            assetIdentifier: 'B-Z',
            title: 'Bundle Z',
            isBundle: true
        });
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

    it('kebab "Configure & send" item opens the project-wide push publish dialog', () => {
        const job = buildJob({ bundleId: 'B-K', bundleName: 'Bundle K' });
        const items = spectator.component.readyKebabFor(job);
        items[0].command?.({} as never);
        expect(pushPublishDialog.open).toHaveBeenCalledWith({
            assetIdentifier: 'B-K',
            title: 'Bundle K',
            isBundle: true
        });
    });

    it('kebab "Generate / download" item opens the project-wide download bundle dialog', () => {
        const job = buildJob({ bundleId: 'B-D' });
        const items = spectator.component.readyKebabFor(job);
        items[1].command?.({} as never);
        expect(downloadBundleDialog.open).toHaveBeenCalledWith('B-D');
    });

    it('readyKebabFor returns a stable reference across calls (no .bind in template)', () => {
        // Class arrow property → same reference forever. Critical for the list
        // component's `kebabMenus` memoization to work.
        expect(spectator.component.readyKebabFor).toBe(spectator.component.readyKebabFor);
    });
});
