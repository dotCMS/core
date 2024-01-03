import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { mockWorkflows } from '@dotcms/utils-testing';

import { DotWorkflowService } from './dot-workflow.service';
import { DotCMSWorkflowMock, mockWorkflowstatus } from './utils/mocks';

describe('DotWorkflowService', () => {
    let spectator: SpectatorHttp<DotWorkflowService>;
    const createHttp = createHttpFactory(DotWorkflowService);

    beforeEach(() => (spectator = createHttp()));

    it('should get workflows', () => {
        spectator.service.get().subscribe((res) => {
            expect(res).toEqual(DotCMSWorkflowMock);
        });

        spectator.expectOne('/api/v1/workflow/schemes', HttpMethod.GET).flush({
            entity: DotCMSWorkflowMock
        });
    });

    it('should get default workflow', () => {
        const defaultSystemWorkflow = mockWorkflows.filter((workflow) => workflow.system);

        spectator.service.getSystem().subscribe((res) => {
            expect(res).toEqual(defaultSystemWorkflow[0]);
        });

        spectator.expectOne('/api/v1/workflow/schemes', HttpMethod.GET);
    });

    it('should get workflow status by inode', () => {
        spectator.service
            .getWorkflowStatus('499b58f8-26cd-4931-9dca-677fd6040b31')
            .subscribe((res) => {
                expect(res).toEqual(mockWorkflowstatus);
            });

        spectator
            .expectOne(
                '/api/v1/workflow/status/499b58f8-26cd-4931-9dca-677fd6040b31',
                HttpMethod.GET
            )
            .flush({
                entity: mockWorkflowstatus
            });
    });
});
