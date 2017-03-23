// Angular 2
import {
  enableDebugTools,
  disableDebugTools
} from '@angular/platform-browser';
import {
  ApplicationRef,
  enableProdMode
} from '@angular/core';

import {StringUtils} from './api/util/string.utils';
import {Config} from './api/util/config';
import {LoggerService} from './api/services/logger.service';
import {CoreWebService} from './api/services/core-web-service';
import {NotLicensedService} from './api/services/not-licensed-service';
import {AccountService} from './api/services/account-service';
import {ApiRoot} from './api/persistence/ApiRoot';
//import {BundleService} from './api/services/bundle-service';
import {DotcmsConfig} from './api/services/system/dotcms-config';
import {DotcmsEventsService} from './api/services/dotcms-events-service';
import {DotRouterService} from './api/services/dot-router-service';
import {FormatDateService} from './api/services/format-date-service';
import {LoginService} from './api/services/login-service';
import {MessageService} from './api/services/messages-service';
import {NotificationsService} from './api/services/notifications-service';
import {RoutingPublicAuthService} from './api/services/routing-public-auth-service';
import {RoutingPrivateAuthService} from './api/services/routing-private-auth-service';
import {RoutingService} from './api/services/routing-service';
import {SiteService} from './api/services/site-service';
import {StringFormat} from './api/util/stringFormat';
import {UserModel} from './api/auth/UserModel';
import {IframeOverlayService} from './api/services/iframe-overlay-service';
import {SocketFactory} from './api/services/protocol/socket-factory';
/*
import {ActionService} from './api/rule-engine/Action';
import {ConditionGroupService} from './api/rule-engine/ConditionGroup';
import {ConditionService} from './api/rule-engine/Condition';
import {GoogleMapService} from './api/maps/GoogleMapService';
import {I18nService} from './api/system/locale/I18n';
import {RuleService} from './api/rule-engine/Rule';
*/
import {Logger} from 'angular2-logger/core';

// ROUTING
import {LocationStrategy, HashLocationStrategy} from '@angular/common';

// Environment Providers
const RULES_ENGINE_SERVICES = [
  /*ActionService,
  ConditionGroupService,
  ConditionService,
  GoogleMapService,
  I18nService,
  RuleService,*/
];

let PROVIDERS: any[] = [
  // common env directives
  StringUtils,
  Config,
  Logger,
  LoggerService,
  CoreWebService,
  NotLicensedService,
  AccountService,
  ApiRoot,
  BundleService,
  DotcmsConfig,
  DotcmsEventsService,
  DotRouterService,
  FormatDateService,
  LoginService,
  MessageService,
  NotificationsService,
  RoutingPublicAuthService,
  RoutingPrivateAuthService,
  RoutingService,
  SiteService,
  StringFormat,
  UserModel,
  IframeOverlayService,
  ...RULES_ENGINE_SERVICES,
  {provide: LocationStrategy, useClass: HashLocationStrategy},
  SocketFactory
];

// Angular debug tools in the dev console
// https://github.com/angular/angular/blob/86405345b781a9dc2438c0fbe3e9409245647019/TOOLS_JS.md
let _decorateModuleRef = <T>(value: T): T => { return value; };

if ('production' === ENV) {
  enableProdMode();

  // Production
  _decorateModuleRef = (modRef: any) => {
    disableDebugTools();

    return modRef;
  };

  PROVIDERS = [
    ...PROVIDERS,
    // custom providers in production
  ];

} else {

  _decorateModuleRef = (modRef: any) => {
    const appRef = modRef.injector.get(ApplicationRef);
    const cmpRef = appRef.components[0];

    let _ng = (<any> window).ng;
    enableDebugTools(cmpRef);
    (<any> window).ng.probe = _ng.probe;
    (<any> window).ng.coreTokens = _ng.coreTokens;
    return modRef;
  };

  // Development
  PROVIDERS = [
    ...PROVIDERS,
    // custom providers in development
  ];

}

export const decorateModuleRef = _decorateModuleRef;

export const ENV_PROVIDERS = [
  ...PROVIDERS
];
