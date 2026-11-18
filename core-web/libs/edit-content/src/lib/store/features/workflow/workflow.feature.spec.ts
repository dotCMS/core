/* eslint-disable @typescript-eslint/no-explicit-any */
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService, SpyObject } from '@openng/spectator/vitest';
import { NEVER, of, throwError } from 'rxjs';
import { expect, vi } from 'vitest';

import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, flush, tick } from '@angular/core/testing';
import { Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotSiteService,
    DotSystemConfigService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { withWorkflow } from './workflow.feature';

import { CurrentContentActionsWithScheme } from '../../../models/dot-edit-content-field.type';
import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { EDIT_CONTENT_HOST } from '../../../services/host/edit-content-host.model';
import {
    MOCK_CONTENTLET_1_TAB,
    MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB,
    MOCK_WORKFLOW_DATA,
    MOCK_WORKFLOW_STATUS
} from '../../../utils/edit-content.mock';
import { parseCurrentActions } from '../../../utils/workflows.utils';
import { initialRootState } from '../../edit-content.store';
import { withContent } from '../content/content.feature';

type DeleteFlags = { hasDeleteActionlet?: boolean; hasDestroyActionlet?: boolean };

describe('WorkflowFeature', () => {
    let spectator: SpectatorService<any>;
    let store: any;
    let workflowActionService: SpyObject<DotWorkflowsActionsService>;
    let workflowActionsFireService: SpyObject<DotWorkflowActionsFireService>;
    let messageService: SpyObject<MessageService>;
    let dotMessageService: SpyObject<DotMessageService>;
    let dotWorkflowService: SpyObject<DotWorkflowService>;
    let dotEditContentService: SpyObject<DotEditContentService>;

    // Post-save navigation is delegated to the EditContentHost port.
    const mockHost = {
        setContentTitle: vi.fn(),
        addBreadcrumb: vi.fn(),
        goToSavedContent: vi.fn(),
        leaveDeletedContent: vi.fn(),
        goToRestoredVersion: vi.fn()
    };

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
                updateContent: (content: DotCMSContentlet) => {
                    patchState(store, { contentlet: content });
                },
                setCurrentContentActions: (
                    currentContentActions: CurrentContentActionsWithScheme
                ) => {
                    patchState(store, { currentContentActions });
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
            DotContentTypeService,
            DotSiteService,
            DotSystemConfigService,
            GlobalStore
        ],
        providers: [
            { provide: EDIT_CONTENT_HOST, useValue: mockHost },
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        Object.values(mockHost).forEach((fn) => fn.mockClear());
        spectator = createStore();
        store = spectator.service;
        workflowActionService = spectator.inject(DotWorkflowsActionsService);
        workflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
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

                expect(mockHost.goToSavedContent).toHaveBeenCalled();
                const [savedArg] = mockHost.goToSavedContent.mock.calls[0];
                expect(savedArg).toEqual({
                    inode: updatedContentlet.inode,
                    title: updatedContentlet.title
                });
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

            describe('when the action deletes the content', () => {
                const deletedContentlet = { ...MOCK_CONTENTLET_1_TAB, inode: '123' };

                const withDeletingAction = (flags: DeleteFlags) => {
                    store.updateContent(deletedContentlet);
                    store.setCurrentContentActions({
                        'scheme-1': [{ ...MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB[0], ...flags }]
                    });
                    // Reset calls made by the contentlet-change effect.
                    workflowActionService.getByInode.mockClear();
                    dotEditContentService.getContentById.mockClear();
                    dotWorkflowService.getWorkflowStatus.mockClear();
                };

                it.each([
                    ['Delete', { hasDeleteActionlet: true }],
                    ['Destroy', { hasDestroyActionlet: true }]
                ])(
                    'should leave the editor without reloading the content after %s',
                    fakeAsync((_name: string, flags: DeleteFlags) => {
                        withDeletingAction(flags);
                        workflowActionsFireService.fireTo.mockReturnValue(
                            of({} as DotCMSContentlet)
                        );

                        store.fireWorkflowAction(mockOptions);
                        tick();
                        flush();

                        expect(mockHost.leaveDeletedContent).toHaveBeenCalledWith(
                            deletedContentlet.contentType
                        );
                        expect(mockHost.goToSavedContent).not.toHaveBeenCalled();
                        expect(dotEditContentService.getContentById).not.toHaveBeenCalled();
                        expect(workflowActionService.getByInode).not.toHaveBeenCalled();
                        expect(dotWorkflowService.getWorkflowStatus).not.toHaveBeenCalled();
                        expect(store.state()).toBe(ComponentStatus.LOADED);
                        expect(store.workflowActionSuccess()).toEqual(deletedContentlet);
                        expect(messageService.add).toHaveBeenCalledWith(
                            expect.objectContaining({ severity: 'success' })
                        );
                    })
                );

                it('should keep the error handling and stay on the content when the delete fails', fakeAsync(() => {
                    withDeletingAction({ hasDeleteActionlet: true });
                    const mockError = new HttpErrorResponse({ status: 500 });
                    workflowActionsFireService.fireTo.mockReturnValue(throwError(() => mockError));

                    store.fireWorkflowAction(mockOptions);
                    tick();

                    expect(
                        spectator.inject(DotHttpErrorManagerService).handle
                    ).toHaveBeenCalledWith(mockError);
                    expect(store.error()).toBe('Error firing workflow action');
                    expect(mockHost.leaveDeletedContent).not.toHaveBeenCalled();
                }));

                it('should keep the reload path for actions that do not delete the content', fakeAsync(() => {
                    withDeletingAction({ hasDeleteActionlet: false, hasDestroyActionlet: false });
                    const savedContentlet = { ...deletedContentlet, inode: '456' };
                    workflowActionsFireService.fireTo.mockReturnValue(of(savedContentlet));
                    dotEditContentService.getContentById.mockReturnValue(of(savedContentlet));
                    dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));

                    store.fireWorkflowAction(mockOptions);
                    tick();
                    flush();

                    expect(dotEditContentService.getContentById).toHaveBeenCalled();
                    expect(mockHost.goToSavedContent).toHaveBeenCalled();
                    expect(mockHost.leaveDeletedContent).not.toHaveBeenCalled();
                }));
            });
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

                // The actions re-fetch should settle into a non-loading state
                expect(store.actionsStatus().status).toBe(ComponentStatus.LOADED);
                expect(store.isLoadingActions()).toBe(false);
            }));

            it('should flag the actions as loading while they are being re-fetched', fakeAsync(() => {
                workflowActionService.getByInode.mockClear();
                // A request that never resolves keeps the re-fetch in flight
                workflowActionService.getByInode.mockReturnValue(NEVER);

                store.updateContent({ ...MOCK_CONTENTLET_1_TAB, inode: '789' });

                spectator.flushEffects();

                expect(store.actionsStatus().status).toBe(ComponentStatus.LOADING);
                expect(store.isLoadingActions()).toBe(true);
            }));

            it('should clear the loading flag when the actions re-fetch fails', fakeAsync(() => {
                workflowActionService.getByInode.mockClear();
                workflowActionService.getByInode.mockReturnValue(
                    throwError(() => new HttpErrorResponse({ status: 500 }))
                );

                store.updateContent({ ...MOCK_CONTENTLET_1_TAB, inode: '999' });

                spectator.flushEffects();
                tick();

                // A failed re-fetch must not leave the workflow actions disabled forever
                expect(store.actionsStatus().status).toBe(ComponentStatus.ERROR);
                expect(store.isLoadingActions()).toBe(false);
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
