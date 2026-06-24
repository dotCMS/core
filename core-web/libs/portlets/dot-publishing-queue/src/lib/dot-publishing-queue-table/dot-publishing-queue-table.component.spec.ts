import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

/* eslint-disable @nx/enforce-module-boundaries */

import {
    DotFormatDateService,
    DotGlobalMessageService,
    DotMessageService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
import { DotClipboardUtil } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

import { DotPublishingQueueTableComponent } from './dot-publishing-queue-table.component';

import { DotPublishingQueueStore } from '../store/dot-publishing-queue.store';

const row = (
    bundleId: string,
    status: PublishAuditStatus = PublishAuditStatus.SUCCESS
): PublishingJobView => ({
    bundleId,
    bundleName: `Bundle ${bundleId}`,
    status,
    filterName: null,
    filterKey: null,
    assetCount: 1,
    assetPreview: [],
    environmentCount: 1,
    createDate: '2026-06-08T10:00:00Z',
    statusUpdated: null,
    numTries: 1
});

describe('DotPublishingQueueTableComponent', () => {
    let spectator: Spectator<DotPublishingQueueTableComponent>;
    let store: ReturnType<typeof makeStoreStub>;

    const bundlesRows = signal<PublishingJobView[]>([]);
    const bundlesStatus = signal<'init' | 'loading' | 'loaded' | 'error'>('loaded');
    const bundlesTotal = signal(0);
    const bundlesPage = signal(1);
    const bundlesSort = signal<string | null>(null);
    const bundlesSortDirection = signal<'asc' | 'desc'>('desc');
    const bundlesSelectedIds = signal<string[]>([]);
    const rowsPerPage = signal(10);

    function makeStoreStub() {
        return {
            bundlesRows,
            bundlesStatus,
            bundlesTotal,
            bundlesPage,
            bundlesSort,
            bundlesSortDirection,
            bundlesSelectedIds,
            rowsPerPage,
            setBundlesPage: jest.fn((p: number) => bundlesPage.set(p)),
            cycleBundlesSort: jest.fn(),
            setBundlesSelection: jest.fn((ids: string[]) => bundlesSelectedIds.set(ids)),
            clearBundlesSelection: jest.fn(() => bundlesSelectedIds.set([])),
            openDetail: jest.fn(),
            openAssetList: jest.fn(),
            deleteBundle: jest.fn(),
            retryBundles: jest.fn()
        };
    }

    let confirmationService: jest.Mocked<ConfirmationService>;
    let clipboard: jest.Mocked<DotClipboardUtil>;
    let globalMessage: jest.Mocked<DotGlobalMessageService>;
    let pushPublishService: jest.Mocked<DotPushPublishDialogService>;
    let downloadService: jest.Mocked<DotDownloadBundleDialogService>;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueTableComponent,
        componentProviders: [
            mockProvider(DotPublishingQueueStore, makeStoreStub()),
            ConfirmationService,
            mockProvider(DotClipboardUtil, {
                copy: jest.fn().mockResolvedValue(true)
            })
        ],
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            mockProvider(DotGlobalMessageService, { error: jest.fn() }),
            mockProvider(DotPushPublishDialogService, { open: jest.fn() }),
            mockProvider(DotDownloadBundleDialogService, { open: jest.fn() }),
            mockProvider(DotFormatDateService)
        ]
    });

    beforeEach(() => {
        bundlesRows.set([row('b1'), row('b2', PublishAuditStatus.FAILED_TO_PUBLISH)]);
        bundlesStatus.set('loaded');
        bundlesTotal.set(2);
        bundlesPage.set(1);
        bundlesSelectedIds.set([]);
        rowsPerPage.set(10);
        spectator = createComponent();
        store = spectator.inject(DotPublishingQueueStore, true) as unknown as ReturnType<
            typeof makeStoreStub
        >;
        confirmationService = spectator.inject(
            ConfirmationService,
            true
        ) as jest.Mocked<ConfirmationService>;
        clipboard = spectator.inject(DotClipboardUtil, true) as jest.Mocked<DotClipboardUtil>;
        globalMessage = spectator.inject(
            DotGlobalMessageService
        ) as jest.Mocked<DotGlobalMessageService>;
        pushPublishService = spectator.inject(
            DotPushPublishDialogService
        ) as jest.Mocked<DotPushPublishDialogService>;
        downloadService = spectator.inject(
            DotDownloadBundleDialogService
        ) as jest.Mocked<DotDownloadBundleDialogService>;
        jest.spyOn(confirmationService, 'confirm').mockImplementation((cfg) => {
            cfg.accept?.();
            return confirmationService;
        });
        jest.clearAllMocks();
    });

    it('renders the table', () => {
        expect(spectator.query(byTestId('pq-bundles-table'))).toBeTruthy();
    });

    it('renders all seven column headers', () => {
        expect(spectator.query(byTestId('pq-bundles-col-bundle-name'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-bundles-col-bundle-id'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-bundles-col-filter'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-bundles-col-items'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-bundles-col-created'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-bundles-col-modified'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-bundles-col-status'))).toBeTruthy();
    });

    it('row click opens the detail dialog', () => {
        spectator.component.onRowClick(row('b1'));
        expect(store.openDetail).toHaveBeenCalledWith('b1');
    });

    it('renders a dot-publishing-status-chip per row', () => {
        const chips = spectator.queryAll('dot-publishing-status-chip');
        expect(chips.length).toBe(2);
    });

    describe('failed-row bundle id styling', () => {
        it('isFailedRow returns true for any FAILURE_STATUSES entry', () => {
            expect(
                spectator.component.isFailedRow(row('x', PublishAuditStatus.FAILED_TO_PUBLISH))
            ).toBe(true);
            expect(
                spectator.component.isFailedRow(row('x', PublishAuditStatus.LICENSE_REQUIRED))
            ).toBe(true);
        });

        it('isFailedRow returns false for success/in-progress', () => {
            expect(spectator.component.isFailedRow(row('x', PublishAuditStatus.SUCCESS))).toBe(
                false
            );
            expect(
                spectator.component.isFailedRow(row('x', PublishAuditStatus.SENDING_TO_ENDPOINTS))
            ).toBe(false);
        });

        it('paints the bundle id in text-red-700 for failed rows', () => {
            // Row at index 1 in the fixture set is FAILED_TO_PUBLISH; index 0 is SUCCESS.
            const ids = spectator.queryAll(byTestId('pq-bundles-bundle-id'));
            expect(ids[0].querySelector('span')?.classList.contains('text-red-700')).toBe(false);
            expect(ids[1].querySelector('span')?.classList.contains('text-red-700')).toBe(true);
        });
    });

    describe('copyToClipboard', () => {
        it('delegates to DotClipboardUtil.copy', async () => {
            (clipboard.copy as jest.Mock).mockResolvedValue(true);
            await spectator.component.copyToClipboard('bundle-xyz');
            expect(clipboard.copy).toHaveBeenCalledWith('bundle-xyz');
            expect(globalMessage.error).not.toHaveBeenCalled();
        });

        it('surfaces a global error toast when copy fails', async () => {
            (clipboard.copy as jest.Mock).mockResolvedValue(false);
            await spectator.component.copyToClipboard('bundle-xyz');
            expect(globalMessage.error).toHaveBeenCalled();
        });
    });

    describe('row kebab menu', () => {
        it('renders a kebab button per row', () => {
            expect(spectator.queryAll(byTestId('pq-bundles-kebab-btn')).length).toBe(2);
        });

        it('kebabFor returns the SAME array reference across change-detection cycles', () => {
            const r = bundlesRows()[0];
            const a = spectator.component.kebabFor(r);
            spectator.detectChanges();
            const b = spectator.component.kebabFor(r);
            expect(b).toBe(a);
        });

        it('SUCCESS row → View details, View Contents, Generate/download, separator, Delete', () => {
            const items = spectator.component.bundlesKebabFor(
                row('b1', PublishAuditStatus.SUCCESS)
            );
            // 2 (view) + 1 (download) + 1 (separator) + 1 (delete) = 5
            expect(items.length).toBe(5);
            expect(items[items.length - 1].styleClass).toBe('p-menuitem-danger');
        });

        it('FAILED row → adds a Retry item', () => {
            const items = spectator.component.bundlesKebabFor(
                row('b1', PublishAuditStatus.FAILED_TO_PUBLISH)
            );
            const labels = items.map((i) => i.label);
            expect(labels.some((l) => l && l.toLowerCase().includes('retry'))).toBe(true);
        });

        it('WAITING_FOR_PUBLISHING row → adds a Configure & send item', () => {
            const items = spectator.component.bundlesKebabFor(
                row('b1', PublishAuditStatus.WAITING_FOR_PUBLISHING)
            );
            const labels = items.map((i) => i.label);
            expect(labels.some((l) => l && l.toLowerCase().includes('configure'))).toBe(true);
        });

        it('View details → store.openDetail', () => {
            const items = spectator.component.bundlesKebabFor(row('b1'));
            items[0].command?.({} as never);
            expect(store.openDetail).toHaveBeenCalledWith('b1');
        });

        it('View Contents → store.openAssetList', () => {
            const items = spectator.component.bundlesKebabFor(row('b1'));
            items[1].command?.({} as never);
            expect(store.openAssetList).toHaveBeenCalledWith('b1');
        });

        it('Retry → store.retryBundles with [bundleId]', () => {
            const items = spectator.component.bundlesKebabFor(
                row('b1', PublishAuditStatus.FAILED_TO_PUBLISH)
            );
            const retry = items.find((i) => i.label?.toLowerCase().includes('retry'));
            retry?.command?.({} as never);
            expect(store.retryBundles).toHaveBeenCalledWith({ bundleIds: ['b1'] });
        });

        it('Configure & send → push publish dialog', () => {
            const items = spectator.component.bundlesKebabFor(
                row('b1', PublishAuditStatus.WAITING_FOR_PUBLISHING)
            );
            const configure = items.find((i) => i.label?.toLowerCase().includes('configure'));
            configure?.command?.({} as never);
            expect(pushPublishService.open).toHaveBeenCalled();
        });

        it('Generate/download → download dialog', () => {
            const items = spectator.component.bundlesKebabFor(row('b1'));
            const download = items.find((i) => i.label?.toLowerCase().includes('download'));
            download?.command?.({} as never);
            expect(downloadService.open).toHaveBeenCalledWith('b1');
        });

        it('Delete → confirmation, then store.deleteBundle on accept', () => {
            const items = spectator.component.bundlesKebabFor(row('b1'));
            const del = items[items.length - 1];
            del.command?.({} as never);
            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(store.deleteBundle).toHaveBeenCalledWith('b1');
        });
    });
});
