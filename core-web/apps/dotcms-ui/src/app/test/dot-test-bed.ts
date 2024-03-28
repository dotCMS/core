import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { LOCALE_ID, Type } from '@angular/core';
import { ComponentFixture, TestBed, TestModuleMetadata } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';

import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import {
    DotAlertConfirmService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageService,
    DotRouterService,
    DotGlobalMessageService,
    DotFormatDateService,
    DotIframeService
} from '@dotcms/data-access';
import {
    ApiRoot,
    BrowserUtil,
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    DotPushPublishDialogService,
    LoggerService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { DotSafeHtmlPipe } from '@dotcms/ui';
import { MockDotRouterService } from '@dotcms/utils-testing';

import { DotCustomEventHandlerService } from '../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogService } from '../api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotUiColorsService } from '../api/services/dot-ui-colors/dot-ui-colors.service';
import { NGFACES_MODULES } from '../modules';

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
            DotSafeHtmlPipe,
            HttpClientTestingModule
        ],
        providers: [
            { provide: DotUiColorsService, useClass: MockDotUiColorsService },
            { provide: LOCALE_ID, useValue: {} },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            {
                /* A service that provides a way to navigate between pages. */
                provide:
                    /* A service that provides a way to navigate between pages. */
                    DotRouterService,
                useClass: MockDotRouterService
            },
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
}
