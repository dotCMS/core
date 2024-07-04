import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { EditableTextComponent } from './editable-text.component';

describe('EditableTextComponent', () => {
    let spectator: Spectator<EditableTextComponent>;

    const createComponent = createComponentFactory(EditableTextComponent);

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should exist', () => {
        expect(spectator.component).toBeTruthy();
    });
});
