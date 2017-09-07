import { RoutingPublicAuthService } from './api/services/routing-public-auth-service';
import { RoutingPrivateAuthService } from './api/services/routing-private-auth-service';
import { Routes, RouterModule, PreloadAllModules } from '@angular/router';
import { ResetPasswordContainer } from './view/components/login/reset-password-component/reset-password-container';
import { PatternLibrary } from './view/components/_common/pattern-library/pattern-library';
import { NotLicensedComponent } from './view/components/not-licensed/not-licensed-component';
import { NgModule } from '@angular/core';
import { MainCoreLegacyComponent } from './view/components/main-core-legacy/main-core-legacy-component';
import { MainComponentLegacy } from './view/components/main-legacy/main-legacy.component';
import { LoginPageComponent } from './view/components/login/login-page-component';
import { LoginContainer } from './view/components/login/login-component/login-container';
import { LogOutContainer } from './view/components/login/login-component/log-out-container';
import { IframeLegacyComponent } from './view/components/iframe-legacy/iframe-legacy-component';
import { ForgotPasswordContainer } from './view/components/login/forgot-password-component/forgot-password-container';
import { environment } from '../environments/environment';

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
        component: PatternLibrary,
        path: 'pl'
    },
    {
        component: NotLicensedComponent,
        path: 'notLicensed'
    },
    {
        canActivate: [RoutingPrivateAuthService],
        component: IframeLegacyComponent,
        path: ':id'
    }
];

const angularChildren: any[] = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: environment.production ? 'c/home' : 'c/pl'
    },
    {
        component: PatternLibrary,
        path: 'c/pl'
    },
    {
        component: NotLicensedComponent,
        path: 'c/notLicensed'
    },
    {
        canActivate: [RoutingPrivateAuthService],
        component: IframeLegacyComponent,
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
                component: ForgotPasswordContainer,
                path: 'forgotPassword'
            },
            {
                component: LoginContainer,
                path: 'login'
            },
            {
                component: ResetPasswordContainer,
                path: 'resetPassword/:token'
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
            // TODO: make sure we need this preloadingStrategy
            preloadingStrategy: PreloadAllModules,
            useHash: true
        })
    ]
})
export class AppRoutingModule {}
