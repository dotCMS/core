import { describe, it, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, mockProvider } from '@ngneat/spectator/jest';
import { signalStore, withState, withMethods, patchState } from '@ngrx/signals';
import { of } from 'rxjs';

import { DotFolderService } from '@dotcms/data-access';
import { createFakeSite } from '@dotcms/utils-testing';

import { withDotAsset } from './withDotAsset';

import {
    DotContentDriveSortOrder,
    DotContentDriveState,
    DotContentDriveStatus
} from '../../../shared/models';
import { ALL_FOLDER, TreeNodeItem } from '../../../utils/tree-folder.utils';

const mockSite = createFakeSite();
const mockTreeNodes: TreeNodeItem[] = [
    {
        key: 'folder-1',
        label: '/documents/',
        data: {
            id: 'folder-1',
            hostname: 'demo.dotcms.com',
            path: '/documents/',
            type: 'folder'
        },
        leaf: false
    },
    {
        key: 'folder-2',
        label: '/images/',
        data: {
            id: 'folder-2',
            hostname: 'demo.dotcms.com',
            path: '/images/',
            type: 'folder'
        },
        leaf: false
    }
];

const initialState: DotContentDriveState = {
    currentSite: mockSite,
    path: '/test/path',
    filters: {},
    items: [],
    status: DotContentDriveStatus.LOADING,
    totalItems: 0,
    pagination: { limit: 40, offset: 0 },
    sort: { field: 'modDate', order: DotContentDriveSortOrder.ASC },
    isTreeExpanded: true,
    selectedNode: ALL_FOLDER
};

export const dotAssetStoreMock = signalStore(
    withState<DotContentDriveState>(initialState),
    withMethods((store) => ({
        setSelectedNode: (node: TreeNodeItem | null) => {
            patchState(store, { selectedNode: node });
        }
    })),
    withDotAsset(),
);

describe('withDotAsset', () => {
    let spectator: SpectatorService<InstanceType<typeof dotAssetStoreMock>>;
    let store: InstanceType<typeof dotAssetStoreMock>;
    let folderService: jest.Mocked<DotFolderService>;

    const createService = createServiceFactory({
        service: dotAssetStoreMock,
        providers: [
            mockProvider(DotFolderService, {
                getFileMasksForFolder: jest.fn().mockReturnValue(of('*.jpg,*.png,*.gif'))
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        folderService = spectator.inject(DotFolderService);
    });

    describe('initial state', () => {
        it('should initialize with default allowedFileTypes state', (done) => {
            expect(store.allowedFileTypes()).toBe('');
            done();
        });
    });

    describe('effect behavior', () => {
        it('should not call getFileMasksForFolder when selectedNode is ALL_FOLDER', () => {
            // Initial state has ALL_FOLDER selected
            expect(folderService.getFileMasksForFolder).not.toHaveBeenCalled();
        });

        it('should not call getFileMasksForFolder when selectedNode is null', (done) => {
            // Set selectedNode to null
            store.setSelectedNode(null);
        
            setTimeout(() => {
                expect(folderService.getFileMasksForFolder).not.toHaveBeenCalled();
                done();
            }, 0);
        });

        it('should call getFileMasksForFolder and update allowedFileTypes when valid folder is selected', (done) => {
            const testNode = mockTreeNodes[0];

            // Set a valid folder node
            store.setSelectedNode(testNode);

            setTimeout(() => {
                expect(folderService.getFileMasksForFolder).toHaveBeenCalledWith(testNode.key);
                expect(store.allowedFileTypes()).toBe('.jpg,.png,.gif');
                done();
            }, 0);
        });

        it('should handle file masks transformation correctly', (done) => {
            const testNode = mockTreeNodes[1];
            folderService.getFileMasksForFolder.mockReturnValue(of('*.pdf,*.doc,*.txt'));

            store.setSelectedNode(testNode);

            setTimeout(() => {
                expect(store.allowedFileTypes()).toBe('.pdf,.doc,.txt');
                done();
            }, 0);
        });

        it('should handle empty file masks', (done) => {
            const testNode = mockTreeNodes[0];
            folderService.getFileMasksForFolder.mockReturnValue(of(''));

            store.setSelectedNode(testNode);

            setTimeout(() => {
                expect(store.allowedFileTypes()).toBe('');
                done();
            }, 0);
        });

        it('should handle single file mask', (done) => {
            const testNode = mockTreeNodes[0];
            folderService.getFileMasksForFolder.mockReturnValue(of('*.jpg'));

            store.setSelectedNode(testNode);

            setTimeout(() => {
                expect(store.allowedFileTypes()).toBe('.jpg');
                done();
            }, 0);
        });
    });
});
