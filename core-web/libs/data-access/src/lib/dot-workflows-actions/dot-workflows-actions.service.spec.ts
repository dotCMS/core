import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import {
    MOCK_SINGLE_WORKFLOW_ACTIONS,
    mockWorkflows,
    mockWorkflowsActions
} from '@dotcms/utils-testing';

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
        const mockResponse = {
            entity: MOCK_SINGLE_WORKFLOW_ACTIONS
        };

        spectator.service.getDefaultActions(contentTypeId).subscribe((res) => {
            expect(res).toEqual(MOCK_SINGLE_WORKFLOW_ACTIONS);
            done();
        });

        spectator
            .expectOne(
                `/api/v1/workflow/initialactions/contenttype/${contentTypeId}`,
                HttpMethod.GET
            )
            .flush(mockResponse);
    });

    it('should get workflow actions by content type name', (done) => {
        const contentTypeName = 'Blog';
        const mockWorkflowActionsResponse = [
            {
                scheme: mockWorkflows[0],
                action: mockWorkflowsActions[0],
                firstStep: {
                    id: '123',
                    name: 'First Step',
                    creationDate: 0,
                    enableEscalation: false,
                    escalationAction: null,
                    escalationTime: 0,
                    resolved: false,
                    schemeId: '123',
                    myOrder: 0
                }
            }
        ];

        spectator.service.getWorkFlowActions(contentTypeName).subscribe((res) => {
            expect(res).toEqual(mockWorkflowActionsResponse);
            done();
        });

        spectator
            .expectOne(
                `/api/v1/workflow/defaultactions/contenttype/${contentTypeName}`,
                HttpMethod.GET
            )
            .flush({
                entity: mockWorkflowActionsResponse
            });
    });

    it('should return empty array when workflow actions response is null', (done) => {
        const contentTypeName = 'Blog';

        spectator.service.getWorkFlowActions(contentTypeName).subscribe((res) => {
            expect(res).toEqual([]);
            done();
        });

        spectator
            .expectOne(
                `/api/v1/workflow/defaultactions/contenttype/${contentTypeName}`,
                HttpMethod.GET
            )
            .flush({
                entity: null
            });
    });
});
