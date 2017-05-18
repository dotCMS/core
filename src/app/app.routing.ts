import {ForgotPasswordContainer} from './view/components/login/forgot-password-component/forgot-password-container';
import {IframeLegacyComponent} from './view/components/iframe-legacy/iframe-legacy-component';
import {LogOutContainer} from './view/components/login/login-component/log-out-container';
import {LoginContainer} from './view/components/login/login-component/login-container';
import {LoginPageComponent} from './view/components/login/login-page-component';
import {MainComponentLegacy} from './view/components/main-legacy/main-legacy-component';
import {MainCoreLegacyComponent} from './view/components/main-core-legacy/main-core-legacy-component';
import {ModuleWithProviders}  from '@angular/core';
import {NotLicensedComponent} from './view/components/not-licensed/not-licensed-component';
import {PatternLibrary} from './view/components/_common/pattern-library/pattern-library';
import {ResetPasswordContainer} from './view/components/login/reset-password-component/reset-password-container';
import {Routes, RouterModule, PreloadAllModules} from '@angular/router';
import {RoutingPrivateAuthService} from './api/services/routing-private-auth-service';
import {RoutingPublicAuthService} from './api/services/routing-public-auth-service';
import {RuleEngineContainer} from './portlets/rule-engine/rule-engine.container';
import {ContentTypesPortletComponent} from './portlets/content-types/content-types-component';
import { ContentTypesCreateEditPortletComponent } from './portlets/content-types-create-edit/content-types-create-edit-component';

let angularComponents: any[] = [];
angularComponents.push({component: RuleEngineContainer, id: 'rules'});
angularComponents.push({component: ContentTypesPortletComponent, id: 'content-types-angular'});
angularComponents.push({component: ContentTypesCreateEditPortletComponent, id: 'content-types-create-edit'});

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

let fromCoreChildren: any[] = [];
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
    {
        canActivate: [RoutingPrivateAuthService],
        component: ContentTypesCreateEditPortletComponent,
        path: 'content-types/create',
    }
];
angularComponents.forEach( component => {
    angularChildren.push({
        canActivate: [RoutingPrivateAuthService],
        component: component.component,
        path: component.path ? component.path : component.id
    });

    fromCoreChildren.push({
        canActivate: [RoutingPrivateAuthService],
        component: component.component,
        path: component.id
    });
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
        children: fromCoreChildren,
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

export const ROUTES: ModuleWithProviders = RouterModule.forRoot(appRoutes, { useHash: true, preloadingStrategy: PreloadAllModules });