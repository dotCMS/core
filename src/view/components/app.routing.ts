import {AppComponent} from './app';
import {ForgotPasswordContainer} from './common/login/forgot-password-component/forgot-password-container';
import {IframeLegacyComponent} from './common/iframe-legacy/Iframe-legacy-component';
import {LoginContainer} from './common/login/login-component/login-container';
import {LoginPageComponent} from './common/login/login-page-component';
import {MainComponent} from './common/main-component/main-component';
import {ModuleWithProviders}  from '@angular/core';
import {PatternLibrary} from './common/pattern-library/pattern-library';
import {ResetPasswordContainer} from './common/login/reset-password-component/reset-password-container';
import {Routes, RouterModule} from '@angular/router';
import {RoutingRootAuthService} from '../../api/services/routing-root-auth-service';
import {RoutingPublicAuthService} from '../../api/services/routing-public-auth-service';
import {RoutingPrivateAuthService} from '../../api/services/routing-private-auth-service';
import {RuleEngineContainer} from './rule-engine/rule-engine.container';
import {CONSTANT} from '../constant';
import {MainCoreComponent} from './main-core-component/MainCoreComponent';

let angularComponents: any[] = [];
angularComponents.push({component: RuleEngineContainer, id: 'RULES_ENGINE_PORTLET'});

let mainComponentChildren = [
                                {
                                    path: '',
                                    pathMatch: 'full',
                                    redirectTo: (CONSTANT.ENV && CONSTANT.ENV === 'DEV') ? 'pl' : 'portlet/EXT_21',
                                },
                                {
                                    component: PatternLibrary,
                                    path: 'pl'
                                },
                                {
                                    component: IframeLegacyComponent,
                                    path: 'portlet/:id'
                                },
                                {
                                    component: RuleEngineContainer,
                                    path: 'html/ng/p/RULES_ENGINE_PORTLET',
                                }
                            ];

let fromCoreChildren: any[] = [];

angularComponents.forEach( component => {
    mainComponentChildren.push({
        component: component.component,
        path: `html/ng/p/${component.id}`
    });

    fromCoreChildren.push({
        component: component.component,
        path: component.id
    });
});

const appRoutes: Routes = [
    {
        canActivate: [RoutingRootAuthService],
        component: AppComponent,
        path: ''
    },
    {
        canActivate: [RoutingPrivateAuthService],
        children: mainComponentChildren,
        component: MainComponent,
        path: 'dotCMS',
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
    }
];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);