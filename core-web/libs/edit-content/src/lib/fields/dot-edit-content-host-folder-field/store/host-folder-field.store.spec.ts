import { patchState } from '@ngrx/signals';
import { unprotected } from '@ngrx/signals/testing';
import { SpyObject, mockProvider } from '@openng/spectator/jest';
import { Subject, of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotPagination,
    TreeNodeItem,
    TreeNodeSelectItem
} from '@dotcms/dotcms-models';
import { DotBrowsingService } from '@dotcms/ui';

import {
    FOLDER_PAGE_LIMIT,
    HostFolderFiledStore,
    LOAD_MORE_NODE_TYPE,
    ROOT_NODE_KEY,
    SITE_PAGE_LIMIT,
    SITE_SEARCH_THRESHOLD,
    SYSTEM_HOST_NAME
} from './host-folder-field.store';

import { TREE_SELECT_MOCK, TREE_SELECT_SITES_MOCK } from '../../../utils/mocks';

function createSitesPageResponse(
    sites: TreeNodeItem[],
    totalEntries?: number,
    currentPage = 1
): { sites: TreeNodeItem[]; pagination: DotPagination } {
    return {
        sites,
        pagination: {
            currentPage,
            perPage: SITE_PAGE_LIMIT,
            totalEntries: totalEntries ?? sites.length
        }
    };
}

function mockSitesPage(
    browsingService: SpyObject<DotBrowsingService>,
    sites: TreeNodeItem[],
    totalEntries?: number
): void {
    browsingService.getSitesPage.mockReturnValue(of(createSitesPageResponse(sites, totalEntries)));
}

