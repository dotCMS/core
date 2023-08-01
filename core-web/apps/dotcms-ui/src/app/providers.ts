import { InjectionToken, Provider } from '@angular/core';
import { TitleStrategy } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotLoginPageResolver } from '@components/login/dot-login-page-resolver.service';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotContentTypesInfoService,
    DotCrudService,
    DotLicenseService,
    DotMessageService,
    DotWorkflowActionsFireService,
    PaginatorService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { CanDeactivateGuardService } from '@services/guards/can-deactivate-guard.service';
import { DotTitleStrategy } from '@shared/services/dot-title-strategy.service';

import { DotAccountService } from './api/services/dot-account-service';
import { DotFormatDateService } from './api/services/dot-format-date-service';
import { DotUiColorsService } from './api/services/dot-ui-colors/dot-ui-colors.service';
import { DotWorkflowEventHandlerService } from './api/services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { AuthGuardService } from './api/services/guards/auth-guard.service';
import { ContentletGuardService } from './api/services/guards/contentlet-guard.service';
import { DefaultGuardService } from './api/services/guards/default-guard.service';
import { MenuGuardService } from './api/services/guards/menu-guard.service';
import { PagesGuardService } from './api/services/guards/pages-guard.service';
import { PublicAuthGuardService } from './api/services/guards/public-auth-guard.service';
import { NotificationsService } from './api/services/notifications-service';
import { ColorUtil } from './api/util/ColorUtil';
import { StringPixels } from './api/util/string-pixels-util';
import { StringFormat } from './api/util/stringFormat';
import { DotSaveOnDeactivateService } from './shared/dot-save-on-deactivate-service/dot-save-on-deactivate.service';
import { DotIframeService } from './view/components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { IframeOverlayService } from './view/components/_common/iframe/service/iframe-overlay.service';

export const LOCATION_TOKEN = new InjectionToken<Location>('Window location object');

const PROVIDERS: Provider[] = [
    { provide: LOCATION_TOKEN, useValue: window.location },
    DotAccountService,
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
    DotFormatDateService,
    IframeOverlayService,
    MenuGuardService,
    NotificationsService,
    PaginatorService,
    PagesGuardService,
    PublicAuthGuardService,
    StringFormat,
    StringPixels,
    DotLoginPageResolver,
    DotLoginPageStateService,
    DotPushPublishDialogService,
    DotWorkflowEventHandlerService,
    DotWorkflowActionsFireService,
    DotGlobalMessageService,
    CanDeactivateGuardService,
    {
        provide: TitleStrategy,
        useClass: DotTitleStrategy
    }
];

export const ENV_PROVIDERS = [...PROVIDERS];
