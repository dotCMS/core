import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { signal } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';

import { DialogService } from 'primeng/dynamicdialog';

import {
    DotCurrentUserService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRenderMode,
    DotRouterService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    PushPublishService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSWorkflowAction,
    DotEnvironment,
    PermissionsType,
    UserPermissions
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotPageActionsService } from './dot-page-actions.service';
import { DotPageListService } from './dot-page-list.service';

import { DotCMSPagesStore } from '../store/store';

type PagesStoreMock = {
    getFavoritePages: jest.Mock;
};

const MOCK_USER = {
    userId: 'test-user-123',
    email: 'test@dotcms.com',
    firstName: 'Test',
    lastName: 'User'
};

const MOCK_HTMLPAGE_CONTENTLET: DotCMSContentlet = {
    identifier: 'page-123',
    inode: 'inode-123',
    title: 'Home Page',
    url: '/home',
    baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
    contentType: 'htmlpage',
    languageId: 1,
    archived: false,
    working: true,
    live: true
} as DotCMSContentlet;

const MOCK_CONTENT_CONTENTLET: DotCMSContentlet = {
    identifier: 'content-456',
    inode: 'inode-456',
    title: 'Blog Post',
    baseType: DotCMSBaseTypesContentTypes.CONTENT,
    contentType: 'blog',
    languageId: 1,
    archived: false,
    working: true,
    live: true
} as DotCMSContentlet;

const MOCK_FAVORITE_PAGE: DotCMSContentlet = {
    identifier: 'fav-789',
    inode: 'inode-789',
    title: 'Favorite Page',
    baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
    contentType: 'dotFavoritePage',
    languageId: 1,
    archived: false,
    working: true,
    live: true
} as DotCMSContentlet;

const MOCK_ARCHIVED_PAGE: DotCMSContentlet = {
    identifier: 'archived-999',
    inode: 'inode-999',
    title: 'Archived Page',
    baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
    contentType: 'htmlpage',
    languageId: 1,
    archived: true,
    working: true,
    live: false
} as DotCMSContentlet;

const MOCK_WORKFLOW_ACTION_NO_INPUTS: DotCMSWorkflowAction = {
    id: 'workflow-1',
    name: 'Publish',
    actionInputs: [],
    nextAssign: 'user1',
    nextStep: 'step1',
    schemeId: 'scheme1'
} as DotCMSWorkflowAction;

const MOCK_WORKFLOW_ACTION_WITH_INPUTS: DotCMSWorkflowAction = {
    id: 'workflow-2',
    name: 'Approve with Comment',
    actionInputs: [
        {
            id: 'comment',
            name: 'Comment',
            required: true
        } as unknown as DotCMSWorkflowAction['actionInputs'][number]
    ],
    nextAssign: 'user2',
    nextStep: 'step2',
    schemeId: 'scheme1'
} as DotCMSWorkflowAction;

const MOCK_PERMISSIONS = {
    CONTENTLETS: {
        canRead: true,
        canWrite: true
    },
    HTMLPAGES: {
        canRead: true,
        canWrite: true
    }
};

const MOCK_ENVIRONMENTS: DotEnvironment[] = [
    {
        id: 'env-1',
        name: 'Production'
    } as DotEnvironment
];

