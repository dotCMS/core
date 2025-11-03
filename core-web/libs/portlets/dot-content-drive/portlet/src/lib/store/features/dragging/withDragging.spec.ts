import { describe, it, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';

import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { createFakeSite } from '@dotcms/utils-testing';

import { withDragging } from './withDragging';

import {
    DotContentDriveSortOrder,
    DotContentDriveState,
    DotContentDriveStatus
} from '../../../shared/models';

const mockSite = createFakeSite();

const mockDragItems: DotContentDriveItem[] = [
    {
        identifier: 'item-1',
        title: 'Test Item 1',
        contentType: 'FileAsset',
        modDate: '2024-01-01',
        modUserName: 'admin',
        inode: 'inode-1'
    } as DotContentDriveItem,
    {
        identifier: 'item-2',
        title: 'Test Item 2',
        contentType: 'FileAsset',
        modDate: '2024-01-02',
        modUserName: 'admin',
        inode: 'inode-2'
    } as DotContentDriveItem
];

const initialState: DotContentDriveState = {
    currentSite: mockSite,
    path: '/test/path',
    filters: {},
    items: [],
    selectedItems: [],
    status: DotContentDriveStatus.LOADING,
    totalItems: 0,
    pagination: { limit: 40, offset: 0 },
    sort: { field: 'modDate', order: DotContentDriveSortOrder.ASC },
    isTreeExpanded: true
};

export const draggingStoreMock = signalStore(
    withState<DotContentDriveState>(initialState),
    withDragging()
);

describe('withDragging', () => {
    let spectator: SpectatorService<InstanceType<typeof draggingStoreMock>>;
    let store: InstanceType<typeof draggingStoreMock>;

    const createService = createServiceFactory({
        service: draggingStoreMock
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('initial state', () => {
        it('should initialize with empty dragItems', () => {
            expect(store.dragItems()).toEqual([]);
        });
    });

    describe('methods', () => {
        describe('setDragItems', () => {
            it('should set drag items', () => {
                store.setDragItems(mockDragItems);

                expect(store.dragItems()).toEqual(mockDragItems);
            });

            it('should replace previous drag items', () => {
                const firstItems = [mockDragItems[0]];
                const secondItems = mockDragItems;

                store.setDragItems(firstItems);
                expect(store.dragItems()).toEqual(firstItems);

                store.setDragItems(secondItems);
                expect(store.dragItems()).toEqual(secondItems);
            });

            it('should handle empty array', () => {
                store.setDragItems(mockDragItems);
                expect(store.dragItems()).toEqual(mockDragItems);

                store.setDragItems([]);
                expect(store.dragItems()).toEqual([]);
            });
        });

        describe('cleanDragItems', () => {
            it('should clear drag items', () => {
                store.setDragItems(mockDragItems);
                expect(store.dragItems()).toEqual(mockDragItems);

                store.cleanDragItems();
                expect(store.dragItems()).toEqual([]);
            });

            it('should work when already empty', () => {
                expect(store.dragItems()).toEqual([]);

                store.cleanDragItems();
                expect(store.dragItems()).toEqual([]);
            });
        });
    });

    describe('integration scenarios', () => {
        it('should handle complete drag workflow', () => {
            // Initial state
            expect(store.dragItems()).toEqual([]);

            // Start dragging
            store.setDragItems(mockDragItems);
            expect(store.dragItems()).toEqual(mockDragItems);
            expect(store.dragItems()).toHaveLength(2);

            // End dragging
            store.cleanDragItems();
            expect(store.dragItems()).toEqual([]);
        });

        it('should handle single item drag', () => {
            const singleItem = [mockDragItems[0]];

            store.setDragItems(singleItem);
            expect(store.dragItems()).toEqual(singleItem);
            expect(store.dragItems()).toHaveLength(1);

            store.cleanDragItems();
            expect(store.dragItems()).toEqual([]);
        });
    });
});
