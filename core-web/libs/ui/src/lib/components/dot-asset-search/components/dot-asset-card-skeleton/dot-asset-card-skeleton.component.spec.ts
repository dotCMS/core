import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';

import { DotAssetCardSkeletonComponent } from './dot-asset-card-skeleton.component';

describe('DotAssetCardSkeletonComponent', () => {
    let spectator: Spectator<DotAssetCardSkeletonComponent>;
    let component: DotAssetCardSkeletonComponent;

    const createComponent = createComponentFactory({
        component: DotAssetCardSkeletonComponent,
        imports: [CardModule, SkeletonModule]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
