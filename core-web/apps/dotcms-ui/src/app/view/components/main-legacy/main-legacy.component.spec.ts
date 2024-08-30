/* eslint-disable @typescript-eslint/no-explicit-any */

import { mockProvider } from '@ngneat/spectator';

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import { DotDownloadBundleDialogModule } from '@components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.module';
import { DotWizardModule } from '@components/_common/dot-wizard/dot-wizard.module';
import { DotCustomEventHandlerService } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { DotUiColorsService } from '@dotcms/app/api/services/dot-ui-colors/dot-ui-colors.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@dotcms/app/test/dot-test-bed';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotEventsService,
    DotGenerateSecurePasswordService,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageDisplayService,
    DotRouterService,
    DotWorkflowActionsFireService,
    DotGlobalMessageService,
    DotWorkflowEventHandlerService,
    DotFormatDateService,
    DotIframeService
} from '@dotcms/data-access';
import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { CoreWebServiceMock, LoginServiceMock, MockDotRouterService } from '@dotcms/utils-testing';

import { MainComponentLegacyComponent } from './main-legacy.component';

import { DotContentletEditorModule } from '../dot-contentlet-editor/dot-contentlet-editor.module';

@Component({
    selector: 'dot-alert-confirm',
    template: ''
})
class MockDotDialogComponent {}

@Component({
    selector: 'dot-toolbar',
    template: ''
})
class MockDotToolbarComponent {
    @Input() collapsed: boolean;
}

@Component({
    selector: 'dot-generate-secure-password',
    template: ''
})
class MockDotGenerateSecurePasswordComponent {}

@Component({
    selector: 'dot-main-nav',
    template: ''
})
class MockDotMainNavComponent {
    @Input() collapsed: boolean;
}

@Component({
    selector: 'dot-message-display',
    template: ''
})
class MockDotMessageDisplayComponent {}

@Component({
    selector: 'dot-large-message-display',
    template: ''
})
class MockDotLargeMessageDisplayComponent {}

@Component({
    selector: 'dot-push-publish-dialog',
    template: ''
})
class MockDotPushPublishDialogComponent {}

describe('MainLegacyComponent', () => {
    let fixture: ComponentFixture<MainComponentLegacyComponent>;
    let de: DebugElement;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                MainComponentLegacyComponent,
                MockDotDialogComponent,
                MockDotMainNavComponent,
                MockDotToolbarComponent,
                MockDotGenerateSecurePasswordComponent,
                MockDotMessageDisplayComponent,
                MockDotLargeMessageDisplayComponent,
                MockDotPushPublishDialogComponent
            ],
            imports: [
                RouterTestingModule,
                DotContentletEditorModule,
                DotDownloadBundleDialogModule,
                DotWizardModule
            ],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotMenuService,
                DotCustomEventHandlerService,
                DotLicenseService,
                DotIframeService,
                DotFormatDateService,
                DotAlertConfirmService,
                ConfirmationService,
                DotcmsEventsService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                LoggerService,
                StringUtils,
                DotWorkflowEventHandlerService,
                ApiRoot,
                UserModel,
                DotMessageDisplayService,
                DotHttpErrorManagerService,
                DotWorkflowActionsFireService,
                DotGlobalMessageService,
                DotEventsService,
                DotGenerateSecurePasswordService,
                mockProvider(DotContentTypeService),
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting()
            ]
        });
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(MainComponentLegacyComponent);
        de = fixture.debugElement;
        dotCustomEventHandlerService = de.injector.get(DotCustomEventHandlerService);
        fixture.detectChanges();
    });
    it('should have basic layout elements', () => {
        expect(de.query(By.css('dot-alert-confirm')) !== null).toBe(true);
        expect(de.query(By.css('dot-toolbar')) !== null).toBe(true);
        expect(de.query(By.css('dot-main-nav')) !== null).toBe(true);
        expect(de.query(By.css('router-outlet')) !== null).toBe(true);
        expect(de.query(By.css('dot-push-publish-dialog')) !== null).toBe(true);
        expect(de.query(By.css('dot-download-bundle-dialog')) !== null).toBe(true);
        expect(de.query(By.css('dot-generate-secure-password')) !== null).toBe(true);
        expect(de.query(By.css('dot-wizard')) !== null).toBe(true);
    });

    it('should have messages components', () => {
        expect(de.query(By.css('dot-large-message-display')) !== null).toBe(true);
        expect(de.query(By.css('dot-large-message-display')) !== null).toBe(true);
    });

    describe('Create Contentlet', () => {
        let createContentlet: DebugElement;
        beforeEach(() => {
            createContentlet = de.query(By.css('dot-create-contentlet'));
        });

        it('should call dotCustomEventHandlerService on customEvent', () => {
            spyOn(dotCustomEventHandlerService, 'handle');
            createContentlet.triggerEventHandler('custom', { data: 'test' });

            expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({
                data: 'test'
            });
        });
    });
});
