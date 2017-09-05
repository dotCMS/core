// Angular 2
import { AccountService } from './api/services/account-service';
import { ColorUtil } from './api/util/ColorUtil';
import { ConfirmationService } from 'primeng/primeng';
import { ContentTypesInfoService } from './api/services/content-types-info';
import { CrudService } from './api/services/crud/crud.service';
import { DotRouterService } from './api/services/dot-router-service';
import { FormatDateService } from './api/services/format-date-service';
import { GravatarService } from './api/services/gravatar-service';
import { IframeOverlayService } from './api/services/iframe-overlay-service';
import { Logger } from 'angular2-logger/core';
import { MessageService } from './api/services/messages-service';
import { NotLicensedService } from './api/services/not-licensed-service';
import { NotificationsService } from './api/services/notifications-service';
import { RoutingPrivateAuthService } from './api/services/routing-private-auth-service';
import { RoutingPublicAuthService } from './api/services/routing-public-auth-service';
import { RoutingService } from './api/services/routing-service';
import { StringFormat } from './api/util/stringFormat';
import { PaginatorService } from './api/services/paginator';


const PROVIDERS: any[] = [
    // common env directives
    AccountService,
    ColorUtil,
    ConfirmationService,
    ContentTypesInfoService,
    CrudService,
    DotRouterService,
    FormatDateService,
    GravatarService,
    IframeOverlayService,
    Logger,
    MessageService,
    NotLicensedService,
    NotificationsService,
    PaginatorService,
    RoutingPrivateAuthService,
    RoutingPublicAuthService,
    RoutingService,
    StringFormat,
];

export const ENV_PROVIDERS = [...PROVIDERS];
