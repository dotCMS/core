import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueToolbarComponent } from './dot-publishing-queue-toolbar.component';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

describe('DotPublishingQueueToolbarComponent', () => {
    let spectator: Spectator<DotPublishingQueueToolbarComponent>;
    let store: ReturnType<typeof makeStoreStub>;

    const activeTab = signal<'queue' | 'history'>('queue');
    const historySelectedIds = signal<string[]>([]);
    const historyTotal = signal<number>(0);

    function makeStoreStub() {
        return {
            search: jest.fn().mockReturnValue(''),
            setSearch: jest.fn(),
            refresh: jest.fn(),
            activeTab,
            historySelectedIds,
            historyTotal,
            retryBundles: jest.fn()
        };
    }

    const createComponent = createComponentFactory({
        component: DotPublishingQueueToolbarComponent,
        componentProviders: [mockProvider(DotPublishingQueueStore, makeStoreStub())],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'publishing-queue.search.placeholder': 'Search bundles',
                    'publishing-queue.refresh': 'Refresh',
                    'publishing-queue.upload-bundle': 'Upload Bundle',
                    'publishing-queue.retry-send': 'Retry Send',
                    'publishing-queue.delete-bundles': 'Delete Bundles',
                    'publishing-queue.selected': 'selected'
                })
            }
        ]
    });

    beforeEach(() => {
        jest.useFakeTimers();
        activeTab.set('queue');
        historySelectedIds.set([]);
        historyTotal.set(0);
        spectator = createComponent();
        store = spectator.inject(DotPublishingQueueStore, true) as unknown as ReturnType<
            typeof makeStoreStub
        >;
        jest.clearAllMocks();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    describe('layout', () => {
        it('renders search, refresh, upload', () => {
            expect(spectator.query(byTestId('pq-search-input'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-refresh-btn'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-upload-btn'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-site-selector'))).toBeFalsy();
        });

        it('upload button click emits uploadClick', () => {
            const emit = jest.fn();
            spectator.component.uploadClick.subscribe(emit);
            const uploadBtn = spectator.query(byTestId('pq-upload-btn'))?.querySelector('button');
            spectator.click(uploadBtn as HTMLButtonElement);
            expect(emit).toHaveBeenCalled();
        });
    });

    describe('search debounce', () => {
        it('calls store.setSearch only after 300ms', () => {
            spectator.component.onSearch('hello');
            jest.advanceTimersByTime(299);
            expect(store.setSearch).not.toHaveBeenCalled();

            jest.advanceTimersByTime(1);
            expect(store.setSearch).toHaveBeenCalledWith('hello');
        });

        it('coalesces rapid typing', () => {
            spectator.component.onSearch('a');
            jest.advanceTimersByTime(100);
            spectator.component.onSearch('ab');
            jest.advanceTimersByTime(100);
            spectator.component.onSearch('abc');
            jest.advanceTimersByTime(300);

            expect(store.setSearch).toHaveBeenCalledTimes(1);
            expect(store.setSearch).toHaveBeenCalledWith('abc');
        });

        it('skips duplicate values (distinctUntilChanged)', () => {
            spectator.component.onSearch('x');
            jest.advanceTimersByTime(300);
            spectator.component.onSearch('x');
            jest.advanceTimersByTime(300);

            expect(store.setSearch).toHaveBeenCalledTimes(1);
        });
    });

    describe('refresh', () => {
        it('clicking the refresh button calls store.refresh', () => {
            const refreshBtn = spectator.query(byTestId('pq-refresh-btn'))?.querySelector('button');
            expect(refreshBtn).toBeTruthy();
            spectator.click(refreshBtn as HTMLButtonElement);
            expect(store.refresh).toHaveBeenCalled();
        });
    });

    describe('Retry Send (selection-gated)', () => {
        it('is hidden on the queue tab even with a selection', () => {
            activeTab.set('queue');
            historySelectedIds.set(['b1']);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-history-bulk-retry'))).toBeFalsy();
            expect(spectator.query(byTestId('pq-bulk-count'))).toBeFalsy();
        });

        it('is hidden on the history tab when nothing is selected', () => {
            activeTab.set('history');
            historySelectedIds.set([]);
            historyTotal.set(5);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-history-bulk-retry'))).toBeFalsy();
        });

        it('shows the retry button + selected-count on the history tab with selection', () => {
            activeTab.set('history');
            historySelectedIds.set(['b1', 'b2']);
            historyTotal.set(5);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-history-bulk-retry'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-bulk-count'))?.textContent).toContain('2');
        });

        it('clicking retry calls retryBundles with the selected ids', () => {
            activeTab.set('history');
            historySelectedIds.set(['b1', 'b2']);
            historyTotal.set(5);
            spectator.detectChanges();
            const btn = spectator.query(byTestId('pq-history-bulk-retry'))?.querySelector('button');
            spectator.click(btn as HTMLButtonElement);
            expect(store.retryBundles).toHaveBeenCalledWith({ bundleIds: ['b1', 'b2'] });
        });
    });

    describe('Delete Bundles (selection-gated)', () => {
        it('is hidden on the queue tab even with a selection', () => {
            activeTab.set('queue');
            historySelectedIds.set(['b1']);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-history-delete-bundles'))).toBeFalsy();
        });

        it('is hidden on the history tab when nothing is selected', () => {
            activeTab.set('history');
            historyTotal.set(5);
            historySelectedIds.set([]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-history-delete-bundles'))).toBeFalsy();
        });

        it('shows on the history tab when there is a selection', () => {
            activeTab.set('history');
            historySelectedIds.set(['b1']);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-history-delete-bundles'))).toBeTruthy();
        });

        it('emits deleteClick when clicked', () => {
            activeTab.set('history');
            historySelectedIds.set(['b1']);
            spectator.detectChanges();
            const emit = jest.fn();
            spectator.component.deleteClick.subscribe(emit);
            const btn = spectator
                .query(byTestId('pq-history-delete-bundles'))
                ?.querySelector('button');
            spectator.click(btn as HTMLButtonElement);
            expect(emit).toHaveBeenCalled();
        });
    });
});
