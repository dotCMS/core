import { Routes } from '@angular/router';

import { DotAppsConfigurationResolver } from './dot-apps-configuration/dot-apps-configuration-resolver.service';
import { DotAppsConfigurationComponent } from './dot-apps-configuration/dot-apps-configuration.component';
import { DotAppsConfigurationDetailResolver } from './dot-apps-configuration-detail/dot-apps-configuration-detail-resolver.service';
import { DotAppsConfigurationDetailComponent } from './dot-apps-configuration-detail/dot-apps-configuration-detail.component';
import { DotAppsListResolver } from './dot-apps-list/dot-apps-list-resolver.service';
import { DotAppsListComponent } from './dot-apps-list/dot-apps-list.component';

import { DotAppsService } from '../../api/services/dot-apps/dot-apps.service';

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
