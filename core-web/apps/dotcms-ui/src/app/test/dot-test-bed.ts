import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotHttpErrorManagerService } from './../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotIframeService } from './../view/components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { ComponentFixture, TestBed, TestModuleMetadata } from '@angular/core/testing';
import { Injector, LOCALE_ID, Provider, ReflectiveInjector, Type } from '@angular/core';
import {
    ApiRoot,
    BrowserUtil,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    DotPushPublishDialogService,
    LoggerService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { ConfirmationService } from 'primeng/api';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NGFACES_MODULES } from '../modules';
import { CommonModule } from '@angular/common';
import { DotEventsService } from '../api/services/dot-events/dot-events.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotFormatDateService } from '../api/services/dot-format-date-service';
import { DotAlertConfirmService } from '../api/services/dot-alert-confirm';
import { DotRouterService } from '../api/services/dot-router/dot-router.service';
import { DotLicenseService } from '../api/services/dot-license/dot-license.service';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotUiColorsService } from '../api/services/dot-ui-colors/dot-ui-colors.service';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

export class MockDotUiColorsService {
    setColors() {
        /* */
    }
}

export const dotEventSocketURLFactory = () => {
    return new DotEventsSocketURL(
        `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
        window.location.protocol === 'https:'
    );
};

/**
 * DOTTestBed its deprecated
 * @deprecated This class is deprecated
 */
export class DOTTestBed {
    private static DEFAULT_CONFIG = {
        imports: [
            ...NGFACES_MODULES,
            CommonModule,
            FormsModule,
            ReactiveFormsModule,
            DotPipesModule,
            HttpClientTestingModule
        ],
        providers: [
            { provide: DotUiColorsService, useClass: MockDotUiColorsService },
            { provide: LOCALE_ID, useValue: {} },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: DotRouterService, useClass: MockDotRouterService },
            ApiRoot,
            BrowserUtil,
            ConfirmationService,
            DotContentletEditorService,
            DotAlertConfirmService,
            DotEventsService,
            DotGlobalMessageService,
            DotHttpErrorManagerService,
            DotIframeService,
            DotMessageService,
            DotEventsSocket,
            { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
            DotcmsConfigService,
            DotcmsEventsService,
            DotFormatDateService,
            LoggerService,
            StringUtils,
            UserModel,
            DotLicenseService,
            DotCustomEventHandlerService,
            DotPushPublishDialogService,
            DotDownloadBundleDialogService
        ]
    };

    public static configureTestingModule(config: TestModuleMetadata): typeof TestBed {
        // tslint:disable-next-line:forin
        for (const property in DOTTestBed.DEFAULT_CONFIG) {
            if (config[property]) {
                DOTTestBed.DEFAULT_CONFIG[property]
                    .filter((provider) => !config[property].includes(provider))
                    .forEach((item) => config[property].unshift(item));
            } else {
                config[property] = DOTTestBed.DEFAULT_CONFIG[property];
            }
        }

        TestBed.configureTestingModule(config);
        TestBed.compileComponents();

        return TestBed;
    }

    public static createComponent<T>(component: Type<T>): ComponentFixture<T> {
        return TestBed.createComponent(component);
    }

    public static resolveAndCreate(providers: Provider[], parent?: Injector): ReflectiveInjector {
        const finalProviders = [];

        DOTTestBed.DEFAULT_CONFIG.providers.forEach((provider) => finalProviders.push(provider));

        providers.forEach((provider) => finalProviders.push(provider));

        return ReflectiveInjector.resolveAndCreate(finalProviders, parent);
    }
}
