import { Routes } from '@angular/router';

import { DotAppsService } from '@dotcms/data-access';

import { DotAppsConfigurationComponent } from './components/dot-apps-configuration/dot-apps-configuration.component';
import { DotAppsConfigurationDetailComponent } from './components/dot-apps-configuration-detail/dot-apps-configuration-detail.component';
import { DotAppsListComponent } from './dot-apps-list/dot-apps-list.component';
import { DotAppsShellComponent } from './dot-apps-shell/dot-apps-shell.component';
import { DotAppsConfigurationDetailResolver } from './services/dot-apps-configuration-detail-resolver/dot-apps-configuration-detail-resolver.service';
import { DotAppsConfigurationResolver } from './services/dot-apps-configuration-resolver/dot-apps-configuration-resolver.service';
import { DotAppsListResolver } from './services/dot-apps-list-resolver/dot-apps-list-resolver.service';

export const dotAppsRoutes: Routes = [
    {
        path: '',
        component: DotAppsShellComponent,
        providers: [
            DotAppsService
            // TODO: Add DotAppsStore here when ready
        ],
        children: [
            {
                path: ':appKey/create/:id',
                component: DotAppsConfigurationDetailComponent,
                resolve: {
                    data: DotAppsConfigurationDetailResolver
                },
                providers: [DotAppsConfigurationDetailResolver]
            },
            {
                path: ':appKey/edit/:id',
                component: DotAppsConfigurationDetailComponent,
                resolve: {
                    data: DotAppsConfigurationDetailResolver
                },
                providers: [DotAppsConfigurationDetailResolver]
            },
            {
                path: ':appKey',
                component: DotAppsConfigurationComponent,
                resolve: {
                    data: DotAppsConfigurationResolver
                },
                providers: [DotAppsConfigurationResolver]
            },
            {
                path: '',
                component: DotAppsListComponent,
                resolve: {
                    dotAppsListResolverData: DotAppsListResolver
                },
                providers: [DotAppsListResolver]
            }
        ]
    }
];
