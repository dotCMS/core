import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DotContentDriveItem } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-folder-list-view',
    standalone: true,
    imports: [],
    templateUrl: './dot-folder-list-view.component.html',
    styleUrl: './dot-folder-list-view.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFolderListViewComponent {
    items = input<DotContentDriveItem[]>([]);
}
