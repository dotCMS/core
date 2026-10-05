/* eslint-disable @typescript-eslint/no-explicit-any */

import { signalStore, withState, patchState } from '@ngrx/signals';
import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@openng/spectator/vitest';
import { NEVER, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotSiteService,
    DotSystemConfigService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotCMSWorkflowAction,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import {
    DOT_SYSTEM_CONFIG_SERVICE_MOCK,
    MOCK_SINGLE_WORKFLOW_ACTIONS
} from '@dotcms/utils-testing';

import { withContent } from './content.feature';

import { EDIT_CONTENT_NAVIGATION_OVERRIDE } from '../../../models/edit-content-navigation-override';
import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { EDIT_CONTENT_HOST } from '../../../services/host/edit-content-host.model';
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
    let dotMessageService: SpyObject<DotMessageService>;

    // Chrome/navigation concerns are delegated to the EditContentHost port.
    // In full-screen the RouterEditContentHost fulfils these; here we assert
    // the feature states the intent (title/breadcrumb) against the port.
    const mockHost = {
        setContentTitle: vi.fn(),
        addBreadcrumb: vi.fn(),
        goToSavedContent: vi.fn(),
        leaveDeletedContent: vi.fn(),
        goToRestoredVersion: vi.fn()
    };

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
            DotMessageService
        ],
        providers: [
            mockProvider(Router, {
                navigate: vi.fn().mockReturnValue(Promise.resolve(true)),
                url: '/test-url',
                events: of()
            }),
            mockProvider(DotSiteService),
            mockProvider(DotSystemConfigService, DOT_SYSTEM_CONFIG_SERVICE_MOCK),
            { provide: EDIT_CONTENT_HOST, useValue: mockHost },
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        Object.values(mockHost).forEach((fn) => fn.mockClear());
        spectator = createStore();
        store = spectator.service;
        contentTypeService = spectator.inject(DotContentTypeService);
        dotEditContentService = spectator.inject(DotEditContentService);
        workflowActionService = spectator.inject(DotWorkflowsActionsService);
        workflowService = spectator.inject(DotWorkflowService);
        router = spectator.inject(Router);
        dotMessageService = spectator.inject(DotMessageService);

        dotMessageService.get.mockImplementation((key) => {
            const messages = {
                New: 'New',
                'dotcms.content.management.platform.title': 'DotCMS'
            };

            return messages[key] || key;
        });
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
            contentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getByInode.mockReturnValue(of([]));
            workflowActionService.getWorkFlowActions.mockReturnValue(of([]));
            workflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));

            store.initializeExistingContent('123');
            tick();

            expect(store.isNew()).toBe(false);
        }));

        it('should return correct computed values for new content', fakeAsync(() => {
            contentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeNewContent('testContentType');
            tick();

            const parsedSchemes = parseWorkflows(MOCK_SINGLE_WORKFLOW_ACTIONS);
            expect(store.schemes()).toEqual(parsedSchemes);
            expect(store.currentSchemeId()).toBe(MOCK_SINGLE_WORKFLOW_ACTIONS[0].scheme.id);
        }));

        // #36883: this is the seeding point the reported bug takes — a single-scheme content
        // type auto-selects its scheme here and seeds `currentContentActions` straight from the
        // default-actions payload. The missing `actionInputs` is fixed in
        // DotWorkflowsActionsService (which is mocked here), so this asserts the remaining half:
        // that the store's reshape carries the array through instead of dropping it.
        it('should carry actionInputs through into currentContentActions for new content', fakeAsync(() => {
            const commentableActions = [
                {
                    ...MOCK_SINGLE_WORKFLOW_ACTIONS[0],
                    action: {
                        ...MOCK_SINGLE_WORKFLOW_ACTIONS[0].action,
                        commentable: true,
                        actionInputs: [{ id: 'commentable', body: {} }]
                    }
                }
            ];
            contentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getDefaultActions.mockReturnValue(of(commentableActions));

            store.initializeNewContent('testContentType');
            tick();

            const schemeId = commentableActions[0].scheme.id;
            expect(store.currentSchemeId()).toBe(schemeId);
            expect(store.currentContentActions()[schemeId][0].actionInputs).toEqual([
                { id: 'commentable', body: {} }
            ]);
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
            contentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
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
            contentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
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
            contentTypeService.getContentTypeWithRender.mockReturnValue(
                throwError(() => mockError)
            );

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
            contentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
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

            contentTypeService.getContentTypeWithRender.mockReturnValue(
                of(contentTypeWithoutEditor)
            );
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

            contentTypeService.getContentTypeWithRender.mockReturnValue(of(contentTypeWithEditor));
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
            contentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
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

            expect(contentTypeService.getContentTypeWithRender).toHaveBeenCalledWith(
                'testContentType'
            );
            expect(workflowActionService.getDefaultActions).toHaveBeenCalledWith('testContentType');

            const parsedSchemes = parseWorkflows(MOCK_SINGLE_WORKFLOW_ACTIONS);

            expect(store.contentType()).toEqual(CONTENT_TYPE_MOCK);
            expect(store.state()).toBe(ComponentStatus.LOADED);
            expect(store.schemes()).toEqual(parsedSchemes);
            expect(store.currentSchemeId()).toBe(MOCK_SINGLE_WORKFLOW_ACTIONS[0].scheme.id);
        }));

        it('should set the title and breadcrumb via the host for new content', fakeAsync(() => {
            store.initializeNewContent('testContentType');
            tick();

            expect(dotMessageService.get).toHaveBeenCalledWith('New');
            // The feature passes the raw label; the host appends any platform suffix.
            expect(mockHost.setContentTitle).toHaveBeenCalledWith('New Test');
            expect(mockHost.addBreadcrumb).toHaveBeenCalledWith({
                label: 'New Test',
                url: '/dotAdmin/#/content/new/Test'
            });
        }));

        it('should reset hiddenFields immediately when initializing new content', fakeAsync(() => {
            contentTypeService.getContentTypeWithRender.mockReturnValue(NEVER);
            patchState(store, { hiddenFields: { field1: true, field2: true } });

            store.initializeNewContent('testContentType');

            expect(store.hiddenFields()).toEqual({});
            expect(store.state()).toBe(ComponentStatus.LOADING);

            tick();
        }));

        it('should handle error when initializing new content', fakeAsync(() => {
            const mockError = new HttpErrorResponse({ status: 404 });
            contentTypeService.getContentTypeWithRender.mockReturnValue(
                throwError(() => mockError)
            );

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
            contentType: 'testContentType',
            title: 'Test Content Title'
        } as DotCMSContentlet;

        const mockActions = [{ id: '1', name: 'Test Action' }] as DotCMSWorkflowAction[];

        beforeEach(() => {
            dotEditContentService.getContentById.mockReturnValue(of(mockContentlet));
            contentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getByInode.mockReturnValue(of(mockActions));
            workflowActionService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
        });

        it('should initialize existing content successfully', fakeAsync(() => {
            store.initializeExistingContent({ inode: '123' });
            tick();

            expect(store.contentlet()).toEqual(mockContentlet);
            expect(store.contentType()).toEqual(CONTENT_TYPE_MOCK);
            expect(store.currentContentActions()).toEqual(parseCurrentActions(mockActions));
            expect(store.state()).toBe(ComponentStatus.LOADED);
        }));

        it('should reset hiddenFields immediately when initializing existing content', fakeAsync(() => {
            dotEditContentService.getContentById.mockReturnValue(NEVER);
            patchState(store, { hiddenFields: { field1: true, field2: true } });

            store.initializeExistingContent({ inode: '123' });

            expect(store.hiddenFields()).toEqual({});
            expect(store.state()).toBe(ComponentStatus.LOADING);

            tick();
        }));

        it('should set the title and breadcrumb via the host for existing content', fakeAsync(() => {
            store.initializeExistingContent({ inode: '123' });
            tick();

            expect(mockHost.setContentTitle).toHaveBeenCalledWith('Test Content Title');
            expect(mockHost.addBreadcrumb).toHaveBeenCalledWith({
                label: 'Test Content Title',
                url: `/dotAdmin/#/content/${testInode}`
            });
        }));

        it('should handle error when initializing existing content', fakeAsync(() => {
            const mockError = new HttpErrorResponse({ status: 404 });
            dotEditContentService.getContentById.mockReturnValue(throwError(() => mockError));

            store.initializeExistingContent({ inode: '123' });
            tick();
            expect(store.state()).toBe(ComponentStatus.ERROR);
            expect(store.error()).toBe(
                'edit.content.sidebar.information.error.initializing.content'
            );

            expect(router.navigate).toHaveBeenCalledWith(['/c/content']);
        }));

        it('should pass inode to getContentTypeWithRender for Velocity variable resolution', fakeAsync(() => {
            store.initializeExistingContent({ inode: testInode });
            tick();

            expect(contentTypeService.getContentTypeWithRender).toHaveBeenCalledWith(
                mockContentlet.contentType,
                testInode
            );
        }));

        it('should set initialContentletState to reset when no scheme or step', fakeAsync(() => {
            const mockContentlet = {
                inode: '123',
                contentType: 'testContentType',
                title: 'Test Content Title'
            } as DotCMSContentlet;

            const workflowStatusWithoutScheme = {
                ...MOCK_WORKFLOW_STATUS,
                scheme: null,
                step: null
            };

            dotEditContentService.getContentById.mockReturnValue(of(mockContentlet));
            contentTypeService.getContentTypeWithRender.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionService.getByInode.mockReturnValue(of([]));
            workflowActionService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowService.getWorkflowStatus.mockReturnValue(of(workflowStatusWithoutScheme));

            store.initializeExistingContent({ inode: '123' });
            tick();

            expect(store.initialContentletState()).toBe('reset');
        }));
    });

    describe('disableNewContentEditor', () => {
        const mockContentlet = {
            inode: '123',
            stInode: 'st-123',
            contentType: 'testContentType'
        } as any;

        beforeEach(() => {
            // Set up the store to have a contentlet
            patchState(store, { contentlet: mockContentlet });
        });

        it('should call updateContentType and navigate to legacy edit page on success', fakeAsync(() => {
            // Arrange
            const workflow1 = {
                archived: false,
                creationDate: new Date(),
                defaultScheme: false,
                description: 'desc',
                entryActionId: null,
                id: 'workflow-1',
                mandatory: false,
                modDate: new Date(),
                name: 'Workflow 1',
                system: false
            };
            const workflow2 = {
                archived: false,
                creationDate: new Date(),
                defaultScheme: false,
                description: 'desc2',
                entryActionId: null,
                id: 'workflow-2',
                mandatory: false,
                modDate: new Date(),
                name: 'Workflow 2',
                system: false
            };
            const contentType = {
                ...CONTENT_TYPE_MOCK,
                id: 'st-123',
                workflows: [workflow1, workflow2],
                metadata: { foo: 'bar', CONTENT_EDITOR2_ENABLED: true }
            };
            patchState(store, { contentType });
            contentTypeService.updateContentType.mockReturnValue(of(contentType));

            // Act
            store.disableNewContentEditor();
            tick();

            // Assert
            expect(contentTypeService.updateContentType).toHaveBeenCalledWith('st-123', {
                ...contentType,
                metadata: {
                    ...contentType.metadata,
                    CONTENT_EDITOR2_ENABLED: false
                },
                workflow: contentType.workflows.map((w: any) => w.id)
            });
            expect(router.navigate).toHaveBeenCalledWith([`/c/content/`, '123']);
        }));
    });
});

