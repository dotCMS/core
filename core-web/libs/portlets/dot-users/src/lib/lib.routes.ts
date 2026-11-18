import { Route } from '@angular/router';

import { DotEnterpriseLicenseResolver, DotPushPublishEnvironmentsResolver } from '@dotcms/ui';

import { DotUsersShellComponent } from './dot-users-shell/dot-users-shell.component';

export const dotUsersRoutes: Route[] = [
    {
        path: '',
        component: DotUsersShellComponent,
        providers: [DotPushPublishEnvironmentsResolver, DotEnterpriseLicenseResolver],
        resolve: {
            pushPublishEnvironments: DotPushPublishEnvironmentsResolver,
            isEnterprise: DotEnterpriseLicenseResolver
        }
    }
];
