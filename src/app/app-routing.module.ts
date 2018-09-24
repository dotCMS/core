import { Routes, RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { MainCoreLegacyComponent } from '@components/main-core-legacy/main-core-legacy-component';
import { MainComponentLegacyComponent } from '@components/main-legacy/main-legacy.component';
import { LoginPageComponent } from '@components/login/login-page-component';
import { LogOutContainerComponent } from '@components/login/login-component/log-out-container';
import { IframePortletLegacyComponent } from '@components/_common/iframe/iframe-porlet-legacy/index';
import { AuthGuardService } from '@services/guards/auth-guard.service';
import { ContentletGuardService } from '@services/guards/contentlet-guard.service';
import { DefaultGuardService } from '@services/guards/default-guard.service';
import { MenuGuardService } from '@services/guards/menu-guard.service';
import { PublicAuthGuardService } from '@services/guards/public-auth-guard.service';

const AUTH_MODULES: Routes = [
    {
        path: 'forgotPassword',
        loadChildren: '@components/login/forgot-password-component/forgot-password.module#ForgotPasswordModule'
    },
    {
        path: 'login',
        loadChildren: '@components/login/login-component/login.module#LoginModule'
    },
    {
        path: 'resetPassword/:token',
        loadChildren: '@components/login/reset-password-component/reset-password.module#ResetPasswordModule'
    },
    {
        path: '',
        children: []
    }
];

const PORTLETS_ANGULAR = [
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'content-types-angular',
        loadChildren: '@portlets/content-types/content-types.module#ContentTypesModule'
    },
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'rules',
        loadChildren: '@portlets/rule-engine/rule-engine.module#RuleEngineModule'
    },
    {
        canActivate: [MenuGuardService],
        canActivateChild: [MenuGuardService],
        path: 'dot-browser',
        loadChildren: '@portlets/dot-browser/dot-browser.module#DotBrowserModule'
    },
    {
        path: 'pl',
        loadChildren: '@components/_common/pattern-library/pattern-library.module#PatternLibraryModule'
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
                        loadChildren: '@portlets/dot-porlet-detail/dot-portlet-detail.module#DotPortletDetailModule',
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
        children: AUTH_MODULES,
        component: LoginPageComponent,
        path: 'public'
    },
    {
        canActivate: [AuthGuardService],
        children: [
            {
                path: 'rules',
                loadChildren: '@portlets/rule-engine/rule-engine.module#RuleEngineModule',
                canActivate: [AuthGuardService]
            }
        ],
        component: MainCoreLegacyComponent,
        path: 'fromCore'
    },
    {
        component: LogOutContainerComponent,
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
