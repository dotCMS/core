import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotGlobalMessageService, DotMessageService } from '@dotcms/data-access';
import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
import { DotClipboardUtil } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueHistoryComponent } from './dot-publishing-queue-history.component';

import { DotPublishingQueueStore } from '../dot-publishing-queue-page/store/dot-publishing-queue.store';

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

describe('DotPublishingQueueHistoryComponent', () => {
    let spectator: Spectator<DotPublishingQueueHistoryComponent>;
    let store: ReturnType<typeof makeStoreStub>;

    const historyRows = signal<PublishingJobView[]>([]);
    const historyStatus = signal<'init' | 'loading' | 'loaded' | 'error'>('loaded');
    const historyTotal = signal(0);
    const historyPage = signal(1);
    const historySort = signal<string | null>(null);
    const historySortDirection = signal<'asc' | 'desc'>('desc');
    const historySelectedIds = signal<string[]>([]);
    const rowsPerPage = signal(10);

    function makeStoreStub() {
        return {
            historyRows,
            historyStatus,
            historyTotal,
            historyPage,
            historySort,
            historySortDirection,
            historySelectedIds,
            rowsPerPage,
            setHistoryPage: jest.fn((p: number) => historyPage.set(p)),
            cycleHistorySort: jest.fn(),
            setHistorySelection: jest.fn((ids: string[]) => historySelectedIds.set(ids)),
            clearHistorySelection: jest.fn(() => historySelectedIds.set([])),
            openDetail: jest.fn(),
            openAssetList: jest.fn(),
            deleteBundle: jest.fn()
        };
    }

    let confirmationService: jest.Mocked<ConfirmationService>;

    let clipboard: jest.Mocked<DotClipboardUtil>;
    let globalMessage: jest.Mocked<DotGlobalMessageService>;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueHistoryComponent,
        componentProviders: [
            mockProvider(DotPublishingQueueStore, makeStoreStub()),
            ConfirmationService,
            mockProvider(DotClipboardUtil, {
                copy: jest.fn().mockResolvedValue(true)
            })
        ],
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            mockProvider(DotGlobalMessageService, { error: jest.fn() })
        ]
    });

    beforeEach(() => {
        historyRows.set([row('b1'), row('b2', PublishAuditStatus.FAILED_TO_PUBLISH)]);
        historyStatus.set('loaded');
        historyTotal.set(2);
        historyPage.set(1);
        historySelectedIds.set([]);
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
        jest.spyOn(confirmationService, 'confirm').mockImplementation((cfg) => {
            cfg.accept?.();
            return confirmationService;
        });
        jest.clearAllMocks();
    });

    it('renders the table', () => {
        expect(spectator.query(byTestId('pq-history-table'))).toBeTruthy();
    });

    it('renders all six column headers (Bundle Name, Bundle Id, Filter, Status, Data Entered, Last Update)', () => {
        expect(spectator.query(byTestId('pq-history-col-bundle-name'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-history-col-bundle-id'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-history-col-filter'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-history-col-status'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-history-col-created'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-history-col-modified'))).toBeTruthy();
    });

    it('renders Bundle Name cell with the row name (falls back to "—" when null)', () => {
        historyRows.set([row('b1'), { ...row('b2'), bundleName: null }]);
        spectator.detectChanges();
        const cells = spectator.queryAll(byTestId('pq-history-bundle-name'));
        expect(cells.length).toBe(2);
        expect(cells[0].textContent?.trim()).toBe('Bundle b1');
        expect(cells[1].textContent?.trim()).toBe('—');
    });

    it('renders rows with status chips', () => {
        const tags = spectator.queryAll(byTestId('pq-history-status'));
        expect(tags.length).toBe(2);
    });

    it('renders Filter cell falling back to "—" when filterName + filterKey are null', () => {
        const cells = spectator.queryAll(byTestId('pq-history-filter'));
        expect(cells.length).toBe(2);
        expect(cells[0].textContent?.trim()).toBe('—');
    });

    it('renders Bundle Id cell with the full id (no truncation) + copy button', () => {
        const cells = spectator.queryAll(byTestId('pq-history-bundle-id'));
        expect(cells.length).toBe(2);
        expect(cells[0].textContent).toContain('b1');
        expect(cells[0].querySelector('[data-testid="pq-history-bundle-id-copy"]')).toBeTruthy();
    });

    it('row click opens the detail dialog', () => {
        spectator.component.onRowClick(row('b1'));
        expect(store.openDetail).toHaveBeenCalledWith('b1');
    });

    it('renders a dot-publishing-status-chip per row', () => {
        const chips = spectator.queryAll('dot-publishing-status-chip');
        expect(chips.length).toBe(2);
    });

    describe('truncateBundleId', () => {
        it('returns the id unchanged when shorter than or equal to 32 chars', () => {
            // standard 26-char ULID
            expect(spectator.component.truncateBundleId('01KVBQPPFQCVG6C9VP4D0V47M0')).toBe(
                '01KVBQPPFQCVG6C9VP4D0V47M0'
            );
            // exactly 32 chars
            expect(spectator.component.truncateBundleId('a'.repeat(32))).toBe('a'.repeat(32));
        });

        it('truncates to the first 32 chars + "…" when longer', () => {
            const long = 'Bulk-product-bundle-01KV6E1E62SNRW4EWWKFW29S9B'; // 47 chars
            const truncated = spectator.component.truncateBundleId(long);
            expect(truncated).toBe(`${long.slice(0, 32)}…`);
            expect(truncated.length).toBe(33); // 32 chars + 1 ellipsis
        });

        it('handles empty / nullish input safely', () => {
            expect(spectator.component.truncateBundleId('')).toBe('');
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
            expect(spectator.queryAll(byTestId('pq-history-kebab-btn')).length).toBe(2);
        });

        // Regression: `<p-menu [model]="…">` thrashes when it receives a new
        // array reference on every CD — the first click only closes the menu
        // and the user has to click twice. `kebabFor` must memoize.
        it('kebabFor returns the SAME array reference across change-detection cycles', () => {
            const r = historyRows()[0];
            const a = spectator.component.kebabFor(r);
            spectator.detectChanges();
            const b = spectator.component.kebabFor(r);
            expect(b).toBe(a);
        });

        it('builds 4 items: View details · View Contents · separator · Delete (no icons)', () => {
            const items = spectator.component.historyKebabFor(row('b1'));
            expect(items.length).toBe(4);
            // No icon on any item — design preference to keep the menu text-only
            expect(items[0].icon).toBeUndefined();
            expect(items[1].icon).toBeUndefined();
            expect(items[2].separator).toBe(true);
            expect(items[3].icon).toBeUndefined();
            expect(items[3].styleClass).toBe('p-menuitem-danger');
        });

        it('View details → store.openDetail', () => {
            const items = spectator.component.historyKebabFor(row('b1'));
            items[0].command?.({} as never);
            expect(store.openDetail).toHaveBeenCalledWith('b1');
        });

        it('View Contents → store.openAssetList', () => {
            const items = spectator.component.historyKebabFor(row('b1'));
            items[1].command?.({} as never);
            expect(store.openAssetList).toHaveBeenCalledWith('b1');
        });

        it('Delete → confirmation, then store.deleteBundle on accept', () => {
            const items = spectator.component.historyKebabFor(row('b1'));
            items[3].command?.({} as never);
            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(store.deleteBundle).toHaveBeenCalledWith('b1');
        });
    });
});
