import {
    ConnectionBackend,
    ResponseOptions,
    Response,
    RequestOptions,
    BaseRequestOptions,
    Http
} from '@angular/http';
import { MockBackend } from '@angular/http/testing';

import { DotWorkflowsActionsService } from './dot-workflows-actions.service';
import { TestBed } from '@angular/core/testing';
import { mockWorkflowsActions } from '@tests/dot-workflows-actions.mock';
import { DotCMSWorkflowAction } from 'dotcms-models';
import { mockWorkflows } from '@tests/dot-workflow-service.mock';
import { DotWizardStep } from '@models/dot-wizard-step/dot-wizard-step.model';
import { DotCommentAndAssignFormComponent } from '@components/_common/forms/dot-comment-and-assign-form/dot-comment-and-assign-form.component';
import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from '../../../../../projects/dotcms-js/src/lib/core/core-web.service.mock';

describe('DotWorkflowsActionsService', () => {
    let dotWorkflowActionsService: DotWorkflowsActionsService;
    let backend;
    let lastConnection;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotWorkflowsActionsService,
                { provide: ConnectionBackend, useClass: MockBackend },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: RequestOptions, useClass: BaseRequestOptions },
                Http
            ]
        });
        dotWorkflowActionsService = TestBed.get(DotWorkflowsActionsService);
        backend = TestBed.get(ConnectionBackend);
        backend.connections.subscribe((connection: any) => {
            lastConnection = connection;
        });
    });

    it('should get actions by workflows', () => {
        let result: DotCMSWorkflowAction[];

        dotWorkflowActionsService
            .getByWorkflows(mockWorkflows)
            .subscribe((actions: DotCMSWorkflowAction[]) => {
                result = actions;
            });

        lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: [...mockWorkflowsActions]
                    }
                })
            )
        );

        expect(lastConnection.request.method).toBe(1);
        expect(lastConnection.request.url).toBe('/api/v1/workflow/schemes/actions/NEW');
        expect(lastConnection.request._body).toEqual({
            schemes: [
                '85c1515c-c4f3-463c-bac2-860b8fcacc34',
                '77a9bf3f-a402-4c56-9b1f-1050b9d345dc',
                'd61a59e1-a49c-46f2-a929-db2b4bfa88b2'
            ]
        });
        expect(result).toEqual([...mockWorkflowsActions]);
    });

    it('should get workflows by inode', () => {
        let result;
        const inode = 'cc2cdf9c-a20d-4862-9454-2a76c1132123';
        dotWorkflowActionsService.getByInode(inode).subscribe(res => {
            result = res;
        });

        lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
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
                    }
                })
            )
        );
        expect(result).toEqual([
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
        expect(lastConnection.request.url).toContain(`v1/workflow/contentlet/${inode}/actions`);
    });

    describe('wizard steps', () => {
        const mockWorkflowActions: DotCMSWorkflowAction = {
            ...mockWorkflowsActions[0]
        };
        let steps: DotWizardStep<any>[];

        it('should merge comment and assign steps', () => {
            const mockWizardSteps: DotWizardStep<any>[] = [
                {
                    component: DotCommentAndAssignFormComponent,
                    data: {
                        assignable: true,
                        commentable: true,
                        roleId: mockWorkflowsActions[0].nextAssign
                    }
                },
                {
                    component: DotPushPublishFormComponent,
                    data: {}
                }
            ];
            steps = dotWorkflowActionsService.setWizardSteps(mockWorkflowActions);

            expect(steps).toEqual(mockWizardSteps);
        });
        it('should return only valid Components ', () => {
            mockWorkflowActions.actionInputs = [
                {
                    body: {},
                    id: 'invalidID'
                },
                {
                    body: {},
                    id: 'invalidID2'
                }
            ];
            steps = dotWorkflowActionsService.setWizardSteps(mockWorkflowActions);
            expect(steps).toEqual([]);
        });
    });
});
