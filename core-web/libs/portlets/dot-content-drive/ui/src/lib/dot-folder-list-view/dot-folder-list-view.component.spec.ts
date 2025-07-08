import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotContentDriveItem } from '@dotcms/dotcms-models';

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

    it('should set items input property', () => {
        const mockItems: DotContentDriveItem[] = [
            { identifier: '123', title: 'Item 1' } as DotContentDriveItem,
            { identifier: '456', title: 'Item 2' } as DotContentDriveItem
        ];

        spectator.setInput('items', mockItems);

        expect(spectator.component.items()).toEqual(mockItems);
    });
});
