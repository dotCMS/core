import {bootstrap}    from '@angular/platform-browser-dynamic';
import {HTTP_PROVIDERS} from '@angular/http'
import {ApiRoot} from '../api/persistence/ApiRoot'
import {GoogleMapService} from '../api/maps/GoogleMapService';
import {UserModel} from '../api/auth/UserModel'
import {RuleService} from '../api/rule-engine/Rule'
import {ActionService} from '../api/rule-engine/Action'
import {ConditionGroupService} from '../api/rule-engine/ConditionGroup'
import {ConditionService} from '../api/rule-engine/Condition'
import {I18nService} from '../api/system/locale/I18n'
import {BundleService} from '../api/services/bundle-service';
import {provideRouter} from '@ngrx/router';
import {AppComponent} from './components/app';
import {RoutingService} from '../api/services/routing-service';
import {provide} from '@angular/core';


new RoutingService().getRoutes().subscribe(menu => {
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
    HTTP_PROVIDERS,
    provide('menuItems', {useValue: menu.menuItems}),
    provideRouter(menu.routes)
  ]);
});
