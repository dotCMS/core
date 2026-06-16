import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { BundleAssetView } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueAssetListDialogComponent } from './dot-publishing-queue-asset-list-dialog.component';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

const ASSETS: BundleAssetView[] = [
    { asset: 'a1', title: 'Asset 1', type: 'contentlet' },
    { asset: 'a2', title: 'Asset 2', type: 'template' }
];

describe('DotPublishingQueueAssetListDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueAssetListDialogComponent>;
    let store: ReturnType<typeof storeStub>;
    let confirmationService: jest.Mocked<ConfirmationService>;

    const selectedAssets = signal<BundleAssetView[]>([]);
    const assetListStatus = signal<'init' | 'loading' | 'loaded' | 'error'>('loading');
    const selectedBundleId = signal<string | null>(null);

    function storeStub() {
        return {
            selectedAssets,
            assetListStatus,
            selectedBundleId,
            removeBundleAsset: jest.fn()
        };
    }

    const createComponent = createComponentFactory({
        component: DotPublishingQueueAssetListDialogComponent,
        componentProviders: [mockProvider(DotPublishingQueueStore, storeStub())],
        providers: [
            ConfirmationService,
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'publishing-queue.column.name': 'Name',
                    'publishing-queue.column.type': 'Type',
                    'publishing-queue.asset-list.empty': 'No items',
                    'publishing-queue.asset-list.remove': 'Remove from bundle',
                    'publishing-queue.asset-list.remove-confirm.header':
                        'Remove asset from bundle?',
                    'publishing-queue.asset-list.remove-confirm.message':
                        'Are you sure you want to remove "{0}" from this bundle?',
                    'publishing-queue.detail.search-assets': 'Search assets',
                    'publishing-queue.detail.assets-no-matches': 'No assets match your search.',
                    'publishing-queue.remove': 'Remove',
                    'publishing-queue.cancel': 'Cancel'
                })
            }
        ]
    });

    beforeEach(() => {
        selectedAssets.set([]);
        assetListStatus.set('loading');
        selectedBundleId.set(null);
        spectator = createComponent();
        store = spectator.inject(DotPublishingQueueStore, true) as unknown as ReturnType<
            typeof storeStub
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
            jest.useFakeTimers();
            try {
                selectedAssets.set(manyAssets);
                assetListStatus.set('loaded');
                spectator.detectChanges();
                expect(spectator.queryAll(byTestId('pq-asset-list-row')).length).toBe(15);

                spectator.component.onSearch('template');
                jest.advanceTimersByTime(300);
                spectator.detectChanges();

                // Half the assets have type 'template' (every odd index) — 7 of 15
                const rows = spectator.queryAll(byTestId('pq-asset-list-row'));
                expect(rows.length).toBe(7);
            } finally {
                jest.useRealTimers();
            }
        });

        it('shows the "no matches" message when search returns zero but bundle has assets', () => {
            selectedAssets.set(manyAssets);
            assetListStatus.set('loaded');
            spectator.detectChanges();

            spectator.component.assetSearch.set('something-that-doesnt-exist');
            spectator.detectChanges();

            expect(spectator.query(byTestId('pq-asset-list-no-matches'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-asset-list-empty'))).toBeFalsy();
        });

        it('resets search when the dialog is reused for a different bundle', () => {
            selectedBundleId.set('A');
            selectedAssets.set(manyAssets);
            assetListStatus.set('loaded');
            spectator.detectChanges();

            spectator.component.assetSearch.set('homepage');
            spectator.detectChanges();
            expect(spectator.component.assetSearch()).toBe('homepage');

            selectedBundleId.set('B');
            spectator.detectChanges();
            expect(spectator.component.assetSearch()).toBe('');
        });
    });
});
