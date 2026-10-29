import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { Mock, MockInstance, Mocked, vi } from 'vitest';

import { signal } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotContentletEditUrlService, DotMessageService } from '@dotcms/data-access';
import { BundleAssetView, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotEditContentOpenerService, DotEditContentOpenOptions } from '@dotcms/edit-content';
import { DotSidePanelRef } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueAssetListDialogComponent } from './dot-publishing-queue-asset-list-dialog.component';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';

/** Whether anything resolved `DotContentletEditUrlService` while the dialog was mounted. */
let editUrlServiceInjected: boolean;

const ASSETS: BundleAssetView[] = [
    { asset: 'a1', title: 'Asset 1', type: 'contentlet', inode: 'i1', content_type_name: 'Blog' },
    { asset: 'a2', title: 'Asset 2', type: 'template' }
];

describe('DotPublishingQueueAssetListDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueAssetListDialogComponent>;
    let store: ReturnType<typeof storeStub>;
    let confirmationService: Mocked<ConfirmationService>;
    let dialogRef: Mocked<DynamicDialogRef>;

    const selectedAssets = signal<BundleAssetView[]>([]);
    const assetListStatus = signal<'init' | 'loading' | 'loaded' | 'error'>('loading');
    const selectedBundleId = signal<string | null>(null);

    function storeStub() {
        return {
            selectedAssets,
            assetListStatus,
            selectedBundleId,
            removeBundleAsset: vi.fn(),
            loadAssets: vi.fn()
        };
    }

    const createComponent = createComponentFactory({
        component: DotPublishingQueueAssetListDialogComponent,
        componentProviders: [mockProvider(DotPublishingQueueStore, storeStub())],
        providers: [
            ConfirmationService,
            mockProvider(DynamicDialogRef, { close: vi.fn() }),
            // Records whether the dialog asked for it at all: it no longer resolves URLs (#38303).
            {
                provide: DotContentletEditUrlService,
                useFactory: () => {
                    editUrlServiceInjected = true;

                    return {
                        resolveEditUrl: vi.fn().mockReturnValue(of('/dotAdmin/#/edit/inode/i1'))
                    };
                }
            },
            mockProvider(DotEditContentOpenerService, {
                open: vi.fn(() => new DotSidePanelRef(() => undefined))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'publishing-queue.column.name': 'Name',
                    'publishing-queue.column.type': 'Type',
                    'publishing-queue.asset-list.empty': 'No items',
                    'publishing-queue.select-bundle.delete-tooltip': 'Delete from bundle',
                    'publishing-queue.asset-list.remove-confirm.header':
                        'Remove asset from bundle?',
                    'publishing-queue.asset-list.remove-confirm.message':
                        'Are you sure you want to remove "{0}" from this bundle?',
                    'publishing-queue.detail.search-assets': 'Search assets',
                    'publishing-queue.detail.assets-no-matches': 'No assets match your search.',
                    'publishing-queue.remove': 'Remove',
                    'publishing-queue.cancel': 'Cancel',
                    'publishing-queue.close': 'Close'
                })
            }
        ]
    });

    beforeEach(() => {
        editUrlServiceInjected = false;
        selectedAssets.set([]);
        assetListStatus.set('loading');
        selectedBundleId.set(null);
        spectator = createComponent();
        store = spectator.inject(DotPublishingQueueStore, true) as unknown as ReturnType<
            typeof storeStub
        >;
        confirmationService = spectator.inject(ConfirmationService) as Mocked<ConfirmationService>;
        dialogRef = spectator.inject(DynamicDialogRef) as Mocked<DynamicDialogRef>;
        vi.spyOn(confirmationService, 'confirm').mockImplementation((cfg) => {
            cfg.accept?.();
            return confirmationService;
        });
        vi.clearAllMocks();
    });

    it('reserves a fixed-height shell + always-mounted table header (no resize jank)', () => {
        // Shell + table render in every state so the dialog doesn't shrink/expand.
        expect(spectator.query(byTestId('pq-asset-list-shell'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-asset-list-table'))).toBeTruthy();
    });

    it('shows skeleton rows when status is loading', () => {
        const skeletons = spectator.queryAll(byTestId('pq-asset-list-skeleton'));
        expect(skeletons.length).toBe(8);
        expect(spectator.query(byTestId('pq-asset-list-row'))).toBeFalsy();
        expect(spectator.query(byTestId('pq-asset-list-empty'))).toBeFalsy();
    });

    it('shows empty state inside the table when loaded with no assets', () => {
        assetListStatus.set('loaded');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-asset-list-empty'))).toBeTruthy();
        expect(spectator.query(byTestId('pq-asset-list-row'))).toBeFalsy();
    });

    it('renders rows when assets are present', () => {
        selectedAssets.set(ASSETS);
        assetListStatus.set('loaded');
        spectator.detectChanges();
        const rows = spectator.queryAll(byTestId('pq-asset-list-row'));
        expect(rows.length).toBe(2);
        expect(spectator.query(byTestId('pq-asset-list-skeleton'))).toBeFalsy();
    });

    it('renders Name + Type columns plus an action column for the trash button', () => {
        selectedAssets.set(ASSETS);
        assetListStatus.set('loaded');
        spectator.detectChanges();
        const headers = spectator.queryAll('th');
        // 2 visible (Name, Type) + 1 empty (action column for the trash icon) = 3
        expect(headers.length).toBe(3);
    });

    /**
     * #38303 (R12) — a contentlet row opens the side panel over this dialog, in whichever editor its
     * type uses; closing the panel returns to the dialog on the same row. Every save, and the close,
     * reload the dialog's asset list (FR-017).
     */
    describe('row click → open editor', () => {
        let windowOpen: MockInstance;

        /** The opener's `open`, as the mock records it. */
        const opened = () => spectator.inject(DotEditContentOpenerService).open as Mock;

        /** The options the dialog handed the opener, on its only `open`. */
        const openOptions = (): DotEditContentOpenOptions => {
            expect(opened().mock.calls.length, 'the row opens one panel').toBe(1);

            return opened().mock.calls[0][1] as DotEditContentOpenOptions;
        };

        /** Clicks the row at `index`, as the author does. */
        const clickRow = (index: number): void =>
            spectator.click(spectator.queryAll(byTestId('pq-asset-list-row'))[index]);

        beforeEach(() => {
            windowOpen = vi.spyOn(window, 'open').mockReturnValue(null);
            selectedAssets.set(ASSETS);
            assetListStatus.set('loaded');
            spectator.detectChanges();
        });

        afterEach(() => windowOpen.mockRestore());

        it('opens a contentlet row in the side panel, naming the Publishing Queue', () => {
            clickRow(0);

            expect(opened().mock.calls.length, 'the row opens one panel').toBe(1);
            expect(opened().mock.calls[0][0]).toEqual({
                mode: 'edit',
                inode: 'i1',
                contentType: 'Blog',
                portletId: 'publishing-queue'
            });
        });

        // The panel then closes with the dialog, not with the portlet.
        it("opens it with the dialog's injector", () => {
            clickRow(0);

            // Compared by identity: printing an injector runs the worker out of memory.
            expect(
                openOptions().injector.get(DotPublishingQueueAssetListDialogComponent, null) ===
                    spectator.component
            ).toBe(true);
        });

        it('reloads the asset list on every save', () => {
            clickRow(0);

            openOptions().saved?.({ inode: 'i1-saved' } as DotCMSContentlet);

            expect(store.loadAssets).toHaveBeenCalledTimes(1);
        });

        it('reloads the asset list when the panel closes', () => {
            clickRow(0);

            openOptions().closed?.();

            expect(store.loadAssets).toHaveBeenCalledTimes(1);
        });

        it('never opens a new tab', () => {
            clickRow(0);

            expect(windowOpen.mock.calls.length).toBe(0);
        });

        it('does nothing for a non-contentlet row', () => {
            clickRow(1);

            expect(opened().mock.calls.length).toBe(0);
            expect(windowOpen.mock.calls.length).toBe(0);
        });

        // The opener picks the editor per content type, so no URL is resolved ahead of a click.
        it('resolves no edit URLs', () => {
            expect(editUrlServiceInjected).toBe(false);
            expect('$assetEditUrls' in spectator.component).toBe(false);
        });

        it('marks contentlet rows as clickable via cursor-pointer', () => {
            const rows = spectator.queryAll(byTestId('pq-asset-list-row'));
            // Contentlet row (a1) gets cursor-pointer; template row (a2) does not.
            expect(rows[0].classList.contains('cursor-pointer')).toBe(true);
            expect(rows[1].classList.contains('cursor-pointer')).toBe(false);
        });
    });

    describe('close button', () => {
        it('renders a Close button in the footer', () => {
            selectedAssets.set(ASSETS);
            assetListStatus.set('loaded');
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-asset-list-close-btn'))).toBeTruthy();
        });

        it('closes the dialog when clicked', () => {
            spectator.component.closeDialog();
            expect(dialogRef.close).toHaveBeenCalled();
        });
    });

    describe('per-row remove asset', () => {
        beforeEach(() => {
            selectedAssets.set(ASSETS);
            assetListStatus.set('loaded');
            spectator.detectChanges();
        });

        it('renders a trash button per row', () => {
            const buttons = spectator.queryAll(byTestId('pq-asset-remove-btn'));
            expect(buttons.length).toBe(2);
        });

        it('renders the trash button with the neutral secondary severity, not danger', () => {
            const buttons = spectator
                .queryAll(Button)
                .filter(
                    (button) => button.el.nativeElement.dataset.testid === 'pq-asset-remove-btn'
                );

            expect(buttons.length).toBe(2);
            buttons.forEach((button) => expect(button.severity).toBe('secondary'));
        });

        it('labels the trash button like the Select Bundle dialog ("Delete from bundle")', () => {
            const [button] = spectator.queryAll(byTestId('pq-asset-remove-btn'));
            expect(button.getAttribute('aria-label')).toBe('Delete from bundle');
        });

        it('confirms before removing, then calls store.removeBundleAsset with the asset id', () => {
            spectator.component.onRemoveAsset(ASSETS[0]);
            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(store.removeBundleAsset).toHaveBeenCalledWith('a1');
        });
    });

    describe('search (visible only when selectedAssets.length > 10)', () => {
        const manyAssets: BundleAssetView[] = Array.from({ length: 15 }, (_, i) => ({
            asset: `a${i}`,
            title: i % 2 === 0 ? `Homepage ${i}` : `Template ${i}`,
            type: i % 2 === 0 ? 'contentlet' : 'template'
        }));

        it('hides the search input when there are 10 or fewer assets', () => {
            selectedAssets.set(ASSETS);
            assetListStatus.set('loaded');
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-asset-list-search'))).toBeFalsy();
            expect(spectator.query(byTestId('pq-asset-list-search-bar'))).toBeFalsy();
        });

        it('renders the search input when there are more than 10 assets', () => {
            selectedAssets.set(manyAssets);
            assetListStatus.set('loaded');
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-asset-list-search-bar'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-asset-list-search'))).toBeTruthy();
        });

        it('filters rows by title or type when search is set', () => {
            vi.useFakeTimers();
            try {
                selectedAssets.set(manyAssets);
                assetListStatus.set('loaded');
                spectator.detectChanges();
                expect(spectator.queryAll(byTestId('pq-asset-list-row')).length).toBe(15);

                spectator.component.onSearch('template');
                vi.advanceTimersByTime(300);
                spectator.detectChanges();

                // Half the assets have type 'template' (every odd index) — 7 of 15
                const rows = spectator.queryAll(byTestId('pq-asset-list-row'));
                expect(rows.length).toBe(7);
            } finally {
                vi.useRealTimers();
            }
        });

        it('shows the "no matches" message when search returns zero but bundle has assets', () => {
            selectedAssets.set(manyAssets);
            assetListStatus.set('loaded');
            spectator.detectChanges();

            spectator.component.$assetSearch.set('something-that-doesnt-exist');
            spectator.detectChanges();

            expect(spectator.query(byTestId('pq-asset-list-no-matches'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-asset-list-empty'))).toBeFalsy();
        });

        it('resets search when the dialog is reused for a different bundle', () => {
            selectedBundleId.set('A');
            selectedAssets.set(manyAssets);
            assetListStatus.set('loaded');
            spectator.detectChanges();

            spectator.component.$assetSearch.set('homepage');
            spectator.detectChanges();
            expect(spectator.component.$assetSearch()).toBe('homepage');

            selectedBundleId.set('B');
            spectator.detectChanges();
            expect(spectator.component.$assetSearch()).toBe('');
        });
    });
});

describe('DotPublishingQueueAssetListDialogComponent — read-only (allowRemove=false)', () => {
    let spectator: Spectator<DotPublishingQueueAssetListDialogComponent>;

    const selectedAssets = signal<BundleAssetView[]>(ASSETS);
    const assetListStatus = signal<'init' | 'loading' | 'loaded' | 'error'>('loaded');
    const selectedBundleId = signal<string | null>('B-1');

    const createComponent = createComponentFactory({
        component: DotPublishingQueueAssetListDialogComponent,
        componentProviders: [
            mockProvider(DotPublishingQueueStore, {
                selectedAssets,
                assetListStatus,
                selectedBundleId,
                removeBundleAsset: vi.fn()
            })
        ],
        providers: [
            ConfirmationService,
            mockProvider(DynamicDialogRef, { close: vi.fn() }),
            mockProvider(DotContentletEditUrlService, {
                resolveEditUrl: vi.fn().mockReturnValue(of('/dotAdmin/#/edit/inode/i1'))
            }),
            mockProvider(DotEditContentOpenerService, {
                open: vi.fn(() => new DotSidePanelRef(() => undefined))
            }),
            { provide: DynamicDialogConfig, useValue: { data: { allowRemove: false } } },
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ]
    });

    beforeEach(() => {
        selectedAssets.set(ASSETS);
        assetListStatus.set('loaded');
        selectedBundleId.set('B-1');
        spectator = createComponent();
    });

    it('reads allowRemove=false from DynamicDialogConfig.data', () => {
        expect(spectator.component.allowRemove).toBe(false);
    });

    it('renders only Name + Type columns (no trash action column)', () => {
        const headers = spectator.queryAll('th');
        expect(headers.length).toBe(2);
    });

    it('does NOT render any trash buttons in rows', () => {
        expect(spectator.query(byTestId('pq-asset-remove-btn'))).toBeFalsy();
        expect(spectator.queryAll(byTestId('pq-asset-list-row')).length).toBe(2);
    });
});
