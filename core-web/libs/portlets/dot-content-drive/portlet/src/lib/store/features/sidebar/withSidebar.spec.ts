import { describe, it, expect } from '@jest/globals';
import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService, mockProvider } from '@openng/spectator/jest';
import { NEVER, of, Subject } from 'rxjs';

import { DotFolderService } from '@dotcms/data-access';
import { DotPagination, FolderSearchView } from '@dotcms/dotcms-models';
import { DotFolderTreeNodeItem } from '@dotcms/portlets/content-drive/ui';
import { createFakeFolderSearchView, createFakeSite } from '@dotcms/utils-testing';

import { withSidebar } from './withSidebar';

import { SYSTEM_HOST } from '../../../shared/constants';
import { DotContentDriveState } from '../../../shared/models';
import { createSiteNode } from '../../../utils/tree-folder.utils';
import { DOT_CONTENT_DRIVE_INITIAL_STATE } from '../../dot-content-drive.store';

const mockSite = createFakeSite();

const EMPTY_PAGINATION = {} as DotPagination;

const searchResult = (folders: FolderSearchView[]) => of({ folders, pagination: EMPTY_PAGINATION });

// Direct children of /documents/ as returned by the search endpoint (parent path + own name).
const mockChildViews: FolderSearchView[] = [
    createFakeFolderSearchView({
        id: 'child-folder-1',
        name: 'images',
        path: '/documents/',
        addChildrenAllowed: true
    }),
    createFakeFolderSearchView({
        id: 'child-folder-2',
        name: 'videos',
        path: '/documents/',
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
    // Seeded from the store's own initial state so this fixture cannot drift from it; only the
    // keys this feature's tests care about are overridden.
    ...DOT_CONTENT_DRIVE_INITIAL_STATE,
    currentSite: mockSite,
    path: '/test/path',
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

    // What `createSiteNode` produces for the mocked site: the row that stands for the site.
    const siteNode: DotFolderTreeNodeItem = createSiteNode(mockSite);

    /**
     * The site node as the tree holds it: the site's folders are its children, so its chevron
     * collapses the site. `searchFolders` is mocked empty unless a test says otherwise, hence the
     * default.
     */
    const siteNodeWithChildren = (
        children: DotFolderTreeNodeItem[] = []
    ): DotFolderTreeNodeItem => ({
        ...siteNode,
        children
    });

    const createService = createServiceFactory({
        service: sidebarStoreMock,
        providers: [
            mockProvider(DotFolderService, {
                searchFolders: jest.fn().mockReturnValue(searchResult([]))
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
            expect(store.folders()).toEqual([siteNodeWithChildren()]);
            expect(store.selectedNode()).toEqual(siteNode);
        });
    });

    describe('the site node', () => {
        const rootViews: FolderSearchView[] = [
            createFakeFolderSearchView({ id: 'a', name: 'activities', path: '/' }),
            createFakeFolderSearchView({ id: 'b', name: 'blog', path: '/' })
        ];

        beforeEach((done) => {
            folderService.searchFolders.mockReturnValue(searchResult(rootViews));
            store.loadFolders();
            setTimeout(done, 0);
        });

        afterEach(() => {
            // The mock is created once with the factory, so a return value set here would otherwise
            // stand for every later test in the file.
            folderService.searchFolders.mockReturnValue(searchResult([]));
        });

        it('should be the only top-level node', () => {
            expect(store.folders()).toHaveLength(1);
            expect(store.folders()[0].key).toBe(mockSite.identifier);
        });

        it('should own the site folders as its children, so its chevron collapses the site', () => {
            // As siblings they sat level with the site while its chevron controlled nothing, and
            // expanding it fetched them again, rendering every root folder twice.
            const children = store.folders()[0].children as DotFolderTreeNodeItem[];

            expect(children.map((child) => child.data!.path)).toEqual(['/activities/', '/blog/']);
        });

        it('should be labelled with the hostname and carry the site identifier', () => {
            expect(store.folders()[0].label).toBe(mockSite.hostname);
            expect(store.folders()[0].data!.id).toBe(mockSite.identifier);
        });

        it('should start expanded, since the site opens showing its folders', () => {
            expect(store.folders()[0].expanded).toBe(true);
        });
    });

    describe('methods', () => {
        describe('loadFolders', () => {
            it('should load folders for current site and path', (done) => {
                store.loadFolders();

                // Wait for async operations to complete
                setTimeout(() => {
                    expect(folderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ siteId: mockSite.identifier })
                    );
                    expect(store.sidebarLoading()).toBe(false);
                    expect(store.folders()).toContainEqual(siteNodeWithChildren());
                    done();
                }, 0);
            });

            it('should flag loading while a reload is in flight', () => {
                // Only the initial state used to set this, so a site change left the previous
                // site's tree on screen with no indication anything was happening — and gave
                // consumers no loaded edge to reveal the opened folder on. That it clears again is
                // covered by the cases above.
                folderService.searchFolders.mockReturnValue(NEVER);

                store.loadFolders();

                expect(store.sidebarLoading()).toBe(true);
            });

            it('should handle empty folder response', (done) => {
                folderService.searchFolders.mockReturnValue(searchResult([]));

                store.loadFolders();

                setTimeout(() => {
                    expect(store.sidebarLoading()).toBe(false);
                    expect(store.folders()).toContainEqual(siteNodeWithChildren());
                    done();
                }, 0);
            });

            // Two triggers can call this concurrently on a cold load: the feature's own `onInit`
            // and the sidebar component's `currentSite` effect. Without cancellation both writes
            // land and the one that *resolves* last wins, so a slower earlier request overwrites a
            // newer complete one — the tree shows the wrong folders until the next reload.
            describe('when a second load starts before the first resolves', () => {
                const viewNamed = (name: string) =>
                    createFakeFolderSearchView({
                        id: `folder-${name}`,
                        name,
                        path: '/',
                        addChildrenAllowed: true
                    });

                const labelsInTree = () =>
                    (store.folders()[0]?.children ?? []).map((child) => child.label);

                it('should keep the newer result when the older one resolves last', (done) => {
                    const stale = new Subject<{
                        folders: FolderSearchView[];
                        pagination: DotPagination;
                    }>();

                    folderService.searchFolders.mockReturnValue(stale);
                    store.loadFolders();

                    folderService.searchFolders.mockReturnValue(searchResult([viewNamed('fresh')]));
                    store.loadFolders();

                    // The first request answers only now, after the second already has.
                    stale.next({ folders: [viewNamed('stale')], pagination: EMPTY_PAGINATION });
                    stale.complete();

                    setTimeout(() => {
                        expect(labelsInTree()).toEqual(['/fresh/']);
                        done();
                    }, 0);
                });

                it('should settle loading once, on the newer result', (done) => {
                    folderService.searchFolders.mockReturnValue(NEVER);
                    store.loadFolders();

                    folderService.searchFolders.mockReturnValue(searchResult([viewNamed('fresh')]));
                    store.loadFolders();

                    setTimeout(() => {
                        expect(store.sidebarLoading()).toBe(false);
                        done();
                    }, 0);
                });
            });
        });

        describe('loadChildFolders', () => {
            it('should load child folders for a specific path', (done) => {
                const testPath = '/documents/images/';
                const host = 'demo.dotcms.com';

                folderService.searchFolders.mockReturnValue(searchResult(mockChildViews));

                store.loadChildFolders(testPath, host).subscribe((result) => {
                    expect(result.folders).toHaveLength(2);
                    expect(folderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({
                            siteId: mockSite.identifier,
                            path: testPath,
                            recursive: false
                        })
                    );
                    done();
                });
            });

            it('should transform folders into tree nodes correctly', (done) => {
                const testPath = '/documents/';

                folderService.searchFolders.mockReturnValue(searchResult(mockChildViews));

                store.loadChildFolders(testPath).subscribe((result) => {
                    expect(result.folders).toHaveLength(2);
                    expect(result.folders[0]).toHaveProperty('key');
                    expect(result.folders[0]).toHaveProperty('label');
                    expect(result.folders[0]).toHaveProperty('data');
                    expect(result.folders[0].data!.type).toBe('folder');
                    done();
                });
            });

            it('should thread the requested page through to the search endpoint', (done) => {
                folderService.searchFolders.mockReturnValue(searchResult(mockChildViews));

                store.loadChildFolders('/documents/', 'demo.dotcms.com', 3).subscribe(() => {
                    expect(folderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ path: '/documents/', page: 3 })
                    );
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
                folderService.searchFolders.mockClear();

                // Simulate component logic: check if node has children before calling loadChildFolders
                const shouldLoadChildren =
                    !nodeWithChildren.children || nodeWithChildren.children.length === 0;

                if (!shouldLoadChildren) {
                    // Don't call loadChildFolders if node already has children
                    expect(nodeWithChildren.children!.length).toBeGreaterThan(0);
                    expect(folderService.searchFolders).not.toHaveBeenCalled();
                } else {
                    // Only call loadChildFolders if node doesn't have children
                    store.loadChildFolders(nodeWithChildren.data!.path!);
                }

                // Verify the service was not called since node has children
                expect(folderService.searchFolders).not.toHaveBeenCalled();
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
                const newFolders = [siteNode, ...mockTreeNodes];

                store.updateFolders(newFolders);

                expect(store.folders()).toEqual(newFolders);
            });

            it('should create a new array reference', () => {
                const originalFolders = store.folders();
                const newFolders = [siteNode, ...mockTreeNodes];

                store.updateFolders(newFolders);

                expect(store.folders()).not.toBe(originalFolders);
                expect(store.folders()).toEqual(newFolders);
            });
        });
    });

    describe('integration scenarios', () => {
        it('should handle child folder expansion workflow', () => {
            // Reset mock for this specific test with proper folder hierarchy
            folderService.searchFolders.mockReturnValue(searchResult(mockChildViews));

            const parentPath = '/documents/';
            // Collected into an array rather than a nullable `let`: TypeScript's control-flow
            // analysis does not track assignments made inside a callback, so after the `null`
            // initializer it still read the variable as `null` and the guard below narrowed it
            // to `never`. This also pins that exactly one value was emitted.
            const emitted: { folders: DotFolderTreeNodeItem[] }[] = [];

            // Load child folders synchronously since of() emits synchronously
            store.loadChildFolders(parentPath).subscribe((result) => {
                emitted.push(result);
            });

            expect(emitted).toHaveLength(1);
            const loadedResult = emitted[0];

            expect(loadedResult.folders.length).toBeGreaterThan(0);

            // Update folders with new children
            const updatedFolders = [...store.folders(), ...loadedResult.folders];
            store.updateFolders(updatedFolders);

            // Verify the folders were updated
            expect(store.folders().length).toBeGreaterThan(1);
        });
    });
});

