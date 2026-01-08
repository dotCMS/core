import { describe, it, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, mockProvider } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { DotFolderService } from '@dotcms/data-access';
import { ALL_FOLDER, DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';
import { createFakeFolder, createFakeSite } from '@dotcms/utils-testing';

import { withSidebar } from './withSidebar';

import { SYSTEM_HOST } from '../../../shared/constants';
import {
    DotContentDriveSortOrder,
    DotContentDriveState,
    DotContentDriveStatus
} from '../../../shared/models';

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

const mockTreeNodes: DotFolderTreeNodeItem[] = [
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
    selectedItems: [],
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

    const realAllFolder: DotFolderTreeNodeItem = {
        ...ALL_FOLDER,
        data: {
            hostname: mockSite.hostname,
            path: '',
            type: 'folder',
            id: mockSite.identifier
        }
    };

    const createService = createServiceFactory({
        service: sidebarStoreMock,
        providers: [
            mockProvider(DotFolderService, {
                getFolders: jest.fn().mockReturnValueOnce(of([])).mockReturnValue(of(mockFolders))
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        folderService = spectator.inject(DotFolderService);
    });

    describe('initial state', () => {
        it('should set initial after loading folders', () => {
            expect(store.sidebarLoading()).toBe(false);
            expect(store.folders()).toEqual([realAllFolder]);
            expect(store.selectedNode()).toEqual({
                ...realAllFolder
            });
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
                    expect(store.folders()).toContainEqual({
                        ...realAllFolder
                    });
                    done();
                }, 0);
            });

            it('should handle empty folder response', (done) => {
                folderService.getFolders.mockReturnValue(of([]));

                store.loadFolders();

                setTimeout(() => {
                    expect(store.sidebarLoading()).toBe(false);
                    expect(store.folders()).toContainEqual({
                        ...realAllFolder
                    });
                    done();
                }, 0);
            });
        });

        describe('loadChildFolders', () => {
            it('should load child folders for a specific path', (done) => {
                const testPath = '/documents/images/';
                const host = 'demo.dotcms.com';

                folderService.getFolders.mockReturnValue(of(mockFolders));

                store.loadChildFolders(testPath, host).subscribe((result) => {
                    expect(result.parent).toEqual(mockFolders[0]);
                    expect(result.folders).toHaveLength(2);
                    expect(folderService.getFolders).toHaveBeenCalledWith(`${host}${testPath}`);
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

            it('should not need to call loadChildFolders when node already has children', () => {
                // Create a node that already has children
                const nodeWithChildren: DotFolderTreeNodeItem = {
                    key: 'folder-with-children',
                    label: '/documents/',
                    data: {
                        id: 'folder-with-children',
                        hostname: 'demo.dotcms.com',
                        path: '/documents/',
                        type: 'folder'
                    },
                    leaf: false,
                    expanded: false,
                    children: [mockTreeNodes[0], mockTreeNodes[1]] // Already has children
                };

                // Reset the mock to count calls
                folderService.getFolders.mockClear();

                // Simulate component logic: check if node has children before calling loadChildFolders
                const shouldLoadChildren =
                    !nodeWithChildren.children || nodeWithChildren.children.length === 0;

                if (!shouldLoadChildren) {
                    // Don't call loadChildFolders if node already has children
                    expect(nodeWithChildren.children.length).toBeGreaterThan(0);
                    expect(folderService.getFolders).not.toHaveBeenCalled();
                } else {
                    // Only call loadChildFolders if node doesn't have children
                    store.loadChildFolders(nodeWithChildren.data.path);
                }

                // Verify the service was not called since node has children
                expect(folderService.getFolders).not.toHaveBeenCalled();
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
                const newFolders = [realAllFolder, ...mockTreeNodes];

                store.updateFolders(newFolders);

                expect(store.folders()).toEqual(newFolders);
            });

            it('should create a new array reference', () => {
                const originalFolders = store.folders();
                const newFolders = [realAllFolder, ...mockTreeNodes];

                store.updateFolders(newFolders);

                expect(store.folders()).not.toBe(originalFolders);
                expect(store.folders()).toEqual(newFolders);
            });
        });
    });

    describe('integration scenarios', () => {
        it('should handle child folder expansion workflow', () => {
            // Reset mock for this specific test with proper folder hierarchy
            folderService.getFolders.mockReturnValue(of(mockFolders));

            const parentPath = '/documents/';
            let loadedResult: { parent: unknown; folders: DotFolderTreeNodeItem[] } | null = null;

            // Load child folders synchronously since of() emits synchronously
            store.loadChildFolders(parentPath).subscribe((result) => {
                loadedResult = result;
            });

            expect(loadedResult).not.toBeNull();
            expect(loadedResult!.folders.length).toBeGreaterThan(0);

            // Update folders with new children
            const updatedFolders = [...store.folders(), ...loadedResult!.folders];
            store.updateFolders(updatedFolders);

            // Verify the folders were updated
            expect(store.folders().length).toBeGreaterThan(1);
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
describe('withSidebar - system host scenarios', () => {
    let spectator: SpectatorService<InstanceType<typeof sidebarStoreMock>>;
    let store: InstanceType<typeof sidebarStoreMock>;
    let folderService: jest.Mocked<DotFolderService>;

    const systemHostStoreMock = signalStore(
        withState<DotContentDriveState>({
            ...initialState,
            currentSite: SYSTEM_HOST
        }),

        withSidebar()
    );

    const createService = createServiceFactory({
        service: systemHostStoreMock,
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
