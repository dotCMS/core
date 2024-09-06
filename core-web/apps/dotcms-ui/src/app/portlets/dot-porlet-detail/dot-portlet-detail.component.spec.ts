import { mockProvider } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import { DotDownloadBundleDialogModule } from '@components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.module';
import { DotCustomEventHandlerService } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { DotUiColorsService } from '@dotcms/app/api/services/dot-ui-colors/dot-ui-colors.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@dotcms/app/test/dot-test-bed';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotCurrentUserService,
    DotEventsService,
    DotGenerateSecurePasswordService,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageDisplayService,
    DotRouterService,
    DotWorkflowActionsFireService,
    DotGlobalMessageService,
    DotIframeService,
    DotWizardService,
    DotWorkflowEventHandlerService,
    PushPublishService,
    DotFormatDateService
} from '@dotcms/data-access';
import {
    ApiRoot,
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { LoginServiceMock, MockDotRouterService } from '@dotcms/utils-testing';

import { DotContentletsModule } from './dot-contentlets/dot-contentlets.module';
import { DotPortletDetailComponent } from './dot-portlet-detail.component';
import { DotWorkflowTaskModule } from './dot-workflow-task/dot-workflow-task.module';

describe('DotPortletDetailComponent', () => {
    let fixture: ComponentFixture<DotPortletDetailComponent>;
    let de: DebugElement;
    let router: ActivatedRoute;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            providers: [
                DotMenuService,
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                DotCustomEventHandlerService,
                DotWorkflowEventHandlerService,
                DotIframeService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                PushPublishService,
                ApiRoot,
                DotFormatDateService,
                UserModel,
                StringUtils,
                DotcmsEventsService,
                LoggerService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                LoggerService,
                DotCurrentUserService,
                DotMessageDisplayService,
                DotWizardService,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                DotWorkflowActionsFireService,
                DotGlobalMessageService,
                DotEventsService,
                DotGenerateSecurePasswordService,
                DotLicenseService,
                mockProvider(DotContentTypeService)
            ],
            declarations: [DotPortletDetailComponent],
            imports: [
                DotWorkflowTaskModule,
                DotContentletsModule,
                RouterTestingModule,
                BrowserAnimationsModule,
                DotDownloadBundleDialogModule,
                HttpClientTestingModule
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotPortletDetailComponent);
        de = fixture.debugElement;
        router = de.injector.get(ActivatedRoute);
    });

    it('should not have dot-workflow-task', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        spyOnProperty<any>(router, 'parent', 'get').and.returnValue({
            parent: {
                snapshot: {
                    params: {
                        id: ''
                    }
                }
            }
        });

        fixture.detectChanges();
        expect(de.query(By.css('dot-workflow-task')) === null).toBe(true);
        expect(de.query(By.css('dot-contentlets')) === null).toBe(false);
    });

    it('should have dot-workflow-task', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        spyOnProperty<any>(router, 'parent', 'get').and.returnValue({
            parent: {
                snapshot: {
                    params: {
                        id: 'workflow'
                    }
                }
            }
        });

        fixture.detectChanges();
        expect(de.query(By.css('dot-workflow-task'))).toBeTruthy();
        expect(de.query(By.css('dot-contentlets')) === null).toBe(true);
    });

    it('should have dot-contentlets', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        spyOnProperty<any>(router, 'parent', 'get').and.returnValue({
            parent: {
                snapshot: {
                    params: {
                        id: 'content'
                    }
                }
            }
        });

        fixture.detectChanges();
        expect(de.query(By.css('dot-contentlets'))).toBeTruthy();
        expect(de.query(By.css('dot-workflow-task')) === null).toBe(true);
    });
});
