import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { DotUploadTabComponent } from './dot-upload-tab.component';

describe('DotUploadTabComponent', () => {
    let spectator: Spectator<DotUploadTabComponent>;
    const createComponent = createComponentFactory(DotUploadTabComponent);

    it('should create', () => {
        spectator = createComponent();

        expect(spectator.component).toBeTruthy();
    });
});
