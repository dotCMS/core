import {ForgotPasswordContainer} from './common/login/forgot-password-component/forgot-password-container';
import {IframeLegacyComponent} from './common/iframe-legacy/iframe-legacy-component';
import {LoginContainer} from './common/login/login-component/login-container';
import {LoginPageComponent} from './common/login/login-page-component';
import {MainComponent} from './common/main-component/main-component';
import {ModuleWithProviders}  from '@angular/core';
import {PatternLibrary} from './common/pattern-library/pattern-library';
import {ResetPasswordContainer} from './common/login/reset-password-component/reset-password-container';
import {Routes, RouterModule} from '@angular/router';
import {RoutingPublicAuthService} from '../../api/services/routing-public-auth-service';
import {RuleEngineContainer} from './rule-engine/rule-engine.container';
import {CONSTANTS} from '../constants';
import {MainCoreComponent} from './main-core-component/MainCoreComponent';
import {NotLicensedComponent} from './not-licensed-component/not-licensed-component';
import {LogOutContainer} from './common/login/login-component/log-out-container';
import {RoutingPrivateAuthService} from '../../api/services/routing-private-auth-service';

let angularComponents: any[] = [];
angularComponents.push({component: RuleEngineContainer, id: 'rules'});

let mainComponentChildren = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: (CONSTANTS.ENV && CONSTANTS.ENV === 'DEV') ? 'pl' : 'home',
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
        redirectTo: (CONSTANTS.ENV && CONSTANTS.ENV === 'DEV') ? 'c/pl' : 'c/home',
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
    }
];
angularComponents.forEach( component => {
    angularChildren.push({
        canActivate: [RoutingPrivateAuthService],
        component: component.component,
        path: component.id
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
        component: MainComponent,
        path: '',
    },
    {
        canActivate: [RoutingPrivateAuthService],
        children: mainComponentChildren,
        component: MainComponent,
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
        component: MainCoreComponent,
        path: 'fromCore'
    },
    {
        component: LogOutContainer,
        path: 'logout'
    }
];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);