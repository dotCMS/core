import { WorkflowService } from './workflow.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { MockBackend } from '@angular/http/testing';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { Workflow } from '../../../shared/models/workflow/workflow.model';

describe('WorkflowService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([WorkflowService]);
        this.workflowService = this.injector.get(WorkflowService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should get workflows', () => {
        let result;
        this.workflowService.get().subscribe(res => {
            result = res;
        });

        this.lastConnection.mockRespond(new Response(new ResponseOptions({
            body: {
                entity: [
                    {
                        hello: 'world',
                        hola: 'mundo'
                    }
                ]
            }
        })));
        expect(result).toEqual([
            {
                hello: 'world',
                hola: 'mundo'
            }
        ]);
        expect(this.lastConnection.request.url).toContain('v1/workflow/schemes');
    });

    it('should get workflows for a page', () => {
        this.workflowService.getPageWorkflows('123').subscribe((workflows: Workflow[]) => {
            expect(workflows).toEqual([
                { name: 'Workflow 1', id: 'one' },
                { name: 'Workflow 2', id: 'two' },
                { name: 'Workflow 3', id: 'three' }
            ]);
        });
    });
});
