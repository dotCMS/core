import { DOTTestBed } from '../../../test/dot-test-bed';
import { Injectable, DebugElement } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotWorkflowTaskComponent } from './dot-workflow-task.component';
import { DotWorkflowTaskDetailService } from '@components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';
import { ComponentFixture, async } from '@angular/core/testing';
import { DotWorkflowTaskDetailModule } from '@components/dot-workflow-task-detail/dot-workflow-task-detail.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from 'src/app/test/login-service.mock';

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
    let taskDetail: DebugElement;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
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
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotWorkflowTaskComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotWorkflowTaskDetailService = de.injector.get(DotWorkflowTaskDetailService);
        dotRouterService = de.injector.get(DotRouterService);
        dotIframeService = de.injector.get(DotIframeService);
        spyOn(dotIframeService, 'reloadData');
        fixture.detectChanges();
        taskDetail = de.query(By.css('dot-workflow-task-detail'));
    });

    it('should call workflow task modal', async(() => {
        const params = {
            header: 'Task Detail',
            id: '74cabf7a-0e9d-48b6-ab1c-8f76d0ad31e0'
        };

        expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledWith(params);
    }));

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
});
