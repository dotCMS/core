import { Route } from '@angular/router';

import { DotEnterpriseLicenseResolver, DotPushPublishEnvironmentsResolver } from '@dotcms/ui';

import { DotLocaleVariablesComponent } from './dot-locale-variables/dot-locale-variables.component';
import { DotLocalesListComponent } from './dot-locales-list/dot-locales-list.component';
import { DotLocalesShellComponent } from './dot-locales-shell/dot-locales-shell.component';

export const DotLocalesRoutes: Route[] = [
    {
        path: '',
        component: DotLocalesShellComponent,
        children: [
            {
                path: '',
                component: DotLocalesListComponent,
                providers: [DotPushPublishEnvironmentsResolver, DotEnterpriseLicenseResolver],
                resolve: {
                    pushPublishEnvironments: DotPushPublishEnvironmentsResolver,
                    isEnterprise: DotEnterpriseLicenseResolver
                }
            },
            {
                path: 'variables',
                component: DotLocaleVariablesComponent
            }
        ]
    }
];
