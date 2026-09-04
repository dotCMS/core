import { createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DialogService } from 'primeng/dynamicdialog';
import { Toast } from 'primeng/toast';

import { EditContentShellComponent } from './edit-content.shell.component';
import {
    AngularImageEditorLauncher,
    IMAGE_EDITOR_LAUNCHER
} from './fields/shared/image-editor-launcher';

describe('EditContentShellComponent', () => {
    let spectator: Spectator<EditContentShellComponent>;
    const createComponent = createComponentFactory(EditContentShellComponent);

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should have p-toast component', () => {
        expect(spectator.query(Toast)).toBeTruthy();
    });

    /**
     * Regression lock for #37398. `DotFileFieldComponent` injects `IMAGE_EDITOR_LAUNCHER` as
     * `{ optional: true }`, so a host losing this provider does not fail loudly — Image/File
     * fields just quietly open the legacy Dojo editor instead of the new one. Asserting it per
     * host means such a regression breaks a test rather than reaching users.
     */
    it('should provide IMAGE_EDITOR_LAUNCHER so fields use the new editor, not legacy Dojo', () => {
        const launcher = spectator.inject(IMAGE_EDITOR_LAUNCHER, true);

        expect(launcher).toBeDefined();
        expect(launcher).toBeInstanceOf(AngularImageEditorLauncher);
    });

    it('should provide the DialogService the launcher opens the editor through', () => {
        expect(spectator.inject(DialogService, true)).toBeTruthy();
    });
});
