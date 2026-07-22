/* eslint-disable @typescript-eslint/no-explicit-any */
import { patchState, signalStore, signalStoreFeature, withMethods, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService, SpyObject } from '@openng/spectator/jest';
import { of, Subject, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';

import {
    DotHttpErrorManagerService,
    DotVersionableService,
    DotMessageService,
    DotContentletService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentletVersion,
    DotPagination,
    DotCMSResponse,
    DotCMSContentlet
} from '@dotcms/dotcms-models';
import { createFakeLanguage } from '@dotcms/utils-testing';

import {
    withHistory,
    DEFAULT_VERSIONS_PER_PAGE,
    DEFAULT_PUSH_PUBLISH_HISTORY_PER_PAGE,
    DEFAULT_LOCALE_ISO_KEY,
    HISTORY_SIDEBAR_TAB_INDEX
} from './history.feature';

import {
    DotHistoryTimelineItemAction,
    DotHistoryTimelineItemActionType,
    DotPushPublishHistoryItem
} from '../../../models/dot-edit-content.model';
import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { EDIT_CONTENT_HOST } from '../../../services/host/edit-content-host.model';
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
    modUserName: 'Admin User',
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
        perPage: DEFAULT_VERSIONS_PER_PAGE,
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
        perPage: DEFAULT_VERSIONS_PER_PAGE,
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

// Mock data for push publish history
const mockPushPublishHistoryItem: DotPushPublishHistoryItem = {
    bundleId: '01K6NY6Z8V92T6SAF582WMTKYQ',
    environment: 'production',
    pushDate: 1701428400000, // 2023-12-01T10:00:00.000Z as timestamp
    pushedBy: 'Admin User'
};

const mockPushPublishHistoryItem2: DotPushPublishHistoryItem = {
    bundleId: '01K6NY6Z8V92T6SAF582WMTLAB',
    environment: 'staging',
    pushDate: 1701514800000, // 2023-12-02T10:00:00.000Z as timestamp
    pushedBy: 'Content Editor'
};

const mockPushPublishHistoryResponse: DotCMSResponse<DotPushPublishHistoryItem[]> = {
    entity: [mockPushPublishHistoryItem, mockPushPublishHistoryItem2],
    pagination: {
        currentPage: 1,
        perPage: DEFAULT_PUSH_PUBLISH_HISTORY_PER_PAGE,
        totalEntries: 2
    } as DotPagination,
    errors: [],
    i18nMessagesMap: {},
    messages: [],
    permissions: []
};

const mockPushPublishHistoryResponsePage2: DotCMSResponse<DotPushPublishHistoryItem[]> = {
    entity: [
        {
            bundleId: '01K6NY6Z8V92T6SAF582WMTMCD',
            environment: 'production',
            pushDate: 1701601200000, // 2023-12-03T10:00:00.000Z as timestamp
            pushedBy: 'Admin User'
        }
    ],
    pagination: {
        currentPage: 2,
        perPage: DEFAULT_PUSH_PUBLISH_HISTORY_PER_PAGE,
        totalEntries: 3
    } as DotPagination,
    errors: [],
    i18nMessagesMap: {},
    messages: [],
    permissions: []
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
    let dotContentletService: SpyObject<DotContentletService>;

    // Restore navigation is delegated to the EditContentHost port.
    const mockHost = {
        setContentTitle: jest.fn(),
        addBreadcrumb: jest.fn(),
        goToSavedContent: jest.fn(),
        goToRestoredVersion: jest.fn()
    };

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
                },
                updatePushPublishHistory: (pushPublishHistory: DotPushPublishHistoryItem[]) => {
                    patchState(store, { pushPublishHistory });
                },
                updatePushPublishHistoryPagination: (pagination: DotPagination | null) => {
                    patchState(store, { pushPublishHistoryPagination: pagination });
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
            DotMessageService,
            DotContentletService,
            Router
        ],
        providers: [{ provide: EDIT_CONTENT_HOST, useValue: mockHost }]
    });

    beforeEach(() => {
        Object.values(mockHost).forEach((fn) => fn.mockClear());
        spectator = createStore();
        store = spectator.service;
        dotEditContentService = spectator.inject(DotEditContentService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
        confirmationService = spectator.inject(ConfirmationService);
        dotVersionableService = spectator.inject(DotVersionableService);
        messageService = spectator.inject(MessageService);
        dotMessageService = spectator.inject(DotMessageService);
        dotContentletService = spectator.inject(DotContentletService);

        // Setup default message service responses
        dotMessageService.get.mockImplementation((key: string) => {
            const messages: Record<string, string> = {
                Success: 'Success',
                success: 'Success',
                'edit.content.sidebar.history.version.deleted.successfully':
                    'Version deleted successfully',
                'edit.content.sidebar.history.delete.confirm.message':
                    'Are you sure you want to delete this version?',
                'edit.content.sidebar.history.delete.confirm.header': 'Delete Version',
                'edit.content.sidebar.history.delete.confirm.accept': 'Delete',
                'edit.content.sidebar.history.delete.confirm.reject': 'Cancel',
                'edit.content.sidebar.history.restore.confirm.message':
                    'Are you sure you want to restore this version?',
                'edit.content.sidebar.history.restore.confirm.header': 'Restore Version',
                'edit.content.sidebar.history.restore.confirm.accept': 'Restore',
                'edit.content.sidebar.history.restore.confirm.reject': 'Cancel',
                'edit.content.sidebar.history.push.publish.delete.all.confirm.message':
                    'Are you sure you want to delete all push publish history for this content?',
                'edit.content.sidebar.history.push.publish.delete.all.confirm.header':
                    'Delete All Push Publish History',
                'edit.content.sidebar.history.push.publish.delete.all.success':
                    'Push publish history deleted successfully',
                'edit.content.sidebar.history.push.publish.delete.all.error':
                    'Failed to delete push publish history',
                Error: 'Error',
                'edit.content.sidebar.history.load.error': 'Failed to load version content'
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

        it('should initialize with empty push publish history state', () => {
            expect(store.pushPublishHistory()).toEqual([]);
            expect(store.pushPublishHistoryPagination()).toBeNull();
            expect(store.pushPublishHistoryStatus()).toEqual({
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

            expect(dotEditContentService.getVersions).toHaveBeenCalledWith(
                'test-identifier',
                {
                    offset: 1,
                    limit: DEFAULT_VERSIONS_PER_PAGE
                },
                mockContentlet.languageId
            );
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

            expect(dotEditContentService.getVersions).toHaveBeenCalledWith(
                'test-identifier',
                {
                    offset: 1,
                    limit: 10
                },
                mockContentlet.languageId
            );
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

    describe('loadPushPublishHistory', () => {
        it('should load initial push publish history (page 1) and set loading state', fakeAsync(() => {
            dotEditContentService.getPushPublishHistory.mockReturnValue(
                of(mockPushPublishHistoryResponse)
            );

            store.loadPushPublishHistory({ identifier: 'test-identifier', page: 1 });
            tick();

            expect(dotEditContentService.getPushPublishHistory).toHaveBeenCalledWith(
                'test-identifier',
                {
                    offset: 1,
                    limit: DEFAULT_PUSH_PUBLISH_HISTORY_PER_PAGE
                }
            );
            // Store sorts push publish history by pushDate descending
            const expectedSorted = [...mockPushPublishHistoryResponse.entity].sort(
                (a, b) => b.pushDate - a.pushDate
            );
            expect(store.pushPublishHistory()).toEqual(expectedSorted);
            expect(store.pushPublishHistoryPagination()).toEqual(
                mockPushPublishHistoryResponse.pagination
            );
            expect(store.pushPublishHistoryStatus()).toEqual({
                status: ComponentStatus.LOADED,
                error: null
            });
        }));

        it('should accumulate push publish history on subsequent pages', fakeAsync(() => {
            // Setup initial state with existing push publish history
            store.updatePushPublishHistory([mockPushPublishHistoryItem]);
            store.updatePushPublishHistoryPagination(mockPushPublishHistoryResponse.pagination);

            dotEditContentService.getPushPublishHistory.mockReturnValue(
                of(mockPushPublishHistoryResponsePage2)
            );

            store.loadPushPublishHistory({ identifier: 'test-identifier', page: 2 });
            tick();

            // Store sorts accumulated push publish history by pushDate descending
            const accumulated = [
                mockPushPublishHistoryItem,
                ...mockPushPublishHistoryResponsePage2.entity
            ];
            const expectedSorted = [...accumulated].sort((a, b) => b.pushDate - a.pushDate);
            expect(store.pushPublishHistory()).toEqual(expectedSorted);
        }));

        it('should handle errors and update error state', fakeAsync(() => {
            const error = new HttpErrorResponse({ error: 'Test error', status: 500 });
            dotEditContentService.getPushPublishHistory.mockReturnValue(throwError(() => error));

            store.loadPushPublishHistory({ identifier: 'test-identifier', page: 1 });
            tick();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
            expect(store.pushPublishHistoryStatus().status).toBe(ComponentStatus.ERROR);
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
                { offset: 1, limit: DEFAULT_VERSIONS_PER_PAGE },
                mockContentlet.languageId
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

        it('should handle VIEW action with working version - exit historical view', () => {
            const workingVersionAction: DotHistoryTimelineItemAction = {
                type: DotHistoryTimelineItemActionType.VIEW,
                item: { ...mockContentletVersion, working: true }
            };

            // Setup store with historical version state
            patchState(store, {
                isViewingHistoricalVersion: true,
                originalContentlet: mockContentlet,
                historicalVersionInode: 'historical-inode'
            });

            store.handleHistoryAction(workingVersionAction);

            expect(store.isViewingHistoricalVersion()).toBe(false);
            expect(store.contentlet()).toEqual(mockContentlet);
            expect(store.originalContentlet()).toBeNull();
            expect(store.historicalVersionInode()).toBeNull();
        });

        it('should handle VIEW action with historical version - load version content', fakeAsync(() => {
            const historicalVersionAction: DotHistoryTimelineItemAction = {
                type: DotHistoryTimelineItemActionType.VIEW,
                item: { ...mockContentletVersion, working: false }
            };

            dotContentletService.getContentletByInode.mockReturnValue(of(mockContentlet));

            store.handleHistoryAction(historicalVersionAction);
            tick();

            expect(dotContentletService.getContentletByInode).toHaveBeenCalledWith(
                mockContentletVersion.inode
            );
        }));

        it('should handle PREVIEW action', () => {
            // Currently a no-op with TODO comment
            expect(() => {
                store.handleHistoryAction(mockAction(DotHistoryTimelineItemActionType.PREVIEW));
            }).not.toThrow();
        });

        it('should handle RESTORE action with confirmation', () => {
            store.handleHistoryAction(mockAction(DotHistoryTimelineItemActionType.RESTORE));

            expect(confirmationService.confirm).toHaveBeenCalled();

            const confirmCall = confirmationService.confirm.mock.calls[0][0];
            expect(confirmCall.message).toBe('Are you sure you want to restore this version?');
            expect(confirmCall.header).toBe('Restore Version');
            expect(confirmCall.acceptLabel).toBe('Restore');
            expect(confirmCall.rejectLabel).toBe('Cancel');
        });

        it('should handle COMPARE action without throwing', () => {
            expect(() => {
                store.handleHistoryAction(mockAction(DotHistoryTimelineItemActionType.COMPARE));
            }).not.toThrow();
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

        describe('VIEW action while in compare view', () => {
            const previousCompareContent = {
                ...mockContentlet,
                inode: 'previous-compare-inode',
                title: 'Previous Compare'
            };

            beforeEach(() => {
                patchState(store, {
                    compareContentlet: previousCompareContent,
                    historicalVersionInode: 'previous-compare-inode',
                    uiState: { ...store.uiState(), view: 'compare' }
                });
            });

            it('should update the compared version and stay in compare view when clicking another version', fakeAsync(() => {
                const clickedContent = {
                    ...mockContentlet,
                    inode: 'clicked-inode',
                    title: 'Clicked Version'
                };
                dotContentletService.getContentletByInode.mockReturnValue(of(clickedContent));

                store.handleHistoryAction({
                    type: DotHistoryTimelineItemActionType.VIEW,
                    item: { ...mockContentletVersion, inode: 'clicked-inode', working: false }
                });
                tick();

                expect(dotContentletService.getContentletByInode).toHaveBeenCalledWith(
                    'clicked-inode'
                );
                expect(store.uiState().view).toBe('compare');
                expect(store.compareContentlet()).toEqual(clickedContent);
                // The current-version side stays fixed (no full-viewport navigation)
                expect(store.contentlet()).toEqual(mockContentlet);
            }));

            it('should mark the newly compared version as active in the version list', fakeAsync(() => {
                const clickedContent = { ...mockContentlet, inode: 'clicked-inode' };
                dotContentletService.getContentletByInode.mockReturnValue(of(clickedContent));

                store.handleHistoryAction({
                    type: DotHistoryTimelineItemActionType.VIEW,
                    item: { ...mockContentletVersion, inode: 'clicked-inode', working: false }
                });
                tick();

                expect(store.historicalVersionInode()).toBe('clicked-inode');
            }));

            it('should exit compare view when clicking the working version', () => {
                store.handleHistoryAction({
                    type: DotHistoryTimelineItemActionType.VIEW,
                    item: { ...mockContentletVersion, working: true }
                });

                expect(store.uiState().view).toBe('form');
                expect(store.compareContentlet()).toBeNull();
                expect(store.historicalVersionInode()).toBeNull();
            });

            it('should keep loading versions full-viewport when not in compare view', fakeAsync(() => {
                patchState(store, {
                    compareContentlet: null,
                    uiState: { ...store.uiState(), view: 'form' }
                });
                const clickedContent = { ...mockContentlet, inode: 'clicked-inode' };
                dotContentletService.getContentletByInode.mockReturnValue(of(clickedContent));

                store.handleHistoryAction({
                    type: DotHistoryTimelineItemActionType.VIEW,
                    item: { ...mockContentletVersion, inode: 'clicked-inode', working: false }
                });
                tick();

                expect(store.uiState().view).toBe('form');
                expect(store.contentlet()).toEqual(clickedContent);
                expect(store.isViewingHistoricalVersion()).toBe(true);
            }));
        });
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

    describe('resetPushPublishHistory', () => {
        it('should reset push publish history to empty array', () => {
            store.updatePushPublishHistory([
                mockPushPublishHistoryItem,
                mockPushPublishHistoryItem2
            ]);

            store.resetPushPublishHistory();

            expect(store.pushPublishHistory()).toEqual([]);
        });

        it('should not affect other push publish history state properties', () => {
            const initialPagination = mockPushPublishHistoryResponse.pagination;
            const initialStatus = { status: ComponentStatus.LOADED, error: null };

            store.updatePushPublishHistoryPagination(initialPagination);
            patchState(store, { pushPublishHistoryStatus: initialStatus });

            store.resetPushPublishHistory();

            expect(store.pushPublishHistoryPagination()).toEqual(initialPagination);
            expect(store.pushPublishHistoryStatus()).toEqual(initialStatus);
        });
    });

    describe('clearPushPublishHistory', () => {
        it('should clear all push publish history state', () => {
            store.updatePushPublishHistory([
                mockPushPublishHistoryItem,
                mockPushPublishHistoryItem2
            ]);
            store.updatePushPublishHistoryPagination(mockPushPublishHistoryResponse.pagination);
            patchState(store, {
                pushPublishHistoryStatus: { status: ComponentStatus.LOADED, error: null }
            });

            store.clearPushPublishHistory();

            expect(store.pushPublishHistory()).toEqual([]);
            expect(store.pushPublishHistoryPagination()).toBeNull();
            expect(store.pushPublishHistoryStatus()).toEqual({
                status: ComponentStatus.INIT,
                error: null
            });
        });
    });

    describe('deletePushPublishHistory', () => {
        it('should delete push publish history successfully', fakeAsync(() => {
            // Mock service call
            dotEditContentService.deletePushPublishHistory.mockReturnValue(of({}));

            // Mock confirmation service to auto-accept
            confirmationService.confirm.mockImplementation((config: any) => {
                if (config.accept) {
                    config.accept();
                }
                return confirmationService;
            });

            store.deletePushPublishHistory('test-identifier');
            tick();

            // Verify service was called
            expect(dotEditContentService.deletePushPublishHistory).toHaveBeenCalledWith(
                'test-identifier'
            );

            // Verify state was cleared
            expect(store.pushPublishHistory()).toEqual([]);
            expect(store.pushPublishHistoryPagination()).toBeNull();
        }));
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
                pagination: {
                    currentPage: 1,
                    perPage: DEFAULT_VERSIONS_PER_PAGE,
                    totalEntries: 0
                } as DotPagination,
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

    describe('Automatic Version Loading Effect', () => {
        beforeEach(() => {
            dotEditContentService.getVersions.mockReturnValue(of(mockVersionsResponse));
            dotEditContentService.getPushPublishHistory.mockReturnValue(
                of(mockPushPublishHistoryResponse)
            );
        });

        it('should automatically load versions when contentlet changes', fakeAsync(() => {
            const newContentlet = {
                ...mockContentlet,
                identifier: 'new-identifier-123',
                inode: 'new-inode-456',
                languageId: 2
            };

            store.updateContentlet(newContentlet);
            spectator.flushEffects();
            tick();

            expect(dotEditContentService.getVersions).toHaveBeenCalledWith(
                'new-identifier-123',
                { offset: 1, limit: DEFAULT_VERSIONS_PER_PAGE },
                2 // The contentlet's languageId
            );
            expect(store.versions()).toEqual(mockVersionsResponse.entity);
        }));

        it('should automatically load push publish history when contentlet changes', fakeAsync(() => {
            const newContentlet = {
                ...mockContentlet,
                identifier: 'new-identifier-123',
                inode: 'new-inode-456',
                languageId: 2
            };

            store.updateContentlet(newContentlet);
            spectator.flushEffects();
            tick();

            expect(dotEditContentService.getPushPublishHistory).toHaveBeenCalledWith(
                'new-identifier-123',
                { offset: 1, limit: DEFAULT_PUSH_PUBLISH_HISTORY_PER_PAGE }
            );
            // Store sorts push publish history by pushDate descending
            const expectedSortedHistory = [...mockPushPublishHistoryResponse.entity].sort(
                (a, b) => b.pushDate - a.pushDate
            );
            expect(store.pushPublishHistory()).toEqual(expectedSortedHistory);
        }));

        it('should automatically load versions when contentlet languageId changes', fakeAsync(() => {
            const updatedContentlet = {
                ...mockContentlet,
                languageId: 2 // Changed from original languageId: 1
            };

            store.updateContentlet(updatedContentlet);
            spectator.flushEffects();
            tick();

            expect(dotEditContentService.getVersions).toHaveBeenCalledWith(
                mockContentlet.identifier,
                { offset: 1, limit: DEFAULT_VERSIONS_PER_PAGE },
                2 // The updated languageId
            );
            expect(store.versions()).toEqual(mockVersionsResponse.entity);
        }));

        it('should replace versions and push publish history when the contentlet changes', fakeAsync(() => {
            // Setup initial data
            store.updateVersions([mockContentletVersion]);
            store.updatePushPublishHistory([mockPushPublishHistoryItem]);
            expect(store.versions()).toHaveLength(1);
            expect(store.pushPublishHistory()).toHaveLength(1);

            const newContentlet = {
                ...mockContentlet,
                identifier: 'new-identifier-789',
                languageId: 3
            };

            store.updateContentlet(newContentlet);
            spectator.flushEffects();
            tick();

            // Both datasets should be cleared and then reloaded
            expect(store.versions()).toEqual(mockVersionsResponse.entity);
            // Store sorts push publish history by pushDate descending
            const expectedSortedAfterClear = [...mockPushPublishHistoryResponse.entity].sort(
                (a, b) => b.pushDate - a.pushDate
            );
            expect(store.pushPublishHistory()).toEqual(expectedSortedAfterClear);
        }));

        it('should not reload anything when only the version inode changes', fakeAsync(() => {
            spectator.flushEffects();
            tick();
            dotEditContentService.getVersions.mockClear();
            dotEditContentService.getPushPublishHistory.mockClear();

            store.updateContentlet({ ...mockContentlet, inode: 'another-version-inode' });
            spectator.flushEffects();
            tick();

            expect(dotEditContentService.getVersions).not.toHaveBeenCalled();
            expect(dotEditContentService.getPushPublishHistory).not.toHaveBeenCalled();
        }));

        it('should reload versions but not push publish history when only the language changes', fakeAsync(() => {
            spectator.flushEffects();
            tick();
            dotEditContentService.getVersions.mockClear();
            dotEditContentService.getPushPublishHistory.mockClear();

            store.updateContentlet({ ...mockContentlet, languageId: 2 });
            spectator.flushEffects();
            tick();

            expect(dotEditContentService.getVersions).toHaveBeenCalledWith(
                mockContentlet.identifier,
                { offset: 1, limit: DEFAULT_VERSIONS_PER_PAGE },
                2
            );
            expect(dotEditContentService.getPushPublishHistory).not.toHaveBeenCalled();
        }));

        it('should discard compare and historical state when the locale changes', fakeAsync(() => {
            spectator.flushEffects();
            tick();

            patchState(store, {
                compareContentlet: { ...mockContentlet, inode: 'compare-inode' },
                historicalVersionInode: 'compare-inode',
                originalContentlet: mockContentlet,
                isViewingHistoricalVersion: false
            });

            store.updateContentlet({ ...mockContentlet, languageId: 2 });
            spectator.flushEffects();
            tick();

            expect(store.compareContentlet()).toBeNull();
            expect(store.historicalVersionInode()).toBeNull();
            expect(store.originalContentlet()).toBeNull();
            expect(store.isViewingHistoricalVersion()).toBe(false);
        }));

        it('should exit compare view when leaving the History sidebar tab', fakeAsync(() => {
            spectator.flushEffects();
            tick();

            patchState(store, {
                compareContentlet: { ...mockContentlet, inode: 'compare-inode' },
                historicalVersionInode: 'compare-inode',
                uiState: {
                    ...store.uiState(),
                    view: 'compare',
                    activeSidebarTab: HISTORY_SIDEBAR_TAB_INDEX
                }
            });
            spectator.flushEffects();

            patchState(store, {
                uiState: { ...store.uiState(), activeSidebarTab: 0 }
            });
            spectator.flushEffects();

            expect(store.uiState().view).toBe('form');
            expect(store.compareContentlet()).toBeNull();
            expect(store.historicalVersionInode()).toBeNull();
        }));

        it('should keep compare view while switching within the History sidebar tab', fakeAsync(() => {
            spectator.flushEffects();
            tick();

            const compareContent = { ...mockContentlet, inode: 'compare-inode' };
            patchState(store, {
                compareContentlet: compareContent,
                uiState: {
                    ...store.uiState(),
                    view: 'compare',
                    activeSidebarTab: HISTORY_SIDEBAR_TAB_INDEX
                }
            });
            spectator.flushEffects();

            expect(store.uiState().view).toBe('compare');
            expect(store.compareContentlet()).toEqual(compareContent);
        }));

        it('should keep compare and historical state when only the version inode changes', fakeAsync(() => {
            spectator.flushEffects();
            tick();

            const compareContent = { ...mockContentlet, inode: 'compare-inode' };
            patchState(store, {
                compareContentlet: compareContent,
                historicalVersionInode: 'compare-inode',
                originalContentlet: mockContentlet
            });

            store.updateContentlet({ ...mockContentlet, inode: 'another-version-inode' });
            spectator.flushEffects();
            tick();

            expect(store.compareContentlet()).toEqual(compareContent);
            expect(store.historicalVersionInode()).toBe('compare-inode');
            expect(store.originalContentlet()).toEqual(mockContentlet);
        }));

        it('should not load data if contentlet has no identifier', fakeAsync(() => {
            const contentletWithoutIdentifier = {
                ...mockContentlet,
                identifier: null
            };

            store.updateContentlet(contentletWithoutIdentifier);
            spectator.flushEffects();
            tick();

            expect(dotEditContentService.getVersions).not.toHaveBeenCalled();
            expect(dotEditContentService.getPushPublishHistory).not.toHaveBeenCalled();
        }));

        it('should not load data if contentlet is null', fakeAsync(() => {
            store.updateContentlet(null);
            spectator.flushEffects();
            tick();

            expect(dotEditContentService.getVersions).not.toHaveBeenCalled();
            expect(dotEditContentService.getPushPublishHistory).not.toHaveBeenCalled();
        }));
    });

    describe('restoreVersion', () => {
        beforeEach(() => {
            store.updateContentlet(mockContentlet);
        });

        it('should restore version and delegate navigation to the host', fakeAsync(() => {
            const restoredVersion = { ...mockContentlet, inode: 'restored-inode' };
            dotVersionableService.bringBack.mockReturnValue(of(restoredVersion));

            store.restoreVersion('test-inode');
            tick();

            expect(dotVersionableService.bringBack).toHaveBeenCalledWith('test-inode');
            // The feature states the intent; whether it actually navigates (mode,
            // inode-changed) is the host's decision, covered in the host spec.
            expect(mockHost.goToRestoredVersion).toHaveBeenCalledWith('restored-inode', undefined);
        }));

        it('should pass the previous inode to the host when restoring', fakeAsync(() => {
            const restoredVersion = { ...mockContentlet, inode: mockContentlet.inode };
            dotVersionableService.bringBack.mockReturnValue(of(restoredVersion));

            patchState(store, { originalContentlet: mockContentlet });

            store.restoreVersion('test-inode');
            tick();

            expect(dotVersionableService.bringBack).toHaveBeenCalledWith('test-inode');
            expect(mockHost.goToRestoredVersion).toHaveBeenCalledWith(
                mockContentlet.inode,
                mockContentlet.inode
            );
        }));

        it('should handle restore errors', fakeAsync(() => {
            const error = new HttpErrorResponse({ error: 'Restore failed', status: 500 });
            dotVersionableService.bringBack.mockReturnValue(throwError(() => error));

            store.restoreVersion('test-inode');
            tick();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
        }));
    });

    describe('loadVersionContent', () => {
        beforeEach(() => {
            store.updateContentlet(mockContentlet);
        });

        it('should load version content and set historical view state', fakeAsync(() => {
            const historicalContent = {
                ...mockContentlet,
                inode: 'historical-inode',
                title: 'Historical Version'
            };
            dotContentletService.getContentletByInode.mockReturnValue(of(historicalContent));

            store.loadVersionContent('historical-inode');
            tick();

            expect(dotContentletService.getContentletByInode).toHaveBeenCalledWith(
                'historical-inode'
            );
            expect(store.contentlet()).toEqual(historicalContent);
            expect(store.isViewingHistoricalVersion()).toBe(true);
            expect(store.historicalVersionInode()).toBe('historical-inode');
            expect(store.originalContentlet()).toEqual(mockContentlet);
        }));

        it('should preserve original contentlet when already viewing historical version', fakeAsync(() => {
            const originalContentlet = { ...mockContentlet, title: 'Original' };
            const historicalContent = {
                ...mockContentlet,
                inode: 'historical-inode',
                title: 'Historical Version'
            };

            // Setup already viewing historical version
            patchState(store, {
                isViewingHistoricalVersion: true,
                originalContentlet: originalContentlet
            });

            dotContentletService.getContentletByInode.mockReturnValue(of(historicalContent));

            store.loadVersionContent('historical-inode');
            tick();

            expect(store.originalContentlet()).toEqual(originalContentlet); // Should preserve original
            expect(store.contentlet()).toEqual(historicalContent);
        }));

        it('should handle load errors and show error message', fakeAsync(() => {
            const error = new HttpErrorResponse({ error: 'Load failed', status: 500 });
            dotContentletService.getContentletByInode.mockReturnValue(throwError(() => error));

            store.loadVersionContent('historical-inode');
            tick();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'error',
                summary: 'Error',
                detail: 'Failed to load version content'
            });
        }));
    });

    describe('exitHistoricalView', () => {
        it('should exit historical view and restore original contentlet', () => {
            const originalContentlet = { ...mockContentlet, title: 'Original' };

            // Setup historical view state
            patchState(store, {
                contentlet: { ...mockContentlet, inode: 'historical-inode', title: 'Historical' },
                isViewingHistoricalVersion: true,
                historicalVersionInode: 'historical-inode',
                originalContentlet: originalContentlet
            });

            store.exitHistoricalView();

            expect(store.contentlet()).toEqual(originalContentlet);
            expect(store.isViewingHistoricalVersion()).toBe(false);
            expect(store.historicalVersionInode()).toBeNull();
            expect(store.originalContentlet()).toBeNull();
        });

        it('should handle case when no original contentlet exists', () => {
            patchState(store, {
                isViewingHistoricalVersion: true,
                historicalVersionInode: 'historical-inode',
                originalContentlet: null
            });

            // Should not throw error
            expect(() => store.exitHistoricalView()).not.toThrow();
        });
    });

    describe('confirmAndRestoreVersion', () => {
        it('should show confirmation dialog with correct messages', () => {
            store.confirmAndRestoreVersion('test-inode');

            expect(confirmationService.confirm).toHaveBeenCalled();

            const confirmCall = confirmationService.confirm.mock.calls[0][0];
            expect(confirmCall.message).toBe('Are you sure you want to restore this version?');
            expect(confirmCall.header).toBe('Restore Version');
            expect(confirmCall.acceptLabel).toBe('Restore');
            expect(confirmCall.rejectLabel).toBe('Cancel');
            expect(confirmCall.icon).toBeUndefined();
            expect(confirmCall.acceptIcon).toBe('hidden');
            expect(confirmCall.rejectIcon).toBe('hidden');
            expect(confirmCall.rejectButtonStyleClass).toBe('p-button-outlined');
        });

        it('should call restoreVersion when confirmed', fakeAsync(() => {
            dotVersionableService.bringBack.mockReturnValue(of(mockContentlet));

            // Mock confirmation service to auto-accept
            confirmationService.confirm.mockImplementation((config: any) => {
                if (config.accept) {
                    config.accept();
                }
                return confirmationService;
            });

            store.confirmAndRestoreVersion('test-inode');
            tick();

            expect(dotVersionableService.bringBack).toHaveBeenCalledWith('test-inode');
        }));
    });

    describe('restoreCurrentHistoricalVersion', () => {
        it('should restore current historical version when viewing one', () => {
            patchState(store, {
                historicalVersionInode: 'historical-inode'
            });

            store.restoreCurrentHistoricalVersion();

            expect(confirmationService.confirm).toHaveBeenCalled();
        });

        it('should do nothing when not viewing historical version', () => {
            patchState(store, {
                historicalVersionInode: null
            });

            store.restoreCurrentHistoricalVersion();

            expect(confirmationService.confirm).not.toHaveBeenCalled();
        });
    });

    describe('Historical Version State Management', () => {
        it('should handle complete historical version workflow', fakeAsync(() => {
            const originalContentlet = { ...mockContentlet, title: 'Original' };
            const historicalContent = {
                ...mockContentlet,
                inode: 'historical-inode',
                title: 'Historical'
            };

            // Start with original content
            store.updateContentlet(originalContentlet);

            // Load historical version
            dotContentletService.getContentletByInode.mockReturnValue(of(historicalContent));
            store.loadVersionContent('historical-inode');
            tick();

            // Verify historical state
            expect(store.isViewingHistoricalVersion()).toBe(true);
            expect(store.contentlet()).toEqual(historicalContent);
            expect(store.originalContentlet()).toEqual(originalContentlet);
            expect(store.historicalVersionInode()).toBe('historical-inode');

            // Exit historical view
            store.exitHistoricalView();

            // Verify back to original state
            expect(store.isViewingHistoricalVersion()).toBe(false);
            expect(store.contentlet()).toEqual(originalContentlet);
            expect(store.originalContentlet()).toBeNull();
            expect(store.historicalVersionInode()).toBeNull();
        }));

        it('should handle switching between historical versions', fakeAsync(() => {
            const originalContentlet = { ...mockContentlet, title: 'Original' };
            const historicalContent1 = {
                ...mockContentlet,
                inode: 'historical-1',
                title: 'Historical 1'
            };
            const historicalContent2 = {
                ...mockContentlet,
                inode: 'historical-2',
                title: 'Historical 2'
            };

            store.updateContentlet(originalContentlet);

            // Load first historical version
            dotContentletService.getContentletByInode.mockReturnValue(of(historicalContent1));
            store.loadVersionContent('historical-1');
            tick();

            expect(store.originalContentlet()).toEqual(originalContentlet);
            expect(store.contentlet()).toEqual(historicalContent1);

            // Switch to second historical version
            dotContentletService.getContentletByInode.mockReturnValue(of(historicalContent2));
            store.loadVersionContent('historical-2');
            tick();

            // Should preserve original contentlet, update current and historical inode
            expect(store.originalContentlet()).toEqual(originalContentlet);
            expect(store.contentlet()).toEqual(historicalContent2);
            expect(store.historicalVersionInode()).toBe('historical-2');
        }));

        it('should clear compareContentlet when loading a preview version', fakeAsync(() => {
            const compareContent = { ...mockContentlet, inode: 'compare-inode', title: 'Compare' };
            const previewContent = { ...mockContentlet, inode: 'preview-inode', title: 'Preview' };

            // Set up compare state
            patchState(store, { compareContentlet: compareContent });
            expect(store.compareContentlet()).toEqual(compareContent);

            // Trigger preview — should clear compare
            dotContentletService.getContentletByInode.mockReturnValue(of(previewContent));
            store.loadVersionContent('preview-inode');
            tick();

            expect(store.compareContentlet()).toBeNull();
            expect(store.isViewingHistoricalVersion()).toBe(true);
        }));
    });

    describe('Compare Version State Management', () => {
        it('should set compareContentlet and switch view to compare on COMPARE action', fakeAsync(() => {
            const compareContent = { ...mockContentlet, inode: 'compare-inode', title: 'Compare' };
            store.updateContentlet(mockContentlet);

            dotContentletService.getContentletByInode.mockReturnValue(of(compareContent));
            store.handleHistoryAction({
                type: DotHistoryTimelineItemActionType.COMPARE,
                item: { ...mockContentletVersion, inode: 'compare-inode' }
            });
            tick();

            expect(store.compareContentlet()).toEqual(compareContent);
            expect(store.uiState().view).toBe('compare');
            expect(store.isViewingHistoricalVersion()).toBe(false);
        }));

        it('should clear isViewingHistoricalVersion when entering compare mode', fakeAsync(() => {
            const compareContent = { ...mockContentlet, inode: 'compare-inode', title: 'Compare' };

            patchState(store, { isViewingHistoricalVersion: true });

            dotContentletService.getContentletByInode.mockReturnValue(of(compareContent));
            store.handleHistoryAction({
                type: DotHistoryTimelineItemActionType.COMPARE,
                item: { ...mockContentletVersion, inode: 'compare-inode' }
            });
            tick();

            expect(store.isViewingHistoricalVersion()).toBe(false);
            expect(store.compareContentlet()).toEqual(compareContent);
        }));

        it('should clear compareContentlet and switch view to form on exitCompareView', () => {
            const compareContent = { ...mockContentlet, inode: 'compare-inode', title: 'Compare' };

            patchState(store, {
                compareContentlet: compareContent,
                uiState: { ...store.uiState(), view: 'compare' }
            });

            store.exitCompareView();

            expect(store.compareContentlet()).toBeNull();
            expect(store.uiState().view).toBe('form');
        });
    });

    describe('loadingVersionInode', () => {
        it('should expose the inode while a version is being fetched and clear it on success', fakeAsync(() => {
            const response$ = new Subject<DotCMSContentlet>();
            dotContentletService.getContentletByInode.mockReturnValue(response$);

            store.loadVersionContent('loading-inode');

            expect(store.loadingVersionInode()).toBe('loading-inode');

            response$.next({ ...mockContentlet, inode: 'loading-inode' });
            response$.complete();
            tick();

            expect(store.loadingVersionInode()).toBeNull();
        }));

        it('should track the inode while updating the comparison and clear it on success', fakeAsync(() => {
            const response$ = new Subject<DotCMSContentlet>();
            dotContentletService.getContentletByInode.mockReturnValue(response$);

            store.handleHistoryAction({
                type: DotHistoryTimelineItemActionType.COMPARE,
                item: { ...mockContentletVersion, inode: 'compare-loading-inode' }
            });

            expect(store.loadingVersionInode()).toBe('compare-loading-inode');

            response$.next({ ...mockContentlet, inode: 'compare-loading-inode' });
            response$.complete();
            tick();

            expect(store.loadingVersionInode()).toBeNull();
        }));

        it('should clear the loading inode when the fetch fails', fakeAsync(() => {
            dotContentletService.getContentletByInode.mockReturnValue(
                throwError(() => new HttpErrorResponse({ status: 500 }))
            );

            store.loadVersionContent('loading-inode');
            tick();

            expect(store.loadingVersionInode()).toBeNull();
        }));

        it('should keep the current compare state when updating the comparison fails', fakeAsync(() => {
            const currentCompare = { ...mockContentlet, inode: 'current-compare-inode' };
            patchState(store, {
                compareContentlet: currentCompare,
                uiState: { ...store.uiState(), view: 'compare' }
            });

            dotContentletService.getContentletByInode.mockReturnValue(
                throwError(() => new HttpErrorResponse({ status: 500 }))
            );

            store.handleHistoryAction({
                type: DotHistoryTimelineItemActionType.COMPARE,
                item: { ...mockContentletVersion, inode: 'failing-inode' }
            });
            tick();

            expect(store.loadingVersionInode()).toBeNull();
            expect(store.compareContentlet()).toEqual(currentCompare);
            expect(store.uiState().view).toBe('compare');
        }));
    });

    describe('compareData', () => {
        const compareContent = { ...mockContentlet, inode: 'compare-inode', title: 'Compare' };

        beforeEach(() => {
            patchState(store, { compareContentlet: compareContent });
        });

        it('should build the language key from languageCode and countryCode for a non-default locale', () => {
            patchState(store, {
                currentLocale: createFakeLanguage({
                    id: 2,
                    language: 'Spanish',
                    languageCode: 'es',
                    countryCode: 'ES'
                })
            });

            expect(store.compareData()).toEqual({
                inode: 'compare-inode',
                identifier: mockContentlet.identifier,
                language: 'es-es'
            });
        });

        it('should use only the languageCode when the locale has no country code', () => {
            patchState(store, {
                currentLocale: createFakeLanguage({
                    id: 3,
                    language: 'Italian',
                    languageCode: 'it',
                    countryCode: ''
                })
            });

            expect(store.compareData().language).toBe('it');
        });

        it('should normalize a mixed-case isoCode to lowercase when present', () => {
            patchState(store, {
                currentLocale: createFakeLanguage({
                    id: 2,
                    language: 'Spanish',
                    languageCode: 'es',
                    countryCode: 'ES',
                    isoCode: 'ES-es'
                })
            });

            expect(store.compareData().language).toBe('es-es');
        });

        it('should fall back to the default locale key when there is no current locale', () => {
            patchState(store, { currentLocale: null });

            expect(store.compareData().language).toBe(DEFAULT_LOCALE_ISO_KEY);
        });

        it('should keep resolving en-us for the default en-US locale (regression)', () => {
            patchState(store, {
                currentLocale: createFakeLanguage({
                    id: 1,
                    language: 'English',
                    languageCode: 'en',
                    countryCode: 'US'
                })
            });

            expect(store.compareData().language).toBe('en-us');
        });
    });
});
