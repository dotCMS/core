import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA, signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotPublishingQueueToolbarComponent,
    SELECT_BUNDLE_ITEM_ID
} from './dot-publishing-queue-toolbar.component';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';
import { DotPublishingQueueStatusFilterComponent } from '../dot-publishing-queue-status-filter/dot-publishing-queue-status-filter.component';

describe('DotPublishingQueueToolbarComponent', () => {
    let spectator: Spectator<DotPublishingQueueToolbarComponent>;
    let store: ReturnType<typeof makeStoreStub>;

    const bundlesSelectedIds = signal<string[]>([]);
    const bundlesTotal = signal<number>(0);
    const draftBundlesTotal = signal<number | null>(null);

    function makeStoreStub() {
        return {
            search: jest.fn().mockReturnValue(''),
            setSearch: jest.fn(),
            refresh: jest.fn(),
            bundlesSelectedIds,
            bundlesTotal,
            draftBundlesTotal,
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
                    'publishing-queue.delete-bundles': 'Remove',
                    'publishing-queue.selected': 'selected',
                    'publishing-queue.bundles': 'Bundles',
                    'publishing-queue.bundles.count': 'Bundles ({0})',
                    'publishing-queue.add-bundle.select': 'Select Bundle',
                    'publishing-queue.add-bundle.upload': 'Upload'
                })
            }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });

    beforeEach(() => {
        jest.useFakeTimers();
        bundlesSelectedIds.set([]);
        bundlesTotal.set(0);
        draftBundlesTotal.set(null);
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

    describe('Bundles (N) trigger', () => {
        it('shows the draft count in the button label', () => {
            draftBundlesTotal.set(22);
            spectator.detectChanges();

            expect(spectator.query(byTestId('pq-add-bundle-btn-label'))?.textContent?.trim()).toBe(
                'Bundles (22)'
            );
        });

        it('shows a bare "Bundles" while the count is unknown, never "(0)"', () => {
            draftBundlesTotal.set(null);
            spectator.detectChanges();

            expect(spectator.query(byTestId('pq-add-bundle-btn-label'))?.textContent?.trim()).toBe(
                'Bundles'
            );
        });

        it('shows "(0)" when the user genuinely has no drafts', () => {
            draftBundlesTotal.set(0);
            spectator.detectChanges();

            expect(spectator.query(byTestId('pq-add-bundle-btn-label'))?.textContent?.trim()).toBe(
                'Bundles (0)'
            );
        });

        it('keeps the aria-label in sync with the visible count', () => {
            draftBundlesTotal.set(7);
            spectator.detectChanges();

            expect(spectator.query(byTestId('pq-add-bundle-btn'))?.getAttribute('aria-label')).toBe(
                'Bundles (7)'
            );
        });
    });

    describe('Add Bundle dropdown', () => {
        /** The menu is `appendTo="body"`, so its rows live outside the fixture. */
        const queryMenu = (selector: string) => document.body.querySelector(selector);

        function openMenu() {
            spectator.click(
                spectator.query(byTestId('pq-add-bundle-btn'))?.querySelector('button') ??
                    (spectator.query(byTestId('pq-add-bundle-btn')) as HTMLElement)
            );
            spectator.detectChanges();
        }

        it('repeats the draft count next to Select Bundle so it matches the button', () => {
            draftBundlesTotal.set(22);
            spectator.detectChanges();
            openMenu();

            expect(queryMenu('[data-testid="pq-select-bundle-count"]')?.textContent?.trim()).toBe(
                '22'
            );
        });

        it('omits the count next to Select Bundle while it is unknown', () => {
            draftBundlesTotal.set(null);
            spectator.detectChanges();
            openMenu();

            expect(queryMenu('[data-testid="pq-select-bundle-count"]')).toBeNull();
        });

        it('does not put a count on the Upload row', () => {
            draftBundlesTotal.set(22);
            spectator.detectChanges();
            openMenu();

            expect(
                document.body.querySelectorAll('[data-testid="pq-select-bundle-count"]').length
            ).toBe(1);
        });

        it('tags the Select Bundle row so the item template can add the count', () => {
            expect(spectator.component.addBundleItems[0].id).toBe(SELECT_BUNDLE_ITEM_ID);
            expect(spectator.component.addBundleItems[1].id).toBeUndefined();
        });

        it('exposes two menu items: Select Bundle + Upload', () => {
            expect(spectator.component.addBundleItems.length).toBe(2);
            expect(spectator.component.addBundleItems[0].label).toBeTruthy();
            expect(spectator.component.addBundleItems[1].label).toBeTruthy();
        });

        it('Upload item → emits uploadClick', () => {
            const emit = jest.fn();
            spectator.component.$uploadClick.subscribe(emit);
            spectator.component.addBundleItems[1].command?.({} as never);
            expect(emit).toHaveBeenCalled();
        });

        it('Select Bundle item → emits selectBundleClick (placeholder for future dialog)', () => {
            const emit = jest.fn();
            spectator.component.$selectBundleClick.subscribe(emit);
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
            const refreshBtn = spectator.query(byTestId('pq-refresh-btn'));
            expect(refreshBtn).toBeTruthy();
            spectator.click(refreshBtn as HTMLButtonElement);
            expect(store.refresh).toHaveBeenCalled();
        });
    });

    describe('Retry Send (selection-gated, swaps with Add Bundle)', () => {
        it('is hidden when nothing is selected — Add Bundle is the primary', () => {
            bundlesSelectedIds.set([]);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-bulk-retry'))).toBeFalsy();
            expect(spectator.query(byTestId('pq-add-bundle-btn'))).toBeTruthy();
        });

        it('replaces Add Bundle as the primary when there is a selection', () => {
            bundlesSelectedIds.set(['b1', 'b2']);
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-bulk-retry'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-add-bundle-btn'))).toBeFalsy();
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
            const emit = jest.fn();
            spectator.component.$deleteClick.subscribe(emit);
            const btn = spectator.query(byTestId('pq-bulk-delete'));
            spectator.click(btn as HTMLButtonElement);
            expect(emit).toHaveBeenCalled();
        });
    });
});
