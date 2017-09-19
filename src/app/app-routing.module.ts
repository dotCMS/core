import { RoutingPublicAuthService } from './api/services/routing-public-auth-service';
import { RoutingPrivateAuthService } from './api/services/routing-private-auth-service';
import { Routes, RouterModule, PreloadAllModules } from '@angular/router';
import { NgModule } from '@angular/core';
import { MainCoreLegacyComponent } from './view/components/main-core-legacy/main-core-legacy-component';
import { MainComponentLegacy } from './view/components/main-legacy/main-legacy.component';
import { LoginPageComponent } from './view/components/login/login-page-component';
import { LogOutContainer } from './view/components/login/login-component/log-out-container';
import { environment } from '../environments/environment';
import { IFramePortletLegacyComponent } from './view/components/_common/iframe/iframe-porlet-legacy/index';

const angularComponents: any[] = [
    {
        path: 'content-types-angular',
        loadChildren: 'app/portlets/content-types/content-types.module#ContentTypesModule'
    },
    {
        path: 'rules',
        loadChildren: 'app/portlets/rule-engine/rule-engine.module#RuleEngineModule',
    },
    {
        path: 'dot-browser',
        loadChildren: 'app/portlets/dot-browser/dot-browser.module#DotBrowserModule'
    },
];

const mainComponentChildren = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: environment.production ? 'home' : 'pl'
    },
    {
        path: 'pl',
        loadChildren: 'app/view/components/_common/pattern-library/pattern-library.module#PatternLibraryModule'
    },
    {
        path: 'notLicensed',
        loadChildren: 'app/view/components/not-licensed/not-licensed.module#NotLicensedModule'
    },
    {
        canActivate: [RoutingPrivateAuthService],
        component: IFramePortletLegacyComponent,
        path: ':id'
    }
];

/*TODO: Should we remove 'angularChildren' since is not used anywhere ? */
const angularChildren: any[] = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: environment.production ? 'c/home' : 'c/pl'
    },
    {
        path: 'pl',
        loadChildren: 'app/view/components/_common/pattern-library/pattern-library.module#PatternLibraryModule'
    },
    {
        path: 'notLicensed',
        loadChildren: 'app/view/components/not-licensed/not-licensed.module#NotLicensedModule'
    },
    {
        canActivate: [RoutingPrivateAuthService],
        component: IFramePortletLegacyComponent,
        path: 'c/:id',
    },
];

const appRoutes: Routes = [
    {
        canActivate: [RoutingPrivateAuthService],
        children: angularComponents,
        component: MainComponentLegacy,
        path: '',
    },
    {
        canActivate: [RoutingPrivateAuthService],
        children: mainComponentChildren,
        component: MainComponentLegacy,
        path: 'c',
    },
    {
        canActivate: [RoutingPublicAuthService],
        children: [
            {
                path: 'forgotPassword',
                loadChildren: 'app/view/components/login/forgot-password-component/forgot-password.module#ForgotPasswordModule'
            },
            {
                path: 'login',
                loadChildren: 'app/view/components/login/login-component/login.module#LoginModule'
            },
            {
                path: 'resetPassword/:token',
                loadChildren: 'app/view/components/login/reset-password-component/reset-password.module#ResetPasswordModule'
            }
        ],
        component: LoginPageComponent,
        path: 'public',
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
        redirectTo: '/public/login',
    }
];

@NgModule({
    exports: [
        RouterModule
    ],
    imports: [
        RouterModule.forRoot(appRoutes, {
            useHash: true
        })
    ]
})
export class AppRoutingModule {}
