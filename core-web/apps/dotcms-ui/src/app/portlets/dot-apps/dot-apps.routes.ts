import { Routes } from '@angular/router';

import { DotAppsService } from '@dotcms/data-access';

import { DotAiConfigDetailLegacyComponent } from './components/dot-ai-config-detail/dot-ai-config-detail-legacy.component';
import { dotAiConfigDetailMatchGuard } from './components/dot-ai-config-detail/dot-ai-config-detail-match.guard';
import { dotAiConfigDetailResolver } from './components/dot-ai-config-detail/dot-ai-config-detail-resolver.service';
import { DotAiConfigDetailComponent } from './components/dot-ai-config-detail/dot-ai-config-detail.component';
import { DotAppsConfigurationComponent } from './components/dot-apps-configuration/dot-apps-configuration.component';
import { DotAppsConfigurationDetailComponent } from './components/dot-apps-configuration-detail/dot-apps-configuration-detail.component';
import { DotAppsListComponent } from './dot-apps-list/dot-apps-list.component';
import { DotAppsConfigurationDetailResolver } from './services/dot-apps-configuration-detail-resolver/dot-apps-configuration-detail-resolver.service';
import { DotAppsConfigurationResolver } from './services/dot-apps-configuration-resolver/dot-apps-configuration-resolver.service';
import { DotAppsListResolver } from './services/dot-apps-list-resolver/dot-apps-list-resolver.service';

export const dotAppsRoutes: Routes = [
    {
        component: DotAiConfigDetailComponent,
        path: 'dotAI/edit/:id',
        canMatch: [dotAiConfigDetailMatchGuard],
        resolve: {
            data: dotAiConfigDetailResolver
        },
        providers: [DotAppsService]
    },
    {
        // Fallback while FEATURE_FLAG_DOTAI_CONFIG_UI is off: the pre-#37048 screen, so existing
        // customers see no change from what they have today.
        component: DotAiConfigDetailLegacyComponent,
        path: 'dotAI/edit/:id',
        resolve: {
            data: dotAiConfigDetailResolver
        },
        providers: [DotAppsService]
    },
    {
        component: DotAiConfigDetailComponent,
        path: 'dotAI/create/:id',
        canMatch: [dotAiConfigDetailMatchGuard],
        resolve: {
            data: dotAiConfigDetailResolver
        },
        providers: [DotAppsService]
    },
    {
        component: DotAiConfigDetailLegacyComponent,
        path: 'dotAI/create/:id',
        resolve: {
            data: dotAiConfigDetailResolver
        },
        providers: [DotAppsService]
    },
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
