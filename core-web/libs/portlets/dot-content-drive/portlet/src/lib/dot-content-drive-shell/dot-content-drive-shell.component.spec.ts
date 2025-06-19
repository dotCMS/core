import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';

import { DotContentDriveShellComponent } from './dot-content-drive-shell.component';

describe('DotContentDriveShellComponent', () => {
    let spectator: Spectator<DotContentDriveShellComponent>;

    const createComponent = createComponentFactory({
        component: DotContentDriveShellComponent,
        imports: [],
        declarations: [],
        detectChanges: true
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('DOM', () => {
        it('should have a dot-folder-list-view', () => {
            const folderListView = spectator.query(DotFolderListViewComponent);

            expect(folderListView).toBeTruthy();
        });
    });

    describe('Interactions', () => {
        // Add tests for component interactions when implemented
    });
});
