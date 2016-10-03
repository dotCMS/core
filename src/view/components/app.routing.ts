import {Component} from '@angular/core';

@Component({
    directives: [],
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [],
    template: '<h1>Fake rules engine</h1>'
})

export class FakeRulesEngine {
    constructor() {

    }
}

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
import {RoutingAuthService} from '../../api/services/routing-auth-service';
import {RuleEngineContainer} from './rule-engine/rule-engine.container';

const appRoutes: Routes = [
    {
        path: 'build',
        component: AppComponent
    },
    {
        path: 'dotCMS',
        component: MainComponent,
        children: [
            {
                path: '',
                redirectTo: 'pl',
                pathMatch: 'full'

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
                component: FakeRulesEngine,
                path: 'html/ng/p/RULES_ENGINE_PORTLET',
                canActivate: [RoutingAuthService]
            }
        ]
    },
    {
        path: 'public',
        component: LoginPageComponent,
        children: [
            {
                path: 'forgotPassword',
                component: ForgotPasswordContainer
            },
            {
                path: 'login',
                component: LoginContainer
            },
            {
                path: 'resetPassword',
                component: ResetPasswordContainer
            }
        ]
    }
];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);