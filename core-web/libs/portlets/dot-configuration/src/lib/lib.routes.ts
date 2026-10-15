import { Route } from '@angular/router';

import { DotEnterpriseLicenseResolver } from '@dotcms/ui';

import { DotConfigurationShellComponent } from './dot-configuration-shell/dot-configuration-shell.component';
import { dotConfigurationUnsavedChangesGuard } from './guards/dot-configuration-unsaved-changes.guard';

export const dotConfigurationRoutes: Route[] = [
    {
        path: '',
        component: DotConfigurationShellComponent,
        canDeactivate: [dotConfigurationUnsavedChangesGuard],
        providers: [DotEnterpriseLicenseResolver],
        resolve: {
            // The navbar logo override is Enterprise-only: the backend drops it on Community.
            isEnterprise: DotEnterpriseLicenseResolver
        }
    }
];
