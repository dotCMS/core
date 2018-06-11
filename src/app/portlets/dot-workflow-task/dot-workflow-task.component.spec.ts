import { DOTTestBed } from '../../test/dot-test-bed';
import { Injectable, DebugElement } from '@angular/core';
import { DotNavigationService } from '../../view/components/dot-navigation/dot-navigation.service';
import { ActivatedRoute } from '@angular/router';
import { DotWorkflowTaskComponent } from './dot-workflow-task.component';
import { DotWorkflowTaskDetailService } from '../../view/components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';
import { DotWorkflowTaskDetailModule } from '../../view/components/dot-workflow-task-detail/dot-workflow-task-detail.module';
import { ComponentFixture, async } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DotMessageService } from '../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../test/dot-message-service.mock';

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet = jasmine.createSpy('goToFirstPortlet');
}

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

    let dotNavigationService: DotNavigationService;
    let dotWorkflowTaskDetailService: DotWorkflowTaskDetailService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotWorkflowTaskComponent],
            imports: [DotWorkflowTaskDetailModule, BrowserAnimationsModule],
            providers: [
                DotWorkflowTaskDetailService,
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            params: {
                                id: '74cabf7a-0e9d-48b6-ab1c-8f76d0ad31e0'
                            }
                        }
                    }
                },
                {
                    provide: DotWorkflowTaskDetailService,
                    useClass: MockDotWorkflowTaskDetailService
                },
                {
                    provide: DotNavigationService,
                    useClass: MockDotNavigationService
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotWorkflowTaskComponent);
        de = fixture.debugElement;
        dotNavigationService = de.injector.get(DotNavigationService);
        dotWorkflowTaskDetailService = de.injector.get(DotWorkflowTaskDetailService);
        fixture.detectChanges();
    });

    it('should call workflow task modal', async(() => {
        const params = {
            header: 'Task Detail',
            id: '74cabf7a-0e9d-48b6-ab1c-8f76d0ad31e0'
        };

        setTimeout(() => {
            expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledWith(params);
        }, 0);
    }));

    it('should call first portlet when modal closed', () => {
        const edit = de.query(By.css('dot-workflow-task-detail'));
        edit.triggerEventHandler('close', {});
        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
    });
});
