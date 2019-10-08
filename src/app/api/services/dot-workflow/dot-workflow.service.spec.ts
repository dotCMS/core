import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotWorkflowService } from './dot-workflow.service';
import { MockBackend } from '@angular/http/testing';
import { ConnectionBackend, ResponseOptions, Response, RequestMethod } from '@angular/http';
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
        expect(this.lastConnection.request.method).toBe(RequestMethod.Get);
    });

    it('should get default workflow', () => {
        const defaultSystemWorkflow = mockWorkflows.filter((workflow) => workflow.system);
        this.dotWorkflowService.getSystem().subscribe((workflow) => {
            expect(workflow).toEqual(defaultSystemWorkflow[0]);
        });
    });
});
