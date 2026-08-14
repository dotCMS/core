import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

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

    describe('getBulkActions', () => {
        const BULK_ACTIONS_URL = '/api/v1/workflow/contentlet/actions/bulk';

        it('should post the contentlet inodes and unwrap the entity', (done) => {
            const view = {
                schemes: [
                    {
                        scheme: { id: 'scheme-1', name: 'Editorial Workflow' },
                        steps: []
                    }
                ]
            };

            spectator.service.getBulkActions({ contentletIds: ['inode-1'] }).subscribe((res) => {
                expect(res).toEqual(view);
                done();
            });

            const req = spectator.expectOne(BULK_ACTIONS_URL, HttpMethod.POST);

            expect(req.request.body).toEqual({ contentletIds: ['inode-1'] });
            req.flush({ entity: view });
        });

        it('should support the query variant for selections spanning pages', (done) => {
            spectator.service
                .getBulkActions({ query: '+contentType:Blog' })
                .subscribe(() => done());

            const req = spectator.expectOne(BULK_ACTIONS_URL, HttpMethod.POST);

            expect(req.request.body).toEqual({ query: '+contentType:Blog' });
            req.flush({ entity: { schemes: [] } });
        });

        it('should fall back to an empty scheme list when the entity is missing', (done) => {
            // Keeps callers from having to null-check before mapping over `schemes`.
            spectator.service.getBulkActions({ contentletIds: ['inode-1'] }).subscribe((res) => {
                expect(res).toEqual({ schemes: [] });
                done();
            });

            spectator.expectOne(BULK_ACTIONS_URL, HttpMethod.POST).flush({ entity: null });
        });
    });

    describe('system action mappings', () => {
        const MAPPING = {
            identifier: 'mapping-1',
            systemAction: 'PUBLISH',
            workflowAction: { id: 'action-publish', schemeId: 'scheme-1' }
        };

        it('should get the mappings a content type owns', (done) => {
            spectator.service.getSystemActionsByContentType('Blog').subscribe((res) => {
                expect(res).toEqual([MAPPING]);
                done();
            });

            spectator
                .expectOne('/api/v1/workflow/contenttypes/Blog/system/actions', HttpMethod.GET)
                .flush({ entity: [MAPPING] });
        });

        it('should get the mappings a scheme owns', (done) => {
            spectator.service.getSystemActionsByScheme('scheme-1').subscribe((res) => {
                expect(res).toEqual([MAPPING]);
                done();
            });

            spectator
                .expectOne('/api/v1/workflow/schemes/scheme-1/system/actions', HttpMethod.GET)
                .flush({ entity: [MAPPING] });
        });

        it('should fall back to an empty list when the entity is missing', (done) => {
            // An unmapped content type is the normal case, not an error — callers iterate the
            // result without null-checking.
            spectator.service.getSystemActionsByContentType('Blog').subscribe((res) => {
                expect(res).toEqual([]);
                done();
            });

            spectator
                .expectOne('/api/v1/workflow/contenttypes/Blog/system/actions', HttpMethod.GET)
                .flush({ entity: null });
        });
    });
});
