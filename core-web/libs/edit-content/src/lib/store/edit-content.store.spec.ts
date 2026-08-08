import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotContentletService,
    DotContentTypeService,
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotSiteService,
    DotSystemConfigService,
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
import { EDIT_CONTENT_HOST } from '../services/host/edit-content-host.model';

describe('DotEditContentStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotEditContentStore>>;
    let store: InstanceType<typeof DotEditContentStore>;

    // The host resolves which content to open. Tests set its return value to
    // drive `initialize()` (route params in prod are the host's concern, not the
    // store's).
    const mockHost = {
        resolveIdentity: jest.fn().mockReturnValue({}),
        reportSaved: jest.fn(),
        reloadContent: jest.fn(),
        setContentTitle: jest.fn(),
        addBreadcrumb: jest.fn(),
        goToSavedContent: jest.fn(),
        goToRestoredVersion: jest.fn()
    };

    const createService = createServiceFactory({
        service: DotEditContentStore,
        providers: [
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
            mockProvider(ConfirmationService),
            mockProvider(Router, {
                navigate: jest.fn().mockReturnValue(Promise.resolve(true)),
                url: '/test-url',
                events: of()
            }),
            mockProvider(DotSiteService),
            mockProvider(DotSystemConfigService),
            { provide: EDIT_CONTENT_HOST, useValue: mockHost },
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        Object.values(mockHost).forEach((fn) => fn.mockClear());
        mockHost.resolveIdentity.mockReturnValue({});
        spectator = createService();
        store = spectator.service;
    });

    it('should create store with initial state', () => {
        expect(store.state()).toBe(ComponentStatus.INIT);
        expect(store.error()).toBeNull();
    });

    it('should initialize push publish history state correctly', () => {
        expect(store.pushPublishHistory()).toEqual([]);
        expect(store.pushPublishHistoryPagination()).toBeNull();
        expect(store.pushPublishHistoryStatus()).toEqual({
            status: ComponentStatus.INIT,
            error: null
        });
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
        // History Feature - Push Publish History
        expect(store.pushPublishHistory).toBeDefined();
        expect(store.pushPublishHistoryPagination).toBeDefined();
        expect(store.pushPublishHistoryStatus).toBeDefined();
        // Methods
        expect(store.initializeNewContent).toBeDefined();
        expect(store.initializeExistingContent).toBeDefined();
        expect(store.initialize).toBeDefined();
        expect(store.loadPushPublishHistory).toBeDefined();
        expect(store.clearPushPublishHistory).toBeDefined();
        expect(store.deletePushPublishHistory).toBeDefined();
    });

    describe('initialize', () => {
        it('should initialize existing content when the host resolves an inode', () => {
            mockHost.resolveIdentity.mockReturnValue({ inode: 'test-inode-123' });

            const mockContentlet: Partial<DotCMSContentlet> = {
                inode: 'test-inode-123',
                contentType: 'testType'
            };
            spectator
                .inject(DotEditContentService)
                .getContentById.mockReturnValue(of(mockContentlet as DotCMSContentlet));
            spectator
                .inject(DotContentTypeService)
                .getContentTypeWithRender.mockReturnValue(of({} as DotCMSContentType));
            spectator.inject(DotWorkflowsActionsService).getByInode.mockReturnValue(of([]));
            spectator.inject(DotWorkflowsActionsService).getWorkFlowActions.mockReturnValue(of([]));
            spectator
                .inject(DotWorkflowService)
                .getWorkflowStatus.mockReturnValue(of({} as DotCMSWorkflowStatus));

            store.initialize();

            expect(spectator.inject(DotEditContentService).getContentById).toHaveBeenCalledWith({
                id: 'test-inode-123',
                depth: DotContentletDepths.TWO
            });
        });

        it('should initialize new content when the host resolves a contentTypeId', () => {
            mockHost.resolveIdentity.mockReturnValue({ contentTypeId: 'test-content-type' });

            const mockContentType: Partial<DotCMSContentType> = {
                id: 'test-content-type',
                name: 'Test Type'
            };
            spectator
                .inject(DotContentTypeService)
                .getContentTypeWithRender.mockReturnValue(of(mockContentType as DotCMSContentType));
            spectator.inject(DotWorkflowsActionsService).getDefaultActions.mockReturnValue(of([]));

            store.initialize();

            expect(
                spectator.inject(DotContentTypeService).getContentTypeWithRender
            ).toHaveBeenCalledWith('test-content-type');
            // An inode was not resolved, so existing-content loading is not triggered.
            expect(spectator.inject(DotEditContentService).getContentById).not.toHaveBeenCalled();
        });

        it('should prioritize inode over contentTypeId when the host resolves both', () => {
            mockHost.resolveIdentity.mockReturnValue({
                inode: 'test-inode-123',
                contentTypeId: 'test-content-type'
            });

            spectator
                .inject(DotEditContentService)
                .getContentById.mockReturnValue(
                    of({ inode: 'test-inode-123' } as DotCMSContentlet)
                );
            spectator
                .inject(DotContentTypeService)
                .getContentTypeWithRender.mockReturnValue(of({} as DotCMSContentType));
            spectator.inject(DotWorkflowsActionsService).getByInode.mockReturnValue(of([]));
            spectator.inject(DotWorkflowsActionsService).getWorkFlowActions.mockReturnValue(of([]));
            spectator
                .inject(DotWorkflowService)
                .getWorkflowStatus.mockReturnValue(of({} as DotCMSWorkflowStatus));

            store.initialize();

            expect(spectator.inject(DotEditContentService).getContentById).toHaveBeenCalled();
        });

        it('should store folderPath resolved by the host', () => {
            mockHost.resolveIdentity.mockReturnValue({
                contentTypeId: 'test-content-type',
                folderPath: 'default/level1/level2/'
            });

            spectator
                .inject(DotContentTypeService)
                .getContentTypeWithRender.mockReturnValue(of({} as DotCMSContentType));
            spectator.inject(DotWorkflowsActionsService).getDefaultActions.mockReturnValue(of([]));

            store.initialize();

            expect(store.queryParams()).toEqual({ folderPath: 'default/level1/level2/' });
        });

        it('should keep default empty queryParams when the host resolves no folderPath', () => {
            mockHost.resolveIdentity.mockReturnValue({ contentTypeId: 'test-content-type' });

            spectator
                .inject(DotContentTypeService)
                .getContentTypeWithRender.mockReturnValue(of({} as DotCMSContentType));
            spectator.inject(DotWorkflowsActionsService).getDefaultActions.mockReturnValue(of([]));

            store.initialize();

            expect(store.queryParams()).toEqual({});
        });
    });
});
