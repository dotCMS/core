import { Routes } from '@angular/router';

import { DotAppsService } from '@dotcms/data-access';

import { dotAiConfigDetailResolver } from './components/dot-ai-config-detail/dot-ai-config-detail-resolver.service';
import { DotAiConfigDetailComponent } from './components/dot-ai-config-detail/dot-ai-config-detail.component';
import { DotAppsConfigurationComponent } from './components/dot-apps-configuration/dot-apps-configuration.component';
import { DotAppsConfigurationDetailComponent } from './components/dot-apps-configuration-detail/dot-apps-configuration-detail.component';
import { DotAppsListComponent } from './dot-apps-list/dot-apps-list.component';
import { dotAppsSamlRedirectGuard } from './guards/dot-apps-saml-redirect.guard';
import { DotAppsConfigurationDetailResolver } from './services/dot-apps-configuration-detail-resolver/dot-apps-configuration-detail-resolver.service';
import { DotAppsConfigurationResolver } from './services/dot-apps-configuration-resolver/dot-apps-configuration-resolver.service';
import { DotAppsListResolver } from './services/dot-apps-list-resolver/dot-apps-list-resolver.service';

export const dotAppsRoutes: Routes = [
    {
        component: DotAiConfigDetailComponent,
        path: 'dotAI/edit/:id',
        resolve: {
            data: dotAiConfigDetailResolver
        },
        providers: [DotAppsService]
    },
    {
        component: DotAiConfigDetailComponent,
        path: 'dotAI/create/:id',
        resolve: {
            data: dotAiConfigDetailResolver
        },
        providers: [DotAppsService]
    },
    {
        component: DotAppsConfigurationDetailComponent,
        path: ':appKey/create/:id',
        canActivate: [dotAppsSamlRedirectGuard],
        resolve: {
            data: DotAppsConfigurationDetailResolver
        },
        providers: [DotAppsService, DotAppsConfigurationDetailResolver]
    },
    {
        component: DotAppsConfigurationDetailComponent,
        path: ':appKey/edit/:id',
        canActivate: [dotAppsSamlRedirectGuard],
        resolve: {
            data: DotAppsConfigurationDetailResolver
        },
        providers: [DotAppsService, DotAppsConfigurationDetailResolver]
    },
    {
        component: DotAppsConfigurationComponent,
        path: ':appKey',
        canActivate: [dotAppsSamlRedirectGuard],
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
