import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotWorkflowService } from './dot-workflow.service';
import { MockBackend } from '@angular/http/testing';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';

describe('DotWorkflowService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotWorkflowService]);
        this.dotWorkflowService = this.injector.get(DotWorkflowService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should get workflows', () => {
        let result;
        this.dotWorkflowService.get().subscribe((res) => {
            result = res;
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: [
                            {
                                hello: 'world',
                                hola: 'mundo'
                            }
                        ]
                    }
                })
            )
        );
        expect(result).toEqual([
            {
                hello: 'world',
                hola: 'mundo'
            }
        ]);
        expect(this.lastConnection.request.url).toContain('v1/workflow/schemes');
    });

    it('should get workflows page actions', () => {
        let result;
        const inode = 'cc2cdf9c-a20d-4862-9454-2a76c1132123';
        this.dotWorkflowService.getContentWorkflowActions(inode).subscribe((res) => {
            result = res;
        });

        this.lastConnection.mockRespond(
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
                                requiresCheckout: true,
                                roleHierarchyForAssign: true,
                                schemeId: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
                                showOn: ['UNLOCKED', 'LOCKED'],
                                stepId: null
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
                requiresCheckout: true,
                roleHierarchyForAssign: true,
                schemeId: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
                showOn: ['UNLOCKED', 'LOCKED'],
                stepId: null
            }
        ]);
        expect(this.lastConnection.request.url).toContain(`v1/workflow/contentlet/${inode}/actions`);
    });

    it('should fire workflows page action', () => {
        const inode = 'cc2cdf9c-a20d-4862-9454-2a76c1132123';
        const actionId = '44d4d4cd-c812-49db-adb1-1030be73e69a';
        spyOn(this.dotWorkflowService, 'fireWorkflowAction').and.callThrough();
        this.dotWorkflowService.fireWorkflowAction(inode, actionId).subscribe();
        expect(this.dotWorkflowService.fireWorkflowAction).toHaveBeenCalledTimes(1);
        expect(this.lastConnection.request.url).toContain(`v1/workflow/actions/${actionId}/fire?inode=${inode}`);
        expect(2).toBe(this.lastConnection.request.method); // 2 is PUT method
    });

});
