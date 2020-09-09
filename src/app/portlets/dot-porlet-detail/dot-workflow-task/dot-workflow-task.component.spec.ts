import { Injectable, DebugElement } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotWorkflowTaskComponent } from './dot-workflow-task.component';
import { DotWorkflowTaskDetailService } from '@components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';
import { ComponentFixture, async, TestBed } from '@angular/core/testing';
import { DotWorkflowTaskDetailModule } from '@components/dot-workflow-task-detail/dot-workflow-task-detail.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
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
import { LoginServiceMock } from 'src/app/test/login-service.mock';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@tests/dot-test-bed';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotWorkflowEventHandlerService } from '@services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { CoreWebServiceMock } from '../../../../../projects/dotcms-js/src/lib/core/core-web.service.mock';
import { BaseRequestOptions, ConnectionBackend, Http, RequestOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import {DotWizardService} from '@services/dot-wizard/dot-wizard.service';
import {DotHttpErrorManagerService} from '@services/dot-http-error-manager/dot-http-error-manager.service';
import {DotAlertConfirmService} from '@services/dot-alert-confirm';
import {ConfirmationService} from 'primeng/api';
import {DotWorkflowActionsFireService} from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import {DotGlobalMessageService} from '@components/_common/dot-global-message/dot-global-message.service';
import {DotEventsService} from '@services/dot-events/dot-events.service';
@Injectable()
class MockDotWorkflowTaskDetailService {
    view = jasmine.createSpy('view');
}

const messageServiceMock = new MockDotMessageService({
    'workflow.task.dialog.header': 'Task Detail'
});

describe('DotWorkflowTaskComponent', () => {
    let fixture: ComponentFixture<DotWorkflowTaskComponent>;
    let de: DebugElement;
    let component: DotWorkflowTaskComponent;
    let dotRouterService: DotRouterService;
    let dotIframeService: DotIframeService;
    let dotWorkflowTaskDetailService: DotWorkflowTaskDetailService;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;
    let taskDetail: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotWorkflowTaskComponent],
            imports: [DotWorkflowTaskDetailModule, BrowserAnimationsModule, RouterTestingModule],
            providers: [
                DotWorkflowTaskDetailService,
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            params: {
                                asset: '74cabf7a-0e9d-48b6-ab1c-8f76d0ad31e0'
                            }
                        }
                    }
                },
                {
                    provide: DotWorkflowTaskDetailService,
                    useClass: MockDotWorkflowTaskDetailService
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                DotCustomEventHandlerService,
                { provide: DotRouterService, useClass: MockDotRouterService },
                DotIframeService,
                DotContentletEditorService,
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                DotDownloadBundleDialogService,
                DotWorkflowEventHandlerService,
                PushPublishService,
                ApiRoot,
                UserModel,
                LoggerService,
                StringUtils,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: ConnectionBackend, useClass: MockBackend },
                { provide: RequestOptions, useClass: BaseRequestOptions },
                Http,
                DotCurrentUserService,
                DotMessageDisplayService,
                DotcmsEventsService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                DotWizardService,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                DotWorkflowActionsFireService,
                DotGlobalMessageService,
                DotEventsService
            ]
        });

        fixture = TestBed.createComponent(DotWorkflowTaskComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotWorkflowTaskDetailService = TestBed.get(DotWorkflowTaskDetailService);
        dotRouterService = TestBed.get(DotRouterService);
        dotIframeService = TestBed.get(DotIframeService);
        dotCustomEventHandlerService = TestBed.get(DotCustomEventHandlerService);
        spyOn(dotIframeService, 'reloadData');
        fixture.detectChanges();
        taskDetail = de.query(By.css('dot-workflow-task-detail'));
    });

    it(
        'should call workflow task modal',
        async(() => {
            const params = {
                header: 'Task Detail',
                id: '74cabf7a-0e9d-48b6-ab1c-8f76d0ad31e0'
            };

            expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledWith(params);
        })
    );

    it('should redirect to /workflow and refresh data when modal closed', () => {
        taskDetail.triggerEventHandler('close', {});
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/c/workflow');
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('workflow');
    });

    it('should redirect to /workflow when edit-task-executed-workflow event is triggered', () => {
        spyOn(component, 'onCloseWorkflowTaskEditor');
        taskDetail.triggerEventHandler('custom', {
            detail: {
                name: 'edit-task-executed-workflow'
            }
        });
        expect(component.onCloseWorkflowTaskEditor).toHaveBeenCalledTimes(1);
    });

    it('should redirect to /workflow when close event is triggered', () => {
        taskDetail.triggerEventHandler('custom', {
            detail: {
                name: 'close'
            }
        });
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/c/workflow');
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('workflow');
    });

    it('should call to dotCustomEventHandlerService with the correct callbaack', () => {
        spyOn(dotCustomEventHandlerService, 'handle');
        const mockEvent = {
            detail: {
                name: 'workflow-wizard',
                data: {
                    callback: 'fileActionCallbackFromAngular'
                }
            }
        };

        taskDetail.triggerEventHandler('custom', {
            detail: {
                name: 'workflow-wizard',
                data: {
                    callback: 'test'
                }
            }
        });
        expect(dotCustomEventHandlerService.handle).toHaveBeenCalledWith(mockEvent);
    });
});
