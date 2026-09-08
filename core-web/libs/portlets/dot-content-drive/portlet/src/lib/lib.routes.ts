import { Route } from '@angular/router';

import { CanDeactivateGuardService, DotContentTypeService } from '@dotcms/data-access';

import { DotContentDriveShellComponent } from './dot-content-drive-shell/dot-content-drive-shell.component';

export const dotContentDriveRoutes: Route[] = [
    {
        path: '',
        component: DotContentDriveShellComponent,
        // DotContentDriveService is providedIn: 'root' (usable from dialog hosts / AssetPicker).
        providers: [DotContentTypeService, CanDeactivateGuardService],
        // Holds the route while a batch still has bytes in flight. The same guard UVE uses, so the
        // shell only has to say when it is unsafe to leave, not how to stop a navigation.
        canDeactivate: [CanDeactivateGuardService]
    }
];