describe('withSidebar - unset site scenarios', () => {
    let spectator: SpectatorService<InstanceType<typeof sidebarStoreMock>>;
    let store: InstanceType<typeof sidebarStoreMock>;
    let folderService: jest.Mocked<DotFolderService>;

    const nullSiteStoreMock = signalStore(
        withState<DotContentDriveState>({
            ...initialState,
            // `undefined`, not `null`: that is what the state holds before init, and every guard
            // downstream is a falsy check, so the branch under test is the same one.
            currentSite: undefined
        }),

        withSidebar()
    );

    const createService = createServiceFactory({
        service: nullSiteStoreMock,
        providers: [
            mockProvider(DotFolderService, {
                searchFolders: jest.fn().mockReturnValue(searchResult(mockChildViews))
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        folderService = spectator.inject(DotFolderService);
    });

    describe('loadFolders with an unset site', () => {
        it('should not load folders when currentSite is null', () => {
            store.loadFolders();

            expect(folderService.searchFolders).not.toHaveBeenCalled();
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
                searchFolders: jest.fn().mockReturnValue(searchResult(mockChildViews))
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        folderService = spectator.inject(DotFolderService);
    });

    describe('loadFolders with an unset site', () => {
        it('should not load folders when currentSite is null', () => {
            store.loadFolders();

            expect(folderService.searchFolders).not.toHaveBeenCalled();
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
                searchFolders: jest.fn().mockReturnValue(searchResult(mockChildViews))
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
                expect(folderService.searchFolders).toHaveBeenCalled();
                expect(store.sidebarLoading()).toBe(false);
                done();
            }, 0);
        });
    });
});