function createSiteList(count: number, prefix = 'site'): TreeNodeItem[] {
    return Array.from({ length: count }, (_, index) => ({
        key: `${prefix}-${index}`,
        label: `${prefix}-${index}.dotcms.com`,
        data: {
            id: `${prefix}-${index}`,
            hostname: `${prefix}-${index}.dotcms.com`,
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder'
    }));
}

function mockResolveSiteByHostname(
    browsingService: SpyObject<DotBrowsingService>,
    resolver: (hostname: string) => TreeNodeItem | null = (hostname) =>
        [...TREE_SELECT_SITES_MOCK, ...TREE_SELECT_MOCK].find((site) => site.label === hostname) ??
        null
): void {
    browsingService.resolveSiteByHostname.mockImplementation((hostname: string) =>
        of(resolver(hostname))
    );
}

describe('HostFolderFiledStore', () => {
    let store: InstanceType<typeof HostFolderFiledStore>;
    let service: SpyObject<DotBrowsingService>;
    let httpErrorManager: SpyObject<DotHttpErrorManagerService>;

    const mockPagination: DotPagination = {
        currentPage: 1,
        perPage: FOLDER_PAGE_LIMIT,
        totalEntries: 0
    };

    const httpError = new HttpErrorResponse({ status: 500, statusText: 'Server Error' });

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                HostFolderFiledStore,
                mockProvider(DotHttpErrorManagerService, {
                    handle: jest.fn()
                }),
                mockProvider(DotBrowsingService, {
                    getSitesPage: jest.fn(() =>
                        of(createSitesPageResponse(TREE_SELECT_SITES_MOCK))
                    ),
                    resolveSiteByHostname: jest.fn(),
                    getCurrentSiteAsTreeNodeItem: jest.fn(),
                    buildTreeByPaths: jest.fn(),
                    searchFolders: jest.fn(() => of({ folders: [], pagination: mockPagination }))
                })
            ]
        });

        store = TestBed.inject(HostFolderFiledStore);
        service = TestBed.inject(DotBrowsingService) as SpyObject<DotBrowsingService>;
        mockResolveSiteByHostname(service);
        httpErrorManager = TestBed.inject(
            DotHttpErrorManagerService
        ) as SpyObject<DotHttpErrorManagerService>;
    });

    describe('Method: loadSites', () => {
        describe('System Host isRequired', () => {
            it('should include System Host when isRequired is false.', () => {
                mockSitesPage(service, TREE_SELECT_SITES_MOCK);
                store.loadSites({ path: null, isRequired: false });
                const hasSystemHost = store.sites().some((item) => item.label === SYSTEM_HOST_NAME);
                expect(hasSystemHost).toBe(true);
            });

            it('should not include System Host when isRequired is true.', () => {
                mockSitesPage(service, TREE_SELECT_SITES_MOCK);
                store.loadSites({ path: null, isRequired: true });
                const hasSystemHost = store.sites().some((item) => item.label === SYSTEM_HOST_NAME);
                expect(hasSystemHost).toBe(false);
            });
        });

        describe('when path is not empty', () => {
            it('should select the node/site if the path is not empty and not required', () => {
                const node = TREE_SELECT_SITES_MOCK[0];
                mockSitesPage(service, TREE_SELECT_SITES_MOCK);
                store.loadSites({ path: node.label, isRequired: false });

                expect(service.getCurrentSiteAsTreeNodeItem).not.toHaveBeenCalled();
                expect(store.selectedSite().key).toBe(node.key);
                expect(store.confirmedNode().key).toBe(node.key);
                expect(store.pendingNode().key).toBe(node.key);
            });

            it('should select the node/site if the path is not empty and is required', () => {
                const node = TREE_SELECT_SITES_MOCK[0];
                mockSitesPage(service, TREE_SELECT_SITES_MOCK);
                store.loadSites({ path: node.label, isRequired: true });

                expect(service.getCurrentSiteAsTreeNodeItem).not.toHaveBeenCalled();
                expect(store.selectedSite().key).toBe(node.key);
            });

            it('should resolve a nested folder path via buildTreeByPaths and set confirmed/pending node', fakeAsync(() => {
                const [site] = TREE_SELECT_MOCK;
                const targetNode = site.children[0];

                mockSitesPage(service, TREE_SELECT_MOCK);
                service.buildTreeByPaths.mockReturnValue(
                    of({
                        node: targetNode,
                        tree: {
                            path: '/',
                            folders: site.children,
                            parent: {
                                hostName: site.data.hostname,
                                id: site.data.id,
                                path: '/',
                                addChildrenAllowed: true
                            }
                        },
                        pagination: {
                            [ROOT_NODE_KEY]: { page: 1, hasMore: false }
                        }
                    })
                );

                store.loadSites({ path: 'demo.dotcms.com/level1', isRequired: false });
                tick();

                expect(service.buildTreeByPaths).toHaveBeenCalledWith(
                    'demo.dotcms.com',
                    'demo.dotcms.com',
                    '/level1/'
                );
                expect(store.selectedSite().key).toBe(site.key);
                expect(store.confirmedNode().key).toBe(targetNode.key);
                expect(store.pendingNode().key).toBe(targetNode.key);
                expect(store.folders()).toEqual(site.children);
                // Level was resolved via buildTreeByPaths, so root pagination is marked as fully loaded
                expect(store.nodePagination()[ROOT_NODE_KEY]).toEqual({
                    page: 1,
                    hasMore: false,
                    loading: false
                });
            }));

            it('should seed nodePagination and inject a Load more sentinel when buildTreeByPaths reports hasMore', fakeAsync(() => {
                const [site] = TREE_SELECT_MOCK;
                const targetNode = {
                    ...site.children[0],
                    key: 'gallery',
                    data: {
                        ...site.children[0].data,
                        path: '/gallery/'
                    },
                    children: undefined
                };
                const rootFolders = [{ ...site.children[0], children: undefined }, targetNode];

                mockSitesPage(service, TREE_SELECT_MOCK);
                service.buildTreeByPaths.mockReturnValue(
                    of({
                        node: targetNode,
                        tree: {
                            path: '/',
                            folders: rootFolders
                        },
                        pagination: {
                            [ROOT_NODE_KEY]: { page: 2, hasMore: true }
                        }
                    })
                );

                store.loadSites({ path: 'demo.dotcms.com/gallery', isRequired: false });
                tick();

                expect(store.confirmedNode().key).toBe('gallery');
                expect(store.nodePagination()[ROOT_NODE_KEY]).toEqual({
                    page: 2,
                    hasMore: true,
                    loading: false
                });

                const folders = store.folders();
                const loadMoreNode = folders[folders.length - 1];
                expect(loadMoreNode.type).toBe(LOAD_MORE_NODE_TYPE);
                expect(loadMoreNode.key).toBe(`load-more:${ROOT_NODE_KEY}`);
                expect(folders.slice(0, -1).map((folder) => folder.key)).toEqual([
                    site.children[0].key,
                    'gallery'
                ]);
            }));
        });

        describe('path normalization', () => {
            it('should normalize a colon-separated persisted path before calling buildTreeByPaths', fakeAsync(() => {
                const [site] = TREE_SELECT_MOCK;
                const targetNode = site.children[0];

                mockSitesPage(service, TREE_SELECT_MOCK);
                service.buildTreeByPaths.mockReturnValue(
                    of({
                        node: targetNode,
                        tree: {
                            path: '/',
                            folders: site.children,
                            parent: {
                                hostName: site.data.hostname,
                                id: site.data.id,
                                path: '/',
                                addChildrenAllowed: true
                            }
                        }
                    })
                );

                store.loadSites({ path: 'demo.dotcms.com:/level1/', isRequired: false });
                tick();

                expect(service.buildTreeByPaths).toHaveBeenCalledWith(
                    'demo.dotcms.com',
                    'demo.dotcms.com',
                    '/level1/'
                );
            }));

            it('should normalize a leading double-slash persisted path before calling buildTreeByPaths', fakeAsync(() => {
                const [site] = TREE_SELECT_MOCK;
                const targetNode = site.children[0];

                mockSitesPage(service, TREE_SELECT_MOCK);
                service.buildTreeByPaths.mockReturnValue(
                    of({
                        node: targetNode,
                        tree: {
                            path: '/',
                            folders: site.children,
                            parent: {
                                hostName: site.data.hostname,
                                id: site.data.id,
                                path: '/',
                                addChildrenAllowed: true
                            }
                        }
                    })
                );

                store.loadSites({ path: '//demo.dotcms.com/level1/', isRequired: false });
                tick();

                expect(service.buildTreeByPaths).toHaveBeenCalledWith(
                    'demo.dotcms.com',
                    'demo.dotcms.com',
                    '/level1/'
                );
            }));
        });

        describe('ancestor expansion', () => {
            it('should mark ancestor folders as expanded so the tree opens to the resolved node, even when buildTreeByPaths does not flag them', fakeAsync(() => {
                const [site] = TREE_SELECT_MOCK;
                const level1 = site.children[0];
                const targetNode = { ...level1.children[0], expanded: false };
                const unexpandedLevel1 = { ...level1, expanded: false, children: [targetNode] };

                mockSitesPage(service, TREE_SELECT_MOCK);
                service.buildTreeByPaths.mockReturnValue(
                    of({
                        node: targetNode,
                        tree: {
                            path: '/',
                            folders: [unexpandedLevel1],
                            parent: {
                                hostName: site.data.hostname,
                                id: site.data.id,
                                path: '/',
                                addChildrenAllowed: true
                            }
                        }
                    })
                );

                store.loadSites({
                    path: 'demo.dotcms.com/level1/child1',
                    isRequired: false
                });
                tick();

                const [resolvedLevel1] = store.folders();
                expect(resolvedLevel1.expanded).toBe(true);
            }));
        });

        describe('when path is empty', () => {
            it('should select System Host if not required', () => {
                mockSitesPage(service, TREE_SELECT_SITES_MOCK);
                store.loadSites({ path: null, isRequired: false });

                expect(service.getCurrentSiteAsTreeNodeItem).not.toHaveBeenCalled();
                expect(store.selectedSite().label).toBe(SYSTEM_HOST_NAME);
            });

            it('should select current site if required', fakeAsync(() => {
                const hostNode = TREE_SELECT_SITES_MOCK[1];
                mockSitesPage(service, TREE_SELECT_SITES_MOCK);
                service.getCurrentSiteAsTreeNodeItem.mockReturnValue(of(hostNode));

                store.loadSites({ path: null, isRequired: true });
                tick();

                expect(service.getCurrentSiteAsTreeNodeItem).toHaveBeenCalled();
                expect(store.selectedSite().label).toBe(hostNode.label);
            }));
        });

        describe('when the resolved site cannot be found', () => {
            it('should surface an error instead of leaving the store silently uninitialized when there are no sites available', fakeAsync(() => {
                mockSitesPage(service, [], 0);
                mockResolveSiteByHostname(service, () => null);

                store.loadSites({ path: null, isRequired: false });
                tick();

                expect(store.selectedSite()).toBe(null);
                expect(store.confirmedNode()).toBe(null);
                expect(store.sitesStatus()).toBe(ComponentStatus.ERROR);
            }));

            it('should surface an error instead of leaving the store silently uninitialized when the persisted path resolves to a hostname not present in the sites list (e.g. an archived/inaccessible site)', fakeAsync(() => {
                mockSitesPage(service, TREE_SELECT_SITES_MOCK);
                mockResolveSiteByHostname(service, () => null);

                store.loadSites({ path: 'unknown-site.dotcms.com/level1', isRequired: false });
                tick();

                expect(service.buildTreeByPaths).not.toHaveBeenCalled();
                expect(store.selectedSite()).toBe(null);
                expect(store.confirmedNode()).toBe(null);
                expect(store.sitesStatus()).toBe(ComponentStatus.ERROR);
            }));
        });
    });

    describe('Computed: pathToSave / copyPath / displayPath', () => {
        it('should format the root site path correctly', () => {
            store.selectSite(TREE_SELECT_MOCK[0]);
            store.commit();
            expect(store.pathToSave()).toBe('demo.dotcms.com:/');
            expect(store.copyPath()).toBe('//demo.dotcms.com/');
            expect(store.displayPath()).toBe('demo.dotcms.com');
        });

        it('should format a nested folder path correctly, matching copyPath', () => {
            const node = TREE_SELECT_MOCK[0].children[0];
            store.setPendingNode(node);
            store.commit();
            expect(store.pathToSave()).toBe('demo.dotcms.com:/level1/');
            expect(store.copyPath()).toBe('//demo.dotcms.com/level1/');
            expect(store.displayPath()).toBe('demo.dotcms.com / level1');
        });

        it('should be null when there is no confirmed node', () => {
            expect(store.pathToSave()).toBe(null);
            expect(store.copyPath()).toBe('');
            expect(store.displayPath()).toBe('');
        });
    });

    describe('Method: selectSite', () => {
        it('should reset folders/search state, stage the site as pending, and load its root folders', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            const mockFolders: TreeNodeItem[] = [
                {
                    key: 'folder-1',
                    label: 'demo.dotcms.com/folder1/',
                    data: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/folder1/',
                        type: 'folder'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false
                }
            ];
            service.searchFolders.mockReturnValue(
                of({
                    folders: mockFolders,
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                })
            );

            store.selectSite(site);

            expect(store.selectedSite()).toBe(site);
            expect(store.pendingNode()).toBe(site);
            expect(store.searchTerm()).toBe('');
            expect(store.siteSearchTerm()).toBe('');
            expect(store.searchResults()).toBe(null);
            expect(service.searchFolders).toHaveBeenCalledWith(
                {
                    siteId: site.data.id,
                    path: '/',
                    recursive: false,
                    page: 1,
                    per_page: FOLDER_PAGE_LIMIT
                },
                site.data.hostname
            );
            expect(store.folders()).toEqual(mockFolders);
        });

        it('should stage the site root when re-selecting the already-selected site, clearing folder selection', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            const folder: TreeNodeItem = {
                key: 'folder-1',
                label: 'demo.dotcms.com/folder1/',
                data: {
                    id: 'folder-1',
                    hostname: 'demo.dotcms.com',
                    path: '/folder1/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                leaf: false
            };
            const mockFolders: TreeNodeItem[] = [folder];
            service.searchFolders.mockReturnValue(
                of({
                    folders: mockFolders,
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                })
            );

            store.selectSite(site);
            store.setPendingNode(folder);
            service.searchFolders.mockClear();

            const foldersBeforeReclick = store.folders();

            store.selectSite(site);

            expect(service.searchFolders).not.toHaveBeenCalled();
            expect(store.pendingNode()).toBe(site);
            expect(store.folders()).toBe(foldersBeforeReclick);
            expect(store.treeSelection()).toBe(null);
        });
    });

    describe('Computed: treeSelection', () => {
        it('should return null when there is no pending node', () => {
            expect(store.treeSelection()).toBe(null);
        });

        it('should resolve the pending node to the current object reference inside folders() by key', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            const folder: TreeNodeItem = {
                key: 'folder-1',
                label: 'demo.dotcms.com/folder1/',
                data: {
                    id: 'folder-1',
                    hostname: 'demo.dotcms.com',
                    path: '/folder1/',
                    type: 'folder'
                }
            };
            service.searchFolders.mockReturnValue(
                of({
                    folders: [folder],
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                })
            );
            store.selectSite(site);

            // A stale reference sharing the same key but not the object living in `folders()`
            // (e.g. after a `structuredClone` elsewhere) should still resolve correctly.
            store.setPendingNode({ ...folder });

            expect(store.treeSelection()).toBe(store.folders()[0]);
        });

        it('should return null when the pending node is not found in the current folders', () => {
            store.setPendingNode({
                key: 'missing',
                label: 'missing',
                data: {
                    id: 'missing',
                    hostname: 'demo.dotcms.com',
                    path: '/missing/',
                    type: 'folder'
                }
            });

            expect(store.treeSelection()).toBe(null);
        });
    });

    describe('Method: expandNode', () => {
        const buildEvent = (node: TreeNodeItem): TreeNodeSelectItem => ({
            originalEvent: new Event('click'),
            node
        });

        it('should lazily load a node children when it has none yet', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);

            const node: TreeNodeItem = {
                key: 'folder-1',
                label: 'demo.dotcms.com/folder1/',
                data: {
                    id: 'folder-1',
                    hostname: 'demo.dotcms.com',
                    path: '/folder1/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                leaf: false
            };
            const childFolders: TreeNodeItem[] = [
                {
                    key: 'child-1',
                    label: 'demo.dotcms.com/folder1/child1/',
                    data: {
                        id: 'child-1',
                        hostname: 'demo.dotcms.com',
                        path: '/folder1/child1/',
                        type: 'folder'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder'
                }
            ];
            service.searchFolders.mockReturnValue(
                of({
                    folders: childFolders,
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                })
            );

            store.expandNode(buildEvent(node));

            expect(service.searchFolders).toHaveBeenCalledWith(
                {
                    siteId: site.data.id,
                    path: node.data.path,
                    recursive: false,
                    page: 1,
                    per_page: FOLDER_PAGE_LIMIT
                },
                site.data.hostname
            );
            expect(node.children).toEqual(childFolders);
            expect(node.loading).toBe(false);
        });

        it('should set node.loading on the tree node while children are loading', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            const node: TreeNodeItem = {
                key: 'folder-1',
                label: 'demo.dotcms.com/folder1/',
                data: {
                    id: 'folder-1',
                    hostname: 'demo.dotcms.com',
                    path: '/folder1/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                leaf: false
            };
            const pending$ = new Subject<{
                folders: TreeNodeItem[];
                pagination: DotPagination;
            }>();

            service.searchFolders.mockImplementation((params) => {
                if (params.path === '/folder1/') {
                    return pending$.asObservable();
                }

                return of({ folders: [], pagination: mockPagination });
            });
            store.selectSite(site);
            patchState(unprotected(store), {
                folders: [node],
                foldersStatus: ComponentStatus.LOADED
            });

            store.expandNode(buildEvent(node));

            expect(store.folders().find((item) => item.key === 'folder-1')?.loading).toBe(true);

            pending$.next({
                folders: [],
                pagination: { currentPage: 1, perPage: 40, totalEntries: 0 }
            });
            pending$.complete();

            expect(store.folders().find((item) => item.key === 'folder-1')?.loading).toBe(false);
        });

        it('should not fetch when the node is a leaf', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockClear();

            const leafNode: TreeNodeItem = {
                key: 'leaf-1',
                label: 'demo.dotcms.com/leaf1/',
                data: {
                    id: 'leaf-1',
                    hostname: 'demo.dotcms.com',
                    path: '/leaf1/',
                    type: 'folder'
                },
                leaf: true
            };

            store.expandNode(buildEvent(leafNode));

            expect(service.searchFolders).not.toHaveBeenCalled();
        });

        it('should not re-fetch when the node already has loaded children', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockClear();

            const nodeWithChildren: TreeNodeItem = {
                key: 'folder-2',
                label: 'demo.dotcms.com/folder2/',
                data: {
                    id: 'folder-2',
                    hostname: 'demo.dotcms.com',
                    path: '/folder2/',
                    type: 'folder'
                },
                children: [
                    {
                        key: 'existing-child',
                        label: 'existing',
                        data: {
                            id: 'existing-child',
                            hostname: 'demo.dotcms.com',
                            path: '/folder2/existing/',
                            type: 'folder'
                        }
                    }
                ]
            };

            store.expandNode(buildEvent(nodeWithChildren));

            expect(service.searchFolders).not.toHaveBeenCalled();
        });

        it('should not expand leaf search results', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            const searchResultNode: TreeNodeItem = {
                key: 'match-1',
                label: 'demo.dotcms.com/match/',
                data: {
                    id: 'match-1',
                    hostname: 'demo.dotcms.com',
                    path: '/match/',
                    type: 'folder'
                },
                leaf: true
            };

            service.searchFolders.mockReturnValue(
                of({
                    folders: [searchResultNode],
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                })
            );
            store.selectSite(site);
            store.openOverlay();
            store.search('match');
            tick(500);

            expect(store.isSearching()).toBe(true);
            service.searchFolders.mockClear();

            store.expandNode(buildEvent(searchResultNode));

            expect(service.searchFolders).not.toHaveBeenCalled();
            expect(searchResultNode.children).toBeUndefined();
        }));

        it('should load sibling nodes concurrently without leaving the first stuck in loading', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockClear();

            const nodeA: TreeNodeItem = {
                key: 'folder-a',
                label: 'demo.dotcms.com/folderA/',
                data: {
                    id: 'folder-a',
                    hostname: 'demo.dotcms.com',
                    path: '/folderA/',
                    type: 'folder'
                },
                leaf: false
            };
            const nodeB: TreeNodeItem = {
                key: 'folder-b',
                label: 'demo.dotcms.com/folderB/',
                data: {
                    id: 'folder-b',
                    hostname: 'demo.dotcms.com',
                    path: '/folderB/',
                    type: 'folder'
                },
                leaf: false
            };
            const childrenA: TreeNodeItem[] = [
                {
                    key: 'child-a',
                    label: 'demo.dotcms.com/folderA/child/',
                    data: {
                        id: 'child-a',
                        hostname: 'demo.dotcms.com',
                        path: '/folderA/child/',
                        type: 'folder'
                    }
                }
            ];
            const childrenB: TreeNodeItem[] = [
                {
                    key: 'child-b',
                    label: 'demo.dotcms.com/folderB/child/',
                    data: {
                        id: 'child-b',
                        hostname: 'demo.dotcms.com',
                        path: '/folderB/child/',
                        type: 'folder'
                    }
                }
            ];
            const pendingA$ = new Subject<{
                folders: TreeNodeItem[];
                pagination: DotPagination;
            }>();

            service.searchFolders.mockImplementation((params) => {
                if (params.path === nodeA.data.path) {
                    return pendingA$.asObservable();
                }

                return of({
                    folders: childrenB,
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                });
            });

            store.expandNode(buildEvent(nodeA));
            expect(
                store.folders().find((item) => item.key === 'folder-a')?.loading ?? nodeA.loading
            ).toBe(true);
            expect(store.nodePagination()['folder-a'].loading).toBe(true);

            store.expandNode(buildEvent(nodeB));

            expect(nodeB.children).toEqual(childrenB);
            expect(nodeB.loading).toBe(false);
            expect(store.nodePagination()['folder-b'].loading).toBe(false);
            expect(store.nodePagination()['folder-a'].loading).toBe(true);

            pendingA$.next({
                folders: childrenA,
                pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
            });
            pendingA$.complete();

            expect(nodeA.children).toEqual(childrenA);
            expect(
                store.folders().find((item) => item.key === 'folder-a')?.loading ?? nodeA.loading
            ).toBe(false);
            expect(store.nodePagination()['folder-a'].loading).toBe(false);
        });
    });

    describe('Method: loadMore', () => {
        it('should request the next page at root level and append the results', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            const firstPage: TreeNodeItem[] = [
                {
                    key: 'folder-1',
                    label: 'demo.dotcms.com/folder1/',
                    data: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/folder1/',
                        type: 'folder'
                    }
                }
            ];
            service.searchFolders.mockReturnValue(
                of({
                    folders: firstPage,
                    pagination: { currentPage: 1, perPage: 1, totalEntries: 2 }
                })
            );
            store.selectSite(site);

            const secondPage: TreeNodeItem[] = [
                {
                    key: 'folder-2',
                    label: 'demo.dotcms.com/folder2/',
                    data: {
                        id: 'folder-2',
                        hostname: 'demo.dotcms.com',
                        path: '/folder2/',
                        type: 'folder'
                    }
                }
            ];
            service.searchFolders.mockReturnValue(
                of({
                    folders: secondPage,
                    pagination: { currentPage: 2, perPage: 1, totalEntries: 2 }
                })
            );

            store.loadMore(null);

            expect(service.searchFolders).toHaveBeenCalledWith(
                {
                    siteId: site.data.id,
                    path: '/',
                    recursive: false,
                    page: 2,
                    per_page: FOLDER_PAGE_LIMIT
                },
                site.data.hostname
            );
            expect(store.folders()).toEqual([...firstPage, ...secondPage]);
            expect(store.nodePagination()[ROOT_NODE_KEY]).toEqual({
                page: 2,
                hasMore: false,
                loading: false
            });
        });
    });

    describe('Load more sentinel node', () => {
        it('should append a load-more sentinel as the last root folder when more pages are available', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            const firstPage: TreeNodeItem[] = [
                {
                    key: 'folder-1',
                    label: 'demo.dotcms.com/folder1/',
                    data: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/folder1/',
                        type: 'folder'
                    }
                }
            ];
            service.searchFolders.mockReturnValue(
                of({
                    folders: firstPage,
                    pagination: { currentPage: 1, perPage: 1, totalEntries: 2 }
                })
            );

            store.selectSite(site);

            const folders = store.folders();
            expect(folders).toHaveLength(2);
            expect(folders[0]).toEqual(firstPage[0]);
            expect(folders[1].type).toBe('load-more');
            expect(folders[1].selectable).toBe(false);
            expect(folders[1].leaf).toBe(true);
        });

        it('should append a load-more sentinel as the last child of an expanded node with more pages', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);

            const node: TreeNodeItem = {
                key: 'folder-1',
                label: 'demo.dotcms.com/folder1/',
                data: {
                    id: 'folder-1',
                    hostname: 'demo.dotcms.com',
                    path: '/folder1/',
                    type: 'folder'
                },
                leaf: false
            };
            const childFirstPage: TreeNodeItem[] = [
                {
                    key: 'child-1',
                    label: 'demo.dotcms.com/folder1/child1/',
                    data: {
                        id: 'child-1',
                        hostname: 'demo.dotcms.com',
                        path: '/folder1/child1/',
                        type: 'folder'
                    }
                }
            ];
            service.searchFolders.mockReturnValue(
                of({
                    folders: childFirstPage,
                    pagination: { currentPage: 1, perPage: 1, totalEntries: 2 }
                })
            );

            store.expandNode({ originalEvent: new Event('click'), node });

            const children = node.children as TreeNodeItem[];
            expect(children).toHaveLength(2);
            expect(children[0]).toEqual(childFirstPage[0]);
            expect(children[1].type).toBe('load-more');
            expect(children[1].selectable).toBe(false);
        });

        it('should drop the sentinel once a level is fully loaded after loading more', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            const node: TreeNodeItem = {
                key: 'folder-1',
                label: 'demo.dotcms.com/folder1/',
                data: {
                    id: 'folder-1',
                    hostname: 'demo.dotcms.com',
                    path: '/folder1/',
                    type: 'folder'
                },
                leaf: false
            };
            const childFirstPage: TreeNodeItem[] = [
                {
                    key: 'child-1',
                    label: 'demo.dotcms.com/folder1/child1/',
                    data: {
                        id: 'child-1',
                        hostname: 'demo.dotcms.com',
                        path: '/folder1/child1/',
                        type: 'folder'
                    }
                }
            ];
            store.selectSite(site);
            service.searchFolders.mockReturnValue(
                of({
                    folders: childFirstPage,
                    pagination: { currentPage: 1, perPage: 1, totalEntries: 2 }
                })
            );
            store.expandNode({ originalEvent: new Event('click'), node });

            const childSecondPage: TreeNodeItem[] = [
                {
                    key: 'child-2',
                    label: 'demo.dotcms.com/folder1/child2/',
                    data: {
                        id: 'child-2',
                        hostname: 'demo.dotcms.com',
                        path: '/folder1/child2/',
                        type: 'folder'
                    }
                }
            ];
            service.searchFolders.mockReturnValue(
                of({
                    folders: childSecondPage,
                    pagination: { currentPage: 2, perPage: 1, totalEntries: 2 }
                })
            );

            store.loadMore(node);

            expect(node.children).toEqual([childFirstPage[0], childSecondPage[0]]);
        });
    });

    describe('Method: search', () => {
        beforeEach(() => {
            store.openOverlay();
        });

        it('should ignore terms shorter than the minimum length', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockClear();

            store.search('a');
            tick(500);

            expect(service.searchFolders).not.toHaveBeenCalled();
            expect(store.searchResults()).toBe(null);
        }));

        it('should search folders when the term is exactly the minimum length (2 chars), matching short folder names', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockClear();

            const results: TreeNodeItem[] = [
                {
                    key: 'fx-1',
                    label: 'demo.dotcms.com/fx/',
                    data: {
                        id: 'fx-1',
                        hostname: 'demo.dotcms.com',
                        path: '/fx/',
                        type: 'folder'
                    }
                }
            ];
            service.searchFolders.mockReturnValue(
                of({
                    folders: results,
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                })
            );

            store.search('fx');
            tick(500);

            expect(service.searchFolders).toHaveBeenCalledWith(
                {
                    siteId: site.data.id,
                    path: '/',
                    recursive: true,
                    name: 'fx',
                    page: 1,
                    per_page: FOLDER_PAGE_LIMIT
                },
                site.data.hostname
            );
            expect(store.searchResults()).toEqual(results);
        }));

        it('should search folders recursively within the selected site when the term is long enough', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockClear();

            const results: TreeNodeItem[] = [
                {
                    key: 'match-1',
                    label: 'demo.dotcms.com/match/',
                    data: {
                        id: 'match-1',
                        hostname: 'demo.dotcms.com',
                        path: '/match/',
                        type: 'folder'
                    }
                }
            ];
            service.searchFolders.mockReturnValue(
                of({
                    folders: results,
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                })
            );

            store.search('match');
            tick(500);

            expect(service.searchFolders).toHaveBeenCalledWith(
                {
                    siteId: site.data.id,
                    path: '/',
                    recursive: true,
                    name: 'match',
                    page: 1,
                    per_page: FOLDER_PAGE_LIMIT
                },
                site.data.hostname
            );
            expect(store.searchResults()).toEqual(results);
        }));

        it('should clear results when the term is cleared', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockReturnValue(
                of({ folders: [{ key: 'x' } as TreeNodeItem], pagination: mockPagination })
            );

            store.search('match');
            tick(500);
            expect(store.searchResults()).not.toBe(null);

            store.search('');
            tick(500);
            expect(store.searchResults()).toBe(null);
        }));

        it('should fetch again when the same term is retyped after closing and reopening the overlay', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            const results: TreeNodeItem[] = [
                {
                    key: 'match-1',
                    label: 'demo.dotcms.com/match/',
                    data: {
                        id: 'match-1',
                        hostname: 'demo.dotcms.com',
                        path: '/match/',
                        type: 'folder'
                    }
                }
            ];

            store.selectSite(site);
            service.searchFolders.mockReturnValue(
                of({
                    folders: results,
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                })
            );
            service.searchFolders.mockClear();

            store.openOverlay();
            store.search('match');
            tick(300);

            expect(service.searchFolders).toHaveBeenCalledTimes(1);
            expect(store.searchResults()).toEqual(results);

            store.closeOverlay();
            store.openOverlay();
            store.search('match');
            tick(300);

            expect(service.searchFolders).toHaveBeenCalledTimes(2);
            expect(store.searchResults()).toEqual(results);
        }));

        it('should surface search errors and delegate to DotHttpErrorManagerService', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockReturnValue(throwError(() => httpError));

            store.search('match');
            tick(500);

            expect(store.searchStatus()).toBe(ComponentStatus.ERROR);
            expect(store.searchLoadFailed()).toBe(true);
            expect(httpErrorManager.handle).toHaveBeenCalledWith(httpError);
        }));

        it('should append a load-more sentinel to searchResults when more pages are available', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockReturnValue(
                of({
                    folders: [{ key: 'match-1' } as TreeNodeItem],
                    pagination: { currentPage: 1, perPage: FOLDER_PAGE_LIMIT, totalEntries: 45 }
                })
            );

            store.search('match');
            tick(500);

            const results = store.searchResults();
            expect(results).toHaveLength(2);
            expect(results[1].type).toBe('load-more');
            expect(store.searchPagination()).toEqual({ page: 1, hasMore: true, loading: false });
        }));

        it('should not append a load-more sentinel when all results fit on one page', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockReturnValue(
                of({
                    folders: [{ key: 'match-1' } as TreeNodeItem],
                    pagination: { currentPage: 1, perPage: FOLDER_PAGE_LIMIT, totalEntries: 1 }
                })
            );

            store.search('match');
            tick(500);

            expect(store.searchResults()).toHaveLength(1);
            expect(store.searchPagination()).toEqual({ page: 1, hasMore: false, loading: false });
        }));
    });

    describe('Method: loadMoreSearchResults', () => {
        beforeEach(() => {
            store.openOverlay();
        });

        it('should append the next page of search results and drop the sentinel once exhausted', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockReturnValue(
                of({
                    folders: [{ key: 'match-1' } as TreeNodeItem],
                    pagination: { currentPage: 1, perPage: FOLDER_PAGE_LIMIT, totalEntries: 45 }
                })
            );

            store.search('match');
            tick(500);
            expect(store.searchResults()).toHaveLength(2);

            service.searchFolders.mockClear();
            service.searchFolders.mockReturnValue(
                of({
                    folders: [{ key: 'match-2' } as TreeNodeItem],
                    pagination: { currentPage: 2, perPage: FOLDER_PAGE_LIMIT, totalEntries: 45 }
                })
            );

            store.loadMoreSearchResults();
            tick();

            expect(service.searchFolders).toHaveBeenCalledWith(
                {
                    siteId: site.data.id,
                    path: '/',
                    recursive: true,
                    name: 'match',
                    page: 2,
                    per_page: FOLDER_PAGE_LIMIT
                },
                site.data.hostname
            );

            const results = store.searchResults();
            expect(results).toEqual([{ key: 'match-1' }, { key: 'match-2' }]);
            expect(store.searchPagination()).toEqual({ page: 2, hasMore: false, loading: false });
        }));

        it('should ignore a stale response if the search term changed while loading more', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockReturnValue(
                of({
                    folders: [{ key: 'match-1' } as TreeNodeItem],
                    pagination: { currentPage: 1, perPage: FOLDER_PAGE_LIMIT, totalEntries: 45 }
                })
            );

            store.search('match');
            tick(500);

            // loadMoreSearchResults subscribes to this Subject; it's never resolved before
            // the term changes, simulating an in-flight "load more" request for 'match'.
            const staleResponse = new Subject<{
                folders: TreeNodeItem[];
                pagination: DotPagination;
            }>();
            service.searchFolders.mockReturnValue(staleResponse);
            store.loadMoreSearchResults();

            // A new search term resolves against its own (immediate) observable, unrelated
            // to the still-pending `staleResponse`.
            service.searchFolders.mockReturnValue(
                of({
                    folders: [{ key: 'other-1' } as TreeNodeItem],
                    pagination: { currentPage: 1, perPage: FOLDER_PAGE_LIMIT, totalEntries: 1 }
                })
            );
            store.search('other');
            tick(500);

            // The stale "load more" request for 'match' finally resolves after the term
            // changed — it must not clobber the current 'other' results.
            staleResponse.next({
                folders: [{ key: 'stale' } as TreeNodeItem],
                pagination: { currentPage: 2, perPage: FOLDER_PAGE_LIMIT, totalEntries: 45 }
            });

            expect(store.searchResults()).toEqual([{ key: 'other-1' }]);
        }));

        it('should be a no-op when there is no active search term', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockClear();

            store.loadMoreSearchResults();

            expect(service.searchFolders).not.toHaveBeenCalled();
        });
    });

    describe('Error handling', () => {
        it('should surface loadSites HTTP errors and delegate to DotHttpErrorManagerService', () => {
            service.getSitesPage.mockReturnValue(throwError(() => httpError));

            store.loadSites({ path: null, isRequired: false });

            expect(store.sitesStatus()).toBe(ComponentStatus.ERROR);
            expect(store.sitesLoadFailed()).toBe(true);
            expect(httpErrorManager.handle).toHaveBeenCalledWith(httpError);
        });

        it('should surface a buildTreeByPaths failure instead of leaving loadSites stuck in LOADING forever', fakeAsync(() => {
            mockSitesPage(service, TREE_SELECT_SITES_MOCK);
            service.buildTreeByPaths.mockReturnValue(throwError(() => httpError));

            store.loadSites({ path: 'demo.dotcms.com/level1', isRequired: false });
            tick();

            expect(store.selectedSite()).toBe(null);
            expect(store.sitesStatus()).toBe(ComponentStatus.ERROR);
            expect(httpErrorManager.handle).toHaveBeenCalledWith(httpError);

            // A subsequent loadSites call must still work — the rxMethod subscription
            // shouldn't have been killed by the earlier unhandled error.
            service.buildTreeByPaths.mockReturnValue(
                of({
                    node: TREE_SELECT_MOCK[0].children[0],
                    tree: {
                        path: '/',
                        folders: [],
                        parent: {
                            hostName: 'demo.dotcms.com',
                            id: 'demo-id',
                            path: '/',
                            addChildrenAllowed: true
                        }
                    }
                })
            );

            store.loadSites({ path: 'demo.dotcms.com/level1', isRequired: false });
            tick();

            expect(store.selectedSite()?.label).toBe('demo.dotcms.com');
            expect(store.sitesStatus()).toBe(ComponentStatus.LOADED);
        }));

        it('should surface loadFolders HTTP errors and delegate to DotHttpErrorManagerService', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            service.searchFolders.mockReturnValue(throwError(() => httpError));

            store.selectSite(site);

            expect(store.foldersStatus()).toBe(ComponentStatus.ERROR);
            expect(store.foldersLoadFailed()).toBe(true);
            expect(httpErrorManager.handle).toHaveBeenCalledWith(httpError);
        });

        it('should keep the folder tree visible when a nested expand fails', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockClear();

            const node: TreeNodeItem = {
                key: 'folder-1',
                label: 'demo.dotcms.com/folder1/',
                data: {
                    id: 'folder-1',
                    hostname: 'demo.dotcms.com',
                    path: '/folder1/',
                    type: 'folder'
                },
                leaf: false
            };

            service.searchFolders.mockReturnValue(throwError(() => httpError));

            store.expandNode({
                originalEvent: new Event('click'),
                node
            });

            expect(store.foldersStatus()).toBe(ComponentStatus.LOADED);
            expect(store.foldersLoadFailed()).toBe(false);
            expect(store.nodePagination()['folder-1'].loading).toBe(false);
            expect(httpErrorManager.handle).toHaveBeenCalledWith(httpError);
        });
    });

    describe('Staged commit: setPendingNode / commit / openOverlay / closeOverlay', () => {
        it('should not update confirmedNode until commit is called', () => {
            const node = TREE_SELECT_MOCK[0].children[0];
            store.setPendingNode(node);

            expect(store.pendingNode()).toBe(node);
            expect(store.confirmedNode()).toBe(null);

            store.commit();
            expect(store.confirmedNode()).toBe(node);
        });

        it('should discard the pending selection when the overlay is closed without committing', () => {
            const confirmed = TREE_SELECT_MOCK[0];
            const pending = TREE_SELECT_MOCK[0].children[0];

            store.setPendingNode(confirmed);
            store.commit();

            store.openOverlay();
            store.setPendingNode(pending);
            expect(store.pendingNode()).toBe(pending);

            store.closeOverlay();

            expect(store.overlayOpen()).toBe(false);
            expect(store.pendingNode()).toBe(confirmed);
            expect(store.confirmedNode()).toBe(confirmed);
            expect(store.siteSearchTerm()).toBe('');
        });

        it('should reset search state when the overlay is closed so reopening shows the browse tree', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            const browseFolders: TreeNodeItem[] = [
                {
                    key: 'folder-1',
                    label: 'demo.dotcms.com/folder1/',
                    data: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/folder1/',
                        type: 'folder'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false
                }
            ];
            const searchResultFolders: TreeNodeItem[] = [
                {
                    key: 'match-1',
                    label: 'demo.dotcms.com/match/',
                    data: {
                        id: 'match-1',
                        hostname: 'demo.dotcms.com',
                        path: '/match/',
                        type: 'folder'
                    }
                }
            ];

            service.searchFolders.mockReturnValue(
                of({
                    folders: browseFolders,
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                })
            );
            store.selectSite(site);

            service.searchFolders.mockReturnValue(
                of({
                    folders: searchResultFolders,
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                })
            );
            store.openOverlay();
            store.search('match');
            tick(500);

            expect(store.searchTerm()).toBe('match');
            expect(store.isSearching()).toBe(true);
            expect(store.searchResults()).toEqual(searchResultFolders);

            store.closeOverlay();

            expect(store.searchTerm()).toBe('');
            expect(store.searchResults()).toBe(null);
            expect(store.isSearching()).toBe(false);
            expect(store.displayedFolders()).toEqual(browseFolders);
        }));
    });

    describe('filteredSites', () => {
        beforeEach(() => {
            mockSitesPage(service, TREE_SELECT_SITES_MOCK);
            store.loadSites({ path: null, isRequired: false });
        });

        it('should mirror the loaded sites list', () => {
            expect(store.filteredSites()).toEqual(store.sites());
        });

        it('should reset siteSearchTerm when selecting a different site', fakeAsync(() => {
            store.openOverlay();
            store.filterSites('demo');
            tick(300);
            store.selectSite(TREE_SELECT_SITES_MOCK[1]);

            expect(store.siteSearchTerm()).toBe('');
        }));
    });

    describe('showSitesSearch', () => {
        const createSite = (label: string): TreeNodeItem => ({
            key: label,
            label,
            data: { id: label, hostname: label, path: '', type: 'site' },
            expandedIcon: 'pi pi-folder-open',
            collapsedIcon: 'pi pi-folder'
        });

        it('should be false when total site count equals SITE_SEARCH_THRESHOLD', fakeAsync(() => {
            const sites = Array.from({ length: SITE_SEARCH_THRESHOLD }, (_, i) =>
                createSite(`site-${i + 1}`)
            );
            mockSitesPage(service, sites, SITE_SEARCH_THRESHOLD);
            store.loadSites({ path: null, isRequired: false });
            tick();

            expect(store.showSitesSearch()).toBe(false);
        }));

        it('should be true when total site count exceeds SITE_SEARCH_THRESHOLD', fakeAsync(() => {
            const sites = Array.from({ length: SITE_SEARCH_THRESHOLD + 1 }, (_, i) =>
                createSite(`site-${i + 1}`)
            );
            mockSitesPage(service, sites, SITE_SEARCH_THRESHOLD + 1);
            store.loadSites({ path: null, isRequired: false });
            tick();

            expect(store.showSitesSearch()).toBe(true);
        }));

        it('should stay true while a site search term is active even with zero results', fakeAsync(() => {
            const sites = Array.from({ length: SITE_SEARCH_THRESHOLD + 1 }, (_, i) =>
                createSite(`site-${i + 1}`)
            );
            mockSitesPage(service, sites, SITE_SEARCH_THRESHOLD + 1);
            store.loadSites({ path: null, isRequired: false });
            tick();

            mockSitesPage(service, [], 0);
            store.openOverlay();
            store.filterSites('no-match');
            tick(300);

            expect(store.showSitesSearch()).toBe(true);
            expect(store.sites()).toEqual([]);
        }));
    });

    describe('showTriggerLoading', () => {
        it('should be false while filtering sites when a confirmed selection already exists', fakeAsync(() => {
            mockSitesPage(service, TREE_SELECT_SITES_MOCK);
            store.loadSites({ path: 'demo.dotcms.com', isRequired: false });
            tick();

            mockSitesPage(service, [TREE_SELECT_SITES_MOCK[0]], 1);
            store.openOverlay();
            store.filterSites('demo');
            tick();

            expect(store.showTriggerLoading()).toBe(false);
            expect(store.displayPath()).toBe('demo.dotcms.com');
        }));
    });

    describe('Loading states', () => {
        const site = TREE_SELECT_SITES_MOCK[0];

        it('should keep sitesLoading true until initial path resolution completes', fakeAsync(() => {
            const sites$ = new Subject<{ sites: TreeNodeItem[]; pagination: DotPagination }>();
            service.getSitesPage.mockReturnValue(sites$.asObservable());

            store.loadSites({ path: 'demo.dotcms.com', isRequired: false });

            expect(store.sitesLoading()).toBe(true);
            expect(store.sitesStatus()).toBe(ComponentStatus.LOADING);
            expect(store.confirmedNode()).toBeNull();

            sites$.next(createSitesPageResponse(TREE_SELECT_SITES_MOCK));
            sites$.complete();
            tick();

            expect(store.sitesLoading()).toBe(false);
            expect(store.sitesStatus()).toBe(ComponentStatus.LOADED);
            expect(store.confirmedNode()?.key).toBe(site.key);
        }));

        it('should keep sitesStatus LOADING after sites fetch until buildTreeByPaths resolves', fakeAsync(() => {
            const tree$ = new Subject<{
                node: TreeNodeItem;
                tree: { path: string; folders: TreeNodeItem[] };
                pagination?: Record<string, { page: number; hasMore: boolean }>;
            }>();
            const [mockSite] = TREE_SELECT_MOCK;
            const targetNode = mockSite.children[0];

            mockSitesPage(service, TREE_SELECT_MOCK);
            service.buildTreeByPaths.mockReturnValue(tree$.asObservable());

            store.loadSites({ path: 'demo.dotcms.com/level1', isRequired: false });
            tick();

            expect(store.sites()).toHaveLength(TREE_SELECT_MOCK.length);
            expect(store.sitesStatus()).toBe(ComponentStatus.LOADING);
            expect(store.sitesLoading()).toBe(true);
            expect(store.confirmedNode()).toBeNull();

            tree$.next({
                node: targetNode,
                tree: {
                    path: '/',
                    folders: mockSite.children
                }
            });
            tree$.complete();
            tick();

            expect(store.sitesStatus()).toBe(ComponentStatus.LOADED);
            expect(store.sitesLoading()).toBe(false);
            expect(store.confirmedNode()?.key).toBe(targetNode.key);
        }));

        it('should expose showFoldersPanelLoading while folders load with an empty list', () => {
            const folders$ = new Subject<{
                folders: TreeNodeItem[];
                pagination: DotPagination;
            }>();
            service.searchFolders.mockReturnValue(folders$.asObservable());

            store.selectSite(site);

            expect(store.showFoldersPanelLoading()).toBe(true);
            expect(store.foldersLoading()).toBe(true);
        });

        it('should expose searchLoading while a backend search is in flight', fakeAsync(() => {
            const search$ = new Subject<{
                folders: TreeNodeItem[];
                pagination: DotPagination;
            }>();
            service.searchFolders.mockReturnValue(search$.asObservable());

            store.selectSite(site);
            store.openOverlay();
            store.search('match');
            tick(500);

            expect(store.searchLoading()).toBe(true);
            expect(store.showFoldersPanelLoading()).toBe(true);
        }));
    });

    describe('showFolderSearch', () => {
        const site = TREE_SELECT_SITES_MOCK[0];
        const mockFolders: TreeNodeItem[] = [
            {
                key: 'folder-1',
                label: 'demo.dotcms.com/folder1/',
                data: {
                    id: 'folder-1',
                    hostname: 'demo.dotcms.com',
                    path: '/folder1/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                leaf: false
            }
        ];

        it('should be false while the folders panel loading state is shown', () => {
            const folders$ = new Subject<{
                folders: TreeNodeItem[];
                pagination: DotPagination;
            }>();
            service.searchFolders.mockReturnValue(folders$.asObservable());

            store.selectSite(site);

            expect(store.foldersStatus()).toBe(ComponentStatus.LOADING);
            expect(store.showFoldersPanelLoading()).toBe(true);
            expect(store.showFolderSearch()).toBe(false);
        });

        it('should be false when folders loaded and the site has no folders', () => {
            service.searchFolders.mockReturnValue(of({ folders: [], pagination: mockPagination }));

            store.selectSite(site);

            expect(store.foldersStatus()).toBe(ComponentStatus.LOADED);
            expect(store.folders()).toEqual([]);
            expect(store.showFolderSearch()).toBe(false);
        });

        it('should be true when folders loaded and the site has folders', () => {
            service.searchFolders.mockReturnValue(
                of({
                    folders: mockFolders,
                    pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                })
            );

            store.selectSite(site);

            expect(store.foldersStatus()).toBe(ComponentStatus.LOADED);
            expect(store.showFolderSearch()).toBe(true);
        });

        it('should be true when a search term is active even if the site has no folders', () => {
            service.searchFolders.mockReturnValue(of({ folders: [], pagination: mockPagination }));

            store.selectSite(site);
            store.openOverlay();
            store.search('ab');

            expect(store.showFolderSearch()).toBe(true);
        });
    });

    describe('filterSites', () => {
        beforeEach(() => {
            mockSitesPage(service, TREE_SELECT_SITES_MOCK);
            store.loadSites({ path: null, isRequired: false });
            store.openOverlay();
        });

        it('should keep existing sites during debounce before the search request', fakeAsync(() => {
            const initialSites = store.sites();

            store.filterSites('demo');
            tick(100);

            expect(store.sites()).toEqual(initialSites);
            expect(store.siteSearchTerm()).toBe('');
        }));

        it('should debounce and query the sites API with the search term', fakeAsync(() => {
            mockSitesPage(service, [TREE_SELECT_SITES_MOCK[0]], 1);
            service.getSitesPage.mockClear();

            store.filterSites('demo');
            expect(store.siteSearchTerm()).toBe('');

            tick(300);

            expect(store.siteSearchTerm()).toBe('demo');
            expect(service.getSitesPage).toHaveBeenCalledWith({
                filter: 'demo',
                perPage: SITE_PAGE_LIMIT,
                page: 1
            });
            expect(store.filteredSites()).toEqual([TREE_SELECT_SITES_MOCK[0]]);
        }));

        it('should reload all sites when the search term is cleared', fakeAsync(() => {
            store.filterSites('');
            tick(300);

            expect(service.getSitesPage).toHaveBeenLastCalledWith({
                filter: '*',
                perPage: SITE_PAGE_LIMIT,
                page: 1
            });
        }));

        it('should fetch again when the same site search term is retyped after closing and reopening the overlay', fakeAsync(() => {
            mockSitesPage(service, [TREE_SELECT_SITES_MOCK[0]], 1);
            service.getSitesPage.mockClear();

            store.openOverlay();
            store.filterSites('demo');
            tick(300);

            expect(service.getSitesPage).toHaveBeenCalledTimes(1);
            expect(store.siteSearchTerm()).toBe('demo');
            expect(store.filteredSites()).toEqual([TREE_SELECT_SITES_MOCK[0]]);

            store.closeOverlay();
            store.openOverlay();
            store.filterSites('demo');
            tick(300);

            expect(service.getSitesPage).toHaveBeenCalledTimes(2);
            expect(store.siteSearchTerm()).toBe('demo');
            expect(store.filteredSites()).toEqual([TREE_SELECT_SITES_MOCK[0]]);
        }));
    });

    describe('sites pagination guards', () => {
        it('should set hasMore true when the first page returns a full page', fakeAsync(() => {
            const pageSites = createSiteList(SITE_PAGE_LIMIT);

            mockSitesPage(service, pageSites);
            store.loadSites({ path: null, isRequired: false });
            tick();

            expect(store.sitesPagination().hasMore).toBe(true);
            expect(store.sitesLoadedPages()).toEqual([1]);
        }));

        it('should set hasMore false when the page returns fewer than SITE_PAGE_LIMIT items', fakeAsync(() => {
            mockSitesPage(service, TREE_SELECT_SITES_MOCK);
            store.loadSites({ path: null, isRequired: false });
            tick();

            expect(store.sitesPagination().hasMore).toBe(false);
        }));
    });

    describe('loadMoreSites', () => {
        it('should append the next page and dedupe existing sites', fakeAsync(() => {
            const pageOne = createSiteList(SITE_PAGE_LIMIT);
            const pageTwo = [createSiteList(1, 'page-two')[0], pageOne[0]];

            mockSitesPage(service, pageOne);
            store.loadSites({ path: null, isRequired: false });
            tick();

            service.getSitesPage.mockReturnValue(of(createSitesPageResponse(pageTwo, 80, 2)));

            store.loadMoreSites();
            tick();

            expect(service.getSitesPage).toHaveBeenLastCalledWith({
                filter: '*',
                perPage: SITE_PAGE_LIMIT,
                page: 2
            });
            expect(store.sites()).toHaveLength(SITE_PAGE_LIMIT + 1);
            expect(store.sitesPagination().hasMore).toBe(false);
            expect(store.sitesLoadedPages()).toEqual([1, 2]);
        }));

        it('should not request the same page twice when loadMoreSites is called repeatedly', fakeAsync(() => {
            const pageOne = createSiteList(SITE_PAGE_LIMIT);
            const pageTwo = createSiteList(1, 'page-two');

            mockSitesPage(service, pageOne);
            store.loadSites({ path: null, isRequired: false });
            tick();

            service.getSitesPage.mockReturnValue(of(createSitesPageResponse(pageTwo, 80, 2)));
            service.getSitesPage.mockClear();

            store.loadMoreSites();
            store.loadMoreSites();
            tick();

            expect(service.getSitesPage).toHaveBeenCalledTimes(1);
            expect(service.getSitesPage).toHaveBeenCalledWith({
                filter: '*',
                perPage: SITE_PAGE_LIMIT,
                page: 2
            });
        }));
    });

    describe('pinned site resolution', () => {
        it('should resolve the path site and prepend it without paginating until found', fakeAsync(() => {
            const pinnedSite = TREE_SELECT_SITES_MOCK[0];
            const pageSites = [TREE_SELECT_SITES_MOCK[1]];

            mockResolveSiteByHostname(service, () => pinnedSite);
            mockSitesPage(service, pageSites, 100);

            store.loadSites({ path: pinnedSite.label, isRequired: false });
            tick();

            expect(service.resolveSiteByHostname).toHaveBeenCalledWith(
                pinnedSite.label,
                SITE_PAGE_LIMIT
            );
            expect(store.sites()[0].key).toBe(pinnedSite.key);
            expect(store.sites()).toHaveLength(2);
        }));
    });
});
