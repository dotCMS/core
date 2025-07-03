import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotFolderListViewComponent } from './dot-folder-list-view.component';

describe('DotFolderListViewComponent', () => {
    let spectator: Spectator<DotFolderListViewComponent>;

    const createComponent = createComponentFactory({
        component: DotFolderListViewComponent,
        imports: [],
        declarations: [],
        detectChanges: true
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('DOM', () => {
        it('should have a placeholder message', () => {
            const placeholderMessage = spectator.query('p');

            expect(placeholderMessage).toBeTruthy();
            expect(placeholderMessage?.textContent).toContain('dot-folder-list-view works');
        });
    });
});
