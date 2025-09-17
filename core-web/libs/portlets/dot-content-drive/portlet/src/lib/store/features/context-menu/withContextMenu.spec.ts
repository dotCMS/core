import { describe, it, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';

import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { withContextMenu } from './withContextMenu';

import {
    DotContentDriveContextMenu,
    DotContentDriveSortOrder,
    DotContentDriveState,
    DotContentDriveStatus
} from '../../../shared/models';

const initialState: DotContentDriveState = {
    currentSite: null,
    path: '',
    filters: {},
    items: [],
    status: DotContentDriveStatus.LOADING,
    totalItems: 0,
    pagination: { limit: 40, offset: 0 },
    sort: { field: 'modDate', order: DotContentDriveSortOrder.ASC },
    isTreeExpanded: true
};

export const contextMenuStoreMock = signalStore(
    withState<DotContentDriveState>(initialState),
    withContextMenu()
);

describe('withContextMenu', () => {
    let spectator: SpectatorService<InstanceType<typeof contextMenuStoreMock>>;
    let store: InstanceType<typeof contextMenuStoreMock>;

    const createService = createServiceFactory({
        service: contextMenuStoreMock
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('initial state', () => {
        it('should initialize with default context menu state', () => {
            expect(store.contextMenu()).toEqual({
                triggeredEvent: null,
                contentlet: null,
                showAddToBundle: false
            });
        });
    });

    describe('methods', () => {
        const mockContentlet = createFakeContentlet();
        const mockEvent = new MouseEvent('contextmenu');

        describe('setContextMenu', () => {
            it('should set the complete context menu state', () => {
                const contextMenu: DotContentDriveContextMenu = {
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: true
                };

                store.setContextMenu(contextMenu);

                expect(store.contextMenu()).toEqual(contextMenu);
            });

            it('should replace the entire context menu state', () => {
                // Set initial state
                store.setContextMenu({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: true
                });

                // Replace with new state
                const newContextMenu: DotContentDriveContextMenu = {
                    triggeredEvent: null,
                    contentlet: null,
                    showAddToBundle: false
                };

                store.setContextMenu(newContextMenu);

                expect(store.contextMenu()).toEqual(newContextMenu);
            });
        });

        describe('patchContextMenu', () => {
            it('should patch only the provided properties', () => {
                // Set initial state
                store.setContextMenu({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: false
                });

                // Patch only showAddToBundle
                store.patchContextMenu({ showAddToBundle: true });

                expect(store.contextMenu()).toEqual({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: true
                });
            });

            it('should patch multiple properties at once', () => {
                // Set initial state
                store.setContextMenu({
                    triggeredEvent: null,
                    contentlet: null,
                    showAddToBundle: false
                });

                // Patch multiple properties
                store.patchContextMenu({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet
                });

                expect(store.contextMenu()).toEqual({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: false
                });
            });

            it('should handle patching with undefined values', () => {
                // Set initial state
                store.setContextMenu({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: true
                });

                // Patch with undefined/null values
                store.patchContextMenu({
                    triggeredEvent: null,
                    contentlet: null
                });

                expect(store.contextMenu()).toEqual({
                    triggeredEvent: null,
                    contentlet: null,
                    showAddToBundle: true
                });
            });
        });

        describe('resetContextMenu', () => {
            it('should reset context menu to default state', () => {
                // Set some state first
                store.setContextMenu({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: true
                });

                // Reset
                store.resetContextMenu();

                expect(store.contextMenu()).toEqual({
                    triggeredEvent: null,
                    contentlet: null,
                    showAddToBundle: false
                });
            });

            it('should reset context menu when already in default state', () => {
                // Context menu is already in default state
                store.resetContextMenu();

                expect(store.contextMenu()).toEqual({
                    triggeredEvent: null,
                    contentlet: null,
                    showAddToBundle: false
                });
            });
        });

        describe('setShowAddToBundle', () => {
            it('should set showAddToBundle to true while preserving other properties', () => {
                // Set initial state
                store.setContextMenu({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: false
                });

                // Set showAddToBundle to true
                store.setShowAddToBundle(true);

                expect(store.contextMenu()).toEqual({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: true
                });
            });

            it('should set showAddToBundle to false while preserving other properties', () => {
                // Set initial state
                store.setContextMenu({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: true
                });

                // Set showAddToBundle to false
                store.setShowAddToBundle(false);

                expect(store.contextMenu()).toEqual({
                    triggeredEvent: mockEvent,
                    contentlet: mockContentlet,
                    showAddToBundle: false
                });
            });

            it('should work with null context menu properties', () => {
                // Start with default state
                store.resetContextMenu();

                // Set showAddToBundle to true
                store.setShowAddToBundle(true);

                expect(store.contextMenu()).toEqual({
                    triggeredEvent: null,
                    contentlet: null,
                    showAddToBundle: true
                });
            });
        });
    });

    describe('integration scenarios', () => {
        const mockContentlet = createFakeContentlet();

        it('should handle typical context menu workflow', () => {
            const mockEvent = new MouseEvent('contextmenu');

            // 1. User right-clicks on item (opens context menu)
            store.patchContextMenu({
                triggeredEvent: mockEvent,
                contentlet: mockContentlet
            });

            expect(store.contextMenu().triggeredEvent).toBe(mockEvent);
            expect(store.contextMenu().contentlet).toBe(mockContentlet);
            expect(store.contextMenu().showAddToBundle).toBe(false);

            // 2. User clicks "Add to Bundle" option
            store.setShowAddToBundle(true);

            expect(store.contextMenu().showAddToBundle).toBe(true);
            expect(store.contextMenu().contentlet).toBe(mockContentlet);

            // 3. User cancels or completes the bundle action
            store.setShowAddToBundle(false);

            expect(store.contextMenu().showAddToBundle).toBe(false);

            // 4. Context menu is closed/reset
            store.resetContextMenu();

            expect(store.contextMenu()).toEqual({
                triggeredEvent: null,
                contentlet: null,
                showAddToBundle: false
            });
        });

        it('should handle switching between different contentlets', () => {
            const mockEvent1 = new MouseEvent('contextmenu');
            const mockEvent2 = new MouseEvent('contextmenu');

            const contentlet1 = { ...mockContentlet, inode: 'contentlet-1' } as DotContentDriveItem;
            const contentlet2 = { ...mockContentlet, inode: 'contentlet-2' } as DotContentDriveItem;

            // Set context menu for first contentlet
            store.setContextMenu({
                triggeredEvent: mockEvent1,
                contentlet: contentlet1,
                showAddToBundle: false
            });

            expect(store.contextMenu().contentlet?.inode).toBe('contentlet-1');

            // Switch to second contentlet
            store.setContextMenu({
                triggeredEvent: mockEvent2,
                contentlet: contentlet2,
                showAddToBundle: false
            });

            expect(store.contextMenu().contentlet?.inode).toBe('contentlet-2');
            expect(store.contextMenu().triggeredEvent).toBe(mockEvent2);
        });
    });
});
