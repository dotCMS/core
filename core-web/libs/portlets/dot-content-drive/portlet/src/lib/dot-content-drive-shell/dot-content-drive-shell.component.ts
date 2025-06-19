import { Component } from '@angular/core';

import { DotListViewComponent } from '@dotcms/portlets/content-drive/ui';

@Component({
    selector: 'dot-content-drive-shell',
    standalone: true,
    imports: [DotListViewComponent],
    templateUrl: './dot-content-drive-shell.component.html',
    styleUrl: './dot-content-drive-shell.component.css'
})
export class DotContentDriveShellComponent {}
