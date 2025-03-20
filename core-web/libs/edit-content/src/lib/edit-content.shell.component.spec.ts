import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Toast } from 'primeng/toast';

import { EditContentShellComponent } from './edit-content.shell.component';

describe('EditContentShellComponent', () => {
    let spectator: Spectator<EditContentShellComponent>;
    const createComponent = createComponentFactory(EditContentShellComponent);

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should have p-toast component', () => {
        expect(spectator.query(Toast)).toBeTruthy();
    });
});
