import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA, signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueToolbarComponent } from './dot-publishing-queue-toolbar.component';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';
import { DotPublishingQueueStatusFilterComponent } from '../dot-publishing-queue-status-filter/dot-publishing-queue-status-filter.component';

describe('DotPublishingQueueToolbarComponent', () => {
    let spectator: Spectator<DotPublishingQueueToolbarComponent>;
    let store: ReturnType<typeof makeStoreStub>;

    const bundlesSelectedIds = signal<string[]>([]);
    const bundlesTotal = signal<number>(0);

    function makeStoreStub() {
        return {
            search: jest.fn().mockReturnValue(''),
            setSearch: jest.fn(),
            refresh: jest.fn(),
            bundlesSelectedIds,
            bundlesTotal,
            retryBundles: jest.fn()
        };
    }

    const createComponent = createComponentFactory({
        component: DotPublishingQueueToolbarComponent,
        overrideComponents: [
            [
                DotPublishingQueueStatusFilterComponent,
                {
                    set: {
                        template: '<div data-testid="pq-status-filter-stub"></div>',
                        imports: []
                    }
                }
            ]
        ],
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
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });

    beforeEach(() => {
        jest.useFakeTimers();
        bundlesSelectedIds.set([]);
        bundlesTotal.set(0);
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
        it('renders search, status filter, refresh, add bundle dropdown', () => {
            expect(spectator.query(byTestId('pq-search-input'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-status-filter-stub'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-refresh-btn'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-add-bundle-btn'))).toBeTruthy();
        });
    });

    describe('Add Bundle dropdown', () => {
        it('exposes two menu items: Select Bundle + Upload', () => {
            expect(spectator.component.addBundleItems.length).toBe(2);
            expect(spectator.component.addBundleItems[0].icon).toBe('pi pi-table');
            expect(spectator.component.addBundleItems[1].icon).toBe('pi pi-upload');
        });

        it('Upload item → emits uploadClick', () => {
            const emit = jest.fn();
            spectator.component.uploadClick.subscribe(emit);
            spectator.component.addBundleItems[1].command?.({} as never);
            expect(emit).toHaveBeenCalled();
        });

        it('Select Bundle item → emits selectBundleClick (placeholder for future dialog)', () => {
            const emit = jest.fn();
            spectator.component.selectBundleClick.subscribe(emit);
            spectator.component.addBundleItems[0].command?.({} as never);
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
        it('is hidden when nothing is selected', () => {
            bundlesSelectedIds.set([]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-bulk-retry'))).toBeFalsy();
            expect(spectator.query(byTestId('pq-bulk-count'))).toBeFalsy();
        });

        it('shows the retry button + selected-count when there is a selection', () => {
            bundlesSelectedIds.set(['b1', 'b2']);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-bulk-retry'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-bulk-count'))?.textContent).toContain('2');
        });

        it('clicking retry calls retryBundles with the selected ids', () => {
            bundlesSelectedIds.set(['b1', 'b2']);
            spectator.detectChanges();
            const btn = spectator.query(byTestId('pq-bulk-retry'))?.querySelector('button');
            spectator.click(btn as HTMLButtonElement);
            expect(store.retryBundles).toHaveBeenCalledWith({ bundleIds: ['b1', 'b2'] });
        });
    });

    describe('Delete Bundles (selection-gated)', () => {
        it('is hidden when nothing is selected', () => {
            bundlesSelectedIds.set([]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-bulk-delete'))).toBeFalsy();
        });

        it('shows when there is a selection', () => {
            bundlesSelectedIds.set(['b1']);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-bulk-delete'))).toBeTruthy();
        });

        it('emits deleteClick when clicked', () => {
            bundlesSelectedIds.set(['b1']);
            spectator.detectChanges();
            const emit = jest.fn();
            spectator.component.deleteClick.subscribe(emit);
            const btn = spectator.query(byTestId('pq-bulk-delete'))?.querySelector('button');
            spectator.click(btn as HTMLButtonElement);
            expect(emit).toHaveBeenCalled();
        });
    });
});
