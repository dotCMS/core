/* eslint-disable @typescript-eslint/no-explicit-any */

import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotCMSWorkflowAction,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { MOCK_SINGLE_WORKFLOW_ACTIONS } from '@dotcms/utils-testing';

import { withContent } from './content.feature';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { MOCK_WORKFLOW_STATUS } from '../../../utils/edit-content.mock';
import { CONTENT_TYPE_MOCK } from '../../../utils/mocks';
import { parseCurrentActions, parseWorkflows } from '../../../utils/workflows.utils';
import { initialRootState } from '../../edit-content.store';

describe('ContentFeature', () => {
    let spectator: SpectatorService<any>;

    let store: any;
    let contentTypeService: SpyObject<DotContentTypeService>;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let workflowActionService: SpyObject<DotWorkflowsActionsService>;
    let workflowService: SpyObject<DotWorkflowService>;
    let router: SpyObject<Router>;

    const createStore = createServiceFactory({
        service: signalStore(
            withState({ ...initialRootState, ...initialRootState }),
            withContent()
        ),
        mocks: [
            DotContentTypeService,
            DotEditContentService,
            DotHttpErrorManagerService,
            DotWorkflowsActionsService,
            DotWorkflowService,
            Router
        ]
    });

    beforeEach(() => {
        spectator = createStore();
        store = spectator.service;
        contentTypeService = spectator.inject(DotContentTypeService);
        dotEditContentService = spectator.inject(DotEditContentService);
        workflowActionService = spectator.inject(DotWorkflowsActionsService);
        workflowService = spectator.inject(DotWorkflowService);
        router = spectator.inject(Router);
    });

    describe('computed properties', () => {
        it('should return isNew as true when no contentlet exists', () => {
            expect(store.isNew()).toBe(true);
        });

        it('should return isNew as false when contentlet exists', fakeAsync(() => {
            const mockContentlet = {
                inode: '123',
                contentType: 'testContentType'
            } as DotCMSContentlet;

            dotEditContentService.getContentById.mockReturnValue(of(mockContentlet));
            contentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getByInode.mockReturnValue(of([]));
            workflowActionService.getWorkFlowActions.mockReturnValue(of([]));
            workflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));

            store.initializeExistingContent('123');
            tick();

            expect(store.isNew()).toBe(false);
        }));

        it('should return correct computed values for new content', fakeAsync(() => {
            contentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeNewContent('testContentType');
            tick();

            const parsedSchemes = parseWorkflows(MOCK_SINGLE_WORKFLOW_ACTIONS);
            expect(store.schemes()).toEqual(parsedSchemes);
            expect(store.currentSchemeId()).toBe(MOCK_SINGLE_WORKFLOW_ACTIONS[0].scheme.id);
        }));

        it('should return correct computed values for existing content', fakeAsync(() => {
            const mockContentlet = {
                inode: '123',
                contentType: 'testContentType'
            } as DotCMSContentlet;

            const expectedActions = [
                {
                    actionInputs: [],
                    assignable: false,
                    commentable: false,
                    condition: '',
                    icon: 'workflowIcon',
                    id: 'ceca71a0-deee-4999-bd47-b01baa1bcfc8',
                    metadata: null,
                    name: 'Save',
                    nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
                    nextStep: 'ee24a4cb-2d15-4c98-b1bd-6327126451f3',
                    nextStepCurrentStep: false,
                    order: 0,
                    owner: null,
                    roleHierarchyForAssign: false,
                    schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
                    showOn: ['NEW', 'EDITING', 'LOCKED', 'PUBLISHED', 'UNPUBLISHED']
                }
            ];

            dotEditContentService.getContentById.mockReturnValue(of(mockContentlet));
            contentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getByInode.mockReturnValue(of(expectedActions));
            workflowActionService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));

            store.initializeExistingContent('123');

            tick();

            // Verify all the expected values
            expect(store.contentlet()).toEqual(mockContentlet);
            expect(store.contentType()).toEqual(CONTENT_TYPE_MOCK);
            expect(store.currentContentActions()).toEqual(parseCurrentActions(expectedActions));
            expect(store.schemes()).toEqual(parseWorkflows(MOCK_SINGLE_WORKFLOW_ACTIONS));
        }));

        it('should return isLoaded as true when state is LOADED', fakeAsync(() => {
            contentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeNewContent('testContentType');
            tick();

            expect(store.isLoaded()).toBe(true);
        }));

        it('should return hasError as true when error exists', fakeAsync(() => {
            const mockError = new HttpErrorResponse({ status: 404 });
            workflowActionService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            contentTypeService.getContentType.mockReturnValue(throwError(() => mockError));

            store.initializeNewContent('testContentType');
            tick();

            expect(store.hasError()).toBe(true);
        }));

        it('should return correct formData', fakeAsync(() => {
            const mockContentlet = {
                inode: '123',
                contentType: 'testContentType'
            } as DotCMSContentlet;

            dotEditContentService.getContentById.mockReturnValue(of(mockContentlet));
            contentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getByInode.mockReturnValue(of([]));
            workflowActionService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));

            store.initializeExistingContent('123');
            tick();

            expect(store.formData()).toEqual({
                contentlet: mockContentlet,
                contentType: CONTENT_TYPE_MOCK
            });
        }));

        it('should return isEnabledNewContentEditor based on content type metadata', fakeAsync(() => {
            // Test when feature flag is false
            const contentTypeWithoutEditor = {
                ...CONTENT_TYPE_MOCK,
                metadata: {
                    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: false
                }
            };

            contentTypeService.getContentType.mockReturnValue(of(contentTypeWithoutEditor));
            workflowActionService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeNewContent('testContentType');
            tick();

            expect(store.isEnabledNewContentEditor()).toBe(false);

            // Test when feature flag is true
            const contentTypeWithEditor = {
                ...CONTENT_TYPE_MOCK,
                metadata: {
                    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
                }
            };

            contentTypeService.getContentType.mockReturnValue(of(contentTypeWithEditor));
            workflowActionService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeNewContent('testContentType');
            tick();

            expect(store.isEnabledNewContentEditor()).toBe(true);
        }));
    });

    describe('initializeNewContent', () => {
        beforeEach(() => {
            contentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
            workflowActionService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowActionService.getByInode.mockReturnValue(of([]));
        });

        it('should initialize new content successfully', fakeAsync(() => {
            store.initializeNewContent('testContentType');
            tick();

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('testContentType');
            expect(workflowActionService.getDefaultActions).toHaveBeenCalledWith('testContentType');

            const parsedSchemes = parseWorkflows(MOCK_SINGLE_WORKFLOW_ACTIONS);

            expect(store.contentType()).toEqual(CONTENT_TYPE_MOCK);
            expect(store.state()).toBe(ComponentStatus.LOADED);
            expect(store.schemes()).toEqual(parsedSchemes);
            expect(store.currentSchemeId()).toBe(MOCK_SINGLE_WORKFLOW_ACTIONS[0].scheme.id);
        }));

        it('should handle error when initializing new content', fakeAsync(() => {
            const mockError = new HttpErrorResponse({ status: 404 });
            contentTypeService.getContentType.mockReturnValue(throwError(() => mockError));

            store.initializeNewContent('testContentType');
            tick();

            expect(store.state()).toBe(ComponentStatus.ERROR);
            expect(store.error()).toBe(
                'edit.content.sidebar.information.error.initializing.content'
            );
        }));
    });

    describe('initializeExistingContent', () => {
        const testInode = '123-test-inode';
        const mockContentlet = {
            inode: testInode,
            contentType: 'testContentType'
        } as DotCMSContentlet;

        const mockActions = [{ id: '1', name: 'Test Action' }] as DotCMSWorkflowAction[];

        beforeEach(() => {
            dotEditContentService.getContentById.mockReturnValue(of(mockContentlet));
            contentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getByInode.mockReturnValue(of(mockActions));
            workflowActionService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
        });

        it('should initialize existing content successfully', fakeAsync(() => {
            store.initializeExistingContent('123');
            tick();

            expect(store.contentlet()).toEqual(mockContentlet);
            expect(store.contentType()).toEqual(CONTENT_TYPE_MOCK);
            expect(store.currentContentActions()).toEqual(parseCurrentActions(mockActions));
            expect(store.state()).toBe(ComponentStatus.LOADED);
        }));

        it('should handle error when initializing existing content', fakeAsync(() => {
            const mockError = new HttpErrorResponse({ status: 404 });
            dotEditContentService.getContentById.mockReturnValue(throwError(() => mockError));

            store.initializeExistingContent('123');
            tick();
            expect(store.state()).toBe(ComponentStatus.ERROR);
            expect(store.error()).toBe(
                'edit.content.sidebar.information.error.initializing.content'
            );

            expect(router.navigate).toHaveBeenCalledWith(['/c/content']);
        }));

        it('should set initialContentletState to reset when no scheme or step', fakeAsync(() => {
            const mockContentlet = {
                inode: '123',
                contentType: 'testContentType'
            } as DotCMSContentlet;

            const workflowStatusWithoutScheme = {
                ...MOCK_WORKFLOW_STATUS,
                scheme: null,
                step: null
            };

            dotEditContentService.getContentById.mockReturnValue(of(mockContentlet));
            contentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getByInode.mockReturnValue(of([]));
            workflowActionService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowService.getWorkflowStatus.mockReturnValue(of(workflowStatusWithoutScheme));

            store.initializeExistingContent('123');
            tick();

            expect(store.initialContentletState()).toBe('reset');
        }));
    });
});
