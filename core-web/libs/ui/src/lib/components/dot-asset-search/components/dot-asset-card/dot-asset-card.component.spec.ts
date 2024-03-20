import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { CardModule } from 'primeng/card';

import { DotAssetCardComponent } from './dot-asset-card.component';

describe('DotAssetCardComponent', () => {
    let spectator: Spectator<DotAssetCardComponent>;
    let component: DotAssetCardComponent;

    const createComponent = createComponentFactory({
        component: DotAssetCardComponent,
        imports: [CardModule]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
