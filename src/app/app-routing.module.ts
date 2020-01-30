import { Routes, RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { MainCoreLegacyComponent } from '@components/main-core-legacy/main-core-legacy-component';
import { MainComponentLegacyComponent } from '@components/main-legacy/main-legacy.component';

import { DotLogOutContainerComponent } from '@components/login/dot-logout-container-component/dot-log-out-container';
import { IframePortletLegacyComponent } from '@components/_common/iframe/iframe-porlet-legacy/index';
import { AuthGuardService } from '@services/guards/auth-guard.service';
import { ContentletGuardService } from '@services/guards/contentlet-guard.service';
import { DefaultGuardService } from '@services/guards/default-guard.service';
import { MenuGuardService } from '@services/guards/menu-guard.service';
import { PublicAuthGuardService } from '@services/guards/public-auth-guard.service';
import { DotLoginPageComponent } from '@components/login/main/dot-login-page.component';
import { DotLoginPageResolver } from '@components/login/dot-login-page-resolver.service';

const PORTLETS_ANGULAR = [
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'content-types-angular',
        loadChildren: '@portlets/dot-content-types/dot-content-types.module#DotContentTypesModule'
    },
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'forms',
        loadChildren: '@portlets/dot-form-builder/dot-form-builder.module#DotFormBuilderModule',
        data: {
            filterBy: 'FORM'
        }
    },
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'rules',
        loadChildren: '@portlets/dot-rules/dot-rules.module#DotRulesModule'
    },
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'dot-browser',
        loadChildren: '@portlets/dot-site-browser/dot-site-browser.module#DotSiteBrowserModule'
    },
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'integration-services',
        loadChildren: '@portlets/dot-service-integration/dot-service-integration.module#DotServiceIntegrationModule'
    },
    {
        path: 'pl',
        loadChildren:
            '@components/_common/pattern-library/pattern-library.module#PatternLibraryModule'
    },
    {
        path: 'notLicensed',
        loadChildren: '@components/not-licensed/not-licensed.module#NotLicensedModule'
    },
    {
        path: 'edit-page',
        loadChildren: '@portlets/dot-edit-page/dot-edit-page.module#DotEditPageModule'
    },
    {
        canActivate: [MenuGuardService],
        path: '',
        children: []
    }
];
const PORTLETS_IFRAME = [
    {
        canActivateChild: [MenuGuardService],
        path: 'c',
        children: [
            {
                component: IframePortletLegacyComponent,
                path: ':id',
                children: [
                    {
                        loadChildren:
                            '@portlets/dot-porlet-detail/dot-portlet-detail.module#DotPortletDetailModule',
                        path: ':asset'
                    }
                ]
            },
            {
                path: '',
                children: []
            }
        ]
    },
    {
        canActivateChild: [ContentletGuardService],
        path: 'add',
        children: [
            {
                component: IframePortletLegacyComponent,
                path: ':id'
            },
            {
                path: '',
                children: []
            }
        ]
    }
];

const appRoutes: Routes = [
    {
        canActivate: [PublicAuthGuardService],
        path: 'public',
        component: DotLoginPageComponent,
        resolve: {
            loginFormInfo: DotLoginPageResolver
        },
        loadChildren: '@components/login/dot-login-page.module#DotLoginPageModule'
    },
    {
        canActivate: [AuthGuardService],
        children: [
            {
                path: 'rules',
                loadChildren: '@portlets/dot-rules/dot-rules.module#DotRulesModule',
                canActivate: [AuthGuardService]
            }
        ],
        component: MainCoreLegacyComponent,
        path: 'fromCore'
    },
    {
        component: DotLogOutContainerComponent,
        path: 'logout'
    },
    {
        canActivate: [AuthGuardService],
        component: MainComponentLegacyComponent,
        children: [...PORTLETS_IFRAME, ...PORTLETS_ANGULAR],
        path: ''
    },
    {
        canActivate: [DefaultGuardService],
        path: '**',
        children: []
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [
        RouterModule.forRoot(appRoutes, {
            useHash: true
        })
    ]
})
export class AppRoutingModule {}
