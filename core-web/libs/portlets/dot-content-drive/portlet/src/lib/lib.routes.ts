import { Route } from '@angular/router';

import { DotContentDriveService, DotContentTypeService } from '@dotcms/data-access';

import { DotContentDriveShellComponent } from './dot-content-drive-shell/dot-content-drive-shell.component';

export const dotContentDriveRoutes: Route[] = [
    {
        path: '',
        component: DotContentDriveShellComponent,
        providers: [DotContentTypeService, DotContentDriveService]
    }
];
