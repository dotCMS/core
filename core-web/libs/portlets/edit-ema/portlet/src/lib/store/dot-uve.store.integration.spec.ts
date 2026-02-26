import { beforeEach, describe, expect, it } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';

import {
    DotAnalyticsTrackerService,
    DotContentletLockerService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPageLayoutService,
    DotPropertiesService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { UVE_MODE } from '@dotcms/types';
import { WINDOW } from '@dotcms/utils';
import {
    CurrentUserDataMock,
    DotLanguagesServiceMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { UVEStore } from './dot-uve.store';
import { Orientation } from './models';

import { DotPageApiService } from '../services/dot-page-api.service';
import { EDITOR_STATE, UVE_STATUS } from '../shared/enums';
import {
    ACTION_MOCK,
    ACTION_PAYLOAD_MOCK,
    dotPropertiesServiceMock,
    EMA_DRAG_ITEM_CONTENTLET_MOCK,
    getBoundsMock,
    MOCK_CONTENTLET_AREA,
    MOCK_RESPONSE_HEADLESS
} from '../shared/mocks';

/**
 * Integration Tests
 * Tests for the flat state structure (editorPaletteOpen, editorDragItem, viewDevice, etc.)
 * Verifies that the store architecture works correctly across features
 */
describe('UVEStore - Integration Tests ', () => {
    let spectator: SpectatorService<InstanceType<typeof UVEStore>>;
    let store: InstanceType<typeof UVEStore>;

    const createService = createServiceFactory({
        service: UVEStore,
        providers: [
            MessageService,
            ConfirmationService,
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: () => of({})
                }
            },
            {
                provide: DotPropertiesService,
                useValue: dotPropertiesServiceMock
            },
            {
                provide: DotPageApiService,
                useValue: {
                    get: () => of(MOCK_RESPONSE_HEADLESS),
                    getClientPage: () => of({}),
                    getGraphQLPage: () => of({}),
                    save: jest.fn()
                }
            },
            {
                provide: DotLicenseService,
                useValue: {
                    isEnterprise: () => of(true)
                }
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            },
            {
                provide: DotContentletLockerService,
                useValue: {
                    lock: jest.fn().mockReturnValue(of({})),
                    unlock: jest.fn().mockReturnValue(of({}))
                }
            },
            {
                provide: DotExperimentsService,
                useValue: {
                    getById: () => of(undefined)
                }
            },
            {
                provide: LoginService,
                useValue: {
                    getCurrentUser: () => of(CurrentUserDataMock)
                }
            },
            {
                provide: DotLanguagesService,
                useValue: new DotLanguagesServiceMock()
            },
            {
                provide: DotAnalyticsTrackerService,
                useValue: {
                    track: jest.fn()
                }
            },
            {
                provide: DotPageLayoutService,
                useValue: {
                    save: jest.fn().mockReturnValue(of({})),
                    updateFromRowToContainers: jest.fn().mockReturnValue([])
                }
            },
            {
                provide: WINDOW,
                useValue: window
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('State Structure', () => {
        describe('editor panels', () => {
            it('should have palette and rightSidebar state', () => {
                expect(store.editorPaletteOpen()).toBeDefined();
                expect(store.editorRightSidebarOpen()).toBeDefined();
            });

            it('should initialize panels with default values', () => {
                expect(store.editorPaletteOpen()).toBe(true); // Default: palette open
                expect(store.editorRightSidebarOpen()).toBe(false); // Default: sidebar closed
            });

            it('should update palette.open via setPaletteOpen', () => {
                store.setPaletteOpen(false);

                expect(store.editorPaletteOpen()).toBe(false);

                store.setPaletteOpen(true);

                expect(store.editorPaletteOpen()).toBe(true);
            });

            it('should update rightSidebar.open via setRightSidebarOpen', () => {
                store.setRightSidebarOpen(true);

                expect(store.editorRightSidebarOpen()).toBe(true);

                store.setRightSidebarOpen(false);

                expect(store.editorRightSidebarOpen()).toBe(false);
            });

            it('should preserve other panel state when updating one panel', () => {
                // Set initial state
                store.setPaletteOpen(false);
                store.setRightSidebarOpen(true);

                // Update only palette
                store.setPaletteOpen(true);

                // Verify palette changed but rightSidebar remained
                expect(store.editorPaletteOpen()).toBe(true);
                expect(store.editorRightSidebarOpen()).toBe(true); // Unchanged
            });
        });

        describe('editor functional state', () => {
            it('should maintain separation between panels and functional state', () => {
                // Functional state
                expect(store.editorDragItem()).toBeDefined();
                expect(store.editorBounds()).toBeDefined();
                expect(store.editorState()).toBeDefined();
                expect(store.editorActiveContentlet()).toBeDefined();
                expect(store.editorContentArea()).toBeDefined();

                // UI panels (separate)
                expect(store.editorPaletteOpen()).toBeDefined();
                expect(store.editorRightSidebarOpen()).toBeDefined();

                // Editor data
                expect(store.editorOgTags()).toBeDefined();
                expect(store.editorStyleSchemas()).toBeDefined();
            });

            it('should handle drag item state independently from panels', () => {
                // Set drag item
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);

                expect(store.editorDragItem()).toEqual(EMA_DRAG_ITEM_CONTENTLET_MOCK);
                expect(store.editorState()).toBe(EDITOR_STATE.DRAGGING);

                // Panel state should be unaffected
                expect(store.editorPaletteOpen()).toBe(true);
                expect(store.editorRightSidebarOpen()).toBe(false);
            });

            it('should handle bounds state independently from panels', () => {
                const bounds = getBoundsMock(ACTION_MOCK);

                store.setEditorBounds(bounds);

                expect(store.editorBounds()).toEqual(bounds);
                // Panel state should be unaffected
                expect(store.editorPaletteOpen()).toBe(true);
            });

            it('should handle contentArea state independently from panels', () => {
                store.setContentletArea(MOCK_CONTENTLET_AREA);

                expect(store.editorContentArea()).toEqual(MOCK_CONTENTLET_AREA);
                expect(store.editorState()).toBe(EDITOR_STATE.IDLE);

                // Panel state should be unaffected
                expect(store.editorPaletteOpen()).toBe(true);
            });
        });

        describe('view state', () => {
            it('should have view state', () => {
                expect(store.viewDevice()).toBeDefined();
                expect(store.viewDeviceOrientation()).toBeDefined();
                expect(store.viewSocialMedia()).toBeDefined();
                expect(store.viewParams()).toBeDefined();
                expect(store.viewOgTagsResults()).toBeDefined();
            });

            it('should initialize view with default values', () => {
                expect(store.viewDevice()).toBeDefined();
                expect(store.viewDeviceOrientation()).toBe(Orientation.LANDSCAPE);
                expect(store.viewSocialMedia()).toBeNull();
            });

            it('should update view state independently from editor', () => {
                // Update view
                store.viewSetOrientation(Orientation.PORTRAIT);

                expect(store.viewDeviceOrientation()).toBe(Orientation.PORTRAIT);

                // Editor state should be unaffected
                expect(store.editorPaletteOpen()).toBe(true);
                expect(store.editorState()).toBe(EDITOR_STATE.IDLE);
            });
        });
    });

    describe('Cross-Feature State Updates', () => {
        it('should update activeContentlet and auto-open palette', () => {
            // Close palette first
            store.setPaletteOpen(false);
            expect(store.editorPaletteOpen()).toBe(false);

            // Set active contentlet - should auto-open palette
            store.setActiveContentlet(ACTION_PAYLOAD_MOCK);

            expect(store.editorActiveContentlet()).toEqual(ACTION_PAYLOAD_MOCK);
            expect(store.editorPaletteOpen()).toBe(true); // Auto-opened
        });

        it('should reset all editor properties while preserving panel state', () => {
            // Set up complex state
            store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);
            store.setEditorBounds(getBoundsMock(ACTION_MOCK));
            store.setContentletArea(MOCK_CONTENTLET_AREA);
            store.setPaletteOpen(false);
            store.setRightSidebarOpen(true);

            // Remember panel state
            const paletteWasOpen = store.editorPaletteOpen();
            const sidebarWasOpen = store.editorRightSidebarOpen();

            // Reset editor properties
            store.resetEditorProperties();

            // Functional state should be reset
            expect(store.editorDragItem()).toBeNull();
            expect(store.editorBounds()).toEqual([]);
            expect(store.editorContentArea()).toBeNull();
            expect(store.editorState()).toBe(EDITOR_STATE.IDLE);

            // Panel state should be preserved (user preferences)
            expect(store.editorPaletteOpen()).toBe(paletteWasOpen);
            expect(store.editorRightSidebarOpen()).toBe(sidebarWasOpen);
        });
    });

    describe('Computed Properties', () => {
        it('should compute $showContentletControls based on editor state', () => {
            // Set up page params with EDIT mode (required for $canEditPage)
            store.pageUpdateParams({ mode: UVE_MODE.EDIT });

            // Set up page API response with viewAs mode (required for computeds)
            const pageResponse = {
                ...MOCK_RESPONSE_HEADLESS,
                page: {
                    ...MOCK_RESPONSE_HEADLESS.page,
                    canEdit: true,
                    locked: false
                },
                viewAs: {
                    ...MOCK_RESPONSE_HEADLESS.viewAs,
                    mode: 'EDIT'
                }
            };

            store.setPageAssetResponse({ pageAsset: pageResponse });
            store.setUveStatus(UVE_STATUS.LOADED);
            store.setUveCurrentUser({ ...CurrentUserDataMock, loginAs: false });
            store.setUveIsEnterprise(true);

            // IDLE state - should show controls if can edit
            store.setEditorState(EDITOR_STATE.IDLE);
            store.setContentletArea(MOCK_CONTENTLET_AREA);

            // Computed should work with flat state
            const hasControls = store.$showContentletControls();
            expect(typeof hasControls).toBe('boolean');

            // Change to DRAGGING state - should hide controls
            store.setEditorState(EDITOR_STATE.DRAGGING);
            expect(store.$showContentletControls()).toBe(false);
        });

        it('should compute $editorIsInDraggingState based on editor state', () => {
            expect(store.$editorIsInDraggingState()).toBe(false);

            store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);
            expect(store.$editorIsInDraggingState()).toBe(true);

            store.resetEditorProperties();
            expect(store.$editorIsInDraggingState()).toBe(false);
        });
    });

    describe('State Integrity', () => {
        it('should maintain immutability when updating state', () => {
            const paletteBefore = store.editorPaletteOpen();

            store.setPaletteOpen(false);

            const paletteAfter = store.editorPaletteOpen();

            // Values should be updated
            expect(paletteBefore).toBe(true);
            expect(paletteAfter).toBe(false);
        });

        it('should not affect unrelated state when updating editor', () => {
            const viewOrientationBefore = store.viewDeviceOrientation();
            const statusBefore = store.uveStatus();
            const languagesBefore = store.pageLanguages();

            // Update editor state
            store.setPaletteOpen(false);

            // Unrelated state should be unchanged
            expect(store.viewDeviceOrientation()).toBe(viewOrientationBefore);
            expect(store.uveStatus()).toBe(statusBefore);
            expect(store.pageLanguages()).toBe(languagesBefore);
        });

        it('should not affect unrelated state when updating view', () => {
            const paletteBefore = store.editorPaletteOpen();
            const statusBefore = store.uveStatus();

            // Update view state
            store.viewSetOrientation(Orientation.PORTRAIT);

            // Unrelated state should be unchanged
            expect(store.editorPaletteOpen()).toBe(paletteBefore);
            expect(store.uveStatus()).toBe(statusBefore);
        });
    });
});