/**
 * An opener can take over how the editor leaves for the legacy editor or after a load error
 * (#37759, FR-028, FR-029). Only Content Drive provides the override; without it the editor
 * navigates exactly as above. The override answers for the editor its opener opened, so it gets
 * what this editor was opened with, and declines for any other one.
 */
describe('ContentFeature - with a navigation override', () => {
    let spectator: SpectatorService<any>;
    let store: any;
    let contentTypeService: SpyObject<DotContentTypeService>;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let router: SpyObject<Router>;

    const OPENED = { inode: 'opened-inode', contentTypeId: undefined };
    const override = { switchToLegacyEditor: vi.fn(), leaveOnLoadError: vi.fn() };
    const mockHost = {
        setContentTitle: vi.fn(),
        addBreadcrumb: vi.fn(),
        goToSavedContent: vi.fn(),
        leaveDeletedContent: vi.fn(),
        goToRestoredVersion: vi.fn(),
        resolveIdentity: vi.fn().mockReturnValue(OPENED)
    };

    const createStore = createServiceFactory({
        service: signalStore(withState({ ...initialRootState }), withContent()),
        mocks: [
            DotContentTypeService,
            DotEditContentService,
            DotHttpErrorManagerService,
            DotWorkflowsActionsService,
            DotWorkflowService,
            DotMessageService
        ],
        providers: [
            mockProvider(Router, {
                navigate: vi.fn().mockReturnValue(Promise.resolve(true)),
                url: '/test-url',
                events: of()
            }),
            mockProvider(DotSiteService),
            mockProvider(DotSystemConfigService, DOT_SYSTEM_CONFIG_SERVICE_MOCK),
            { provide: EDIT_CONTENT_HOST, useValue: mockHost },
            { provide: EDIT_CONTENT_NAVIGATION_OVERRIDE, useValue: override },
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        override.switchToLegacyEditor.mockReset();
        override.leaveOnLoadError.mockReset();
        spectator = createStore();
        store = spectator.service;
        contentTypeService = spectator.inject(DotContentTypeService);
        dotEditContentService = spectator.inject(DotEditContentService);
        router = spectator.inject(Router);
        // The Router mock is created once with the factory, so its calls carry over between tests.
        router.navigate.mockClear();
    });

    describe('switch to the old editor', () => {
        const contentlet = { inode: '123', identifier: 'id-1' } as DotCMSContentlet;
        const contentType = { ...CONTENT_TYPE_MOCK, id: 'st-123', variable: 'Blog', workflows: [] };

        beforeEach(() => {
            patchState(store, { contentlet, contentType });
            contentTypeService.updateContentType.mockReturnValue(of(contentType));
        });

        it('lets the override reopen the content, with what the editor was opened with', fakeAsync(() => {
            override.switchToLegacyEditor.mockReturnValue(true);

            store.disableNewContentEditor();
            tick();

            expect(override.switchToLegacyEditor).toHaveBeenCalledWith(OPENED, contentlet, 'Blog');
            expect(router.navigate).not.toHaveBeenCalled();
        }));

        it("navigates as main does when the override declines (another opener's editor)", fakeAsync(() => {
            override.switchToLegacyEditor.mockReturnValue(false);

            store.disableNewContentEditor();
            tick();

            expect(router.navigate).toHaveBeenCalledWith([`/c/content/`, '123']);
        }));

        it('lets the override reopen a create that was never saved', fakeAsync(() => {
            override.switchToLegacyEditor.mockReturnValue(true);
            patchState(store, { contentlet: null });

            store.disableNewContentEditor();
            tick();

            expect(override.switchToLegacyEditor).toHaveBeenCalledWith(OPENED, null, 'Blog');
            expect(router.navigate).not.toHaveBeenCalled();
        }));
    });

    describe('load error', () => {
        const error = new HttpErrorResponse({ status: 404 });

        beforeEach(() => {
            dotEditContentService.getContentById.mockReturnValue(throwError(() => error));
        });

        it('shows the standard error and lets the override close its editor', fakeAsync(() => {
            override.leaveOnLoadError.mockReturnValue(true);

            store.initializeExistingContent({ inode: '123' });
            tick();

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
            expect(override.leaveOnLoadError).toHaveBeenCalledWith(OPENED);
            expect(router.navigate).not.toHaveBeenCalled();
        }));

        it("navigates as main does when the override declines (another opener's editor)", fakeAsync(() => {
            override.leaveOnLoadError.mockReturnValue(false);

            store.initializeExistingContent({ inode: '123' });
            tick();

            expect(router.navigate).toHaveBeenCalledWith(['/c/content']);
        }));
    });
});
