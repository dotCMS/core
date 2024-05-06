import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { mockWorkflows, mockWorkflowsActions } from '@dotcms/utils-testing';

import { DotWorkflowsActionsService } from './dot-workflows-actions.service';

describe('DotWorkflowsActionsService', () => {
    let spectator: SpectatorHttp<DotWorkflowsActionsService>;
    const createHttp = createHttpFactory(DotWorkflowsActionsService);

    beforeEach(() => (spectator = createHttp()));

    it('should get actions by workflows', (done) => {
        spectator.service
            .getByWorkflows(mockWorkflows)
            .subscribe((actions: DotCMSWorkflowAction[]) => {
                expect(actions).toEqual([...mockWorkflowsActions]);
                done();
            });

        spectator
            .expectOne('/api/v1/workflow/schemes/actions/NEW', HttpMethod.POST)
            .flush({ entity: [...mockWorkflowsActions] });
    });

    it('should get workflows by inode', (done) => {
        const inode = 'cc2cdf9c-a20d-4862-9454-2a76c1132123';
        spectator.service.getByInode(inode).subscribe((res) => {
            expect(res).toEqual(mockWorkflowsActions);
            done();
        });

        spectator.expectOne(`/api/v1/workflow/contentlet/${inode}/actions`, HttpMethod.GET).flush({
            entity: mockWorkflowsActions
        });
    });

    it('should get default actions by content type', (done) => {
        const contentTypeId = '123';
        const mockResponse = mockWorkflowsActions.map((action) => ({
            action
        }));

        spectator.service.getDefaultActions(contentTypeId).subscribe((res) => {
            expect(res).toEqual(mockWorkflowsActions);
            done();
        });

        spectator
            .expectOne(
                `/api/v1/workflow/initialactions/contenttype/${contentTypeId}`,
                HttpMethod.GET
            )
            .flush({
                entity: mockResponse
            });
    });
});
