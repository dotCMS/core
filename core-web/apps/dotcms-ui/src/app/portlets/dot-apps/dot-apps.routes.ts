import { Routes } from '@angular/router';

import { DotAppsService } from '@dotcms/data-access';

import { DotAppsConfigurationComponent } from './components/dot-apps-configuration/dot-apps-configuration.component';
import { DotAppsConfigurationDetailComponent } from './components/dot-apps-configuration-detail/dot-apps-configuration-detail.component';
import { DotAppsListComponent } from './dot-apps-list/dot-apps-list.component';
import { DotAppsConfigurationDetailResolver } from './services/dot-apps-configuration-detail-resolver/dot-apps-configuration-detail-resolver.service';
import { DotAppsConfigurationResolver } from './services/dot-apps-configuration-resolver/dot-apps-configuration-resolver.service';
import { DotAppsListResolver } from './services/dot-apps-list-resolver/dot-apps-list-resolver.service';

export const dotAppsRoutes: Routes = [
    {
        component: DotAppsConfigurationDetailComponent,
        path: ':appKey/create/:id',
        resolve: {
            data: DotAppsConfigurationDetailResolver
        },
        providers: [DotAppsService, DotAppsConfigurationDetailResolver]
    },
    {
        component: DotAppsConfigurationDetailComponent,
        path: ':appKey/edit/:id',
        resolve: {
            data: DotAppsConfigurationDetailResolver
        },
        providers: [DotAppsService, DotAppsConfigurationDetailResolver]
    },
    {
        component: DotAppsConfigurationComponent,
        path: ':appKey',
        resolve: {
            data: DotAppsConfigurationResolver
        },
        providers: [DotAppsService, DotAppsConfigurationResolver]
    },
    {
        path: '',
        component: DotAppsListComponent,
        resolve: {
            dotAppsListResolverData: DotAppsListResolver
        },
        providers: [DotAppsService, DotAppsListResolver]
    }
];
