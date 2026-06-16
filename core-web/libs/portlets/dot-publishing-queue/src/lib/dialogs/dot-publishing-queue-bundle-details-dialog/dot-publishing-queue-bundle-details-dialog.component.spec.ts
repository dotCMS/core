import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService, DotPublishingQueueService } from '@dotcms/data-access';
import {
    BundleAssetView,
    PublishAuditStatus,
    PublishingJobDetailView
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueBundleDetailsDialogComponent } from './dot-publishing-queue-bundle-details-dialog.component';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

const detailFixture = (
    overrides: Partial<PublishingJobDetailView> = {}
): PublishingJobDetailView => ({
    bundleId: 'b-1',
    bundleName: 'My Bundle',
    status: PublishAuditStatus.SUCCESS,
    filterName: 'Default',
    filterKey: 'default.yml',
    assetCount: 2,
    environments: [
        {
            id: 'env-1',
            name: 'Prod',
            endpoints: [
                {
                    id: 'ep-1',
                    serverName: 'srv1',
                    address: '127.0.0.1',
                    port: '443',
                    protocol: 'https',
                    status: PublishAuditStatus.SUCCESS,
                    statusMessage: 'ok',
                    stackTrace: null
                }
            ]
        }
    ],
    timestamps: {
        bundleStart: '2026-06-08T10:00:00Z',
        bundleEnd: '2026-06-08T10:01:00Z',
        publishStart: '2026-06-08T10:01:00Z',
        publishEnd: '2026-06-08T10:02:00Z',
        createDate: '2026-06-08T10:00:00Z',
        statusUpdated: '2026-06-08T10:02:00Z'
    },
    numTries: 1,
    ...overrides
});

describe('DotPublishingQueueBundleDetailsDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueBundleDetailsDialogComponent>;

    const detail = signal<PublishingJobDetailView | null>(null);
    const detailStatus = signal<'init' | 'loading' | 'loaded' | 'error'>('loading');
    const detailAssets = signal<BundleAssetView[]>([]);
    const detailAssetsStatus = signal<'init' | 'loading' | 'loaded' | 'error'>('loaded');
    const detailBundleId = signal<string | null>(null);

    const createComponent = createComponentFactory({
        component: DotPublishingQueueBundleDetailsDialogComponent,
        providers: [
            mockProvider(DotPublishingQueueStore, {
                detail,
                detailStatus,
                detailAssets,
                detailAssetsStatus,
                detailBundleId
            }),
            mockProvider(DotPublishingQueueService, {
                getBundleDownloadUrl: jest.fn((id: string) => `/api/bundle/_download/${id}`)
            }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ]
    });

    beforeEach(() => {
        detail.set(null);
        detailStatus.set('loading');
        detailAssets.set([]);
        detailAssetsStatus.set('loaded');
        detailBundleId.set(null);
        spectator = createComponent();
    });

    it('shows skeletons when loading', () => {
        expect(spectator.query(byTestId('pq-detail-meta'))).toBeFalsy();
    });

    it('renders metadata + endpoints once loaded', () => {
        detail.set(detailFixture());
        detailStatus.set('loaded');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-detail-meta'))).toBeTruthy();
        expect(spectator.queryAll(byTestId('pq-detail-endpoint-row')).length).toBe(1);
    });

    it('shows download button only for completed bundles', () => {
        detail.set(detailFixture({ status: PublishAuditStatus.SUCCESS }));
        detailStatus.set('loaded');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-detail-download-btn'))).toBeTruthy();
    });

    it('hides download button for failed bundles', () => {
        detail.set(detailFixture({ status: PublishAuditStatus.FAILED_TO_PUBLISH }));
        detailStatus.set('loaded');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-detail-download-btn'))).toBeFalsy();
    });

    it('shows empty-endpoints message when environments is empty', () => {
        detail.set(detailFixture({ environments: [] }));
        detailStatus.set('loaded');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-detail-endpoints-empty'))).toBeTruthy();
    });

    it('shows error state', () => {
        detail.set(null);
        detailStatus.set('error');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-detail-error'))).toBeTruthy();
    });

    describe('assets section', () => {
        it('reserves space (shell + table header) even while loading so the dialog does not jump', () => {
            detail.set(detailFixture());
            detailStatus.set('loaded');
            detailAssetsStatus.set('loading');
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-detail-assets-shell'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-detail-assets-table'))).toBeTruthy();
            expect(spectator.queryAll(byTestId('pq-detail-asset-skeleton')).length).toBe(5);
            expect(spectator.query(byTestId('pq-detail-asset-row'))).toBeFalsy();
            expect(spectator.query(byTestId('pq-detail-assets-empty'))).toBeFalsy();
        });

        it('renders the asset rows when items are loaded', () => {
            detail.set(detailFixture());
            detailStatus.set('loaded');
            detailAssets.set([
                { asset: 'a1', title: 'Page 1', type: 'contentlet' },
                { asset: 'a2', title: 'Template 1', type: 'template' }
            ]);
            detailAssetsStatus.set('loaded');
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-detail-assets-shell'))).toBeTruthy();
            expect(spectator.queryAll(byTestId('pq-detail-asset-row')).length).toBe(2);
            expect(spectator.query(byTestId('pq-detail-asset-skeleton'))).toBeFalsy();
        });

        it('shows the empty placeholder inside the shell when loaded with no assets', () => {
            detail.set(detailFixture());
            detailStatus.set('loaded');
            detailAssets.set([]);
            detailAssetsStatus.set('loaded');
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-detail-assets-shell'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-detail-assets-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-detail-asset-row'))).toBeFalsy();
        });
    });

    describe('assets search (visible only when assetCount > 10)', () => {
        const manyAssets = Array.from({ length: 15 }, (_, i) => ({
            id: `a${i}`,
            title: i % 2 === 0 ? `Homepage ${i}` : `Template ${i}`,
            type: i % 2 === 0 ? 'contentlet' : 'template'
        }));

        it('hides the search input when assetCount <= 10', () => {
            detail.set(detailFixture({ assetCount: 4 }));
            detailStatus.set('loaded');
            detailAssets.set([{ asset: 'a1', title: 'Asset 1', type: 'contentlet' }]);
            detailAssetsStatus.set('loaded');
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-detail-assets-search'))).toBeFalsy();
        });

        it('renders the search input when assetCount > 10', () => {
            detail.set(detailFixture({ assetCount: 15 }));
            detailStatus.set('loaded');
            detailAssets.set(manyAssets);
            detailAssetsStatus.set('loaded');
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-detail-assets-search'))).toBeTruthy();
        });

        it('filters rows by title or type when search is set', () => {
            jest.useFakeTimers();
            try {
                detail.set(detailFixture({ assetCount: 15 }));
                detailStatus.set('loaded');
                detailAssets.set(manyAssets);
                detailAssetsStatus.set('loaded');
                spectator.detectChanges();
                expect(spectator.queryAll(byTestId('pq-detail-asset-row')).length).toBe(15);

                spectator.component.onSearch('template');
                jest.advanceTimersByTime(300);
                spectator.detectChanges();

                // Half the assets have type 'template' (every odd index) — 7 of 15
                const rows = spectator.queryAll(byTestId('pq-detail-asset-row'));
                expect(rows.length).toBe(7);
            } finally {
                jest.useRealTimers();
            }
        });

        it('shows the "no matches" message when search returns zero but bundle has assets', () => {
            detail.set(detailFixture({ assetCount: 15 }));
            detailStatus.set('loaded');
            detailAssets.set(manyAssets);
            detailAssetsStatus.set('loaded');
            spectator.detectChanges();

            spectator.component.assetSearch.set('something-that-doesnt-exist');
            spectator.detectChanges();

            expect(spectator.query(byTestId('pq-detail-assets-no-matches'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-detail-assets-empty'))).toBeFalsy();
        });

        it('resets search when the dialog is reused for a different bundle', () => {
            // Open the dialog on bundle A and let the init effect settle.
            detailBundleId.set('A');
            detail.set(detailFixture({ bundleId: 'A', assetCount: 15 }));
            detailStatus.set('loaded');
            detailAssetsStatus.set('loaded');
            spectator.detectChanges();

            // Set the search AFTER the effect has run with 'A'.
            spectator.component.assetSearch.set('homepage');
            spectator.detectChanges();
            expect(spectator.component.assetSearch()).toBe('homepage');

            // Switching to a different bundle id triggers the reset effect.
            detailBundleId.set('B');
            spectator.detectChanges();
            expect(spectator.component.assetSearch()).toBe('');
        });
    });
});
