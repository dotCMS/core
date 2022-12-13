import { ColorUtil } from './api/util/ColorUtil';
import { DotContentTypesInfoService, PaginatorService } from '@dotcms/data-access';
import { DotCrudService } from '@dotcms/data-access';
import { DotAlertConfirmService } from '@dotcms/data-access';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotIframeService } from './view/components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotLicenseService } from '@dotcms/data-access';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { DotMessageService } from '@dotcms/data-access';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotSaveOnDeactivateService } from './shared/dot-save-on-deactivate-service/dot-save-on-deactivate.service';
import { IframeOverlayService } from './view/components/_common/iframe/service/iframe-overlay.service';

import { StringFormat } from './api/util/stringFormat';
import { StringPixels } from './api/util/string-pixels-util';
import { InjectionToken, Provider } from '@angular/core';
import { DotContentTypeService } from '@dotcms/data-access';
import { DotLoginPageResolver } from '@components/login/dot-login-page-resolver.service';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { ConfirmationService } from 'primeng/api';
import { DotAccountService } from './api/services/dot-account-service';
import { AuthGuardService } from './api/services/guards/auth-guard.service';
import { DotWorkflowEventHandlerService } from './api/services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { DotUiColorsService } from './api/services/dot-ui-colors/dot-ui-colors.service';
import { DotFormatDateService } from './api/services/dot-format-date-service';
import { ContentletGuardService } from './api/services/guards/contentlet-guard.service';
import { DefaultGuardService } from './api/services/guards/default-guard.service';
import { MenuGuardService } from './api/services/guards/menu-guard.service';
import { PublicAuthGuardService } from './api/services/guards/public-auth-guard.service';
import { NotificationsService } from './api/services/notifications-service';
import { LayoutEditorCanDeactivateGuardService } from '@services/guards/layout-editor-can-deactivate-guard.service';
import { PagesGuardService } from './api/services/guards/pages-guard.service';

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
    LayoutEditorCanDeactivateGuardService
];

export const ENV_PROVIDERS = [...PROVIDERS];
