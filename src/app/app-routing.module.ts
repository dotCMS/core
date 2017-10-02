import { RoutingPublicAuthService } from './api/services/routing-public-auth.service';
import { RoutingPrivateAuthService } from './api/services/routing-private-auth.service';
import { Routes, RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { MainCoreLegacyComponent } from './view/components/main-core-legacy/main-core-legacy-component';
import { MainComponentLegacy } from './view/components/main-legacy/main-legacy.component';
import { LoginPageComponent } from './view/components/login/login-page-component';
import { LogOutContainer } from './view/components/login/login-component/log-out-container';
import { IframePortletLegacyComponent } from './view/components/_common/iframe/iframe-porlet-legacy/index';
import { RoutingContentletAuthService } from './api/services/routing-contentlet-auth.service';

const PORTLETS_ANGULAR: Routes = [
    {
        canActivateChild: [RoutingPrivateAuthService],
        path: 'content-types-angular',
        loadChildren: 'app/portlets/content-types/content-types.module#ContentTypesModule'
    },
    {
        canActivateChild: [RoutingPrivateAuthService],
        path: 'rules',
        loadChildren: 'app/portlets/rule-engine/rule-engine.module#RuleEngineModule'
    },
    {
        canActivateChild: [RoutingPrivateAuthService],
        path: 'dot-browser',
        loadChildren: 'app/portlets/dot-browser/dot-browser.module#DotBrowserModule'
    },
    {
        canActivateChild: [RoutingPrivateAuthService],
        path: 'pl',
        loadChildren:
            'app/view/components/_common/pattern-library/pattern-library.module#PatternLibraryModule'
    },
    {
        canActivateChild: [RoutingPrivateAuthService],
        path: 'notLicensed',
        loadChildren:
            'app/view/components/not-licensed/not-licensed.module#NotLicensedModule'
    }
];

const PORTLETS_IFRAME: Routes = [
    {
        canActivateChild: [RoutingContentletAuthService],
        path: '',
        children: [
            {
                component: IframePortletLegacyComponent,
                path: 'add/:id'
            }
        ]
    },
    {
        canActivateChild: [RoutingPrivateAuthService],
        path: '',
        children: [
            {
                component: IframePortletLegacyComponent,
                path: 'c/:id'
            }
        ]
    }
];

const AUTH_MODULES: Routes = [
    {
        path: 'forgotPassword',
        loadChildren:
            'app/view/components/login/forgot-password-component/forgot-password.module#ForgotPasswordModule'
    },
    {
        path: 'login',
        loadChildren: 'app/view/components/login/login-component/login.module#LoginModule'
    },
    {
        path: 'resetPassword/:token',
        loadChildren:
            'app/view/components/login/reset-password-component/reset-password.module#ResetPasswordModule'
    }
];

const appRoutes: Routes = [
    {
        canActivate: [RoutingPrivateAuthService],
        children: [...PORTLETS_ANGULAR, ...PORTLETS_IFRAME],
        component: MainComponentLegacy,
        path: ''
    },
    {
        canActivate: [RoutingPublicAuthService],
        children: AUTH_MODULES,
        component: LoginPageComponent,
        path: 'public'
    },
    {
        canActivate: [RoutingPrivateAuthService],
        children: [
            {
                path: 'rules',
                loadChildren: 'app/portlets/rule-engine/rule-engine.module#RuleEngineModule',
                canActivate: [RoutingPrivateAuthService]
            }
        ],
        component: MainCoreLegacyComponent,
        path: 'fromCore'
    },
    {
        component: LogOutContainer,
        path: 'logout'
    },
    {
        canActivate: [RoutingPublicAuthService],
        path: '**',
        pathMatch: 'full',
        redirectTo: '/public/login'
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
