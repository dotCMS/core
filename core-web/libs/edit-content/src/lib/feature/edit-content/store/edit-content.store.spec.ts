import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Location } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';

import { skip } from 'rxjs/operators';

import {
    DotMessageService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { DotEditContentStore } from './edit-content.store';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { BINARY_FIELD_CONTENTLET, CONTENT_TYPE_MOCK } from '../../../utils/mocks';

describe('DotEditContentStore', () => {
    let spectator: SpectatorService<DotEditContentStore>;
    let dotEditContentService: DotEditContentService;
    let dotWorkflowsActionsService: DotWorkflowsActionsService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let location: Location;

    const createService = createServiceFactory({
        service: DotEditContentStore,
        imports: [HttpClientTestingModule],
        providers: [
            DotMessageService,
            MessageService,
            Location,
            {
                provide: DotWorkflowActionsFireService,
                useValue: {
                    fireTo: jest.fn().mockReturnValue(of(BINARY_FIELD_CONTENTLET))
                }
            },
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: jest.fn().mockReturnValue(of(mockWorkflowsActions)),
                    getDefaultActions: jest.fn().mockReturnValue(of(mockWorkflowsActions))
                }
            },
            {
                provide: DotEditContentService,
                useValue: {
                    getContentType: jest.fn().mockReturnValue(of(CONTENT_TYPE_MOCK)),
                    getContentById: jest.fn().mockReturnValue(of(BINARY_FIELD_CONTENTLET))
                }
            },
            {
                provide: ActivatedRoute,
                useValue: { snapshot: { params: { contentType: undefined, id: '1' } } }
            }
        ]
    });

    describe('Existing content', () => {
        beforeEach(() => {
            spectator = createService();
            dotEditContentService = spectator.inject(DotEditContentService);
            dotWorkflowsActionsService = spectator.inject(DotWorkflowsActionsService);
            dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
            location = spectator.inject(Location);
        });

        it('should have a initial value', (done) => {
            const spyContent = jest.spyOn(dotEditContentService, 'getContentById');
            const spyContentType = jest.spyOn(dotEditContentService, 'getContentType');
            const spyWorkflow = jest.spyOn(dotWorkflowsActionsService, 'getByInode');

            spectator.service.vm$.subscribe((state) => {
                expect(state).toEqual({
                    actions: mockWorkflowsActions,
                    fields: CONTENT_TYPE_MOCK.fields,
                    layout: CONTENT_TYPE_MOCK.layout,
                    contentType: BINARY_FIELD_CONTENTLET.contentType,
                    contentlet: BINARY_FIELD_CONTENTLET
                });

                expect(spyContent).toHaveBeenCalledWith('1');
                expect(spyContentType).toHaveBeenCalledWith(BINARY_FIELD_CONTENTLET.contentType);
                expect(spyWorkflow).toHaveBeenCalledWith('1', DotRenderMode.EDITING);
                done();
            });
        });

        describe('updaters', () => {
            it('should update the state', (done) => {
                // Skip the initial value
                spectator.service.vm$.pipe(skip(1)).subscribe((state) => {
                    expect(state).toEqual({
                        actions: [],
                        fields: CONTENT_TYPE_MOCK.fields,
                        layout: CONTENT_TYPE_MOCK.layout,
                        contentType: BINARY_FIELD_CONTENTLET.contentType,
                        contentlet: BINARY_FIELD_CONTENTLET
                    });
                    done();
                });
                spectator.service.updateState({
                    actions: [],
                    contentType: CONTENT_TYPE_MOCK,
                    contentlet: BINARY_FIELD_CONTENTLET
                });
            });

            it('should update the contentlet and actions', (done) => {
                const NEW_BINARY_FIELD_CONTENTLET = {
                    ...BINARY_FIELD_CONTENTLET,
                    title: 'new title'
                };

                spectator.service.vm$.pipe(skip(1)).subscribe((state) => {
                    expect(state).toEqual({
                        ...state,
                        actions: [],
                        contentlet: NEW_BINARY_FIELD_CONTENTLET
                    });
                    done();
                });
                spectator.service.updateContentletAndActions({
                    actions: [],
                    contentlet: NEW_BINARY_FIELD_CONTENTLET
                });
            });
        });

        describe('effects', () => {
            it('should call fireWorkflowAction and update the state and url', (done) => {
                const fireWorkflowActionSpy = jest.spyOn(dotWorkflowActionsFireService, 'fireTo');
                const workflowSpy = jest.spyOn(dotWorkflowsActionsService, 'getByInode');
                const updateStateSpy = jest.spyOn(spectator.service, 'updateContentletAndActions');
                const locationSpy = jest.spyOn(location, 'replaceState');

                const mockParams = {
                    actionId: mockWorkflowsActions[0].id,
                    formData: {
                        title: 'new title',
                        inode: '12345'
                    }
                };

                const mockWFActionPayload = {
                    actionId: mockParams.actionId,
                    data: {
                        contentlet: {
                            ...mockParams.formData,
                            contentType: BINARY_FIELD_CONTENTLET.contentType
                        }
                    },
                    inode: BINARY_FIELD_CONTENTLET.inode
                };

                spectator.service.vm$.pipe(skip(1)).subscribe(() => {
                    expect(fireWorkflowActionSpy).toHaveBeenCalledWith(mockWFActionPayload);
                    expect(workflowSpy).toHaveBeenCalledWith(
                        BINARY_FIELD_CONTENTLET.inode,
                        DotRenderMode.EDITING
                    );
                    expect(updateStateSpy).toHaveBeenCalledWith({
                        contentlet: BINARY_FIELD_CONTENTLET,
                        actions: mockWorkflowsActions
                    });
                    expect(locationSpy).toHaveBeenCalledWith(
                        `/content/${BINARY_FIELD_CONTENTLET.inode}`
                    );
                    done();
                });

                spectator.service.fireWorkflowActionEffect(mockParams);
            });
        });
    });

    describe('New content', () => {
        beforeEach(() => {
            spectator = createService({
                providers: [
                    {
                        provide: ActivatedRoute,
                        useValue: {
                            snapshot: { params: { contentType: 'newContentType', id: null } }
                        }
                    }
                ]
            });
            dotEditContentService = spectator.inject(DotEditContentService);
            dotWorkflowsActionsService = spectator.inject(DotWorkflowsActionsService);
        });

        it('should have a initial value', (done) => {
            const spyContentType = jest.spyOn(dotEditContentService, 'getContentType');
            const spyWorkflow = jest.spyOn(dotWorkflowsActionsService, 'getDefaultActions');

            spectator.service.vm$.subscribe((state) => {
                expect(state).toEqual({
                    actions: mockWorkflowsActions,
                    fields: CONTENT_TYPE_MOCK.fields,
                    layout: CONTENT_TYPE_MOCK.layout,
                    contentType: CONTENT_TYPE_MOCK.variable,
                    contentlet: null
                });

                expect(spyContentType).toHaveBeenCalledWith('new');
                expect(spyWorkflow).toHaveBeenCalledWith('1', DotRenderMode.EDITING);
                done();
            });
        });
    });
});
