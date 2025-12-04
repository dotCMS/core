import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';

import { ConfirmationService, MessageService } from 'primeng/api';

import { DotMessageService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotContentDriveItem } from '@dotcms/dotcms-models';

import { DotContentDriveWorkflowActionsComponent } from './dot-content-drive-workflow-actions.component';

import { SUCCESS_MESSAGE_LIFE } from '../../../../shared/constants';
import { DotContentDriveNavigationService } from '../../../../shared/services';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';
import {
    ContentDriveWorkflowAction,
    DEFAULT_WORKFLOW_ACTIONS,
    WORKFLOW_ACTION_ID
} from '../../../../utils/workflow-actions';

describe('DotContentDriveWorkflowActionsComponent', () => {
    let spectator: Spectator<DotContentDriveWorkflowActionsComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let messageService: SpyObject<MessageService>;
    let dotWorkflowActionsFireService: SpyObject<DotWorkflowActionsFireService>;
    let navigationService: SpyObject<DotContentDriveNavigationService>;
    let confirmationService: SpyObject<ConfirmationService>;

    const mockSelectedItems = signal<DotContentDriveItem[]>([]);

    const createComponent = createComponentFactory({
        component: DotContentDriveWorkflowActionsComponent,
        providers: [
            provideHttpClient(),
            mockProvider(DotContentDriveStore, {
                selectedItems: mockSelectedItems,
                loadItems: jest.fn(),
                setStatus: jest.fn(),
                setSelectedItems: jest.fn()
            }),
            mockProvider(MessageService, {
                add: jest.fn()
            }),
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
            }),
            mockProvider(DotWorkflowActionsFireService, {
                fireDefaultAction: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(DotContentDriveNavigationService, {
                editContent: jest.fn(),
                editPage: jest.fn()
            }),
            mockProvider(ConfirmationService, {
                confirm: jest.fn()
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        messageService = spectator.inject(MessageService, true);
        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService, true);
        navigationService = spectator.inject(DotContentDriveNavigationService, true);
        confirmationService = spectator.inject(ConfirmationService, true);

        // Setup spies
        jest.spyOn(dotWorkflowActionsFireService, 'fireDefaultAction').mockReturnValue(of([]));
        jest.spyOn(confirmationService, 'confirm').mockReturnValue(confirmationService);
        jest.spyOn(store, 'loadItems');
        jest.spyOn(store, 'setStatus');
        jest.spyOn(store, 'setSelectedItems');
        jest.spyOn(messageService, 'add');

        // Reset selected items signal before each test
        mockSelectedItems.set([]);

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Component Rendering', () => {
        it('should create the component', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should render all workflow action buttons', () => {
            const buttons = spectator.queryAll('[data-testid^="workflow-action-"]');
            expect(buttons.length).toBe(DEFAULT_WORKFLOW_ACTIONS.length);
        });

        it('should render buttons with correct data-testid attributes', () => {
            DEFAULT_WORKFLOW_ACTIONS.forEach((action) => {
                const button = spectator.query(`[data-testid="workflow-action-${action.id}"]`);
                expect(button).toBeTruthy();
            });
        });

        it('should render confirmation dialog component', () => {
            const confirmDialog = spectator.query('p-confirmdialog');
            expect(confirmDialog).toBeTruthy();
        });

        it('should render button labels with message keys', () => {
            const firstButton = spectator.query(
                `[data-testid="workflow-action-${DEFAULT_WORKFLOW_ACTIONS[0].id}"]`
            );
            expect(firstButton).toBeTruthy();
            expect(firstButton?.textContent).toBeTruthy();
        });
    });

    describe('Button Visibility', () => {
        it('should hide all buttons when no items are selected', () => {
            mockSelectedItems.set([]);
            spectator.detectChanges();

            DEFAULT_WORKFLOW_ACTIONS.forEach((action) => {
                const button = spectator.query(
                    `[data-testid="workflow-action-${action.id}"]`
                ) as HTMLElement;
                if (button && action.showWhen) {
                    expect(button.style.display).toBe('none');
                }
            });
        });

        it('should show "Edit Content" button for single non-archived contentlet', () => {
            mockSelectedItems.set([
                {
                    archived: false,
                    live: true,
                    working: false,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ]);
            spectator.detectChanges();

            const editContentButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.GOT_TO_EDIT_CONTENTLET}"]`
            ) as HTMLElement;

            expect(editContentButton?.style.display).not.toBe('none');
        });

        it('should show "Edit Page" button for single non-archived page', () => {
            mockSelectedItems.set([
                {
                    archived: false,
                    live: true,
                    working: false,
                    baseType: 'HTMLPAGE',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ]);
            spectator.detectChanges();

            const editPageButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.GOT_TO_EDIT_PAGE}"]`
            ) as HTMLElement;

            expect(editPageButton?.style.display).not.toBe('none');
        });

        it('should show "Publish" button for non-archived, non-live items', () => {
            mockSelectedItems.set([
                {
                    archived: false,
                    live: false,
                    working: true,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ]);
            spectator.detectChanges();

            const publishButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.PUBLISH}"]`
            ) as HTMLElement;

            expect(publishButton?.style.display).not.toBe('none');
        });

        it('should show "Download" button for all assets', () => {
            mockSelectedItems.set([
                {
                    archived: false,
                    live: true,
                    working: false,
                    baseType: 'FILEASSET',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ]);
            spectator.detectChanges();

            const downloadButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.DOWNLOAD}"]`
            ) as HTMLElement;

            expect(downloadButton?.style.display).not.toBe('none');
        });
    });

    describe('Navigation Actions', () => {
        it('should navigate to edit contentlet when "Edit Content" button is clicked', () => {
            const mockItem = {
                archived: false,
                live: true,
                working: false,
                baseType: 'CONTENT',
                inode: 'test-inode-1',
                identifier: 'test-id'
            } as DotContentDriveItem;

            mockSelectedItems.set([mockItem]);
            spectator.detectChanges();

            const editContentButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.GOT_TO_EDIT_CONTENTLET}"]`
            );

            spectator.click(editContentButton);

            expect(navigationService.editContent).toHaveBeenCalledWith(mockItem);
        });

        it('should navigate to edit page when "Edit Page" button is clicked', () => {
            const mockItem = {
                archived: false,
                live: true,
                working: false,
                baseType: 'HTMLPAGE',
                inode: 'test-inode-1',
                identifier: 'test-id'
            } as DotContentDriveItem;

            mockSelectedItems.set([mockItem]);
            spectator.detectChanges();

            const editPageButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.GOT_TO_EDIT_PAGE}"]`
            );

            spectator.click(editPageButton);

            expect(navigationService.editPage).toHaveBeenCalledWith(mockItem);
        });
    });

    describe('Workflow Actions without Confirmation', () => {
        it('should execute "Publish" action directly without confirmation', () => {
            const mockItems = [
                {
                    archived: false,
                    live: false,
                    working: true,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ];

            mockSelectedItems.set(mockItems);
            spectator.detectChanges();

            const publishButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.PUBLISH}"]`
            );

            spectator.click(publishButton);

            expect(dotWorkflowActionsFireService.fireDefaultAction).toHaveBeenCalledWith({
                action: WORKFLOW_ACTION_ID.PUBLISH,
                inodes: ['test-inode-1']
            });

            expect(confirmationService.confirm).not.toHaveBeenCalled();
        });

        it('should pass multiple inodes when multiple items are selected', () => {
            const mockItems = [
                {
                    archived: false,
                    live: false,
                    working: true,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem,
                {
                    archived: false,
                    live: false,
                    working: true,
                    baseType: 'CONTENT',
                    inode: 'test-inode-2'
                } as DotContentDriveItem
            ];

            mockSelectedItems.set(mockItems);
            spectator.detectChanges();

            const publishButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.PUBLISH}"]`
            );

            spectator.click(publishButton);

            expect(dotWorkflowActionsFireService.fireDefaultAction).toHaveBeenCalledWith({
                action: WORKFLOW_ACTION_ID.PUBLISH,
                inodes: ['test-inode-1', 'test-inode-2']
            });
        });
    });

    describe('Workflow Actions with Confirmation', () => {
        it('should show confirmation dialog for "Archive" action', () => {
            const mockItems = [
                {
                    archived: false,
                    live: true,
                    working: false,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ];

            mockSelectedItems.set(mockItems);
            spectator.detectChanges();

            const archiveButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.ARCHIVE}"]`
            );

            spectator.click(archiveButton);

            expect(confirmationService.confirm).toHaveBeenCalledWith({
                message: 'content.drive.worflow.action.archive.confirm',
                header: 'Confirmation',
                acceptLabel: 'dot.common.yes',
                rejectLabel: 'dot.common.no',
                accept: expect.any(Function)
            });

            expect(dotWorkflowActionsFireService.fireDefaultAction).not.toHaveBeenCalled();
        });

        it('should execute action when confirmation is accepted', () => {
            const mockItems = [
                {
                    archived: false,
                    live: true,
                    working: false,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ];

            mockSelectedItems.set(mockItems);
            spectator.detectChanges();

            jest.spyOn(confirmationService, 'confirm').mockImplementation((config) => {
                config.accept?.();

                return confirmationService;
            });

            const archiveButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.ARCHIVE}"]`
            );

            spectator.click(archiveButton);

            expect(dotWorkflowActionsFireService.fireDefaultAction).toHaveBeenCalledWith({
                action: WORKFLOW_ACTION_ID.ARCHIVE,
                inodes: ['test-inode-1']
            });
        });

        it('should not execute action when confirmation is rejected', () => {
            const mockItems = [
                {
                    archived: false,
                    live: true,
                    working: false,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ];

            mockSelectedItems.set(mockItems);
            spectator.detectChanges();

            jest.spyOn(confirmationService, 'confirm').mockImplementation(() => {
                return confirmationService;
            });

            const archiveButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.ARCHIVE}"]`
            );

            spectator.click(archiveButton);

            expect(dotWorkflowActionsFireService.fireDefaultAction).not.toHaveBeenCalled();
        });
    });

    describe('Workflow Action Success', () => {
        it('should reload items after successful workflow action', () => {
            const mockItems = [
                {
                    archived: false,
                    live: false,
                    working: true,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ];

            mockSelectedItems.set(mockItems);
            spectator.detectChanges();

            const publishButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.PUBLISH}"]`
            );

            spectator.click(publishButton);

            expect(store.loadItems).toHaveBeenCalled();
        });

        it('should display success message after successful workflow action', () => {
            const mockItems = [
                {
                    archived: false,
                    live: false,
                    working: true,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ];

            mockSelectedItems.set(mockItems);
            spectator.detectChanges();

            const publishButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.PUBLISH}"]`
            );

            spectator.click(publishButton);

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'success',
                summary: 'content-drive.toast.workflow-executed',
                detail: 'content-drive.toast.workflow-executed-detail'
            });
        });
    });

    describe('Workflow Action Error', () => {
        it('should display error message when workflow action fails', () => {
            const mockError = new Error('Something went wrong');

            jest.spyOn(dotWorkflowActionsFireService, 'fireDefaultAction').mockReturnValue(
                throwError(() => mockError)
            );

            const mockItems = [
                {
                    archived: false,
                    live: false,
                    working: true,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ];

            mockSelectedItems.set(mockItems);
            spectator.detectChanges();

            const publishButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.PUBLISH}"]`
            );

            spectator.click(publishButton);

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'error',
                summary: 'content-drive.toast.workflow-error',
                detail: 'Something went wrong'
            });
        });

        it('should not reload items when workflow action fails', () => {
            const mockError = new Error('Something went wrong');

            jest.spyOn(dotWorkflowActionsFireService, 'fireDefaultAction').mockReturnValue(
                throwError(() => mockError)
            );

            const mockItems = [
                {
                    archived: false,
                    live: false,
                    working: true,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ];

            mockSelectedItems.set(mockItems);
            spectator.detectChanges();

            const publishButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.PUBLISH}"]`
            );

            spectator.click(publishButton);

            expect(store.loadItems).not.toHaveBeenCalled();
        });
    });

    describe('shouldShowAction method', () => {
        it('should return true when action has no showWhen conditions', () => {
            const action: ContentDriveWorkflowAction = {
                name: 'Test Action',
                id: WORKFLOW_ACTION_ID.PUBLISH
            };

            const result = spectator.component['shouldShowAction'](action);

            expect(result).toBe(true);
        });

        it('should return true when all conditions match', () => {
            mockSelectedItems.set([
                {
                    archived: false,
                    live: false,
                    working: true,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ]);
            spectator.detectChanges();

            const action: ContentDriveWorkflowAction = {
                name: 'Publish',
                id: WORKFLOW_ACTION_ID.PUBLISH,
                showWhen: {
                    noneArchived: true,
                    noneLive: true
                }
            };

            const result = spectator.component['shouldShowAction'](action);

            expect(result).toBe(true);
        });

        it('should return false when any condition does not match', () => {
            mockSelectedItems.set([
                {
                    archived: false,
                    live: true,
                    working: true,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ]);
            spectator.detectChanges();

            const action: ContentDriveWorkflowAction = {
                name: 'Publish',
                id: WORKFLOW_ACTION_ID.PUBLISH,
                showWhen: {
                    noneArchived: true,
                    noneLive: true
                }
            };

            const result = spectator.component['shouldShowAction'](action);

            expect(result).toBe(false);
        });
    });

    describe('Download Action', () => {
        it('should trigger download when download button is clicked', () => {
            const mockAsset = {
                archived: false,
                live: true,
                working: false,
                baseType: 'FILEASSET',
                inode: 'test-asset-inode',
                title: 'test-document.pdf',
                fileAsset: '/dA/test-asset-id/fileAsset/test-document.pdf'
            } as unknown as DotContentDriveItem;

            mockSelectedItems.set([mockAsset]);
            spectator.detectChanges();

            const windowSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

            const downloadButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.DOWNLOAD}"]`
            );

            spectator.click(downloadButton);

            expect(windowSpy).toHaveBeenCalledWith(
                expect.stringContaining('force_download=true'),
                '_self'
            );
            expect(windowSpy).toHaveBeenCalledWith(
                expect.stringContaining(mockAsset.fileAsset),
                '_self'
            );

            windowSpy.mockRestore();
        });

        it('should display success message after download is triggered', () => {
            const mockAsset = {
                archived: false,
                live: true,
                working: false,
                baseType: 'DOTASSET',
                inode: 'test-asset-inode',
                title: 'test-image.jpg',
                asset: '/dA/test-asset-id/asset/test-image.jpg'
            } as unknown as DotContentDriveItem;

            mockSelectedItems.set([mockAsset]);
            spectator.detectChanges();

            jest.spyOn(window, 'open').mockImplementation(() => null);

            const downloadButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.DOWNLOAD}"]`
            );

            spectator.click(downloadButton);

            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'success',
                summary: 'content-drive.toast.download-success',
                detail: 'content-drive.toast.download-success-detail',
                life: SUCCESS_MESSAGE_LIFE
            });
        });

        it('should not download when no asset is selected', () => {
            mockSelectedItems.set([]);
            spectator.detectChanges();

            const windowSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

            spectator.component['download']();

            expect(windowSpy).not.toHaveBeenCalled();

            windowSpy.mockRestore();
        });

        it('should handle DOTASSET type correctly', () => {
            const mockAsset = {
                archived: false,
                live: true,
                working: false,
                baseType: 'DOTASSET',
                inode: 'test-asset-inode',
                title: 'test-asset.png',
                assetVersion: '/dA/version/test-asset.png',
                asset: '/dA/test-asset.png'
            } as unknown as DotContentDriveItem;

            mockSelectedItems.set([mockAsset]);
            spectator.detectChanges();

            const windowSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

            const downloadButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.DOWNLOAD}"]`
            );

            spectator.click(downloadButton);

            // Should use assetVersion if available
            expect(windowSpy).toHaveBeenCalledWith(
                expect.stringContaining(mockAsset.assetVersion),
                '_self'
            );

            windowSpy.mockRestore();
        });

        it('should handle FILEASSET type correctly', () => {
            const mockAsset = {
                archived: false,
                live: true,
                working: false,
                baseType: 'FILEASSET',
                inode: 'test-asset-inode',
                title: 'document.pdf',
                fileAssetVersion: '/dA/version/document.pdf',
                fileAsset: '/dA/document.pdf'
            } as unknown as DotContentDriveItem;

            mockSelectedItems.set([mockAsset]);
            spectator.detectChanges();

            const windowSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

            const downloadButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.DOWNLOAD}"]`
            );

            spectator.click(downloadButton);

            // Should use fileAssetVersion if available
            expect(windowSpy).toHaveBeenCalledWith(
                expect.stringContaining(mockAsset.fileAssetVersion),
                '_self'
            );

            windowSpy.mockRestore();
        });
    });

    describe('Integration Tests', () => {
        it('should handle full workflow for archive action with confirmation', () => {
            const mockItems = [
                {
                    archived: false,
                    live: true,
                    working: false,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ];

            mockSelectedItems.set(mockItems);
            spectator.detectChanges();

            jest.spyOn(confirmationService, 'confirm').mockImplementation((config) => {
                config.accept?.();

                return confirmationService;
            });

            const archiveButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.ARCHIVE}"]`
            );

            spectator.click(archiveButton);

            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(dotWorkflowActionsFireService.fireDefaultAction).toHaveBeenCalled();
            expect(store.loadItems).toHaveBeenCalled();
            expect(messageService.add).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: 'success'
                })
            );
        });

        it('should handle selection changes and update button visibility', () => {
            mockSelectedItems.set([]);
            spectator.detectChanges();

            let publishButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.PUBLISH}"]`
            ) as HTMLElement;
            expect(publishButton?.style.display).toBe('none');

            mockSelectedItems.set([
                {
                    archived: false,
                    live: false,
                    working: true,
                    baseType: 'CONTENT',
                    inode: 'test-inode-1'
                } as DotContentDriveItem
            ]);
            spectator.detectChanges();

            publishButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.PUBLISH}"]`
            ) as HTMLElement;
            expect(publishButton?.style.display).not.toBe('none');
        });
    });
});
