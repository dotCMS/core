import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { DotSeoImagePreviewComponent } from './dot-seo-image-preview.component';

describe('DotSeoImagePreviewComponent', () => {
    let spectator: Spectator<DotSeoImagePreviewComponent>;
    const createComponent = createComponentFactory(DotSeoImagePreviewComponent);

    it('should create', () => {
        spectator = createComponent();

        expect(spectator.component).toBeTruthy();
    });
});
