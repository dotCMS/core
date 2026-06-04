import { Route } from '@angular/router';

import { DotEnterpriseLicenseResolver, DotPushPublishEnvironmentsResolver } from '@dotcms/ui';

import { DotPluginsShellComponent } from './dot-plugins-shell/dot-plugins-shell.component';

export const dotPluginsRoutes: Route[] = [
    {
        path: '',
        component: DotPluginsShellComponent,
        providers: [DotPushPublishEnvironmentsResolver, DotEnterpriseLicenseResolver],
        resolve: {
            pushPublishEnvironments: DotPushPublishEnvironmentsResolver,
            isEnterprise: DotEnterpriseLicenseResolver
        }
    }
];
