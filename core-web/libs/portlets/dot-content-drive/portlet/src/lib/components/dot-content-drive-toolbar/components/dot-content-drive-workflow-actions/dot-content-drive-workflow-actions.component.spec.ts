import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@openng/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';

import { MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotContentDriveItem } from '@dotcms/dotcms-models';

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
    let messageService: SpyObject<MessageService>;
    let navigationService: SpyObject<DotContentDriveNavigationService>;

    const mockSelectedItems = signal<DotContentDriveItem[]>([]);

    const createComponent = createComponentFactory({
        component: DotContentDriveWorkflowActionsComponent,
        providers: [
            provideHttpClient(),
            mockProvider(DotContentDriveStore, {
                selectedItems: mockSelectedItems
            }),
            mockProvider(MessageService, {
                add: jest.fn()
            }),
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key) => key as string)
            }),
            mockProvider(DotContentDriveNavigationService, {
                editContent: jest.fn(),
                editPage: jest.fn()
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        messageService = spectator.inject(MessageService, true);
        navigationService = spectator.inject(DotContentDriveNavigationService, true);

        jest.spyOn(messageService, 'add');

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
            } as DotCMSContentlet;

            mockSelectedItems.set([mockItem]);
            spectator.detectChanges();

            const editContentButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.GOT_TO_EDIT_CONTENTLET}"]`
            );

            spectator.click(editContentButton!);

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
            } as DotCMSContentlet;

            mockSelectedItems.set([mockItem]);
            spectator.detectChanges();

            const editPageButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.GOT_TO_EDIT_PAGE}"]`
            );

            spectator.click(editPageButton!);

            expect(navigationService.editPage).toHaveBeenCalledWith(mockItem);
        });
    });

    describe('shouldShowAction method', () => {
        it('should return true when action has no showWhen conditions', () => {
            const action: ContentDriveWorkflowAction = {
                name: 'Test Action',
                id: WORKFLOW_ACTION_ID.DOWNLOAD
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
                name: 'Rename',
                id: WORKFLOW_ACTION_ID.RENAME,
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
                name: 'Rename',
                id: WORKFLOW_ACTION_ID.RENAME,
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
            } as unknown as DotCMSContentlet;

            mockSelectedItems.set([mockAsset]);
            spectator.detectChanges();

            const windowSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

            const downloadButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.DOWNLOAD}"]`
            );

            spectator.click(downloadButton!);

            expect(windowSpy).toHaveBeenCalledWith(
                expect.stringContaining('force_download=true'),
                '_self'
            );
            expect(windowSpy).toHaveBeenCalledWith(
                expect.stringContaining(mockAsset['fileAsset'] as string),
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
            } as unknown as DotCMSContentlet;

            mockSelectedItems.set([mockAsset]);
            spectator.detectChanges();

            jest.spyOn(window, 'open').mockImplementation(() => null);

            const downloadButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.DOWNLOAD}"]`
            );

            spectator.click(downloadButton!);

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
            } as unknown as DotCMSContentlet;

            mockSelectedItems.set([mockAsset]);
            spectator.detectChanges();

            const windowSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

            const downloadButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.DOWNLOAD}"]`
            );

            spectator.click(downloadButton!);

            expect(windowSpy).toHaveBeenCalledWith(
                expect.stringContaining(mockAsset['assetVersion'] as string),
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
            } as unknown as DotCMSContentlet;

            mockSelectedItems.set([mockAsset]);
            spectator.detectChanges();

            const windowSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

            const downloadButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.DOWNLOAD}"]`
            );

            spectator.click(downloadButton!);

            expect(windowSpy).toHaveBeenCalledWith(
                expect.stringContaining(mockAsset['fileAssetVersion'] as string),
                '_self'
            );

            windowSpy.mockRestore();
        });
    });

    describe('Integration Tests', () => {
        it('should handle selection changes and update button visibility', () => {
            mockSelectedItems.set([]);
            spectator.detectChanges();

            let editButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.GOT_TO_EDIT_CONTENTLET}"]`
            ) as HTMLElement;
            expect(editButton?.style.display).toBe('none');

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

            editButton = spectator.query(
                `[data-testid="workflow-action-${WORKFLOW_ACTION_ID.GOT_TO_EDIT_CONTENTLET}"]`
            ) as HTMLElement;
            expect(editButton?.style.display).not.toBe('none');
        });
    });
});
