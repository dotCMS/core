import {bootstrap} from '@angular/platform-browser-dynamic';
import {HTTP_PROVIDERS} from '@angular/http';
import {ApiRoot} from '../api/persistence/ApiRoot';
import {GoogleMapService} from '../api/maps/GoogleMapService';
import {UserModel} from '../api/auth/UserModel';
import {RuleService} from '../api/rule-engine/Rule';
import {ActionService} from '../api/rule-engine/Action';
import {ConditionGroupService} from '../api/rule-engine/ConditionGroup';
import {ConditionService} from '../api/rule-engine/Condition';
import {I18nService} from '../api/system/locale/I18n';
import {BundleService} from '../api/services/bundle-service';
import {provideRouter} from '@ngrx/router';
import {AppComponent} from './components/app';
import {AppConfigurationService} from '../api/services/system/app-configuration-service';
import {provide} from '@angular/core';
import {disableDeprecatedForms, provideForms} from '@angular/forms';
import {MdIconRegistry} from '@angular2-material/icon/icon';
import {CoreWebService} from "../api/services/core-web-service";
import {LoginService} from "../api/services/login-service";
import {RoutingService} from "../api/services/routing-service";

import {ResetPasswordContainer} from "./components/common/login/reset-password-component/reset-password-container";
import {LoginContainer} from "./components/common/login/login-component/login-container";
import {ForgotPasswordContainer} from "./components/common/login/forgot-password-component/forgot-password-container";
import {LoginPageComponent} from "./components/common/login/login-page-component";
import {IframeLegacyComponent} from "./components/common/iframe-legacy/IframeLegacyComponent";
import {MainComponent} from "./components/common/main-component/main-component";
import { Routes } from '@ngrx/router';

new AppConfigurationService().getConfigProperties().subscribe(config => {

    let routes: Routes = [
        { path: 'dotCMS',
            component: MainComponent,
            children: [{
                component: IframeLegacyComponent,
                path: '/portlet/:id'
            }]
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
                    path: 'resetPassword/:userId',
                    component: ResetPasswordContainer
                }
            ]
        }
    ];

    bootstrap(AppComponent, [
        ApiRoot,
        GoogleMapService,
        I18nService,
        UserModel,
        RuleService,
        BundleService,
        ActionService,
        ConditionGroupService,
        ConditionService,
        AppConfigurationService,
        RoutingService,
        LoginService,
        CoreWebService,
        HTTP_PROVIDERS,
        MdIconRegistry,
        provide('dotcmsConfig', {useValue: config.dotcmsConfig}),
        provide('routes', {useValue: routes}),
        provideRouter(routes),
        // Form controls use the new @angular/forms package. To make migration easier, you can alternatively install
        // alpha.5-3, which is the same as alpha.6 without the new forms package.
        // Please see: https://angular.io/docs/ts/latest/guide/forms.html
        disableDeprecatedForms(),
        provideForms()
    ]);
});
