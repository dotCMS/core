/* eslint-disable @typescript-eslint/no-explicit-any */
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotEditContentService } from '@dotcms/edit-content/services/dot-edit-content.service';

import { contentInitialState, ContentState } from './content.feature';
import { withWorkflow } from './workflow.feature';

import {
    MOCK_CONTENTLET_1_TAB,
    MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB,
    MOCK_WORKFLOW_DATA,
    MOCK_WORKFLOW_STATUS
} from '../../utils/edit-content.mock';
import { CONTENT_TYPE_MOCK } from '../../utils/mocks';
import { parseCurrentActions, parseWorkflows } from '../../utils/workflows.utils';
import { initialRootState } from '../edit-content.store';

const mockInitialStateWithContent: ContentState = {
    ...contentInitialState,
    contentlet: MOCK_CONTENTLET_1_TAB,
    contentType: CONTENT_TYPE_MOCK,
    schemes: parseWorkflows(MOCK_WORKFLOW_DATA)
};

describe('WorkflowFeature', () => {
    let spectator: SpectatorService<any>;
    let store: any;
    let workflowActionService: SpyObject<DotWorkflowsActionsService>;
    let workflowActionsFireService: SpyObject<DotWorkflowActionsFireService>;
    let router: SpyObject<Router>;
    let messageService: SpyObject<MessageService>;
    let dotMessageService: SpyObject<DotMessageService>;
    let dotWorkflowService: SpyObject<DotWorkflowService>;
    let dotEditContentService: SpyObject<DotEditContentService>;

    const createStore = createServiceFactory({
        service: signalStore(
            withState({ ...initialRootState, ...mockInitialStateWithContent }),
            withWorkflow()
        ),
        mocks: [
            DotEditContentService,
            DotWorkflowsActionsService,
            DotWorkflowActionsFireService,
            DotHttpErrorManagerService,
            DotMessageService,
            MessageService,
            Router,
            DotWorkflowService
        ]
    });

    beforeEach(() => {
        spectator = createStore();
        store = spectator.service;
        workflowActionService = spectator.inject(DotWorkflowsActionsService);
        workflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
        router = spectator.inject(Router);
        messageService = spectator.inject(MessageService);
        dotMessageService = spectator.inject(DotMessageService);
        dotWorkflowService = spectator.inject(DotWorkflowService);
        dotEditContentService = spectator.inject(DotEditContentService);
        dotMessageService.get.mockReturnValue('Success Message');
    });

    describe('methods', () => {
        describe('fireWorkflowAction', () => {
            const mockOptions = {
                inode: '123',
                actionId: MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB[0].id,
                formData: {}
            };

            it('should fire workflow action successfully', fakeAsync(() => {
                const updatedContentlet = { ...MOCK_CONTENTLET_1_TAB, inode: '456' };
                dotEditContentService.getContentById.mockReturnValue(of(updatedContentlet));
                workflowActionsFireService.fireTo.mockReturnValue(of(updatedContentlet));
                workflowActionService.getByInode.mockReturnValue(
                    of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
                );
                dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));

                store.fireWorkflowAction(mockOptions);
                tick();

                expect(store.state()).toBe(ComponentStatus.LOADED);
                expect(store.contentlet()).toEqual(updatedContentlet);
                expect(store.currentContentActions()).toEqual(
                    parseCurrentActions(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
                );

                expect(router.navigate).toHaveBeenCalledWith(
                    ['/content', updatedContentlet.inode],
                    expect.any(Object)
                );
                expect(messageService.add).toHaveBeenCalled();
            }));

            it('should handle error when firing workflow action', fakeAsync(() => {
                const mockError = new HttpErrorResponse({ status: 500 });
                workflowActionsFireService.fireTo.mockReturnValue(throwError(() => mockError));

                store.fireWorkflowAction(mockOptions);
                tick();

                expect(store.state()).toBe(ComponentStatus.LOADED);
                expect(store.error()).toBe('Error firing workflow action');
            }));
        });

        describe('setSelectedWorkflow', () => {
            it('should set selected workflow', () => {
                const newSchemeId = MOCK_WORKFLOW_DATA[0].scheme.id;
                store.setSelectedWorkflow(newSchemeId);
                expect(store.currentSchemeId()).toBe(newSchemeId);
            });
        });
    });

    describe('computed properties', () => {
        describe('getScheme', () => {
            it('should return undefined when no scheme is selected', () => {
                expect(store.getScheme()).toBeUndefined();
            });

            it('should return the correct scheme when one is selected', () => {
                const schemeId = MOCK_WORKFLOW_DATA[0].scheme.id;
                store.setSelectedWorkflow(schemeId);
                expect(store.getScheme()).toEqual(MOCK_WORKFLOW_DATA[0].scheme);
            });
        });

        describe('fireWorkflowAction', () => {
            const mockOptions = {
                inode: '123',
                actionId: MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB[0].id,
                formData: {}
            };

            it('should handle reset action correctly', fakeAsync(() => {
                workflowActionsFireService.fireTo.mockReturnValue(of({} as DotCMSContentlet));
                workflowActionService.getByInode.mockReturnValue(
                    of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
                );

                store.fireWorkflowAction(mockOptions);
                tick();

                expect(store.getCurrentStep()).toBeNull();
                expect(messageService.add).toHaveBeenCalledWith(
                    expect.objectContaining({
                        detail: 'Success Message',
                        icon: 'pi pi-spin pi-spinner',
                        severity: 'info',
                        summary: 'Success Message'
                    })
                );
            }));

            it('should show processing message when action starts', fakeAsync(() => {
                workflowActionsFireService.fireTo.mockReturnValue(of(MOCK_CONTENTLET_1_TAB));

                store.fireWorkflowAction(mockOptions);
                tick();

                expect(messageService.add).toHaveBeenCalledWith(
                    expect.objectContaining({
                        severity: 'info',
                        icon: 'pi pi-spin pi-spinner'
                    })
                );
                tick();
            }));
        });

        describe('getCurrentStep', () => {
            it('should return first step for new content with selected workflow', () => {
                // Set up a new content scenario by selecting a workflow
                const schemeId = MOCK_WORKFLOW_DATA[0].scheme.id;
                store.setSelectedWorkflow(schemeId);

                expect(store.getCurrentStep()).toEqual(MOCK_WORKFLOW_DATA[0].firstStep);
            });

            it('should return current step for existing content', fakeAsync(() => {
                // Mock a workflow action that would update the current step
                const updatedContentlet = { ...MOCK_CONTENTLET_1_TAB, inode: '456' };
                dotEditContentService.getContentById.mockReturnValue(of(updatedContentlet));
                workflowActionsFireService.fireTo.mockReturnValue(of(updatedContentlet));
                workflowActionService.getByInode.mockReturnValue(
                    of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
                );

                store.fireWorkflowAction({
                    inode: '123',
                    actionId: MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB[0].id,
                    formData: {}
                });
                tick();

                expect(store.getCurrentStep()).toBeDefined();
            }));
        });
    });
});
