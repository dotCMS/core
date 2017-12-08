import { WorkflowService } from './workflow.service';
import { DOTTestBed } from '../../../test/dot-test-bed';

describe('WorkflowService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([WorkflowService]);

        this.workflowService = this.injector.get(WorkflowService);
    });

    xit('should return valid Workflow values', () => {});
});
