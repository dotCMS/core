import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { NotLicensedService } from './../api/services/not-licensed-service';
import { DotHttpErrorManagerService } from './../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotIframeService } from './../view/components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { ConnectionBackend, RequestOptions, BaseRequestOptions, Http } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { TestBed, TestModuleMetadata, ComponentFixture } from '@angular/core/testing';
import { Type, Provider, Injector, ReflectiveInjector, LOCALE_ID } from '@angular/core';
import {
    ApiRoot,
    BrowserUtil,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    LoggerService,
    StringUtils,
    UserModel,
    DotEventsSocketURL,
    DotEventsSocket,
    DotPushPublishDialogService
} from 'dotcms-js';
import { ConfirmationService } from 'primeng/primeng';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NGFACES_MODULES } from '../modules';
import { CommonModule } from '@angular/common';
import { DotEventsService } from '../api/services/dot-events/dot-events.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { FormatDateService } from '../api/services/format-date-service';
import { DotAlertConfirmService } from '../api/services/dot-alert-confirm';
import { DotRouterService } from '../api/services/dot-router/dot-router.service';
import { DotLicenseService } from '../api/services/dot-license/dot-license.service';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotUiColorsService } from '../api/services/dot-ui-colors/dot-ui-colors.service';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

export class MockDotUiColorsService {
    setColors() {}
}

export const dotEventSocketURLFactory = () => {
    return new DotEventsSocketURL(
        `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
        window.location.protocol === 'https:'
    );
};

export class DOTTestBed {
    private static DEFAULT_CONFIG = {
        imports: [
            ...NGFACES_MODULES,
            CommonModule,
            FormsModule,
            ReactiveFormsModule,
            DotPipesModule
        ],
        providers: [
            { provide: ConnectionBackend, useClass: MockBackend },
            { provide: RequestOptions, useClass: BaseRequestOptions },
            { provide: DotUiColorsService, useClass: MockDotUiColorsService },
            { provide: LOCALE_ID, useValue: {} },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: NotLicensedService, useValue: { init() {} } },
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
            FormatDateService,
            Http,
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
                    .filter(provider => !config[property].includes(provider))
                    .forEach(item => config[property].unshift(item));
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

        DOTTestBed.DEFAULT_CONFIG.providers.forEach(provider => finalProviders.push(provider));

        providers.forEach(provider => finalProviders.push(provider));

        return ReflectiveInjector.resolveAndCreate(finalProviders, parent);
    }
}
