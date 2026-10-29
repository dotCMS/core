import { mockProvider } from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotCurrentUserService,
    DotEventsService,
    DotFormatDateService,
    DotGenerateSecurePasswordService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotLicenseService,
    DotMessageDisplayService,
    DotRouterService,
    DotUiColorsService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    PushPublishService
} from '@dotcms/data-access';
import {
    ApiRoot,
    DotcmsConfigService,
    DotPushPublishDialogService,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { LoginServiceMock, MockDotRouterService } from '@dotcms/utils-testing';

import { DotEditContentDeepLinkComponent } from './dot-edit-content-deep-link/dot-edit-content-deep-link.component';
import { DotPortletDetailComponent } from './dot-portlet-detail.component';

import { DotCustomEventHandlerService } from '../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogService } from '../../api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotMenuService } from '../../api/services/dot-menu.service';
import { MockDotUiColorsService } from '../../test/dot-test-bed';
import { DotDownloadBundleDialogComponent } from '../../view/components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.component';
import { IframeOverlayService } from '../../view/components/_common/iframe/service/iframe-overlay.service';
import { DotWorkflowTaskDetailService } from '../../view/components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';

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
                provideHttpClient(),
                provideHttpClientTesting(),
                PushPublishService,
                ApiRoot,
                DotFormatDateService,
                UserModel,
                StringUtils,
                LoggerService,
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
                DotWorkflowTaskDetailService,
                DotDownloadBundleDialogService,
                DotPushPublishDialogService,
                IframeOverlayService,
                mockProvider(DotContentTypeService)
            ],
            imports: [
                DotPortletDetailComponent,
                RouterTestingModule,
                BrowserAnimationsModule,
                DotDownloadBundleDialogComponent
            ]
        }).overrideComponent(DotPortletDetailComponent, {
            // The deep link opens a side panel on init; its own spec covers that.
            remove: { imports: [DotEditContentDeepLinkComponent] },
            add: { imports: [MockComponent(DotEditContentDeepLinkComponent)] }
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotPortletDetailComponent);
        de = fixture.debugElement;
        router = de.injector.get(ActivatedRoute);
    });

    it('should not have dot-workflow-task when parent route id is empty', () => {
        Object.defineProperty(router, 'parent', {
            value: {
                snapshot: {
                    params: {
                        id: ''
                    }
                }
            },
            writable: true
        });

        fixture.detectChanges();
        expect(de.query(By.css('dot-workflow-task')) === null).toBe(true);
        expect(de.query(By.css('dot-edit-content-deep-link')) === null).toBe(false);
    });

    it('should have dot-workflow-task when parent route id is workflow', () => {
        Object.defineProperty(router, 'parent', {
            value: {
                snapshot: {
                    params: {
                        id: 'workflow'
                    }
                }
            },
            writable: true
        });

        fixture.detectChanges();
        expect(de.query(By.css('dot-workflow-task'))).toBeTruthy();
        expect(de.query(By.css('dot-edit-content-deep-link')) === null).toBe(true);
    });

    it('should open the content deep link when parent route id is content', () => {
        Object.defineProperty(router, 'parent', {
            value: {
                snapshot: {
                    params: {
                        id: 'content'
                    }
                }
            },
            writable: true
        });

        fixture.detectChanges();
        expect(de.query(By.css('dot-edit-content-deep-link'))).toBeTruthy();
        expect(de.query(By.css('dot-workflow-task')) === null).toBe(true);
    });
});
