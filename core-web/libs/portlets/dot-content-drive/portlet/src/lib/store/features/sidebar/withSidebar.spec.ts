import { describe, it, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, mockProvider } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { DotFolderService } from '@dotcms/data-access';
import { TreeNodeItem } from '@dotcms/portlets/content-drive/ui';
import { createFakeFolder, createFakeSite } from '@dotcms/utils-testing';

import { withSidebar } from './withSidebar';

import {
    DotContentDriveSortOrder,
    DotContentDriveState,
    DotContentDriveStatus
} from '../../../shared/models';
import { ALL_FOLDER } from '../../../utils/tree-folder.utils';

const mockSite = createFakeSite();

const mockFolders = [
    createFakeFolder({
        id: 'parent-folder',
        path: '/documents/',
        hostName: 'demo.dotcms.com',
        addChildrenAllowed: true
    }),
    createFakeFolder({
        id: 'child-folder-1',
        path: '/documents/images/',
        hostName: 'demo.dotcms.com',
        addChildrenAllowed: true
    }),
    createFakeFolder({
        id: 'child-folder-2',
        path: '/documents/videos/',
        hostName: 'demo.dotcms.com',
        addChildrenAllowed: true
    })
];

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
    isTreeExpanded: true
};

export const sidebarStoreMock = signalStore(
    withState<DotContentDriveState>(initialState),
    withSidebar()
);

describe('withSidebar', () => {
    let spectator: SpectatorService<InstanceType<typeof sidebarStoreMock>>;
    let store: InstanceType<typeof sidebarStoreMock>;
    let folderService: jest.Mocked<DotFolderService>;

    const createService = createServiceFactory({
        service: sidebarStoreMock,
        providers: [
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of(mockFolders))
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        folderService = spectator.inject(DotFolderService);
    });

    describe('initial state', () => {
        it('should initialize with default sidebar state', () => {
            expect(store.sidebarLoading()).toBe(true);
            expect(store.folders()).toEqual([]);
            expect(store.selectedNode()).toEqual(ALL_FOLDER);
        });
    });

    describe('methods', () => {
        describe('loadFolders', () => {
            it('should load folders for current site and path', (done) => {
                store.loadFolders();

                // Wait for async operations to complete
                setTimeout(() => {
                    expect(folderService.getFolders).toHaveBeenCalled();
                    expect(store.sidebarLoading()).toBe(false);
                    expect(store.folders()).toContain(ALL_FOLDER);
                    done();
                }, 0);
            });

            it('should handle empty folder response', (done) => {
                folderService.getFolders.mockReturnValue(of([]));

                store.loadFolders();

                setTimeout(() => {
                    expect(store.sidebarLoading()).toBe(false);
                    expect(store.folders()).toContain(ALL_FOLDER);
                    done();
                }, 0);
            });
        });

        describe('loadChildFolders', () => {
            it('should load child folders for a specific path', (done) => {
                const testPath = '/documents/images/';

                folderService.getFolders.mockReturnValue(of(mockFolders));

                store.loadChildFolders(testPath).subscribe((result) => {
                    expect(result.parent).toEqual(mockFolders[0]);
                    expect(result.folders).toHaveLength(2);
                    expect(folderService.getFolders).toHaveBeenCalledWith(testPath);
                    done();
                });
            });

            it('should transform folders into tree nodes correctly', (done) => {
                const testPath = '/documents/';

                store.loadChildFolders(testPath).subscribe((result) => {
                    expect(result.folders).toHaveLength(2);
                    expect(result.folders[0]).toHaveProperty('key');
                    expect(result.folders[0]).toHaveProperty('label');
                    expect(result.folders[0]).toHaveProperty('data');
                    expect(result.folders[0].data.type).toBe('folder');
                    done();
                });
            });
        });

        describe('setSelectedNode', () => {
            it('should set the selected node', () => {
                const testNode = mockTreeNodes[0];

                store.setSelectedNode(testNode);

                expect(store.selectedNode()).toEqual(testNode);
            });

            it('should replace the previous selected node', () => {
                const firstNode = mockTreeNodes[0];
                const secondNode = mockTreeNodes[1];

                store.setSelectedNode(firstNode);
                expect(store.selectedNode()).toEqual(firstNode);

                store.setSelectedNode(secondNode);
                expect(store.selectedNode()).toEqual(secondNode);
            });
        });

        describe('updateFolders', () => {
            it('should update the folders array', () => {
                const newFolders = [ALL_FOLDER, ...mockTreeNodes];

                store.updateFolders(newFolders);

                expect(store.folders()).toEqual(newFolders);
            });

            it('should create a new array reference', () => {
                const originalFolders = store.folders();
                const newFolders = [ALL_FOLDER, ...mockTreeNodes];

                store.updateFolders(newFolders);

                expect(store.folders()).not.toBe(originalFolders);
                expect(store.folders()).toEqual(newFolders);
            });
        });
    });

    describe('integration scenarios', () => {
        it('should handle complete folder loading workflow', (done) => {
            // Initial state
            expect(store.sidebarLoading()).toBe(true);
            expect(store.folders()).toEqual([]);

            // Load folders
            store.loadFolders();

            setTimeout(() => {
                // Verify folders are loaded
                expect(store.sidebarLoading()).toBe(false);
                expect(store.folders()).toContain(ALL_FOLDER);

                // Select a node
                const nodeToSelect = mockTreeNodes[0];
                store.setSelectedNode(nodeToSelect);

                expect(store.selectedNode()).toEqual(nodeToSelect);
                done();
            }, 0);
        });

        it('should handle child folder expansion workflow', (done) => {
            const parentPath = '/documents/';

            // Load child folders
            store.loadChildFolders(parentPath).subscribe((result) => {
                // Update folders with new children
                const updatedFolders = [...store.folders(), ...result.folders];
                store.updateFolders(updatedFolders);

                expect(store.folders()).toContain(result.folders[0]);
                expect(store.folders()).toContain(result.folders[1]);
                done();
            });
        });
    });
});

