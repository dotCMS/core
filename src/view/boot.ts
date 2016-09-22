import {Routes} from '@ngrx/router';
import {ActionService} from '../api/rule-engine/Action';
import {ApiRoot} from '../api/persistence/ApiRoot';
import {AppComponent} from './components/app';
import {AppConfigurationService} from '../api/services/system/app-configuration-service';
import {bootstrap} from '@angular/platform-browser-dynamic';
import {BundleService} from '../api/services/bundle-service';
import {ConditionGroupService} from '../api/rule-engine/ConditionGroup';
import {ConditionService} from '../api/rule-engine/Condition';
import {CoreWebService} from '../api/services/core-web-service';
import {disableDeprecatedForms, provideForms} from '@angular/forms';
import {ForgotPasswordContainer} from './components/common/login/forgot-password-component/forgot-password-container';
import {FormatDate} from '../api/services/format-date-service';
import {GoogleMapService} from '../api/maps/GoogleMapService';
import {HTTP_PROVIDERS} from '@angular/http';
import {I18nService} from '../api/system/locale/I18n';
import {IframeLegacyComponent} from './components/common/iframe-legacy/iframe-legacy-component';
import {LoginContainer} from './components/common/login/login-component/login-container';
import {LoginPageComponent} from './components/common/login/login-page-component';
import {LoginService} from '../api/services/login-service';
import {MainComponent} from './components/common/main-component/main-component';
import {MdIconRegistry} from '@angular2-material/icon/icon';
import {provideRouter} from '@ngrx/router';
import {provide} from '@angular/core';
import {ResetPasswordContainer} from './components/common/login/reset-password-component/reset-password-container';
import {RoutingService} from '../api/services/routing-service';
import {RuleService} from '../api/rule-engine/Rule';
import {SiteService} from '../api/services/site-service';
import {DotcmsEventsService} from '../api/services/dotcms-events-service';
import {UserModel} from '../api/auth/UserModel';

new AppConfigurationService().getConfigProperties().subscribe(config => {
    let routes: Routes = [
        {
            path: 'dotCMS',
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
                    path: 'resetPassword',
                    component: ResetPasswordContainer
                }
            ]
        }
    ];

    bootstrap(AppComponent, [
        FormatDate,
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
        DotcmsEventsService,
        SiteService,
        CoreWebService,
        HTTP_PROVIDERS,
        MdIconRegistry,
        provide('dotcmsConfig', {useValue: config.dotcmsConfig}),
        provide('routes', {useValue: routes}),
        provide('user', {useValue: config.dotcmsConfig.configParams.user}),
        provideRouter(routes),
        // Form controls use the new @angular/forms package. To make migration easier, you can alternatively install
        // alpha.5-3, which is the same as alpha.6 without the new forms package.
        // Please see: https://angular.io/docs/ts/latest/guide/forms.html
        disableDeprecatedForms(),
        provideForms()
    ]);
});
