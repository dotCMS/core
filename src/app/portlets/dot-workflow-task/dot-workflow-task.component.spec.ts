import { DOTTestBed } from '../../test/dot-test-bed';
import { Injectable, DebugElement } from '@angular/core';
import { DotNavigationService } from '../../view/components/dot-navigation/dot-navigation.service';
import { ActivatedRoute } from '@angular/router';
import { DotWorkflowTaskComponent } from './dot-workflow-task.component';
import { DotWorkflowTaskDetailService } from '../../view/components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';
import { ComponentFixture } from '@angular/core/testing';

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet = jasmine.createSpy('goToFirstPortlet');
}

@Injectable()
class MockDotWorkflowTaskDetailService {
    view = jasmine.createSpy('view');
}

describe('DotWorkflowTaskComponent', () => {
    let fixture: ComponentFixture<DotWorkflowTaskComponent>;
    let de: DebugElement;

    let dotNavigationService: DotNavigationService;
    let dotWorkflowTaskDetailService: DotWorkflowTaskDetailService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotWorkflowTaskComponent],
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
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotWorkflowTaskComponent);
        de = fixture.debugElement;
        dotNavigationService = de.injector.get(DotNavigationService);
        dotWorkflowTaskDetailService = de.injector.get(DotWorkflowTaskDetailService);
    });

    it('should call first portlet & workflow task modal', () => {
        fixture.detectChanges();

        const params = {
            id: '74cabf7a-0e9d-48b6-ab1c-8f76d0ad31e0'
        };

        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalled();
        expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledWith(params);
    });
});
