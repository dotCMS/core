import { Route } from '@angular/router';

import { DotContentDriveService, DotContentTypeService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';

import { DotContentDriveShellComponent } from './lib/dot-content-drive-shell/dot-content-drive-shell.component';

export const DotContentDriveRoutes: Route[] = [
    {
        path: '',
        component: DotContentDriveShellComponent,
        providers: [GlobalStore, DotContentTypeService, DotContentDriveService]
    }
];
