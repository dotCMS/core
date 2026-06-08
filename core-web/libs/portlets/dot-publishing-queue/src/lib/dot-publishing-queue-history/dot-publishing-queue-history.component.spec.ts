import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
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
    let confirmationService: jest.Mocked<ConfirmationService>;

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
            retryBundles: jest.fn(),
            deleteBundlesBulk: jest.fn()
        };
    }

    const createComponent = createComponentFactory({
        component: DotPublishingQueueHistoryComponent,
        componentProviders: [mockProvider(DotPublishingQueueStore, makeStoreStub())],
        providers: [
            ConfirmationService,
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
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
            ConfirmationService
        ) as jest.Mocked<ConfirmationService>;
        jest.spyOn(confirmationService, 'confirm').mockImplementation((cfg) => {
            cfg.accept?.();
            return confirmationService;
        });
        jest.clearAllMocks();
    });

    it('renders the table', () => {
        expect(spectator.query(byTestId('pq-history-table'))).toBeTruthy();
    });

    it('renders rows with status chips', () => {
        const tags = spectator.queryAll(byTestId('pq-history-status'));
        expect(tags.length).toBe(2);
    });

    it('shows the bulk action bar only when there is a selection', () => {
        expect(spectator.query(byTestId('pq-history-bulk-bar'))).toBeFalsy();

        historySelectedIds.set(['b1']);
        spectator.detectChanges();

        expect(spectator.query(byTestId('pq-history-bulk-bar'))).toBeTruthy();
    });

    it('row click opens the detail dialog', () => {
        spectator.component.onRowClick(row('b1'));
        expect(store.openDetail).toHaveBeenCalledWith('b1');
    });

    it('bulk retry calls retryBundles with the selected ids', () => {
        historySelectedIds.set(['b1', 'b2']);
        spectator.component.onBulkRetry();
        expect(store.retryBundles).toHaveBeenCalledWith({ bundleIds: ['b1', 'b2'] });
    });

    it('bulk remove opens confirmation, then calls deleteBundlesBulk on accept', () => {
        historySelectedIds.set(['b1', 'b2']);
        spectator.component.onBulkRemove();
        expect(confirmationService.confirm).toHaveBeenCalled();
        expect(store.deleteBundlesBulk).toHaveBeenCalledWith(['b1', 'b2']);
    });

    it('statusSeverity maps SUCCESS → success and failures → danger', () => {
        expect(spectator.component.statusSeverity(PublishAuditStatus.SUCCESS)).toBe('success');
        expect(spectator.component.statusSeverity(PublishAuditStatus.FAILED_TO_PUBLISH)).toBe(
            'danger'
        );
    });
});
