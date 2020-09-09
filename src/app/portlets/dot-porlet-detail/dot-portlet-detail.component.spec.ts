import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { DotPortletDetailComponent } from './dot-portlet-detail.component';
import { DotWorkflowTaskModule } from './dot-workflow-task/dot-workflow-task.module';
import { DotMenuService } from '@services/dot-menu.service';
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
} from 'dotcms-js';
import { LoginServiceMock } from '../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotContentletsModule } from './dot-contentlets/dot-contentlets.module';
import { ActivatedRoute } from '@angular/router';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@tests/dot-test-bed';
import { DotDownloadBundleDialogModule } from '@components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.module';
import { CoreWebServiceMock } from '../../../../projects/dotcms-js/src/lib/core/core-web.service.mock';
import { BaseRequestOptions, ConnectionBackend, Http, RequestOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { DotWorkflowEventHandlerService } from '@services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { FormatDateService } from '@services/format-date-service';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotWizardService } from '@services/dot-wizard/dot-wizard.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { ConfirmationService } from 'primeng/api';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';

describe('DotPortletDetailComponent', () => {
    let fixture: ComponentFixture<DotPortletDetailComponent>;
    let de: DebugElement;
    let router: ActivatedRoute;

    beforeEach(
        async(() => {
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
                    { provide: ConnectionBackend, useClass: MockBackend },
                    { provide: RequestOptions, useClass: BaseRequestOptions },
                    Http,
                    PushPublishService,
                    ApiRoot,
                    FormatDateService,
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
                    DotEventsService
                ],
                declarations: [DotPortletDetailComponent],
                imports: [
                    DotWorkflowTaskModule,
                    DotContentletsModule,
                    RouterTestingModule,
                    BrowserAnimationsModule,
                    DotDownloadBundleDialogModule
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotPortletDetailComponent);
        de = fixture.debugElement;
        router = de.injector.get(ActivatedRoute);
    });

    it('should not have dot-workflow-task', () => {
        spyOnProperty(router, 'parent', 'get').and.returnValue({
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
        spyOnProperty(router, 'parent', 'get').and.returnValue({
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
        spyOnProperty(router, 'parent', 'get').and.returnValue({
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
