import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { DotEditableTextComponent } from './dot-editable-text.component';

describe('DotEditableTextComponent', () => {
    let spectator: Spectator<DotEditableTextComponent>;

    const createComponent = createComponentFactory(DotEditableTextComponent);

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should exist', () => {
        expect(spectator.component).toBeTruthy();
    });
});
