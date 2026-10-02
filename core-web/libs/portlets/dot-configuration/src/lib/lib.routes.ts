import { Route } from '@angular/router';

import { DotEnterpriseLicenseResolver } from '@dotcms/ui';

import { DotConfigurationShellComponent } from './dot-configuration-shell/dot-configuration-shell.component';

export const dotConfigurationRoutes: Route[] = [
    {
        path: '',
        component: DotConfigurationShellComponent,
        providers: [DotEnterpriseLicenseResolver],
        resolve: {
            // The navbar logo override is Enterprise-only: the backend drops it on Community.
            isEnterprise: DotEnterpriseLicenseResolver
        }
    }
];
