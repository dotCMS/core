import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { BundleAssetView } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueAssetListDialogComponent } from './dot-publishing-queue-asset-list-dialog.component';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

const ASSETS: BundleAssetView[] = [
    { id: 'a1', title: 'Asset 1', type: 'contentlet', state: 'PUBLISH' },
    { id: 'a2', title: 'Asset 2', type: 'template' }
];

describe('DotPublishingQueueAssetListDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueAssetListDialogComponent>;

    const selectedAssets = signal<BundleAssetView[]>([]);
    const assetListStatus = signal<'init' | 'loading' | 'loaded' | 'error'>('loading');

    const createComponent = createComponentFactory({
        component: DotPublishingQueueAssetListDialogComponent,
        providers: [
            mockProvider(DotPublishingQueueStore, {
                selectedAssets,
                assetListStatus
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'publishing-queue.column.name': 'Name',
                    'publishing-queue.column.type': 'Type',
                    'publishing-queue.column.state': 'State',
                    'publishing-queue.asset-list.empty': 'No items'
                })
            }
        ]
    });

    beforeEach(() => {
        selectedAssets.set([]);
        assetListStatus.set('loading');
        spectator = createComponent();
    });

    it('shows loading skeleton when status is loading', () => {
        expect(spectator.query(byTestId('pq-asset-list-loading'))).toBeTruthy();
    });

    it('shows empty state when loaded with no assets', () => {
        assetListStatus.set('loaded');
        spectator.detectChanges();
        expect(spectator.query(byTestId('pq-asset-list-empty'))).toBeTruthy();
    });

    it('renders rows when assets are present', () => {
        selectedAssets.set(ASSETS);
        assetListStatus.set('loaded');
        spectator.detectChanges();
        const rows = spectator.queryAll(byTestId('pq-asset-list-row'));
        expect(rows.length).toBe(2);
    });

    it('renders "—" for missing state', () => {
        selectedAssets.set([ASSETS[1]]);
        assetListStatus.set('loaded');
        spectator.detectChanges();
        const stateCell = spectator.query(byTestId('pq-asset-state'));
        expect(stateCell?.textContent?.trim()).toBe('—');
    });
});
