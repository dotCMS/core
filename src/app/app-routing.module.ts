import { ForgotPasswordContainer } from './view/components/login/forgot-password-component/forgot-password-container';
import { IframeLegacyComponent } from './view/components/iframe-legacy/iframe-legacy-component';
import { LogOutContainer } from './view/components/login/login-component/log-out-container';
import { LoginContainer } from './view/components/login/login-component/login-container';
import { LoginPageComponent } from './view/components/login/login-page-component';
import { MainComponentLegacy } from './view/components/main-legacy/main-legacy.component';
import { MainCoreLegacyComponent } from './view/components/main-core-legacy/main-core-legacy-component';
import { ModuleWithProviders}  from '@angular/core';
import { NgModule } from '@angular/core';
import { NotLicensedComponent } from './view/components/not-licensed/not-licensed-component';
import { PatternLibrary } from './view/components/_common/pattern-library/pattern-library';
import { ResetPasswordContainer } from './view/components/login/reset-password-component/reset-password-container';
import { Routes, RouterModule, PreloadAllModules } from '@angular/router';
import { RoutingPrivateAuthService } from './api/services/routing-private-auth-service';
import { RoutingPublicAuthService } from './api/services/routing-public-auth-service';
import { RuleEngineContainer } from './portlets/rule-engine/rule-engine.container';
import {DotBrowserComponent} from './portlets/dot-browser/dot-browser-component';

let angularComponents: any[] = [
    {
        component: RuleEngineContainer,
        id: 'rules'
    },
    {
        component: DotBrowserComponent,
        id: 'dot-browser'
    },
    {
        id: 'content-types-angular',
        loadChildren: './portlets/content-types/content-types.module#ContentTypesModule',
    },
];

let mainComponentChildren = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: (process.env.ENV && process.env.ENV === process.env.DEV_MODE) ? 'pl' : 'home',
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

let angularChildren: any[] = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: (process.env.ENV && process.env.ENV === 'DEV') ? 'c/pl' : 'c/home',
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
angularComponents.forEach(item => {
    let route: any = {
        canActivate: [RoutingPrivateAuthService],
        path: item.path ? item.path : item.id
    };

    if (item.loadChildren) {
        route.loadChildren = item.loadChildren;
    } else if (item.component) {
        route.component = item.component;
    }

    angularChildren.push(route);
});

const appRoutes: Routes = [
    {
        canActivate: [RoutingPrivateAuthService],
        children: angularChildren,
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
                canActivate: [RoutingPrivateAuthService],
                component: RuleEngineContainer,
                path: 'rules'
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
