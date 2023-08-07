import { NgModule } from '@angular/core';
import { RouteReuseStrategy, RouterModule, Routes } from '@angular/router';

import { IframePortletLegacyComponent } from '@components/_common/iframe/iframe-porlet-legacy/index';
import { DotIframePortletLegacyResolver } from '@components/_common/iframe/service/dot-iframe-porlet-legacy-resolver.service';
import { DotLoginPageResolver } from '@components/login/dot-login-page-resolver.service';
import { DotLogOutContainerComponent } from '@components/login/dot-logout-container-component/dot-log-out-container';
import { DotLoginPageComponent } from '@components/login/main/dot-login-page.component';
import { MainCoreLegacyComponent } from '@components/main-core-legacy/main-core-legacy-component';
import { MainComponentLegacyComponent } from '@components/main-legacy/main-legacy.component';
import { DotCustomReuseStrategyService } from '@shared/dot-custom-reuse-strategy/dot-custom-reuse-strategy.service';

import { AuthGuardService } from './api/services/guards/auth-guard.service';
import { ContentletGuardService } from './api/services/guards/contentlet-guard.service';
import { DefaultGuardService } from './api/services/guards/default-guard.service';
import { MenuGuardService } from './api/services/guards/menu-guard.service';
import { PagesGuardService } from './api/services/guards/pages-guard.service';
import { PublicAuthGuardService } from './api/services/guards/public-auth-guard.service';

const PORTLETS_ANGULAR = [
    {
        path: 'containers',
        loadChildren: () =>
            import('@dotcms/app/portlets/dot-containers/dot-containers.module').then(
                (m) => m.DotContainersModule
            )
    },
    {
        path: 'categories',
        loadChildren: () =>
            import('@dotcms/app/portlets/dot-categories/dot-categories.module').then(
                (m) => m.DotCategoriesModule
            )
    },
    {
        path: 'templates',
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        loadChildren: () =>
            import('@portlets/dot-templates/dot-templates.module').then((m) => m.DotTemplatesModule)
    },
    {
        path: 'content-types-angular',
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        data: {
            reuseRoute: false
        },
        loadChildren: () =>
            import('@portlets/dot-content-types/dot-content-types.module').then(
                (m) => m.DotContentTypesModule
            )
    },
    {
        path: 'forms',
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        loadChildren: () =>
            import('@portlets/dot-form-builder/dot-form-builder.module').then(
                (m) => m.DotFormBuilderModule
            ),
        data: {
            filterBy: 'FORM'
        }
    },
    {
        path: 'rules',
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        loadChildren: () =>
            import('@portlets/dot-rules/dot-rules.module').then((m) => m.DotRulesModule)
    },
    {
        // canActivate: [MenuGuardService],
        // canActivateChild: [MenuGuardService],
        path: 'starter',
        loadChildren: () =>
            import('@portlets/dot-starter/dot-starter.module').then((m) => m.DotStarterModule)
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
        path: '',
        canActivate: [MenuGuardService],
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
                            import(
                                '@components/dot-contentlet-editor/dot-contentlet-editor.routing.module'
                            ).then((m) => m.DotContentletEditorRoutingModule),
                        path: 'new'
                    },
                    {
                        loadChildren: () =>
                            import('@portlets/dot-porlet-detail/dot-portlet-detail.module').then(
                                (m) => m.DotPortletDetailModule
                            ),
                        path: ':asset',
                        data: {
                            reuseRoute: false
                        }
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
        canActivate: [MenuGuardService, PagesGuardService],
        path: 'pages',
        loadChildren: () =>
            import('@portlets/dot-pages/dot-pages.module').then((m) => m.DotPagesModule)
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
        path: 'public',
        canActivate: [PublicAuthGuardService],
        component: DotLoginPageComponent,
        resolve: {
            loginFormInfo: DotLoginPageResolver
        },
        loadChildren: () =>
            import('@components/login/dot-login-page.module').then((m) => m.DotLoginPageModule)
    },
    {
        path: 'fromCore',
        canActivate: [AuthGuardService],
        children: [
            {
                path: 'rules',
                loadChildren: () =>
                    import('@portlets/dot-rules/dot-rules.module').then((m) => m.DotRulesModule),
                canActivate: [AuthGuardService]
            }
        ],
        component: MainCoreLegacyComponent
    },
    {
        path: 'logout',
        component: DotLogOutContainerComponent
    },
    {
        path: '',
        canActivate: [AuthGuardService],
        component: MainComponentLegacyComponent,
        children: [...PORTLETS_IFRAME, ...PORTLETS_ANGULAR]
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
            useHash: true,
            onSameUrlNavigation: 'reload'
        })
    ],
    providers: [{ provide: RouteReuseStrategy, useClass: DotCustomReuseStrategyService }]
})
export class AppRoutingModule {}
