import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';

import { ScrollerModule } from 'primeng/scroller';

import { DotAssetCardListComponent } from './dot-asset-card-list.component';

import { DotAssetCardComponent } from '../dot-asset-card/dot-asset-card.component';
import { DotAssetCardSkeletonComponent } from '../dot-asset-card-skeleton/dot-asset-card-skeleton.component';

describe('DotAssetCardListComponent', () => {
    let spectator: Spectator<DotAssetCardListComponent>;
    let component: DotAssetCardListComponent;

    const createComponent = createComponentFactory({
        component: DotAssetCardListComponent,
        imports: [
            CommonModule,
            ScrollerModule,
            DotAssetCardComponent,
            DotAssetCardSkeletonComponent
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
