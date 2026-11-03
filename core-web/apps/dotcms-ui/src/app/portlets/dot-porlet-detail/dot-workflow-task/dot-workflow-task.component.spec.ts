/* eslint-disable @typescript-eslint/no-explicit-any */

import { mockProvider } from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { type Mock, vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
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
    DotFormatDateService,
    DotGenerateSecurePasswordService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
    DotPropertiesService,
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
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { FeaturedFlags } from '@dotcms/dotcms-models';
import {
    LoginServiceMock,
    MockDotMessageService,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotWorkflowTaskComponent } from './dot-workflow-task.component';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogService } from '../../../api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { MockDotUiColorsService } from '../../../test/dot-test-bed';
import { IframeOverlayService } from '../../../view/components/_common/iframe/service/iframe-overlay.service';
import { DotWorkflowTaskDetailComponent } from '../../../view/components/dot-workflow-task-detail/dot-workflow-task-detail.component';
import { DotWorkflowTaskDetailService } from '../../../view/components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';

@Injectable()
class MockDotWorkflowTaskDetailService {
    view = vi.fn();
}

const messageServiceMock = new MockDotMessageService({
    'workflow.task.dialog.header': 'Task Detail'
});

const createFeatureFlagResponse = (enabled = 'NOT_FOUND') => ({
    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: enabled
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
            imports: [
                DotWorkflowTaskDetailComponent,
                BrowserAnimationsModule,
                RouterTestingModule,
                DotWorkflowTaskComponent
            ],
            providers: [
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
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                DotCustomEventHandlerService,
                DotLicenseService,
                DotMenuService,
                IframeOverlayService,
                { provide: DotRouterService, useClass: MockDotRouterService },
                DotIframeService,
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                DotDownloadBundleDialogService,
                DotWorkflowEventHandlerService,
                PushPublishService,
                ApiRoot,
                UserModel,
                LoggerService,
                StringUtils,
                provideHttpClient(),
                provideHttpClientTesting(),
                DotCurrentUserService,
                DotMessageDisplayService,
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
                mockProvider(DotContentTypeService),
                {
                    provide: DotPropertiesService,
                    useValue: {
                        getKeys: () => of(createFeatureFlagResponse())
                    }
                }
            ]
        });

        TestBed.overrideComponent(DotWorkflowTaskComponent, {
            set: {
                providers: [
                    {
                        provide: DotWorkflowTaskDetailService,
                        useClass: MockDotWorkflowTaskDetailService
                    }
                ]
            }
        });

        fixture = TestBed.createComponent(DotWorkflowTaskComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        // Get the service from the component's injector since it has its own provider
        dotWorkflowTaskDetailService = de.injector.get(DotWorkflowTaskDetailService);
        dotRouterService = TestBed.inject(DotRouterService);
        dotIframeService = TestBed.inject(DotIframeService);
        dotCustomEventHandlerService = TestBed.inject(DotCustomEventHandlerService);
        vi.spyOn(dotIframeService, 'reloadData');
        fixture.detectChanges();
        taskDetail = de.query(By.css('dot-workflow-task-detail'));
    });

    it('should call workflow task modal', () => {
        const params = {
            header: 'Task Detail',
            id: '74cabf7a-0e9d-48b6-ab1c-8f76d0ad31e0'
        };

        expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledWith(params);
        expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledTimes(1);
    });

    it('should redirect to /workflow and refresh data when modal closed', () => {
        taskDetail.triggerEventHandler('shutdown', {});
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/c/workflow');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('workflow');
        expect(dotIframeService.reloadData).toHaveBeenCalledTimes(1);
    });

    it('should redirect to /workflow when edit-task-executed-workflow event is triggered', () => {
        vi.spyOn(component, 'onCloseWorkflowTaskEditor');
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
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('workflow');
        expect(dotIframeService.reloadData).toHaveBeenCalledTimes(1);
    });

    it('should call to dotCustomEventHandlerService with the correct callbaack', () => {
        vi.spyOn(dotCustomEventHandlerService, 'handle');
        const mockEvent = {
            detail: {
                name: 'workflow-wizard',
                data: {
                    workflow: {
                        actionInputs: []
                    },
                    callback: 'test',
                    inode: '123'
                }
            }
        };

        taskDetail.triggerEventHandler('custom', mockEvent);
        expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith(mockEvent);
    });

    /**
     * The detail's "Edit Content" sends `edit-contentlet` (`view_workflow_task.jsp`, `doEdit`):
     * the content opens in the side panel above the task dialog, and the detail is viewed again
     * once the panel saves or closes, so it shows the content's current step (#38303, FR-025a).
     */
    describe('edit-contentlet from the task detail', () => {
        const TASK = {
            header: 'Task Detail',
            id: '74cabf7a-0e9d-48b6-ab1c-8f76d0ad31e0'
        };

        /** The handlers the component hands `openContent`, run after the task list reloads. */
        type AfterPanel = { saved?: () => void; closed?: () => void };

        let openContent: Mock;

        /** What the component handed `openContent` besides the request, on its only call. */
        const afterPanel = (): AfterPanel => {
            expect(openContent.mock.calls.length, 'the component opens one panel').toBe(1);

            return (openContent.mock.calls[0] as unknown[])[1] as AfterPanel;
        };

        beforeEach(() => {
            openContent = vi
                .spyOn(dotCustomEventHandlerService, 'openContent')
                .mockImplementation(() => undefined) as unknown as Mock;
            vi.spyOn(dotCustomEventHandlerService, 'handle');

            taskDetail.triggerEventHandler('custom', {
                detail: {
                    name: 'edit-contentlet',
                    data: { inode: 'content-inode', contentType: 'Blog' }
                }
            });
        });

        it('should open the content through openContent with the inode and content type the detail sent', () => {
            expect(openContent).toHaveBeenCalledWith(
                { mode: 'edit', inode: 'content-inode', contentType: 'Blog' },
                { saved: expect.any(Function), closed: expect.any(Function) }
            );
            expect(dotCustomEventHandlerService.handle).not.toHaveBeenCalled();
        });

        it('should view the same task again after the panel saves', () => {
            afterPanel().saved?.();

            expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledTimes(2);
            expect(dotWorkflowTaskDetailService.view).toHaveBeenLastCalledWith(TASK);
        });

        it('should view the same task again after the panel closes', () => {
            afterPanel().closed?.();

            expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledTimes(2);
            expect(dotWorkflowTaskDetailService.view).toHaveBeenLastCalledWith(TASK);
        });

        it('should keep the task open, not return to the task list', () => {
            afterPanel().saved?.();
            afterPanel().closed?.();

            expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
        });
    });
});
