import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotWorkflowService } from './dot-workflow.service';
import { MockBackend } from '@angular/http/testing';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { mockWorkflows } from '../../../test/dot-workflow-service.mock';

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

    it('should fire workflows page action', () => {
        const inode = 'cc2cdf9c-a20d-4862-9454-2a76c1132123';
        const actionId = '44d4d4cd-c812-49db-adb1-1030be73e69a';
        spyOn(this.dotWorkflowService, 'fireWorkflowAction').and.callThrough();
        this.dotWorkflowService.fireWorkflowAction(inode, actionId).subscribe();
        expect(this.dotWorkflowService.fireWorkflowAction).toHaveBeenCalledTimes(1);
        expect(this.lastConnection.request.url).toContain(
            `v1/workflow/actions/${actionId}/fire?inode=${inode}`
        );
        expect(2).toBe(this.lastConnection.request.method); // 2 is PUT method
    });

    it('should get default workflow', () => {
        const defaultSystemWorkflow = mockWorkflows.filter((workflow) => workflow.system);
        this.dotWorkflowService.getSystem().subscribe((workflow) => {
            expect(workflow).toEqual(defaultSystemWorkflow[0]);
        });
    });
});
