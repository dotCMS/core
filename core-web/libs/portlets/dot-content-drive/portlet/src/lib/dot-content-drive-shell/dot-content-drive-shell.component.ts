import { Component } from '@angular/core';

import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';

@Component({
    selector: 'dot-content-drive-shell',
    standalone: true,
    imports: [DotFolderListViewComponent],
    templateUrl: './dot-content-drive-shell.component.html',
    styleUrl: './dot-content-drive-shell.component.css'
})
export class DotContentDriveShellComponent {}
