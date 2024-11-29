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
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { contentInitialState, ContentState } from './content.feature';
import { withWorkflow } from './workflow.feature';

import {
    MOCK_CONTENTLET_1_TAB,
    MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB,
    MOCK_WORKFLOW_DATA
} from '../../../../utils/edit-content.mock';
import { CONTENT_TYPE_MOCK } from '../../../../utils/mocks';
import { parseCurrentActions, parseWorkflows } from '../../../../utils/workflows.utils';
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

    const createStore = createServiceFactory({
        service: signalStore(
            withState({ ...initialRootState, ...mockInitialStateWithContent }),
            withWorkflow()
        ),
        mocks: [
            DotWorkflowsActionsService,
            DotWorkflowActionsFireService,
            DotHttpErrorManagerService,
            DotMessageService,
            MessageService,
            Router
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
                workflowActionsFireService.fireTo.mockReturnValue(of(updatedContentlet));
                workflowActionService.getByInode.mockReturnValue(
                    of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
                );

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
});
