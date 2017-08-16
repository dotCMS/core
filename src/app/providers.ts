// Angular 2
import { enableDebugTools, disableDebugTools } from '@angular/platform-browser';
import { ApplicationRef, enableProdMode } from '@angular/core';

import { AccountService } from './api/services/account-service';
import { ApiRoot } from './api/persistence/ApiRoot';
import { BrowserUtil } from './api/util/browser-util';
import { BundleService } from './api/services/bundle-service';
import { ColorUtil } from './api/util/ColorUtil';
import { Config } from './api/util/config';
import { ConfirmationService } from 'primeng/primeng';
import { ContentTypesInfoService } from './api/services/content-types-info';
import { CoreWebService } from './api/services/core-web-service';
import { CrudService } from './api/services/crud/crud.service';
import { DotRouterService } from './api/services/dot-router-service';
import { DotcmsConfig } from './api/services/system/dotcms-config';
import { DotcmsEventsService } from './api/services/dotcms-events-service';
import { FormatDateService } from './api/services/format-date-service';
import { GravatarService } from './api/services/gravatar-service';
import { IframeOverlayService } from './api/services/iframe-overlay-service';
import { Logger } from 'angular2-logger/core';
import { LoggerService } from './api/services/logger.service';
import { LoginService } from './api/services/login-service';
import { MessageService } from './api/services/messages-service';
import { NotLicensedService } from './api/services/not-licensed-service';
import { NotificationsService } from './api/services/notifications-service';
import { RoutingPrivateAuthService } from './api/services/routing-private-auth-service';
import { RoutingPublicAuthService } from './api/services/routing-public-auth-service';
import { RoutingService } from './api/services/routing-service';

import { SiteService } from './api/services/site-service';
import { SocketFactory } from './api/services/protocol/socket-factory';
import { StringFormat } from './api/util/stringFormat';
import { StringUtils } from './api/util/string.utils';
import { UserModel } from './api/auth/UserModel';

// ROUTING
import { LocationStrategy, HashLocationStrategy } from '@angular/common';
import { PaginatorService } from './api/services/paginator';
import { environment } from '../environments/environment';



let PROVIDERS: any[] = [
    // common env directives
    AccountService,
    ApiRoot,
    BrowserUtil,
    BundleService,
    ColorUtil,
    Config,
    CoreWebService,
    ConfirmationService,
    ContentTypesInfoService,
    CrudService,
    DotRouterService,
    DotcmsConfig,
    DotcmsEventsService,
    FormatDateService,
    GravatarService,
    IframeOverlayService,
    Logger,
    LoggerService,
    LoginService,
    MessageService,
    NotLicensedService,
    NotificationsService,
    PaginatorService,
    RoutingPrivateAuthService,
    RoutingPublicAuthService,
    RoutingService,
    SiteService,
    SocketFactory,
    StringFormat,
    StringUtils,
    UserModel,
    ColorUtil,
];

export const ENV_PROVIDERS = [...PROVIDERS];
