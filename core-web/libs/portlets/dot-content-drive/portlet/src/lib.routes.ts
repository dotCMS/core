import { Route } from '@angular/router';

import { GlobalStore } from '@dotcms/store';

import { DotContentDriveShellComponent } from './lib/dot-content-drive-shell/dot-content-drive-shell.component';

export const DotContentDriveRoutes: Route[] = [
    {
        path: '',
        component: DotContentDriveShellComponent,
        providers: [GlobalStore]
    }
];
