import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { vi } from 'vitest';

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
            search: vi.fn().mockReturnValue(''),
            setSearch: vi.fn(),
            refresh: vi.fn(),
            bundlesSelectedIds,
            bundlesTotal,
            retryBundles: vi.fn()
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
                    'publishing-queue.delete-bundles': 'Remove',
                    'publishing-queue.selected': 'selected',
                    'publishing-queue.publishing-environments': 'Publishing Environments'
                })
            }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });

    beforeEach(() => {
        vi.useFakeTimers();
        bundlesSelectedIds.set([]);
        bundlesTotal.set(0);
        spectator = createComponent();
        store = spectator.inject(DotPublishingQueueStore, true) as unknown as ReturnType<
            typeof makeStoreStub
        >;
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    describe('layout', () => {
        it('renders search, status filter, refresh, Publishing Environments and Upload Bundle', () => {
            expect(spectator.query(byTestId('pq-search-input'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-status-filter-stub'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-refresh-btn'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-publishing-environments-btn'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-upload-bundle-btn'))).toBeTruthy();
        });

        it('no longer renders the Bundles (N) menu', () => {
            expect(spectator.query(byTestId('pq-add-bundle-btn'))).toBeFalsy();
        });
    });

    describe('Upload Bundle', () => {
        it('emits uploadClick when clicked', () => {
            const emit = vi.fn();
            spectator.component.$uploadClick.subscribe(emit);
            spectator.click(spectator.query(byTestId('pq-upload-bundle-btn')) as HTMLElement);
            expect(emit).toHaveBeenCalled();
        });

        it('is labelled "Upload Bundle"', () => {
            expect(spectator.query(byTestId('pq-upload-bundle-btn'))?.textContent?.trim()).toBe(
                'Upload Bundle'
            );
        });
    });

    describe('Publishing Environments', () => {
        it('is an outlined button labelled "Publishing Environments"', () => {
            const btn = spectator.query(byTestId('pq-publishing-environments-btn'));
            expect(btn?.textContent?.trim()).toBe('Publishing Environments');
            expect(btn?.classList).toContain('p-button-outlined');
        });

        it('does nothing yet when clicked — the dialog ships separately', () => {
            const emit = vi.fn();
            spectator.component.$uploadClick.subscribe(emit);
            spectator.click(
                spectator.query(byTestId('pq-publishing-environments-btn')) as HTMLElement
            );
            expect(emit).not.toHaveBeenCalled();
            expect(store.refresh).not.toHaveBeenCalled();
        });
    });

    describe('search debounce', () => {
        it('calls store.setSearch only after 300ms', () => {
            spectator.component.onSearch('hello');
            vi.advanceTimersByTime(299);
            expect(store.setSearch).not.toHaveBeenCalled();

            vi.advanceTimersByTime(1);
            expect(store.setSearch).toHaveBeenCalledWith('hello');
        });

        it('coalesces rapid typing', () => {
            spectator.component.onSearch('a');
            vi.advanceTimersByTime(100);
            spectator.component.onSearch('ab');
            vi.advanceTimersByTime(100);
            spectator.component.onSearch('abc');
            vi.advanceTimersByTime(300);

            expect(store.setSearch).toHaveBeenCalledTimes(1);
            expect(store.setSearch).toHaveBeenCalledWith('abc');
        });

        it('skips duplicate values (distinctUntilChanged)', () => {
            spectator.component.onSearch('x');
            vi.advanceTimersByTime(300);
            spectator.component.onSearch('x');
            vi.advanceTimersByTime(300);

            expect(store.setSearch).toHaveBeenCalledTimes(1);
        });
    });

    describe('refresh', () => {
        it('clicking the refresh button calls store.refresh', () => {
            const refreshBtn = spectator.query(byTestId('pq-refresh-btn'));
            expect(refreshBtn).toBeTruthy();
            spectator.click(refreshBtn as HTMLButtonElement);
            expect(store.refresh).toHaveBeenCalled();
        });
    });

    describe('Retry Send (selection-gated, swaps with Upload Bundle)', () => {
        it('is hidden when nothing is selected — Upload Bundle is the primary', () => {
            bundlesSelectedIds.set([]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-bulk-retry'))).toBeFalsy();
            expect(spectator.query(byTestId('pq-upload-bundle-btn'))).toBeTruthy();
        });

        it('replaces Publishing Environments and Upload Bundle when there is a selection', () => {
            bundlesSelectedIds.set(['b1', 'b2']);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-bulk-retry'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-upload-bundle-btn'))).toBeFalsy();
            expect(spectator.query(byTestId('pq-publishing-environments-btn'))).toBeFalsy();
        });

        it('does not show a separate selected-count label', () => {
            bundlesSelectedIds.set(['b1', 'b2']);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-bulk-count'))).toBeFalsy();
        });

        it('clicking retry calls retryBundles with the selected ids', () => {
            bundlesSelectedIds.set(['b1', 'b2']);
            spectator.detectChanges();
            const btn = spectator.query(byTestId('pq-bulk-retry'));
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
            const emit = vi.fn();
            spectator.component.$deleteClick.subscribe(emit);
            const btn = spectator.query(byTestId('pq-bulk-delete'));
            spectator.click(btn as HTMLButtonElement);
            expect(emit).toHaveBeenCalled();
        });
    });
});
