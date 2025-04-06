import { Route } from '@angular/router';

import { DriveComponent } from './drive/drive.component';

export const driveRoutes: Route[] = [
    { path: '', component: DriveComponent },
    {
        path: ':asset',
        loadChildren: () =>
            // eslint-disable-next-line @nx/enforce-module-boundaries
            import('@portlets/dot-porlet-detail/dot-portlet-detail.module').then(
                (m) => m.DotPortletDetailModule
            )
    }
];
