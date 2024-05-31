import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { DotEditContentHostFolderFieldComponent } from './dot-edit-content-host-folder-field.component';

import { createFormGroupDirectiveMock } from '../../utils/mocks';

describe('DotEditContentHostFolderFieldComponent', () => {
    let spectator: Spectator<DotEditContentHostFolderFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentHostFolderFieldComponent,
        componentViewProviders: [
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock() }
        ],
        providers: [FormGroupDirective],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create the component', () => {
        expect(spectator.component).toBeTruthy();
    });
});
