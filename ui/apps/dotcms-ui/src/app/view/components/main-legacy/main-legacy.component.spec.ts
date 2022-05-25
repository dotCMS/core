/* eslint-disable @typescript-eslint/no-explicit-any */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement, Component, Input } from '@angular/core';
import { MainComponentLegacyComponent } from './main-legacy.component';
import { RouterTestingModule } from '@angular/router/testing';
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
import { LoginServiceMock } from '../../../test/login-service.mock';
import { By } from '@angular/platform-browser';
import { DotIframeService } from '../_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotContentletEditorModule } from '../dot-contentlet-editor/dot-contentlet-editor.module';
import { DotMenuService } from '@services/dot-menu.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogModule } from '@components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.module';
import { DotWizardModule } from '@components/_common/dot-wizard/dot-wizard.module';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@tests/dot-test-bed';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { ConfirmationService } from 'primeng/api';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotWorkflowEventHandlerService } from '@services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotGenerateSecurePasswordService } from '@services/dot-generate-secure-password/dot-generate-secure-password.service';

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
            imports: [
                RouterTestingModule,
                DotContentletEditorModule,
                DotDownloadBundleDialogModule,
                DotWizardModule,
                HttpClientTestingModule
            ],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotMenuService,
                DotCustomEventHandlerService,
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
                DotGenerateSecurePasswordService
            ],
            declarations: [
                MainComponentLegacyComponent,
                MockDotDialogComponent,
                MockDotMainNavComponent,
                MockDotToolbarComponent,
                MockDotGenerateSecurePasswordComponent,
                MockDotMessageDisplayComponent,
                MockDotLargeMessageDisplayComponent,
                MockDotPushPublishDialogComponent
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

            expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({ data: 'test' });
        });
    });
});
