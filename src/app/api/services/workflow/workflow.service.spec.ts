import { WorkflowService } from './workflow.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { MockBackend } from '@angular/http/testing';
import { ConnectionBackend } from '@angular/http';

describe('WorkflowService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([WorkflowService]);
        this.workflowService = this.injector.get(WorkflowService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should do a request to Workflow Schemes', () => {
        this.workflowService.get().subscribe();
        expect(this.lastConnection.request.url).toContain('v1/workflow/schemes');
    });
});
