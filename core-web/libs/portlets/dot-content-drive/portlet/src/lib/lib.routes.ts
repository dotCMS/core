import { Route } from '@angular/router';

import { DotContentTypeService } from '@dotcms/data-access';

import { DotContentDriveShellComponent } from './dot-content-drive-shell/dot-content-drive-shell.component';

export const dotContentDriveRoutes: Route[] = [
    {
        path: '',
        component: DotContentDriveShellComponent,
        // DotContentDriveService is providedIn: 'root' (usable from dialog hosts / AssetPicker).
        providers: [DotContentTypeService]
    }
];
