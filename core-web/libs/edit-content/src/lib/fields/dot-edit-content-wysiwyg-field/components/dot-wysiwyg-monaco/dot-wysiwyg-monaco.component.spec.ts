import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { DotWysiwygMonacoComponent } from './dot-wysiwyg-monaco.component';

describe('DotWysiwygMonacoComponent', () => {
    let spectator: Spectator<DotWysiwygMonacoComponent>;
    const createComponent = createComponentFactory(DotWysiwygMonacoComponent);

    it('should create', () => {
        spectator = createComponent();

        expect(spectator.component).toBeTruthy();
    });
});
