/* eslint-disable @typescript-eslint/no-explicit-any */
import { expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, flush, tick } from '@angular/core/testing';
import { Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';

import { withWorkflow } from './workflow.feature';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import {
    MOCK_CONTENTLET_1_TAB,
    MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB,
    MOCK_WORKFLOW_DATA,
    MOCK_WORKFLOW_STATUS
} from '../../../utils/edit-content.mock';
import { parseCurrentActions } from '../../../utils/workflows.utils';
import { initialRootState } from '../../edit-content.store';
import { withContent } from '../content/content.feature';

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
            withState({
                ...initialRootState,
                schemes: {
                    [MOCK_WORKFLOW_DATA[0].scheme.id]: {
                        scheme: MOCK_WORKFLOW_DATA[0].scheme,
                        actions: [MOCK_WORKFLOW_DATA[0].action],
                        firstStep: MOCK_WORKFLOW_DATA[0].firstStep
                    }
                }
            }),
            withContent(),
            withWorkflow(),
            withMethods((store) => ({
                updateContent: (content) => {
                    patchState(store, { contentlet: content });
                }
            }))
        ),
        mocks: [
            DotEditContentService,
            DotWorkflowsActionsService,
            DotWorkflowActionsFireService,
            DotHttpErrorManagerService,
            DotMessageService,
            MessageService,
            Router,
            DotWorkflowService,
            DotContentTypeService
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

        // Initialize the workflow actions
        workflowActionService.getByInode.mockReturnValue(
            of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
        );
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
                dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
                store.initializeExistingContent(MOCK_CONTENTLET_1_TAB.inode);

                store.fireWorkflowAction(mockOptions);
                tick();
                flush();

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

            it('should handle reset action correctly', fakeAsync(() => {
                // Initialize contentlet first
                const mockContentlet = { ...MOCK_CONTENTLET_1_TAB, inode: '123' };
                store.updateContent(mockContentlet);

                workflowActionsFireService.fireTo.mockReturnValue(of({} as DotCMSContentlet));
                dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
                workflowActionService.getByInode.mockReturnValue(of([]));
                dotEditContentService.getContentById.mockReturnValue(of(mockContentlet));

                store.fireWorkflowAction(mockOptions);
                tick();
                flush();

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

            it('should show processing message when action starts', fakeAsync(() => {
                workflowActionsFireService.fireTo.mockReturnValue(of(MOCK_CONTENTLET_1_TAB));

                store.fireWorkflowAction(mockOptions);
                tick();
                flush();

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
                dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));

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

    describe('effects', () => {
        describe('contentlet change effect', () => {
            it('should automatically update workflow actions when contentlet changes', fakeAsync(() => {
                // Reset any previous calls
                workflowActionService.getByInode.mockClear();

                // Update the contentlet using the proper method
                const updatedContentlet = { ...MOCK_CONTENTLET_1_TAB, inode: '456' };

                // Use the updateContent method to update the contentlet
                store.updateContent(updatedContentlet);

                spectator.flushEffects();

                // Verify the effect called updateCurrentContentActions
                expect(workflowActionService.getByInode).toHaveBeenCalledWith(
                    updatedContentlet.inode,
                    'EDITING'
                );

                // Verify the state was updated with the new actions
                expect(store.currentContentActions()).toEqual(
                    parseCurrentActions(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
                );
            }));

            it('should not update workflow actions when contentlet has no inode', fakeAsync(() => {
                // Setup
                workflowActionService.getByInode.mockClear();

                // Update with a contentlet that has no inode
                const contentletWithoutInode = { ...MOCK_CONTENTLET_1_TAB, inode: '' };

                // Use the updateContent method to update the contentlet
                store.updateContent(contentletWithoutInode);

                tick();

                // Verify the service was not called
                expect(workflowActionService.getByInode).not.toHaveBeenCalled();
            }));
        });
    });
});