describe('withSidebar - null site scenarios', () => {
    let spectator: SpectatorService<InstanceType<typeof sidebarStoreMock>>;
    let store: InstanceType<typeof sidebarStoreMock>;
    let folderService: jest.Mocked<DotFolderService>;

    const nullSiteStoreMock = signalStore(
        withState<DotContentDriveState>({
            ...initialState,
            currentSite: null
        }),

        withSidebar()
    );

    const createService = createServiceFactory({
        service: nullSiteStoreMock,
        providers: [
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of(mockFolders))
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        folderService = spectator.inject(DotFolderService);
    });

    describe('loadFolders with null site', () => {
        it('should not load folders when currentSite is null', () => {
            store.loadFolders();

            expect(folderService.getFolders).not.toHaveBeenCalled();
        });
    });
});

describe('withSidebar - undefined path scenarios', () => {
    let spectator: SpectatorService<InstanceType<typeof sidebarStoreMock>>;
    let store: InstanceType<typeof sidebarStoreMock>;
    let folderService: jest.Mocked<DotFolderService>;

    const undefinedPathStoreMock = signalStore(
        withState<DotContentDriveState>({
            ...initialState,
            path: undefined
        }),
        withSidebar()
    );

    const createService = createServiceFactory({
        service: undefinedPathStoreMock,
        providers: [
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValue(of(mockFolders))
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        folderService = spectator.inject(DotFolderService);
    });

    describe('loadFolders with undefined path', () => {
        it('should handle undefined path correctly', (done) => {
            store.loadFolders();

            setTimeout(() => {
                expect(folderService.getFolders).toHaveBeenCalled();
                expect(store.sidebarLoading()).toBe(false);
                done();
            }, 0);
        });
    });
});
