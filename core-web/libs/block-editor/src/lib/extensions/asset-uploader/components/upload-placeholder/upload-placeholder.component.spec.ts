import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { UploadPlaceholderComponent } from './upload-placeholder.component';

describe('UploadPlaceholderComponent', () => {
    let spectator: Spectator<UploadPlaceholderComponent>;
    const createComponent = createComponentFactory({
        component: UploadPlaceholderComponent,
        providers: []
    });

    beforeEach(() => (spectator = createComponent()));

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
