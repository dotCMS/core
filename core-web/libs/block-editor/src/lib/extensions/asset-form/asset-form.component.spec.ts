import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { AssetFormComponent } from './asset-form.component';

describe('AssetFormComponent', () => {
    let spectator: Spectator<AssetFormComponent>;
    const createComponent = createComponentFactory(AssetFormComponent);

    beforeEach(() => (spectator = createComponent()));

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
