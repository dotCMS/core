import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { DotCMSContentletWorkflowActions, DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import {
    createFakeWorkflowAction,
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

    describe('actionInputs derivation (#36883)', () => {
        // The default/initial-action endpoints return `WorkflowDefaultActionView`, which wraps the
        // raw `WorkflowAction` and carries NO `actionInputs` key — unlike the per-inode endpoint,
        // which returns `WorkflowActionView` with the array already built server-side. The editor
        // gates its input wizard on `actionInputs.length`, so without derivation a commentable
        // action fires with no dialog. Fixtures below deliberately omit the key, the way the real
        // payload does.
        const rawCommentableAction = createFakeWorkflowAction({
            commentable: true,
            id: 'publish-action-id'
        });

        const wrap = (action: Partial<DotCMSWorkflowAction>) => [
            { scheme: mockWorkflows[0], action, firstStep: { id: 'first-step' } }
        ];

        it('should derive actionInputs for getDefaultActions', () => {
            const contentTypeId = '123';
            let result: DotCMSContentletWorkflowActions[] = [];

            spectator.service.getDefaultActions(contentTypeId).subscribe((res) => (result = res));

            spectator
                .expectOne(
                    `/api/v1/workflow/initialactions/contenttype/${contentTypeId}`,
                    HttpMethod.GET
                )
                .flush({ entity: wrap(rawCommentableAction) });

            expect(result[0].action.actionInputs).toEqual([{ id: 'commentable', body: {} }]);
        });

        it('should derive actionInputs for getWorkFlowActions', () => {
            const contentTypeName = 'Blog';
            let result: DotCMSContentletWorkflowActions[] = [];

            spectator.service
                .getWorkFlowActions(contentTypeName)
                .subscribe((res) => (result = res));

            spectator
                .expectOne(
                    `/api/v1/workflow/defaultactions/contenttype/${contentTypeName}`,
                    HttpMethod.GET
                )
                .flush({ entity: wrap(rawCommentableAction) });

            expect(result[0].action.actionInputs).toEqual([{ id: 'commentable', body: {} }]);
        });

        it('should derive an empty array for an action with no inputs, never undefined', () => {
            // Guards the opposite failure mode: these actions must keep firing directly.
            let result: DotCMSContentletWorkflowActions[] = [];

            spectator.service.getDefaultActions('123').subscribe((res) => (result = res));

            spectator
                .expectOne('/api/v1/workflow/initialactions/contenttype/123', HttpMethod.GET)
                .flush({ entity: wrap({ ...rawCommentableAction, commentable: false }) });

            expect(result[0].action.actionInputs).toEqual([]);
        });

        it('should preserve actionInputs the server already populated', () => {
            // Keeps the derivation from clobbering a payload that already carries the array —
            // relevant if these endpoints ever start returning WorkflowActionView.
            const serverProvided = [{ id: 'pushPublish', body: {} }];
            let result: DotCMSContentletWorkflowActions[] = [];

            spectator.service.getDefaultActions('123').subscribe((res) => (result = res));

            spectator
                .expectOne('/api/v1/workflow/initialactions/contenttype/123', HttpMethod.GET)
                .flush({ entity: wrap({ ...rawCommentableAction, actionInputs: serverProvided }) });

            expect(result[0].action.actionInputs).toEqual(serverProvided);
        });

        it('should NOT re-derive on getByInode — the server already populates it there', () => {
            // Deriving here would mask a future backend regression in the endpoint that is
            // supposed to build the array itself.
            //
            // The fixture omits `actionInputs` entirely while setting `commentable: true`. That
            // combination is what makes this guard bite: `withDerivedActionInputs` short-circuits
            // on any NON-EMPTY array, so a fixture that already carried one would pass whether or
            // not derivation ran. With the key absent, applying derivation to getByInode would
            // produce a `commentable` entry and fail this assertion.
            //
            // Verified by mutation: adding `map(withDerivedActionInputs)` to getByInode turns this
            // test red.
            const inode = 'cc2cdf9c-a20d-4862-9454-2a76c1132123';
            let result: DotCMSWorkflowAction[] = [];

            spectator.service.getByInode(inode).subscribe((res) => (result = res));

            spectator
                .expectOne(`/api/v1/workflow/contentlet/${inode}/actions`, HttpMethod.GET)
                .flush({ entity: [{ ...rawCommentableAction, commentable: true }] });

            expect(result[0].actionInputs).toBeUndefined();
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
});
