import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { CardModule } from 'primeng/card';
import { Skeleton, SkeletonModule } from 'primeng/skeleton';

import { DotAssetCardSkeletonComponent } from './dot-asset-card-skeleton.component';

describe('DotAssetCardSkeletonComponent', () => {
    let spectator: Spectator<DotAssetCardSkeletonComponent>;

    const createComponent = createComponentFactory({
        component: DotAssetCardSkeletonComponent,
        imports: [CardModule, SkeletonModule],
        declarations: [Skeleton]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should have the four skelestons components', () => {
        const skeletons = spectator.queryAll(Skeleton);
        expect(skeletons).toHaveLength(4);
    });

    it('should have the right inputs for each p-skeleton', () => {
        const headerSkeleton = spectator.query('[data-testId="p-skeleton-header"]');
        expect(headerSkeleton.getAttribute('shape')).toEqual('square');
        expect(headerSkeleton.getAttribute('size')).toEqual('94px');

        const bodySkeleton = spectator.query('[data-testId="p-skeleton-body"]');
        expect(bodySkeleton.getAttribute('height')).toEqual('1rem');

        const state1Skeleton = spectator.query('[data-testId="p-skeleton-state-1"]');
        expect(state1Skeleton.getAttribute('width')).toEqual('2rem');
        expect(state1Skeleton.getAttribute('height')).toEqual('1rem');

        const state2Skeleton = spectator.query('[data-testId="p-skeleton-state-2"]');
        expect(state2Skeleton.getAttribute('shape')).toEqual('circle');
        expect(state2Skeleton.getAttribute('size')).toEqual('16px');
    });
});
