import { Route } from '@angular/router';

import { DotContentTypeService } from '@dotcms/data-access';

import { DotContentDriveShellComponent } from './dot-content-drive-shell/dot-content-drive-shell.component';

export const dotContentDriveRoutes: Route[] = [
    {
        path: '',
        component: DotContentDriveShellComponent,
        // DotContentDriveService is providedIn: 'root' (usable from dialog hosts / AssetPicker).
        providers: [DotContentTypeService],
        /**
         * Asked while a batch still has bytes in flight, and answered `false`, which **cancels**
         * the navigation.
         *
         * Deliberately not `CanDeactivateGuardService`, the guard UVE and Templates use. That one
         * refuses by filtering a shared subject, so the navigation is left *pending* rather than
         * cancelled, and whoever releases the lock later lets it complete — which for an upload
         * means the author is thrown out of the portlet minutes after they chose to stay. UVE wants
         * the resume because it force-saves first; there is nothing to save here.
         */
        canDeactivate: [(component: DotContentDriveShellComponent) => component.canLeaveRoute()]
    }
];
