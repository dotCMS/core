import { DotWorkflowsActionsService } from './dot-workflows-actions.service';
import { TestBed, getTestBed } from '@angular/core/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { mockWorkflows } from '@tests/dot-workflow-service.mock';
import { mockWorkflowsActions } from '@tests/dot-workflows-actions.mock';
import { DotCMSWorkflowAction } from 'dotcms-models';

describe('DotWorkflowsActionsService', () => {
    let injector: TestBed;
    let dotWorkflowsActionsService: DotWorkflowsActionsService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotWorkflowsActionsService
            ]
        });
        injector = getTestBed();
        dotWorkflowsActionsService = injector.get(DotWorkflowsActionsService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should get actions by workflows', () => {
        dotWorkflowsActionsService
            .getByWorkflows(mockWorkflows)
            .subscribe((actions: DotCMSWorkflowAction[]) => {
                expect(actions).toEqual([...mockWorkflowsActions]);
            });

        const req = httpMock.expectOne('/api/v1/workflow/schemes/actions/NEW');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({
            schemes: [
                '85c1515c-c4f3-463c-bac2-860b8fcacc34',
                '77a9bf3f-a402-4c56-9b1f-1050b9d345dc',
                'd61a59e1-a49c-46f2-a929-db2b4bfa88b2'
            ]
        });
        req.flush({ entity: [...mockWorkflowsActions] });
    });

    it('should get workflows by inode', () => {
        const inode = 'cc2cdf9c-a20d-4862-9454-2a76c1132123';
        dotWorkflowsActionsService.getByInode(inode).subscribe((res: any) => {
            expect(res).toEqual([
                {
                    assignable: true,
                    commentable: true,
                    condition: '',
                    icon: 'workflowIcon',
                    id: '44d4d4cd-c812-49db-adb1-1030be73e69a',
                    name: 'Assign Workflow',
                    nextAssign: 'db0d2bca-5da5-4c18-b5d7-87f02ba58eb6',
                    nextStep: '43e16aac-5799-46d0-945c-83753af39426',
                    nextStepCurrentStep: false,
                    order: 0,
                    owner: null,
                    roleHierarchyForAssign: true,
                    schemeId: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
                    showOn: ['UNLOCKED', 'LOCKED']
                }
            ]);
        });

        const req = httpMock.expectOne(`v1/workflow/contentlet/${inode}/actions`);
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: [
                {
                    assignable: true,
                    commentable: true,
                    condition: '',
                    icon: 'workflowIcon',
                    id: '44d4d4cd-c812-49db-adb1-1030be73e69a',
                    name: 'Assign Workflow',
                    nextAssign: 'db0d2bca-5da5-4c18-b5d7-87f02ba58eb6',
                    nextStep: '43e16aac-5799-46d0-945c-83753af39426',
                    nextStepCurrentStep: false,
                    order: 0,
                    owner: null,
                    roleHierarchyForAssign: true,
                    schemeId: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
                    showOn: ['UNLOCKED', 'LOCKED']
                }
            ]
        });
    });
});
