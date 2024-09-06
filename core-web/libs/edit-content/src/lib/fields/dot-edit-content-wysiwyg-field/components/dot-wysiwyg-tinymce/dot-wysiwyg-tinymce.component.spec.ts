import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { DotWysiwygTinymceComponent } from './dot-wysiwyg-tinymce.component';

describe('DotWysiwygTinymceComponent', () => {
    let spectator: Spectator<DotWysiwygTinymceComponent>;
    const createComponent = createComponentFactory(DotWysiwygTinymceComponent);

    it('should create', () => {
        spectator = createComponent();

        expect(spectator.component).toBeTruthy();
    });
});
