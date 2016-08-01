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

new AppConfigurationService().getConfigProperties().subscribe(config => {
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
        HTTP_PROVIDERS,
        MdIconRegistry,
        provide('menuItems', {useValue: config.menuItems}),
        provide('dotcmsConfig', {useValue: config.dotcmsConfig}),
        provideRouter(config.routes),
        // Form controls use the new @angular/forms package. To make migration easier, you can alternatively install
        // alpha.5-3, which is the same as alpha.6 without the new forms package.
        // Please see: https://angular.io/docs/ts/latest/guide/forms.html
        disableDeprecatedForms(),
        provideForms(),
    ]);
});
