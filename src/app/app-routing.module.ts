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
import { DotIframePortletLegacyResolver } from '@components/_common/iframe/service/dot-iframe-porlet-legacy-resolver.service';

const PORTLETS_ANGULAR = [
    {
        // canActivate: [MenuGuardService],
        // canActivateChild: [MenuGuardService],
        path: 'templates',
        loadChildren: () =>
            import('@portlets/dot-templates/dot-templates.module').then(
                (m) => m.DotTemplatesModule
            )
    },
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'content-types-angular',
        loadChildren: () =>
            import('@portlets/dot-content-types/dot-content-types.module').then(
                (m) => m.DotContentTypesModule
            )
    },
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'forms',
        loadChildren: () =>
            import('@portlets/dot-form-builder/dot-form-builder.module').then(
                (m) => m.DotFormBuilderModule
            ),
        data: {
            filterBy: 'FORM'
        }
    },
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'rules',
        loadChildren: () =>
            import('@portlets/dot-rules/dot-rules.module').then((m) => m.DotRulesModule)
    },
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'dot-browser',
        loadChildren: () =>
            import('@portlets/dot-site-browser/dot-site-browser.module').then(
                (m) => m.DotSiteBrowserModule
            )
    },
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'apps',
        loadChildren: () =>
            import('@portlets/dot-apps/dot-apps.module').then((m) => m.DotAppsModule)
    },
    {
        path: 'notLicensed',
        loadChildren: () =>
            import('@components/not-licensed/not-licensed.module').then((m) => m.NotLicensedModule)
    },
    {
        path: 'edit-page',
        loadChildren: () =>
            import('@portlets/dot-edit-page/dot-edit-page.module').then((m) => m.DotEditPageModule)
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
                        loadChildren: () =>
                            import('@portlets/dot-porlet-detail/dot-portlet-detail.module').then(
                                (m) => m.DotPortletDetailModule
                            ),
                        path: ':asset'
                    }
                ],
                resolve: {
                    canAccessPortlet: DotIframePortletLegacyResolver
                }
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
        loadChildren: () =>
            import('@components/login/dot-login-page.module').then((m) => m.DotLoginPageModule)
    },
    {
        canActivate: [AuthGuardService],
        children: [
            {
                path: 'rules',
                loadChildren: () =>
                    import('@portlets/dot-rules/dot-rules.module').then((m) => m.DotRulesModule),
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
