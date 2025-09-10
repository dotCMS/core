/* eslint-disable @typescript-eslint/no-explicit-any */

import { mockProvider } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement, Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
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
    DotGenerateSecurePasswordService,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
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
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import {
    CoreWebServiceMock,
    LoginServiceMock,
    MockDotMessageService,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotWorkflowTaskComponent } from './dot-workflow-task.component';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogService } from '../../../api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotUiColorsService } from '../../../api/services/dot-ui-colors/dot-ui-colors.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '../../../test/dot-test-bed';
import { DotContentletEditorService } from '../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotWorkflowTaskDetailModule } from '../../../view/components/dot-workflow-task-detail/dot-workflow-task-detail.module';
import { DotWorkflowTaskDetailService } from '../../../view/components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';

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
            imports: [
                DotWorkflowTaskDetailModule,
                BrowserAnimationsModule,
                RouterTestingModule,
                HttpClientTestingModule
            ],
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
                DotLicenseService,
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
                DotFormatDateService,
                DotWorkflowActionsFireService,
                DotGlobalMessageService,
                DotGenerateSecurePasswordService,
                DotEventsService,
                mockProvider(DotContentTypeService)
            ]
        });

        fixture = TestBed.createComponent(DotWorkflowTaskComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotWorkflowTaskDetailService = TestBed.inject(DotWorkflowTaskDetailService);
        dotRouterService = TestBed.inject(DotRouterService);
        dotIframeService = TestBed.inject(DotIframeService);
        dotCustomEventHandlerService = TestBed.inject(DotCustomEventHandlerService);
        spyOn(dotIframeService, 'reloadData');
        fixture.detectChanges();
        taskDetail = de.query(By.css('dot-workflow-task-detail'));
    });

    it('should call workflow task modal', () => {
        const params = {
            header: 'Task Detail',
            id: '74cabf7a-0e9d-48b6-ab1c-8f76d0ad31e0'
        };

        expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledWith(params);
    });

    it('should redirect to /workflow and refresh data when modal closed', () => {
        taskDetail.triggerEventHandler('shutdown', {});
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
                    callback: 'test'
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
        expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith(mockEvent);
    });
});
