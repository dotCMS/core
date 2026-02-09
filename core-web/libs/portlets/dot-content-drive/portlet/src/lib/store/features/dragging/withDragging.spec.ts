import { describe, it, expect, beforeEach } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';

import {
    DotCMSContentlet,
    DotContentDriveFolder,
    DotContentDriveItem
} from '@dotcms/dotcms-models';
import { createFakeSite } from '@dotcms/utils-testing';

import { withDragging } from './withDragging';

import {
    DotContentDriveSortOrder,
    DotContentDriveState,
    DotContentDriveStatus
} from '../../../shared/models';

const mockSite = createFakeSite();

const mockContentlet1: DotCMSContentlet = {
    identifier: 'contentlet-1',
    title: 'Test Contentlet 1',
    contentType: 'FileAsset',
    modDate: '2024-01-01',
    modUserName: 'admin',
    inode: 'inode-1'
} as DotCMSContentlet;

const mockContentlet2: DotCMSContentlet = {
    identifier: 'contentlet-2',
    title: 'Test Contentlet 2',
    contentType: 'Blog',
    modDate: '2024-01-02',
    modUserName: 'admin',
    inode: 'inode-2'
} as DotCMSContentlet;

const mockFolder1: DotContentDriveFolder = {
    __icon__: 'folderIcon',
    defaultFileType: '',
    description: '',
    extension: 'folder',
    filesMasks: '',
    hasTitleImage: false,
    hostId: 'host-1',
    iDate: 1234567890,
    identifier: 'folder-1',
    inode: 'inode-folder-1',
    mimeType: 'folder',
    modDate: 1234567890,
    name: 'Test Folder 1',
    owner: 'admin',
    parent: '/',
    path: '/test-folder-1/',
    permissions: [],
    showOnMenu: true,
    sortOrder: 0,
    title: 'Test Folder 1',
    type: 'folder'
};

const mockFolder2: DotContentDriveFolder = {
    __icon__: 'folderIcon',
    defaultFileType: '',
    description: '',
    extension: 'folder',
    filesMasks: '',
    hasTitleImage: false,
    hostId: 'host-2',
    iDate: 1234567891,
    identifier: 'folder-2',
    inode: 'inode-folder-2',
    mimeType: 'folder',
    modDate: 1234567891,
    name: 'Test Folder 2',
    owner: 'admin',
    parent: '/',
    path: '/test-folder-2/',
    permissions: [],
    showOnMenu: true,
    sortOrder: 0,
    title: 'Test Folder 2',
    type: 'folder'
};

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
            expect(store.dragItems()).toEqual({
                folders: [],
                contentlets: []
            });
        });
    });

    describe('methods', () => {
        describe('setDragItems', () => {
            it('should set drag items and separate folders from contentlets', () => {
                const mixedItems: DotContentDriveItem[] = [
                    mockFolder1,
                    mockContentlet1,
                    mockFolder2,
                    mockContentlet2
                ];

                store.setDragItems(mixedItems);

                expect(store.dragItems()).toEqual({
                    folders: [mockFolder1, mockFolder2],
                    contentlets: [mockContentlet1, mockContentlet2]
                });
            });

            it('should set only contentlets when no folders are provided', () => {
                const contentlets: DotContentDriveItem[] = [mockContentlet1, mockContentlet2];

                store.setDragItems(contentlets);

                expect(store.dragItems()).toEqual({
                    folders: [],
                    contentlets: [mockContentlet1, mockContentlet2]
                });
            });

            it('should set only folders when no contentlets are provided', () => {
                const folders: DotContentDriveItem[] = [mockFolder1, mockFolder2];

                store.setDragItems(folders);

                expect(store.dragItems()).toEqual({
                    folders: [mockFolder1, mockFolder2],
                    contentlets: []
                });
            });

            it('should replace previous drag items', () => {
                const firstItems: DotContentDriveItem[] = [mockContentlet1];
                const secondItems: DotContentDriveItem[] = [mockFolder1, mockContentlet2];

                store.setDragItems(firstItems);
                expect(store.dragItems()).toEqual({
                    folders: [],
                    contentlets: [mockContentlet1]
                });

                store.setDragItems(secondItems);
                expect(store.dragItems()).toEqual({
                    folders: [mockFolder1],
                    contentlets: [mockContentlet2]
                });
            });

            it('should handle empty array', () => {
                const items: DotContentDriveItem[] = [mockFolder1, mockContentlet1];

                store.setDragItems(items);
                expect(store.dragItems()).toEqual({
                    folders: [mockFolder1],
                    contentlets: [mockContentlet1]
                });

                store.setDragItems([]);
                expect(store.dragItems()).toEqual({
                    folders: [],
                    contentlets: []
                });
            });
        });

        describe('cleanDragItems', () => {
            it('should clear drag items', () => {
                const items: DotContentDriveItem[] = [mockFolder1, mockContentlet1];

                store.setDragItems(items);
                expect(store.dragItems()).toEqual({
                    folders: [mockFolder1],
                    contentlets: [mockContentlet1]
                });

                store.cleanDragItems();
                expect(store.dragItems()).toEqual({
                    folders: [],
                    contentlets: []
                });
            });

            it('should work when already empty', () => {
                expect(store.dragItems()).toEqual({
                    folders: [],
                    contentlets: []
                });

                store.cleanDragItems();
                expect(store.dragItems()).toEqual({
                    folders: [],
                    contentlets: []
                });
            });
        });
    });

    describe('integration scenarios', () => {
        it('should handle complete drag workflow', () => {
            // Initial state
            expect(store.dragItems()).toEqual({
                folders: [],
                contentlets: []
            });

            // Start dragging
            const items: DotContentDriveItem[] = [mockFolder1, mockContentlet1, mockContentlet2];
            store.setDragItems(items);
            expect(store.dragItems()).toEqual({
                folders: [mockFolder1],
                contentlets: [mockContentlet1, mockContentlet2]
            });
            expect(store.dragItems().folders).toHaveLength(1);
            expect(store.dragItems().contentlets).toHaveLength(2);

            // End dragging
            store.cleanDragItems();
            expect(store.dragItems()).toEqual({
                folders: [],
                contentlets: []
            });
        });

        it('should handle single item drag', () => {
            const singleItem: DotContentDriveItem[] = [mockContentlet1];

            store.setDragItems(singleItem);
            expect(store.dragItems()).toEqual({
                folders: [],
                contentlets: [mockContentlet1]
            });
            expect(store.dragItems().contentlets).toHaveLength(1);
            expect(store.dragItems().folders).toHaveLength(0);

            store.cleanDragItems();
            expect(store.dragItems()).toEqual({
                folders: [],
                contentlets: []
            });
        });

        it('should handle single folder drag', () => {
            const singleFolder: DotContentDriveItem[] = [mockFolder1];

            store.setDragItems(singleFolder);
            expect(store.dragItems()).toEqual({
                folders: [mockFolder1],
                contentlets: []
            });
            expect(store.dragItems().folders).toHaveLength(1);
            expect(store.dragItems().contentlets).toHaveLength(0);

            store.cleanDragItems();
            expect(store.dragItems()).toEqual({
                folders: [],
                contentlets: []
            });
        });
    });
});
