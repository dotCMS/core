import { InjectionToken, Provider } from '@angular/core';
import { TitleStrategy } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import {
    CanDeactivateGuardService,
    DotAlertConfirmService,
    DotContentTypeService,
    DotContentTypesInfoService,
    DotCrudService,
    DotFormatDateService,
    DotGenerateSecurePasswordService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotSessionStorageService,
    DotSystemConfigService,
    DotUiColorsService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    EmaAppConfigurationService,
    PaginatorService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';

import { DotAccountService } from './api/services/dot-account-service';
import { DotAppsService } from './api/services/dot-apps/dot-apps.service';
import { DotDownloadBundleDialogService } from './api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotMenuService } from './api/services/dot-menu.service';
import { AuthGuardService } from './api/services/guards/auth-guard.service';
import { ContentletGuardService } from './api/services/guards/contentlet-guard.service';
import { DefaultGuardService } from './api/services/guards/default-guard.service';
import { MenuGuardService } from './api/services/guards/menu-guard.service';
import { PagesGuardService } from './api/services/guards/pages-guard.service';
import { PublicAuthGuardService } from './api/services/guards/public-auth-guard.service';
import { NotificationsService } from './api/services/notifications-service';
import { ColorUtil } from './api/util/ColorUtil';
import { StringFormat } from './api/util/stringFormat';
import { DotSaveOnDeactivateService } from './shared/dot-save-on-deactivate-service/dot-save-on-deactivate.service';
import { DotTitleStrategy } from './shared/services/dot-title-strategy.service';
import { DotIframePortletLegacyResolver } from './view/components/_common/iframe/service/dot-iframe-porlet-legacy-resolver.service';
import { IframeOverlayService } from './view/components/_common/iframe/service/iframe-overlay.service';
import { DotLoginPageResolver } from './view/components/login/dot-login-page-resolver.service';
import { DotLoginPageStateService } from './view/components/login/shared/services/dot-login-page-state.service';

export const LOCATION_TOKEN = new InjectionToken<Location>('Window location object');

const PROVIDERS: Provider[] = [
    { provide: LOCATION_TOKEN, useValue: window.location },
    EmaAppConfigurationService,
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
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotSaveOnDeactivateService,
    DotUiColorsService,
    DotFormatDateService,
    DotGenerateSecurePasswordService,
    IframeOverlayService,
    DotIframePortletLegacyResolver,
    MenuGuardService,
    NotificationsService,
    PaginatorService,
    PagesGuardService,
    PublicAuthGuardService,
    StringFormat,
    DotLoginPageResolver,
    DotLoginPageStateService,
    DotPushPublishDialogService,
    DotWorkflowEventHandlerService,
    DotWorkflowActionsFireService,
    DotGlobalMessageService,
    CanDeactivateGuardService,
    DotSessionStorageService,
    DotAppsService,
    DotDownloadBundleDialogService,
    {
        provide: TitleStrategy,
        useClass: DotTitleStrategy
    },
    GlobalStore,
    DotSystemConfigService
];

export const ENV_PROVIDERS = [...PROVIDERS];
