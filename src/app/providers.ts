import { AccountService } from '@services/account-service';
import { AuthGuardService } from '@services/guards/auth-guard.service';
import { ColorUtil } from './api/util/ColorUtil';
import { ConfirmationService } from 'primeng/primeng';
import { DotContentTypesInfoService } from '@services/dot-content-types-info';
import { ContentletGuardService } from '@services/guards/contentlet-guard.service';
import { DotCrudService } from '@services/dot-crud/dot-crud.service';
import { DefaultGuardService } from '@services/guards/default-guard.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotIframeService } from './view/components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotMenuService } from '@services/dot-menu.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotSaveOnDeactivateService } from './shared/dot-save-on-deactivate-service/dot-save-on-deactivate.service';
import { FormatDateService } from '@services/format-date-service';
import { IframeOverlayService } from './view/components/_common/iframe/service/iframe-overlay.service';
import { MenuGuardService } from '@services/guards/menu-guard.service';
import { NotLicensedService } from '@services/not-licensed-service';
import { NotificationsService } from '@services/notifications-service';
import { PaginatorService } from '@services/paginator';
import { PublicAuthGuardService } from '@services/guards/public-auth-guard.service';
import { StringFormat } from './api/util/stringFormat';
import { StringPixels } from './api/util/string-pixels-util';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { InjectionToken } from '@angular/core';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';
import { DotLoginPageResolver } from '@components/login/dot-login-page-resolver.service';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { DotPushPublishDialogService } from 'dotcms-js';

export const LOCATION_TOKEN = new InjectionToken<Location>('Window location object');

const PROVIDERS: any[] = [
    { provide: LOCATION_TOKEN, useValue: window.location },
    AccountService,
    AuthGuardService,
    ColorUtil,
    ConfirmationService,
    DotContentTypesInfoService,
    ContentletGuardService,
    DotCrudService,
    DefaultGuardService,
    DotAlertConfirmService,
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotLicenseService,
    DotMenuService,
    DotMessageService,
    DotRouterService,
    DotSaveOnDeactivateService,
    DotUiColorsService,
    FormatDateService,
    IframeOverlayService,
    MenuGuardService,
    NotLicensedService,
    NotificationsService,
    PaginatorService,
    PublicAuthGuardService,
    StringFormat,
    StringPixels,
    DotLoginPageResolver,
    DotLoginPageStateService,
    DotPushPublishDialogService
];

export const ENV_PROVIDERS = [...PROVIDERS];
