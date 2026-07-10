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
            setRowsPerPage: jest.fn((r: number) => rowsPerPage.set(r)),
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

    describe('pagination', () => {
        it('persists a rows-per-page change so the next fetch picks it up', () => {
            // PrimeNG fires onLazyLoad with the new `rows` when the page-size
            // dropdown changes. The handler must route that into the store, or
            // the next fetch still goes out with the stale size.
            spectator.component.onLazyLoad({ first: 0, rows: 40 });
            expect(store.setRowsPerPage).toHaveBeenCalledWith(40);
        });

        it('routes a page-only change through setBundlesPage (not setRowsPerPage)', () => {
            spectator.component.onLazyLoad({ first: 10, rows: 10 }); // page 2 with same size
            expect(store.setBundlesPage).toHaveBeenCalledWith(2);
            expect(store.setRowsPerPage).not.toHaveBeenCalled();
        });
    });

    it('renders a dot-publishing-status-chip per row', () => {
        const chips = spectator.queryAll('dot-publishing-status-chip');
        expect(chips.length).toBe(2);
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

        it('Configure & send → push publish dialog with isBundle: true', () => {
            const items = spectator.component.bundlesKebabFor(
                row('b1', PublishAuditStatus.WAITING_FOR_PUBLISHING)
            );
            const configure = items.find((i) => i.label?.toLowerCase().includes('configure'));
            configure?.command?.({} as never);
            // The payload's `isBundle: true` routes the submit through the
            // bundle endpoint (vs asset). Regressing this quietly ships publishes
            // to the wrong endpoint, so pin the exact shape.
            expect(pushPublishService.open).toHaveBeenCalledWith(
                expect.objectContaining({ isBundle: true, assetIdentifier: 'b1' })
            );
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

    describe('onSelectionChange', () => {
        // p-table emits the checked *rows* (full objects). The store's
        // `bundlesSelectedIds` only cares about ids, so the handler must map
        // one to the other — otherwise the toolbar's bulk-action guard reads
        // "no selection" even after the user checks boxes.
        it('maps rows to bundle ids and pushes into the store', () => {
            spectator.component.onSelectionChange([row('b1'), row('b2')]);
            expect(store.setBundlesSelection).toHaveBeenCalledWith(['b1', 'b2']);
        });

        it('clears the store selection when no rows are checked', () => {
            spectator.component.onSelectionChange([]);
            expect(store.setBundlesSelection).toHaveBeenCalledWith([]);
        });
    });

    describe('truncateBundleId', () => {
        it('returns the id unchanged when it fits under the display cap', () => {
            const short = 'a'.repeat(20);
            expect(spectator.component.truncateBundleId(short)).toBe(short);
        });

        it('truncates and appends an ellipsis when the id exceeds the cap', () => {
            const long = 'a'.repeat(50);
            const truncated = spectator.component.truncateBundleId(long);
            expect(truncated.endsWith('…')).toBe(true);
            expect(truncated.length).toBeLessThan(long.length);
        });

        it('returns the input as-is for empty strings', () => {
            expect(spectator.component.truncateBundleId('')).toBe('');
        });
    });

    describe('right-click context menu', () => {
        it('onRowContextMenu preventDefaults the browser menu and pins the row', () => {
            const event = {
                preventDefault: jest.fn(),
                stopPropagation: jest.fn()
            } as unknown as MouseEvent;
            spectator.component.onRowContextMenu(event, row('b1'));
            expect(event.preventDefault).toHaveBeenCalled();
            expect(spectator.component.contextMenuRow()).toEqual(row('b1'));
        });

        it('contextMenuItems mirrors the same kebab items for the pinned row', () => {
            const event = {
                preventDefault: jest.fn(),
                stopPropagation: jest.fn()
            } as unknown as MouseEvent;
            spectator.component.onRowContextMenu(
                event,
                row('b2', PublishAuditStatus.FAILED_TO_PUBLISH)
            );
            const items = spectator.component.contextMenuItems();
            expect(items.length).toBeGreaterThan(0);
            // Same-status row should carry the retry action just like the kebab.
            expect(items.some((i) => i.label?.toLowerCase().includes('retry'))).toBe(true);
        });

        it('contextMenuItems is empty until a row has been right-clicked', () => {
            expect(spectator.component.contextMenuItems()).toEqual([]);
        });
    });

    describe('$ptConfig', () => {
        // The pass-through swaps between "fill the container" (empty/loading)
        // and "natural width" (rows present) — otherwise PrimeNG's default
        // width:100% squeezes the Filter column when Bundle Id has extra space.
        it('uses natural width when rows are present', () => {
            bundlesRows.set([row('b1')]);
            spectator.detectChanges();
            expect(spectator.component.$ptConfig().table.style.width).toBe('auto');
        });

        it('fills the container when the rows list is empty', () => {
            bundlesRows.set([]);
            spectator.detectChanges();
            const style = spectator.component.$ptConfig().table.style as Record<string, string>;
            expect(style.width).toBe('100%');
            expect(style.height).toBe('100%');
        });
    });
});
