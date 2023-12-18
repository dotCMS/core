import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Location } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';

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
            MessageService,
            DotMessageService,
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
                    getContentById: jest.fn().mockReturnValue(of(BINARY_FIELD_CONTENTLET)),
                    saveContentlet: jest.fn().mockReturnValue(of({}))
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        dotEditContentService = spectator.inject(DotEditContentService);
        dotWorkflowsActionsService = spectator.inject(DotWorkflowsActionsService);
        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
        location = spectator.inject(Location);
    });

    it('should have a initial value', (done) => {
        spectator.service.vm$.subscribe((state) => {
            expect(state).toEqual({
                actions: [],
                contentType: null,
                contentlet: null
            });
            done();
        });
    });

    describe('updaters', () => {
        it('should update the state', (done) => {
            spectator.service.vm$.pipe(skip(1)).subscribe((state) => {
                expect(state).toEqual({
                    actions: [],
                    contentType: CONTENT_TYPE_MOCK,
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
            spectator.service.vm$.pipe(skip(1)).subscribe((state) => {
                expect(state).toEqual({
                    actions: mockWorkflowsActions,
                    contentType: null,
                    contentlet: BINARY_FIELD_CONTENTLET
                });
                done();
            });
            spectator.service.updateContentletAndActions({
                actions: mockWorkflowsActions,
                contentlet: BINARY_FIELD_CONTENTLET
            });
        });
    });

    describe('effects', () => {
        describe('loadContentEffect', () => {
            describe('when it is a new content', () => {
                it('should call dotEditContentService and update state', (done) => {
                    const contentSpy = jest.spyOn(dotEditContentService, 'getContentType');
                    const workflowSpy = jest.spyOn(dotWorkflowsActionsService, 'getDefaultActions');
                    const updateStateSpy = jest.spyOn(spectator.service, 'updateState');

                    spectator.service.vm$.pipe(skip(1)).subscribe(() => {
                        expect(contentSpy).toHaveBeenCalledWith('123');
                        expect(workflowSpy).toHaveBeenCalledWith('123');
                        expect(updateStateSpy).toHaveBeenCalledWith({
                            contentlet: null,
                            contentType: CONTENT_TYPE_MOCK,
                            actions: mockWorkflowsActions
                        });
                        done();
                    });

                    spectator.service.loadContentEffect({
                        isNewContent: true,
                        idOrVar: '123'
                    });
                });
            });

            describe('when it is a existing content', () => {
                it('should call dotEditContentService and update state', (done) => {
                    const contentSpy = jest.spyOn(dotEditContentService, 'getContentById');
                    const workflowSpy = jest.spyOn(dotWorkflowsActionsService, 'getByInode');
                    const updateStateSpy = jest.spyOn(spectator.service, 'updateState');

                    spectator.service.vm$.pipe(skip(1)).subscribe(() => {
                        expect(contentSpy).toHaveBeenCalledWith('123');
                        expect(workflowSpy).toHaveBeenCalledWith('123', DotRenderMode.EDITING);
                        expect(updateStateSpy).toHaveBeenCalledWith({
                            contentlet: BINARY_FIELD_CONTENTLET,
                            contentType: CONTENT_TYPE_MOCK,
                            actions: mockWorkflowsActions
                        });
                        done();
                    });

                    spectator.service.loadContentEffect({
                        isNewContent: false,
                        idOrVar: '123'
                    });
                });
            });
        });

        it('should call fireWorkflowAction and update the state and url', (done) => {
            const fireWorkflowActionSpy = jest.spyOn(dotWorkflowActionsFireService, 'fireTo');
            const workflowSpy = jest.spyOn(dotWorkflowsActionsService, 'getByInode');
            const updateStateSpy = jest.spyOn(spectator.service, 'updateContentletAndActions');
            const locationSpy = jest.spyOn(location, 'replaceState');

            const mockParams = {
                inode: '123',
                actionId: mockWorkflowsActions[0].id,
                data: {
                    contentlet: {
                        title: 'new title',
                        inode: '12345'
                    }
                }
            };

            spectator.service.vm$.pipe(skip(1)).subscribe(() => {
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
                done();
            });

            spectator.service.fireWorkflowActionEffect(mockParams);
        });
    });
});