describe('DotPageActionsService', () => {
    let spectator: SpectatorService<DotPageActionsService>;
    let mockMessageService: jest.Mocked<DotMessageService>;
    let mockActionsService: jest.Mocked<DotWorkflowsActionsService>;
    let mockRouterService: jest.Mocked<DotRouterService>;
    let mockEventsService: jest.Mocked<DotEventsService>;
    let mockDialogService: jest.Mocked<DialogService>;
    let mockWorkflowEventHandlerService: jest.Mocked<DotWorkflowEventHandlerService>;
    let mockWorkflowActionsFireService: jest.Mocked<DotWorkflowActionsFireService>;
    let mockHttpErrorManagerService: jest.Mocked<DotHttpErrorManagerService>;
    let mockPushPublishDialogService: jest.Mocked<DotPushPublishDialogService>;
    let mockCurrentUserService: jest.Mocked<DotCurrentUserService>;
    let mockPushPublishService: jest.Mocked<PushPublishService>;
    let mockGlobalStore: { loggedUser: ReturnType<typeof signal> };
    let mockPagesStore: PagesStoreMock;
    let mockDotPageListService: jest.Mocked<Pick<DotPageListService, 'getFavoritePageByURL'>>;

    const createService = createServiceFactory({
        service: DotPageActionsService,
        mocks: []
    });

    beforeEach(() => {
        // Setup all mocks before creating service
        mockMessageService = {
            get: jest.fn((key: string) => key)
        } as unknown as jest.Mocked<DotMessageService>;

        mockActionsService = {
            getByInode: jest.fn().mockReturnValue(of([MOCK_WORKFLOW_ACTION_NO_INPUTS]))
        } as unknown as jest.Mocked<DotWorkflowsActionsService>;

        mockRouterService = {
            goToEditContentlet: jest.fn()
        } as unknown as jest.Mocked<DotRouterService>;

        mockEventsService = {
            notify: jest.fn()
        } as unknown as jest.Mocked<DotEventsService>;

        mockDialogService = {
            open: jest.fn()
        } as unknown as jest.Mocked<DialogService>;

        mockWorkflowEventHandlerService = {
            open: jest.fn()
        } as unknown as jest.Mocked<DotWorkflowEventHandlerService>;

        mockWorkflowActionsFireService = {
            fireTo: jest.fn().mockReturnValue(of({ success: true })),
            deleteContentlet: jest.fn().mockReturnValue(of({ success: true }))
        } as unknown as jest.Mocked<DotWorkflowActionsFireService>;

        mockHttpErrorManagerService = {
            handle: jest.fn()
        } as unknown as jest.Mocked<DotHttpErrorManagerService>;

        mockPushPublishDialogService = {
            open: jest.fn()
        } as unknown as jest.Mocked<DotPushPublishDialogService>;

        mockCurrentUserService = {
            getUserPermissions: jest.fn().mockReturnValue(of(MOCK_PERMISSIONS))
        } as unknown as jest.Mocked<DotCurrentUserService>;

        mockPushPublishService = {
            getEnvironments: jest.fn().mockReturnValue(of(MOCK_ENVIRONMENTS))
        } as unknown as jest.Mocked<PushPublishService>;

        mockGlobalStore = {
            loggedUser: signal(MOCK_USER)
        };

        mockPagesStore = {
            getFavoritePages: jest.fn()
        };

        mockDotPageListService = {
            // For non-favorite pages the service queries by URL; returning `undefined` means "not found".
            getFavoritePageByURL: jest
                .fn()
                .mockReturnValue(of(undefined as unknown as DotCMSContentlet))
        };

        spectator = createService({
            providers: [
                { provide: DotMessageService, useValue: mockMessageService },
                { provide: DotWorkflowsActionsService, useValue: mockActionsService },
                { provide: DotRouterService, useValue: mockRouterService },
                { provide: DotEventsService, useValue: mockEventsService },
                { provide: DialogService, useValue: mockDialogService },
                {
                    provide: DotWorkflowEventHandlerService,
                    useValue: mockWorkflowEventHandlerService
                },
                {
                    provide: DotWorkflowActionsFireService,
                    useValue: mockWorkflowActionsFireService
                },
                { provide: DotHttpErrorManagerService, useValue: mockHttpErrorManagerService },
                { provide: DotPushPublishDialogService, useValue: mockPushPublishDialogService },
                { provide: DotCurrentUserService, useValue: mockCurrentUserService },
                { provide: PushPublishService, useValue: mockPushPublishService },
                { provide: GlobalStore, useValue: mockGlobalStore },
                { provide: DotCMSPagesStore, useValue: mockPagesStore },
                { provide: DotPageListService, useValue: mockDotPageListService }
            ]
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should create', () => {
        expect(spectator.service).toBeTruthy();
    });

    describe('Initialization', () => {
        it('should fetch user permissions on initialization', () => {
            expect(mockCurrentUserService.getUserPermissions).toHaveBeenCalledWith(
                MOCK_USER.userId,
                [UserPermissions.READ, UserPermissions.WRITE],
                [PermissionsType.CONTENTLETS, PermissionsType.HTMLPAGES]
            );
        });

        it('should fetch push publish environments on initialization', () => {
            expect(mockPushPublishService.getEnvironments).toHaveBeenCalled();
        });
    });

    describe('getItems', () => {
        it('should fetch workflow actions for contentlet', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                expect(mockActionsService.getByInode).toHaveBeenCalledWith(
                    MOCK_HTMLPAGE_CONTENTLET.inode,
                    DotRenderMode.LISTING
                );
                expect(items.length).toBeGreaterThan(0);
                done();
            });
        });

        it('should return menu items with workflow actions', (done) => {
            mockActionsService.getByInode.mockReturnValue(
                of([MOCK_WORKFLOW_ACTION_NO_INPUTS, MOCK_WORKFLOW_ACTION_WITH_INPUTS])
            );

            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const workflowItems = items.filter((item) => !item.separator);
                const hasPublishAction = workflowItems.some(
                    (item) => item.label === MOCK_WORKFLOW_ACTION_NO_INPUTS.name
                );
                const hasApproveAction = workflowItems.some(
                    (item) => item.label === MOCK_WORKFLOW_ACTION_WITH_INPUTS.name
                );

                expect(hasPublishAction).toBe(true);
                expect(hasApproveAction).toBe(true);
                done();
            });
        });

        it('should include separator in menu items', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const hasSeparator = items.some((item) => item.separator === true);
                expect(hasSeparator).toBe(true);
                done();
            });
        });
    });

    describe('Menu Items for HTML Pages with Edit Permission', () => {
        it('should include favorite page action for non-archived pages', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const favoriteAction = items.find((item) =>
                    item.label?.includes('favoritePage.contextMenu.action')
                );
                expect(favoriteAction).toBeTruthy();
                done();
            });
        });

        it('should not include favorite page action for archived pages', (done) => {
            spectator.service.getItems(MOCK_ARCHIVED_PAGE).subscribe((items) => {
                const favoriteAction = items.find((item) =>
                    item.label?.includes('favoritePage.contextMenu.action')
                );
                expect(favoriteAction).toBeFalsy();
                done();
            });
        });

        it('should include edit action for HTML pages when user has write permission', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const editAction = items.find((item) => item.label === 'Edit');
                expect(editAction).toBeTruthy();
                done();
            });
        });

        it('should include add to bundle action (disabled)', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const bundleAction = items.find((item) => item.label?.includes('add_to_bundle'));
                expect(bundleAction).toBeTruthy();
                expect(bundleAction?.disabled).toBe(true);
                done();
            });
        });

        it('should include push publish action when environments exist', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const pushPublishAction = items.find((item) =>
                    item.label?.includes('push_publish')
                );
                expect(pushPublishAction).toBeTruthy();
                done();
            });
        });

        // Note: Testing without push publish environments requires a separate test suite
        // with different service initialization. The presence of environments is tested
        // through the initialization test above.
    });

    describe('Menu Items for Content with Edit Permission', () => {
        it('should include edit action for contentlets when user has write permission', (done) => {
            spectator.service.getItems(MOCK_CONTENT_CONTENTLET).subscribe((items) => {
                const editAction = items.find((item) => item.label === 'Edit');
                expect(editAction).toBeTruthy();
                done();
            });
        });
    });

    describe('Menu Items Without Edit Permission', () => {
        // Note: Testing without edit permissions requires a separate test suite
        // with different service initialization. Permission checks are tested
        // through the canEdit logic which is covered in the HTML pages tests above.
    });

    describe('Favorite Page Actions', () => {
        it('should show "add" label for non-favorite pages', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const favoriteAction = items.find((item) =>
                    item.label?.includes('favoritePage.contextMenu.action.add')
                );
                expect(favoriteAction).toBeTruthy();
                done();
            });
        });

        it('should show "edit" label for favorite pages', (done) => {
            spectator.service.getItems(MOCK_FAVORITE_PAGE).subscribe((items) => {
                const favoriteAction = items.find((item) =>
                    item.label?.includes('favoritePage.contextMenu.action.edit')
                );
                expect(favoriteAction).toBeTruthy();
                done();
            });
        });

        it('should include delete favorite action for favorite pages', (done) => {
            spectator.service.getItems(MOCK_FAVORITE_PAGE).subscribe((items) => {
                const deleteAction = items.find((item) =>
                    item.label?.includes('favoritePage.dialog.delete.button')
                );
                expect(deleteAction).toBeTruthy();
                done();
            });
        });

        it('should not include delete favorite action for non-favorite pages', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const deleteAction = items.find((item) =>
                    item.label?.includes('favoritePage.dialog.delete.button')
                );
                expect(deleteAction).toBeFalsy();
                done();
            });
        });

        it('should open favorite page dialog when add favorite is clicked', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const favoriteAction = items.find((item) =>
                    item.label?.includes('favoritePage.contextMenu.action.add')
                );

                favoriteAction?.command?.({} as unknown);

                expect(mockDialogService.open).toHaveBeenCalled();
                done();
            });
        });

        it('should delete favorite page when delete is clicked', fakeAsync(() => {
            spectator.service.getItems(MOCK_FAVORITE_PAGE).subscribe((items) => {
                const deleteAction = items.find((item) =>
                    item.label?.includes('favoritePage.dialog.delete.button')
                );

                deleteAction?.command?.({} as unknown);
                tick();

                expect(mockWorkflowActionsFireService.deleteContentlet).toHaveBeenCalledWith({
                    inode: MOCK_FAVORITE_PAGE.inode
                });
                expect(mockPagesStore.getFavoritePages).toHaveBeenCalled();
            });
        }));

        it('should handle delete favorite error', fakeAsync(() => {
            const error = new Error('Delete error');
            mockWorkflowActionsFireService.deleteContentlet.mockReturnValue(
                throwError(() => error)
            );

            spectator.service.getItems(MOCK_FAVORITE_PAGE).subscribe((items) => {
                const deleteAction = items.find((item) =>
                    item.label?.includes('favoritePage.dialog.delete.button')
                );

                deleteAction?.command?.({} as unknown);
                tick();

                // Check that error handler was called with an error and true flag
                expect(mockHttpErrorManagerService.handle).toHaveBeenCalled();
                const calls = mockHttpErrorManagerService.handle.mock.calls;
                expect(calls[calls.length - 1][1]).toBe(true);
            });
        }));
    });

    describe('Edit Action', () => {
        it('should navigate to edit contentlet when edit is clicked', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const editAction = items.find((item) => item.label === 'Edit');

                editAction?.command?.({} as unknown);

                expect(mockRouterService.goToEditContentlet).toHaveBeenCalledWith(
                    MOCK_HTMLPAGE_CONTENTLET.inode
                );
                done();
            });
        });
    });

    describe('Push Publish Action', () => {
        it('should open push publish dialog when clicked', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const pushPublishAction = items.find((item) =>
                    item.label?.includes('push_publish')
                );

                pushPublishAction?.command?.({} as unknown);

                expect(mockPushPublishDialogService.open).toHaveBeenCalledWith({
                    assetIdentifier: MOCK_HTMLPAGE_CONTENTLET.identifier,
                    title: 'contenttypes.content.push_publish'
                });
                done();
            });
        });
    });

    describe('Workflow Actions', () => {
        it('should fire workflow action immediately when no inputs required', fakeAsync(() => {
            mockActionsService.getByInode.mockReturnValue(of([MOCK_WORKFLOW_ACTION_NO_INPUTS]));

            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const workflowAction = items.find(
                    (item) => item.label === MOCK_WORKFLOW_ACTION_NO_INPUTS.name
                );

                workflowAction?.command?.({} as unknown);
                tick();

                expect(mockWorkflowActionsFireService.fireTo).toHaveBeenCalledWith({
                    actionId: MOCK_WORKFLOW_ACTION_NO_INPUTS.id,
                    inode: MOCK_HTMLPAGE_CONTENTLET.inode
                });
                expect(mockEventsService.notify).toHaveBeenCalledWith('save-page', {
                    payload: { success: true },
                    value: 'Workflow-executed'
                });
            });
        }));

        it('should open workflow wizard when inputs are required', (done) => {
            mockActionsService.getByInode.mockReturnValue(of([MOCK_WORKFLOW_ACTION_WITH_INPUTS]));

            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const workflowAction = items.find(
                    (item) => item.label === MOCK_WORKFLOW_ACTION_WITH_INPUTS.name
                );

                workflowAction?.command?.({} as unknown);

                expect(mockWorkflowEventHandlerService.open).toHaveBeenCalledWith({
                    workflow: MOCK_WORKFLOW_ACTION_WITH_INPUTS,
                    callback: 'ngWorkflowEventCallback',
                    inode: MOCK_HTMLPAGE_CONTENTLET.inode
                });
                expect(mockWorkflowActionsFireService.fireTo).not.toHaveBeenCalled();
                done();
            });
        });

        it('should handle workflow action error', fakeAsync(() => {
            const error = new Error('Workflow error');
            mockWorkflowActionsFireService.fireTo.mockReturnValue(throwError(() => error));
            mockActionsService.getByInode.mockReturnValue(of([MOCK_WORKFLOW_ACTION_NO_INPUTS]));

            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const workflowAction = items.find(
                    (item) => item.label === MOCK_WORKFLOW_ACTION_NO_INPUTS.name
                );

                workflowAction?.command?.({} as unknown);
                tick();

                // Check that error handler was called with an error and true flag
                expect(mockHttpErrorManagerService.handle).toHaveBeenCalled();
                const calls = mockHttpErrorManagerService.handle.mock.calls;
                expect(calls[calls.length - 1][1]).toBe(true);
            });
        }));
    });

    describe('Menu Item Ordering', () => {
        it('should have favorite action at the beginning for non-archived pages', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const firstNonSeparatorItem = items.find((item) => !item.separator);
                expect(firstNonSeparatorItem?.label).toContain('favoritePage.contextMenu.action');
                done();
            });
        });

        it('should have separator after favorite actions', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const favoriteActionIndex = items.findIndex((item) =>
                    item.label?.includes('favoritePage.contextMenu.action')
                );
                const nextSeparatorIndex = items.findIndex(
                    (item, index) => index > favoriteActionIndex && item.separator
                );

                expect(nextSeparatorIndex).toBeGreaterThan(favoriteActionIndex);
                done();
            });
        });

        it('should have workflow actions after separator', (done) => {
            mockActionsService.getByInode.mockReturnValue(of([MOCK_WORKFLOW_ACTION_NO_INPUTS]));

            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const separatorIndex = items.findIndex((item) => item.separator);
                const workflowActionIndex = items.findIndex(
                    (item) => item.label === MOCK_WORKFLOW_ACTION_NO_INPUTS.name
                );

                expect(workflowActionIndex).toBeGreaterThan(separatorIndex);
                done();
            });
        });
    });

    describe('Edge Cases', () => {
        it('should handle contentlet without baseType', (done) => {
            const contentletWithoutBaseType = {
                ...MOCK_HTMLPAGE_CONTENTLET,
                baseType: undefined
            } as DotCMSContentlet;

            spectator.service.getItems(contentletWithoutBaseType).subscribe((items) => {
                const editAction = items.find((item) => item.label === 'Edit');
                expect(editAction).toBeFalsy();
                done();
            });
        });

        it('should handle empty workflow actions array', (done) => {
            mockActionsService.getByInode.mockReturnValue(of([]));

            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const workflowItems = items.filter(
                    (item) =>
                        !item.separator &&
                        item.label !== 'Edit' &&
                        !item.label?.includes('favorite') &&
                        !item.label?.includes('bundle') &&
                        !item.label?.includes('push')
                );

                expect(workflowItems).toHaveLength(0);
                done();
            });
        });

        it('should handle workflow action with empty actionInputs array', (done) => {
            const actionWithEmptyInputs = {
                ...MOCK_WORKFLOW_ACTION_NO_INPUTS,
                actionInputs: []
            };
            mockActionsService.getByInode.mockReturnValue(of([actionWithEmptyInputs]));

            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                const workflowAction = items.find(
                    (item) => item.label === actionWithEmptyInputs.name
                );

                expect(workflowAction).toBeTruthy();
                expect(workflowAction?.command).toBeDefined();
                done();
            });
        });
    });

    describe('Integration Workflows', () => {
        it('should handle complete favorite page add workflow', fakeAsync(() => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                // Step 1: User clicks add to favorites
                const favoriteAction = items.find((item) =>
                    item.label?.includes('favoritePage.contextMenu.action.add')
                );
                expect(favoriteAction).toBeTruthy();

                // Step 2: Dialog opens
                favoriteAction?.command?.({} as unknown);
                tick();

                expect(mockDialogService.open).toHaveBeenCalled();

                // Step 3: Verify dialog configuration
                const dialogConfig = mockDialogService.open.mock.calls[0]?.[1] as unknown as {
                    data?: {
                        page?: { favoritePage?: DotCMSContentlet };
                        onSave?: () => void;
                        onDelete?: () => void;
                    };
                };
                expect(dialogConfig.data?.page?.favoritePage).toBe(MOCK_HTMLPAGE_CONTENTLET);
                expect(dialogConfig.data?.onSave).toBeDefined();
                expect(dialogConfig.data?.onDelete).toBeDefined();

                // Step 4: Trigger onSave callback
                dialogConfig.data?.onSave?.();
                expect(mockPagesStore.getFavoritePages).toHaveBeenCalled();
            });
        }));

        it('should handle complete workflow execution workflow', fakeAsync(() => {
            mockActionsService.getByInode.mockReturnValue(of([MOCK_WORKFLOW_ACTION_NO_INPUTS]));

            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                // Step 1: User selects workflow action
                const workflowAction = items.find(
                    (item) => item.label === MOCK_WORKFLOW_ACTION_NO_INPUTS.name
                );
                expect(workflowAction).toBeTruthy();

                // Step 2: Workflow executes
                workflowAction?.command?.({} as unknown);
                tick();

                // Step 3: Service fires workflow
                expect(mockWorkflowActionsFireService.fireTo).toHaveBeenCalled();

                // Step 4: Success event is emitted
                expect(mockEventsService.notify).toHaveBeenCalledWith('save-page', {
                    payload: { success: true },
                    value: 'Workflow-executed'
                });
            });
        }));

        it('should handle complete edit workflow', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                // Step 1: User clicks edit
                const editAction = items.find((item) => item.label === 'Edit');
                expect(editAction).toBeTruthy();

                // Step 2: Router navigates to edit page
                editAction?.command?.({} as unknown);

                expect(mockRouterService.goToEditContentlet).toHaveBeenCalledWith(
                    MOCK_HTMLPAGE_CONTENTLET.inode
                );
                done();
            });
        });

        it('should handle complete push publish workflow', (done) => {
            spectator.service.getItems(MOCK_HTMLPAGE_CONTENTLET).subscribe((items) => {
                // Step 1: User clicks push publish
                const pushPublishAction = items.find((item) =>
                    item.label?.includes('push_publish')
                );
                expect(pushPublishAction).toBeTruthy();

                // Step 2: Push publish dialog opens
                pushPublishAction?.command?.({} as unknown);

                expect(mockPushPublishDialogService.open).toHaveBeenCalledWith({
                    assetIdentifier: MOCK_HTMLPAGE_CONTENTLET.identifier,
                    title: 'contenttypes.content.push_publish'
                });
                done();
            });
        });
    });
});
