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
import { MockDotMessageService, mockWorkflowsActions } from '@dotcms/utils-testing';

import { DotEditContentStore } from './edit-content.store';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { BINARY_FIELD_CONTENTLET, CONTENT_TYPE_MOCK } from '../../../utils/mocks';

const messageServiceMock = new MockDotMessageService({
    'dot.common.message.success': 'Success',
    'edit.content.fire.action.success': 'Content published'
});

describe('DotEditContentStore', () => {
    let spectator: SpectatorService<DotEditContentStore>;
    let dotWorkflowsActionsService: DotWorkflowsActionsService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let location: Location;
    let messageService: MessageService;

    const createService = createServiceFactory({
        service: DotEditContentStore,
        imports: [HttpClientTestingModule],
        providers: [
            Location,
            MessageService,
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
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

    beforeEach(() => {
        spectator = createService();
        dotWorkflowsActionsService = spectator.inject(DotWorkflowsActionsService);
        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
        messageService = spectator.inject(MessageService);
        location = spectator.inject(Location);

        spectator.service.setState({
            actions: mockWorkflowsActions,
            contentType: CONTENT_TYPE_MOCK,
            contentlet: BINARY_FIELD_CONTENTLET,
            loading: false,
            layout: {
                showSidebar: true
            }
        });
    });

    it('should create the store', () => {
        expect(spectator.service).toBeDefined();
    });

    describe('updaters', () => {
        it('should update the state', (done) => {
            // Skip the initial value
            spectator.service.vm$.pipe(skip(1)).subscribe((state) => {
                expect(state).toEqual({
                    actions: [],
                    contentType: CONTENT_TYPE_MOCK,
                    contentlet: BINARY_FIELD_CONTENTLET,
                    loading: false,
                    layout: {
                        showSidebar: true
                    }
                });
                done();
            });
            spectator.service.updateState({
                actions: [],
                contentType: CONTENT_TYPE_MOCK,
                contentlet: BINARY_FIELD_CONTENTLET,
                loading: false,
                layout: {
                    showSidebar: true
                }
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

        it('should update the sidebar state', (done) => {
            spectator.service.updateSidebarState(false);
            spectator.service.layout$.pipe().subscribe((state) => {
                expect(state).toEqual({
                    showSidebar: false
                });
                done();
            });
        });
    });

    describe('effects', () => {
        it('should call fireWorkflowAction and update the state and url', (done) => {
            const fireWorkflowActionSpy = jest.spyOn(dotWorkflowActionsFireService, 'fireTo');
            const workflowSpy = jest.spyOn(dotWorkflowsActionsService, 'getByInode');
            const updateStateSpy = jest.spyOn(spectator.service, 'updateContentletAndActions');
            const locationSpy = jest.spyOn(location, 'replaceState');
            const spyMessage = jest.spyOn(messageService, 'add');

            const mockParams = {
                actionId: mockWorkflowsActions[0].id,
                data: {
                    contentlet: {
                        title: 'new title',
                        inode: '12345',
                        contentType: BINARY_FIELD_CONTENTLET.contentType
                    }
                },
                inode: BINARY_FIELD_CONTENTLET.inode
            };

            spectator.service.fireWorkflowActionEffect(mockParams);

            spectator.service.vm$.subscribe(() => {
                expect(fireWorkflowActionSpy).toHaveBeenCalledWith(mockParams);
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

                expect(spyMessage).toHaveBeenCalledWith({
                    severity: 'success',
                    summary: 'Success',
                    detail: 'Content published'
                });

                done();
            });
        });
    });
});
