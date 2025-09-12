/* eslint-disable @typescript-eslint/no-explicit-any */
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { patchState, signalStore, signalStoreFeature, withMethods, withState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';

import { ConfirmationService, MessageService } from 'primeng/api';

import {
    DotHttpErrorManagerService,
    DotVersionableService,
    DotMessageService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentletVersion,
    DotPagination,
    DotCMSResponse,
    DotCMSContentlet
} from '@dotcms/dotcms-models';

import { withHistory } from './history.feature';

import {
    DotHistoryTimelineItemAction,
    DotHistoryTimelineItemActionType
} from '../../../models/dot-edit-content.model';
import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { initialRootState } from '../../edit-content.store';

// Mock data
const mockContentletVersion: DotCMSContentletVersion = {
    archived: false,
    country: 'US',
    countryCode: 'US',
    experimentVariant: false,
    inode: '18f707db-ebf3-45f8-9b5a-d8bf6a6f383a',
    isoCode: 'en-US',
    language: 'English',
    languageCode: 'en',
    languageFlag: 'us',
    languageId: 1,
    live: true,
    modDate: 1701428400000, // 2023-12-01T10:00:00.000Z as timestamp
    modUser: 'dotcms.org.1',
    title: 'Test Content Version 1',
    working: true
};

const mockContentletVersion2: DotCMSContentletVersion = {
    ...mockContentletVersion,
    inode: '28f707db-ebf3-45f8-9b5a-d8bf6a6f384b',
    title: 'Test Content Version 2',
    modDate: 1701514800000, // 2023-12-02T10:00:00.000Z as timestamp
    live: false,
    working: false
};

const mockVersionsResponse: DotCMSResponse<DotCMSContentletVersion[]> = {
    entity: [mockContentletVersion, mockContentletVersion2],
    pagination: {
        currentPage: 1,
        perPage: 20,
        totalEntries: 2
    } as DotPagination,
    errors: [],
    i18nMessagesMap: {},
    messages: [],
    permissions: []
};

const mockVersionsResponsePage2: DotCMSResponse<DotCMSContentletVersion[]> = {
    entity: [
        {
            ...mockContentletVersion,
            inode: '38f707db-ebf3-45f8-9b5a-d8bf6a6f385c',
            title: 'Test Content Version 3'
        }
    ],
    pagination: {
        currentPage: 2,
        perPage: 20,
        totalEntries: 3
    } as DotPagination,
    errors: [],
    i18nMessagesMap: {},
    messages: [],
    permissions: []
};

const mockContentlet: DotCMSContentlet = {
    identifier: '758cb37699eae8500d64acc16ebc468e',
    inode: '18f707db-ebf3-45f8-9b5a-d8bf6a6f383a',
    title: 'Test Content',
    archived: false,
    baseType: 'CONTENT',
    contentType: 'TestContentType',
    folder: 'SYSTEM_FOLDER',
    hasLiveVersion: true,
    hasTitleImage: false,
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    hostName: 'demo.dotcms.com',
    languageId: 1,
    live: true,
    locked: false,
    modDate: '1701428400000',
    modUser: 'dotcms.org.1',
    modUserName: 'Admin User',
    owner: 'dotcms.org.1',
    sortOrder: 0,
    stInode: '0121c052881956cd95bfe5dde968ca07',
    url: '/content.40e5d7cd-2117-47d5-b96d-3278b188deeb',
    working: true,
    deleted: false,
    titleImage: 'test'
};

describe('HistoryFeature', () => {
    let spectator: SpectatorService<any>;
    let store: any;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;
    let confirmationService: SpyObject<ConfirmationService>;
    let dotVersionableService: SpyObject<DotVersionableService>;
    let messageService: SpyObject<MessageService>;
    let dotMessageService: SpyObject<DotMessageService>;

    const withTest = () =>
        signalStoreFeature(
            withState({
                ...initialRootState,
                contentlet: mockContentlet
            }),
            withMethods((store) => ({
                updateContentlet: (contentlet: DotCMSContentlet | null) => {
                    patchState(store, { contentlet });
                },
                updateVersions: (versions: DotCMSContentletVersion[]) => {
                    patchState(store, { versions });
                },
                updateVersionsPagination: (pagination: DotPagination | null) => {
                    patchState(store, { versionsPagination: pagination });
                }
            }))
        );

    const createStore = createServiceFactory({
        service: signalStore(withTest(), withHistory()),
        mocks: [
            DotEditContentService,
            DotHttpErrorManagerService,
            ConfirmationService,
            DotVersionableService,
            MessageService,
            DotMessageService
        ]
    });

    beforeEach(() => {
        spectator = createStore();
        store = spectator.service;
        dotEditContentService = spectator.inject(DotEditContentService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
        confirmationService = spectator.inject(ConfirmationService);
        dotVersionableService = spectator.inject(DotVersionableService);
        messageService = spectator.inject(MessageService);
        dotMessageService = spectator.inject(DotMessageService);

        // Setup default message service responses
        dotMessageService.get.mockImplementation((key: string) => {
            const messages: Record<string, string> = {
                Success: 'Success',
                'edit.content.sidebar.history.version.deleted.successfully':
                    'Version deleted successfully',
                'edit.content.sidebar.history.delete.confirm.message':
                    'Are you sure you want to delete this version?',
                'edit.content.sidebar.history.delete.confirm.header': 'Delete Version',
                'edit.content.sidebar.history.delete.confirm.accept': 'Delete',
                'edit.content.sidebar.history.delete.confirm.reject': 'Cancel'
            };
            return messages[key] || key;
        });
    });

    describe('Store Initialization', () => {
        it('should initialize with empty versions state', () => {
            expect(store.versions()).toEqual([]);
            expect(store.versionsPagination()).toBeNull();
            expect(store.versionsStatus()).toEqual({
                status: ComponentStatus.INIT,
                error: null
            });
        });
    });

    describe('loadVersions', () => {
        it('should load initial versions (page 1) and set loading state', fakeAsync(() => {
            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponse));

            store.loadVersions({ identifier: 'test-identifier', page: 1 });
            tick();

            expect(dotEditContentService.getVersions).toHaveBeenCalledWith('test-identifier', {
                offset: 1,
                limit: 20
            });
            expect(store.versions()).toEqual(mockVersionsResponse.entity);
            expect(store.versionsPagination()).toEqual(mockVersionsResponse.pagination);
            expect(store.versionsStatus()).toEqual({
                status: ComponentStatus.LOADED,
                error: null
            });
        }));

        it('should show loading state only on initial load (page 1)', fakeAsync(() => {
            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponse));

            store.loadVersions({ identifier: 'test-identifier', page: 1 });
            tick();

            expect(store.versionsStatus().status).toBe(ComponentStatus.LOADED);
            expect(store.versions()).toEqual(mockVersionsResponse.entity);
        }));

        it('should not show loading state on subsequent pages (page 2+)', fakeAsync(() => {
            // Setup initial state with existing versions
            store.updateVersions([mockContentletVersion]);
            store.updateVersionsPagination(mockVersionsResponse.pagination);

            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponsePage2));

            store.loadVersions({ identifier: 'test-identifier', page: 2 });
            tick();

            expect(store.versionsStatus().status).toBe(ComponentStatus.LOADED);
            // Should have accumulated versions
            expect(store.versions().length).toBe(2); // original + new from page 2
        }));

        it('should accumulate versions on subsequent pages', fakeAsync(() => {
            // Setup initial state with existing versions
            store.updateVersions([mockContentletVersion]);
            store.updateVersionsPagination(mockVersionsResponse.pagination);

            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponsePage2));

            store.loadVersions({ identifier: 'test-identifier', page: 2 });
            tick();

            expect(store.versions()).toEqual([
                mockContentletVersion,
                ...mockVersionsResponsePage2.entity
            ]);
        }));

        it('should reset versions when loading page 1 with existing content', fakeAsync(() => {
            // Setup initial state with existing versions
            store.updateVersions([mockContentletVersion]);
            store.updateVersionsPagination(mockVersionsResponse.pagination);

            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponse));

            store.loadVersions({ identifier: 'test-identifier', page: 1 });
            tick();

            expect(store.versions()).toEqual(mockVersionsResponse.entity);
        }));

        it('should reset versions when loading new content (no existing pagination)', fakeAsync(() => {
            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponse));

            store.loadVersions({ identifier: 'new-identifier', page: 2 });
            tick();

            // Should reset even on page 2 if it's new content
            expect(store.versions()).toEqual(mockVersionsResponse.entity);
        }));

        it('should use custom pagination limit from current state', fakeAsync(() => {
            const customPagination: DotPagination = {
                currentPage: 1,
                perPage: 10,
                totalEntries: 2
            };
            store.updateVersionsPagination(customPagination);

            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponse));

            store.loadVersions({ identifier: 'test-identifier', page: 1 });
            tick();

            expect(dotEditContentService.getVersions).toHaveBeenCalledWith('test-identifier', {
                offset: 1,
                limit: 10
            });
        }));

        it('should handle errors and update error state', fakeAsync(() => {
            const error = new HttpErrorResponse({ error: 'Test error', status: 500 });
            dotEditContentService.getVersions.mockReturnValue(throwError(() => error));

            store.loadVersions({ identifier: 'test-identifier', page: 1 });
            tick();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
            expect(store.versionsStatus().status).toBe(ComponentStatus.ERROR);
        }));
    });

    describe('deleteVersion', () => {
        beforeEach(() => {
            store.updateContentlet(mockContentlet);
        });

        it('should delete version and reload versions list', fakeAsync(() => {
            dotVersionableService.deleteVersion.mockReturnValue(of({}));
            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponse));

            store.deleteVersion('test-inode');
            tick();

            expect(dotVersionableService.deleteVersion).toHaveBeenCalledWith('test-inode');
            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'success',
                summary: 'Success',
                detail: 'Version deleted successfully'
            });
            expect(dotEditContentService.getVersions).toHaveBeenCalledWith(
                mockContentlet.identifier,
                { offset: 1, limit: 20 }
            );
            expect(store.versions()).toEqual(mockVersionsResponse.entity);
        }));

        it('should set loading state during versions reload', fakeAsync(() => {
            dotVersionableService.deleteVersion.mockReturnValue(of({}));
            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponse));

            store.deleteVersion('test-inode');
            tick();

            expect(store.versionsStatus().status).toBe(ComponentStatus.LOADED);
            expect(store.versions()).toEqual(mockVersionsResponse.entity);
        }));

        it('should handle deletion error', fakeAsync(() => {
            const error = new HttpErrorResponse({ error: 'Delete failed', status: 500 });
            dotVersionableService.deleteVersion.mockReturnValue(throwError(() => error));

            store.deleteVersion('test-inode');
            tick();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
            expect(store.versionsStatus().status).toBe(ComponentStatus.ERROR);
        }));

        it('should handle versions reload error after successful deletion', fakeAsync(() => {
            const reloadError = new HttpErrorResponse({ error: 'Reload failed', status: 500 });
            dotVersionableService.deleteVersion.mockReturnValue(of({}));
            dotEditContentService.getVersions.mockReturnValue(throwError(() => reloadError));

            store.deleteVersion('test-inode');
            tick();

            expect(messageService.add).toHaveBeenCalled(); // Success message still shown
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
            expect(store.versionsStatus().status).toBe(ComponentStatus.ERROR);
        }));

        it('should not reload versions if no contentlet identifier', fakeAsync(() => {
            store.updateContentlet(null);
            dotVersionableService.deleteVersion.mockReturnValue(of({}));

            store.deleteVersion('test-inode');
            tick();

            expect(dotVersionableService.deleteVersion).toHaveBeenCalledWith('test-inode');
            expect(messageService.add).toHaveBeenCalled();
            expect(dotEditContentService.getVersions).not.toHaveBeenCalled();
        }));
    });

    describe('handleHistoryAction', () => {
        const mockAction = (
            type: DotHistoryTimelineItemActionType
        ): DotHistoryTimelineItemAction => ({
            type,
            item: mockContentletVersion
        });

        it('should handle PREVIEW action', () => {
            const consoleSpy = jest.spyOn(console, 'log').mockImplementation();

            store.handleHistoryAction(mockAction(DotHistoryTimelineItemActionType.PREVIEW));

            expect(consoleSpy).toHaveBeenCalledWith('Preview version:', mockContentletVersion);
            consoleSpy.mockRestore();
        });

        it('should handle RESTORE action', () => {
            const consoleSpy = jest.spyOn(console, 'log').mockImplementation();

            store.handleHistoryAction(mockAction(DotHistoryTimelineItemActionType.RESTORE));

            expect(consoleSpy).toHaveBeenCalledWith('Restore version:', mockContentletVersion);
            consoleSpy.mockRestore();
        });

        it('should handle COMPARE action', () => {
            const consoleSpy = jest.spyOn(console, 'log').mockImplementation();

            store.handleHistoryAction(mockAction(DotHistoryTimelineItemActionType.COMPARE));

            expect(consoleSpy).toHaveBeenCalledWith('Compare version:', mockContentletVersion);
            consoleSpy.mockRestore();
        });

        it('should handle DELETE action with confirmation dialog', () => {
            store.handleHistoryAction(mockAction(DotHistoryTimelineItemActionType.DELETE));

            expect(confirmationService.confirm).toHaveBeenCalled();

            // Verify the confirmation was called with the correct message
            const confirmCall = confirmationService.confirm.mock.calls[0][0];
            expect(confirmCall.message).toBe('Are you sure you want to delete this version?');
            expect(confirmCall.header).toBe('Delete Version');
            expect(confirmCall.acceptLabel).toBe('Delete');
            expect(confirmCall.rejectLabel).toBe('Cancel');
        });

        it('should call deleteVersion when DELETE action is confirmed', fakeAsync(() => {
            dotVersionableService.deleteVersion.mockReturnValue(of({}));
            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponse));
            store.updateContentlet(mockContentlet);

            // Mock confirmation service to auto-accept
            confirmationService.confirm.mockImplementation((config: any) => {
                if (config.accept) {
                    config.accept();
                }
                return confirmationService;
            });

            store.handleHistoryAction(mockAction(DotHistoryTimelineItemActionType.DELETE));
            tick();

            expect(dotVersionableService.deleteVersion).toHaveBeenCalledWith(
                mockContentletVersion.inode
            );
        }));
    });

    describe('resetVersions', () => {
        it('should reset versions to empty array', () => {
            store.updateVersions([mockContentletVersion, mockContentletVersion2]);

            store.resetVersions();

            expect(store.versions()).toEqual([]);
        });

        it('should not affect other state properties', () => {
            const initialPagination = mockVersionsResponse.pagination;
            const initialStatus = { status: ComponentStatus.LOADED, error: null };

            store.updateVersionsPagination(initialPagination);
            patchState(store, { versionsStatus: initialStatus });

            store.resetVersions();

            expect(store.versionsPagination()).toEqual(initialPagination);
            expect(store.versionsStatus()).toEqual(initialStatus);
        });
    });

    describe('clearVersions', () => {
        it('should clear all versions state', () => {
            store.updateVersions([mockContentletVersion, mockContentletVersion2]);
            store.updateVersionsPagination(mockVersionsResponse.pagination);
            patchState(store, {
                versionsStatus: { status: ComponentStatus.LOADED, error: null }
            });

            store.clearVersions();

            expect(store.versions()).toEqual([]);
            expect(store.versionsPagination()).toBeNull();
            expect(store.versionsStatus()).toEqual({
                status: ComponentStatus.INIT,
                error: null
            });
        });
    });

    describe('Edge Cases and Integration', () => {
        it('should handle multiple rapid loadVersions calls', fakeAsync(() => {
            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponse));

            // Simulate rapid calls
            store.loadVersions({ identifier: 'test-identifier', page: 1 });
            store.loadVersions({ identifier: 'test-identifier', page: 1 });
            store.loadVersions({ identifier: 'test-identifier', page: 1 });

            tick();

            // Should handle gracefully without errors
            expect(store.versions()).toEqual(mockVersionsResponse.entity);
            expect(store.versionsStatus().status).toBe(ComponentStatus.LOADED);
        }));

        it('should handle empty versions response', fakeAsync(() => {
            const emptyResponse: DotCMSResponse<DotCMSContentletVersion[]> = {
                entity: [],
                pagination: { currentPage: 1, perPage: 20, totalEntries: 0 } as DotPagination,
                errors: [],
                i18nMessagesMap: {},
                messages: [],
                permissions: []
            };
            dotEditContentService.getVersions.mockReturnValue(of(emptyResponse));

            store.loadVersions({ identifier: 'test-identifier', page: 1 });
            tick();

            expect(store.versions()).toEqual([]);
            expect(store.versionsStatus().status).toBe(ComponentStatus.LOADED);
        }));

        it('should maintain state consistency during error recovery', fakeAsync(() => {
            // First, load successfully
            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponse));
            store.loadVersions({ identifier: 'test-identifier', page: 1 });
            tick();

            expect(store.versions()).toEqual(mockVersionsResponse.entity);

            // Then, simulate an error on next load
            const error = new HttpErrorResponse({ error: 'Network error', status: 500 });
            dotEditContentService.getVersions.mockReturnValue(throwError(() => error));
            store.loadVersions({ identifier: 'test-identifier', page: 2 });
            tick();

            // Versions should remain from successful load
            expect(store.versions()).toEqual(mockVersionsResponse.entity);
            expect(store.versionsStatus().status).toBe(ComponentStatus.ERROR);
        }));
    });
});
