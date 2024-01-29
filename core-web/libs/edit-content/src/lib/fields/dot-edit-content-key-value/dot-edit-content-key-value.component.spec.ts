import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { DotEditContentKeyValueComponent } from './dot-edit-content-key-value.component';

describe('DotEditContentKeyValueComponent', () => {
    let spectator: Spectator<DotEditContentKeyValueComponent>;
    const createComponent = createComponentFactory(DotEditContentKeyValueComponent);

    it('should create', () => {
        spectator = createComponent();

        expect(spectator.component).toBeTruthy();
    });
});
