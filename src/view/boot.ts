
import {bootstrap}    from '@angular/platform-browser-dynamic';
import {HTTP_PROVIDERS} from '@angular/http'
import {ApiRoot} from '../api/persistence/ApiRoot'
import {GoogleMapService} from "../api/maps/GoogleMapService";
import {UserModel} from "../api/auth/UserModel"
import {RuleService} from "../api/rule-engine/Rule"
import {ActionService} from "../api/rule-engine/Action"
import {ConditionGroupService} from "../api/rule-engine/ConditionGroup"
import {ConditionService} from "../api/rule-engine/Condition"
import {I18nService} from "../api/system/locale/I18n"
import {RuleEngineContainer} from "./components/rule-engine/rule-engine.container";
import {BundleService} from "../api/services/bundle-service";

import { provideRouter } from '@ngrx/router';
import { Routes } from '@ngrx/router';

import {AppComponent} from "./components/app";
import {Routing} from "./components/Routing";

import {provide} from '@angular/core';
import {ANGULAR_PORTLET3} from "./components/ANGULAR_PORTLET3";
import {ANGULAR_PORTLET4} from "./components/ANGULAR_PORTLET4";


let routing = new Routing();

routing.getRoutes().then( menu => {
  console.log('MENU', menu);

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
