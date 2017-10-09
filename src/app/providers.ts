import { AccountService } from './api/services/account-service';
import { ColorUtil } from './api/util/ColorUtil';
import { ConfirmationService } from 'primeng/primeng';
import { ContentTypesInfoService } from './api/services/content-types-info';
import { CrudService } from './api/services/crud/crud.service';
import { DotConfirmationService } from './api/services/dot-confirmation';
import { DotMenuService } from './api/services/dot-menu.service';
import { DotRouterService } from './api/services/dot-router-service';
import { FormatDateService } from './api/services/format-date-service';
import { GravatarService } from './api/services/gravatar-service';
import { IframeOverlayService } from './view/components/_common/iframe/service/iframe-overlay.service';
import { Logger } from 'angular2-logger/core';
import { MessageService } from './api/services/messages-service';
import { NotLicensedService } from './api/services/not-licensed-service';
import { NotificationsService } from './api/services/notifications-service';
import { PaginatorService } from './api/services/paginator';
import { RoutingContentletAuthService } from './api/services/routing-contentlet-auth.service';
import { RoutingPrivateAuthService } from './api/services/routing-private-auth.service';
import { RoutingPublicAuthService } from './api/services/routing-public-auth.service';
import { StringFormat } from './api/util/stringFormat';


const PROVIDERS: any[] = [
    // common env directives
    AccountService,
    ColorUtil,
    ConfirmationService,
    ContentTypesInfoService,
    CrudService,
    DotMenuService,
    DotRouterService,
    FormatDateService,
    GravatarService,
    IframeOverlayService,
    Logger,
    MessageService,
    NotLicensedService,
    DotConfirmationService,
    NotificationsService,
    PaginatorService,
    RoutingContentletAuthService,
    RoutingPrivateAuthService,
    RoutingPublicAuthService,
    StringFormat,
];

export const ENV_PROVIDERS = [...PROVIDERS];
