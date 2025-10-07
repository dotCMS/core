import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ActivatedRoute, ActivatedRouteSnapshot } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotContentletService,
    DotContentTypeService,
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotVersionableService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotContentletDepths,
    DotCMSContentType,
    DotCMSContentlet,
    DotCMSWorkflowStatus
} from '@dotcms/dotcms-models';

import { DotEditContentStore } from './edit-content.store';

import { DotEditContentService } from '../services/dot-edit-content.service';

describe('DotEditContentStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotEditContentStore>>;
    let store: InstanceType<typeof DotEditContentStore>;
    let mockActivatedRoute: Partial<ActivatedRoute>;

    const createService = createServiceFactory({
        service: DotEditContentStore,
        providers: [
            {
                provide: ActivatedRoute,
                useFactory: () => mockActivatedRoute
            },
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotEditContentService),
            mockProvider(DotContentTypeService),
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotWorkflowService),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(MessageService),
            mockProvider(DotMessageService),
            mockProvider(DotContentletService),
            mockProvider(DotLanguagesService),
            mockProvider(DotCurrentUserService),
            mockProvider(DialogService),
            mockProvider(DotVersionableService),
            mockProvider(ConfirmationService)
        ]
    });

    beforeEach(() => {
        mockActivatedRoute = {
            snapshot: {
                params: {}
            } as ActivatedRouteSnapshot
        };
        spectator = createService();
        store = spectator.service;
    });

    it('should create store with initial state', () => {
        expect(store.state()).toBe(ComponentStatus.INIT);
        expect(store.error()).toBeNull();
        expect(store.isDialogMode()).toBe(false);
    });

    it('should compose with all required features', () => {
        // Verify features are composed into the store
        expect(store.contentType).toBeDefined();
        expect(store.contentlet).toBeDefined();
        expect(store.information).toBeDefined();
        expect(store.locales).toBeDefined();
        expect(store.canLock).toBeDefined();
        expect(store.systemDefaultLocale).toBeDefined();
        expect(store.currentLocale).toBeDefined();
        expect(store.currentIdentifier).toBeDefined();
        expect(store.localesStatus).toBeDefined();
        expect(store.showWorkflowActions).toBeDefined();
        // UI Feature
        expect(store.uiState).toBeDefined();
        expect(store.isSidebarOpen).toBeDefined();
        expect(store.activeTab).toBeDefined();
        expect(store.activeSidebarTab).toBeDefined();
        // User Feature
        expect(store.currentUser).toBeDefined();
        // Methods
        expect(store.enableDialogMode).toBeDefined();
        expect(store.initializeNewContent).toBeDefined();
        expect(store.initializeExistingContent).toBeDefined();
        expect(store.initializeDialogMode).toBeDefined();
        expect(store.initializeAsPortlet).toBeDefined();
    });

    describe('initializeDialogMode', () => {
        it('should enable dialog mode and initialize new content when contentTypeId is provided', () => {
            // Arrange
            const options = { contentTypeId: 'test-content-type-id' };

            // Mock the services that initializeNewContent would call
            const mockContentType: Partial<DotCMSContentType> = {
                id: 'test-content-type-id',
                name: 'Test Type'
            };
            spectator
                .inject(DotContentTypeService)
                .getContentType.mockReturnValue(of(mockContentType as DotCMSContentType));
            spectator.inject(DotWorkflowsActionsService).getDefaultActions.mockReturnValue(of([]));

            // Act
            store.initializeDialogMode(options);

            // Assert - check state changes instead of method calls
            expect(store.isDialogMode()).toBe(true);
            // The actual initialization is asynchronous, but we can verify dialog mode was enabled
        });

        it('should enable dialog mode and initialize existing content when contentletInode is provided', () => {
            // Arrange
            const options = { contentletInode: 'test-inode-123' };

            // Mock the services that initializeExistingContent would call
            const mockContentlet: Partial<DotCMSContentlet> = {
                inode: 'test-inode-123',
                contentType: 'testType'
            };
            spectator
                .inject(DotEditContentService)
                .getContentById.mockReturnValue(of(mockContentlet as DotCMSContentlet));
            spectator
                .inject(DotContentTypeService)
                .getContentType.mockReturnValue(of({} as DotCMSContentType));
            spectator.inject(DotWorkflowsActionsService).getByInode.mockReturnValue(of([]));
            spectator.inject(DotWorkflowsActionsService).getWorkFlowActions.mockReturnValue(of([]));
            spectator
                .inject(DotWorkflowService)
                .getWorkflowStatus.mockReturnValue(of({} as DotCMSWorkflowStatus));

            // Act
            store.initializeDialogMode(options);

            // Assert - check state changes
            expect(store.isDialogMode()).toBe(true);
        });

        it('should enable dialog mode when both contentTypeId and contentletInode are provided but prioritize contentTypeId', () => {
            // Arrange
            const options = {
                contentTypeId: 'test-content-type-id',
                contentletInode: 'test-inode-123'
            };

            // Mock the services for new content
            const mockContentType: Partial<DotCMSContentType> = {
                id: 'test-content-type-id',
                name: 'Test Type'
            };
            spectator
                .inject(DotContentTypeService)
                .getContentType.mockReturnValue(of(mockContentType as DotCMSContentType));
            spectator.inject(DotWorkflowsActionsService).getDefaultActions.mockReturnValue(of([]));

            // Act
            store.initializeDialogMode(options);

            // Assert
            expect(store.isDialogMode()).toBe(true);
            // Since contentTypeId is prioritized, existing content services should not be called
            expect(spectator.inject(DotEditContentService).getContentById).not.toHaveBeenCalled();
        });
    });

    describe('initializeAsPortlet', () => {
        it('should skip initialization when in dialog mode', () => {
            // Arrange
            store.enableDialogMode();
            if (mockActivatedRoute.snapshot) {
                mockActivatedRoute.snapshot.params = {
                    contentType: 'test-content-type',
                    id: 'test-inode'
                };
            }

            // Act
            store.initializeAsPortlet();

            // Assert - services should not be called when in dialog mode
            expect(spectator.inject(DotContentTypeService).getContentType).not.toHaveBeenCalled();
            expect(spectator.inject(DotEditContentService).getContentById).not.toHaveBeenCalled();
        });

        it('should initialize existing content when inode parameter is present', () => {
            // Arrange
            expect(store.isDialogMode()).toBe(false);
            if (mockActivatedRoute.snapshot) {
                mockActivatedRoute.snapshot.params = {
                    id: 'test-inode-123',
                    contentType: 'test-content-type'
                };
            }

            // Mock services
            const mockContentlet: Partial<DotCMSContentlet> = {
                inode: 'test-inode-123',
                contentType: 'testType'
            };
            spectator
                .inject(DotEditContentService)
                .getContentById.mockReturnValue(of(mockContentlet as DotCMSContentlet));
            spectator
                .inject(DotContentTypeService)
                .getContentType.mockReturnValue(of({} as DotCMSContentType));
            spectator.inject(DotWorkflowsActionsService).getByInode.mockReturnValue(of([]));
            spectator.inject(DotWorkflowsActionsService).getWorkFlowActions.mockReturnValue(of([]));
            spectator
                .inject(DotWorkflowService)
                .getWorkflowStatus.mockReturnValue(of({} as DotCMSWorkflowStatus));

            // Act
            store.initializeAsPortlet();

            // Assert - verify the correct service is called with correct parameters
            expect(spectator.inject(DotEditContentService).getContentById).toHaveBeenCalledWith({
                id: 'test-inode-123',
                depth: DotContentletDepths.TWO
            });
        });

        it('should initialize new content when only contentType parameter is present', () => {
            // Arrange
            expect(store.isDialogMode()).toBe(false);
            if (mockActivatedRoute.snapshot) {
                mockActivatedRoute.snapshot.params = { contentType: 'test-content-type' };
            }

            // Mock services
            const mockContentType: Partial<DotCMSContentType> = {
                id: 'test-content-type',
                name: 'Test Type'
            };
            spectator
                .inject(DotContentTypeService)
                .getContentType.mockReturnValue(of(mockContentType as DotCMSContentType));
            spectator.inject(DotWorkflowsActionsService).getDefaultActions.mockReturnValue(of([]));

            // Act
            store.initializeAsPortlet();

            // Assert - verify the correct service is called
            expect(spectator.inject(DotContentTypeService).getContentType).toHaveBeenCalledWith(
                'test-content-type'
            );
        });
    });
});
