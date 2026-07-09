import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';

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
    ROOT_NODE_KEY,
    SYSTEM_HOST_NAME
} from './host-folder-field.store';

import { TREE_SELECT_MOCK, TREE_SELECT_SITES_MOCK } from '../../../utils/mocks';

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
                    getSitesTreePath: jest.fn(() => of(TREE_SELECT_SITES_MOCK)),
                    searchFolders: jest.fn(() => of({ folders: [], pagination: mockPagination }))
                })
            ]
        });

        store = TestBed.inject(HostFolderFiledStore);
        service = TestBed.inject(DotBrowsingService) as SpyObject<DotBrowsingService>;
        httpErrorManager = TestBed.inject(
            DotHttpErrorManagerService
        ) as SpyObject<DotHttpErrorManagerService>;
    });

    describe('Method: loadSites', () => {
        describe('System Host isRequired', () => {
            it('should include System Host when isRequired is false.', () => {
                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
                store.loadSites({ path: null, isRequired: false });
                const hasSystemHost = store.sites().some((item) => item.label === SYSTEM_HOST_NAME);
                expect(hasSystemHost).toBe(true);
            });

            it('should not include System Host when isRequired is true.', () => {
                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
                store.loadSites({ path: null, isRequired: true });
                const hasSystemHost = store.sites().some((item) => item.label === SYSTEM_HOST_NAME);
                expect(hasSystemHost).toBe(false);
            });
        });

        describe('when path is not empty', () => {
            it('should select the node/site if the path is not empty and not required', () => {
                const node = TREE_SELECT_SITES_MOCK[0];
                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
                store.loadSites({ path: node.label, isRequired: false });

                expect(service.getCurrentSiteAsTreeNodeItem).not.toHaveBeenCalled();
                expect(store.selectedSite().key).toBe(node.key);
                expect(store.confirmedNode().key).toBe(node.key);
                expect(store.pendingNode().key).toBe(node.key);
            });

            it('should select the node/site if the path is not empty and is required', () => {
                const node = TREE_SELECT_SITES_MOCK[0];
                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
                store.loadSites({ path: node.label, isRequired: true });

                expect(service.getCurrentSiteAsTreeNodeItem).not.toHaveBeenCalled();
                expect(store.selectedSite().key).toBe(node.key);
            });

            it('should resolve a nested folder path via buildTreeByPaths and set confirmed/pending node', fakeAsync(() => {
                const [site] = TREE_SELECT_MOCK;
                const targetNode = site.children[0];

                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_MOCK));
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

                store.loadSites({ path: 'demo.dotcms.com/level1', isRequired: false });
                tick();

                expect(service.buildTreeByPaths).toHaveBeenCalledWith('demo.dotcms.com/level1');
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
        });

        describe('path normalization', () => {
            it('should normalize a colon-separated persisted path before calling buildTreeByPaths', fakeAsync(() => {
                const [site] = TREE_SELECT_MOCK;
                const targetNode = site.children[0];

                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_MOCK));
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

                expect(service.buildTreeByPaths).toHaveBeenCalledWith('demo.dotcms.com/level1/');
            }));

            it('should normalize a leading double-slash persisted path before calling buildTreeByPaths', fakeAsync(() => {
                const [site] = TREE_SELECT_MOCK;
                const targetNode = site.children[0];

                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_MOCK));
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

                expect(service.buildTreeByPaths).toHaveBeenCalledWith('demo.dotcms.com/level1/');
            }));
        });

        describe('ancestor expansion', () => {
            it('should mark ancestor folders as expanded so the tree opens to the resolved node, even when buildTreeByPaths does not flag them', fakeAsync(() => {
                const [site] = TREE_SELECT_MOCK;
                const level1 = site.children[0];
                const targetNode = { ...level1.children[0], expanded: false };
                const unexpandedLevel1 = { ...level1, expanded: false, children: [targetNode] };

                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_MOCK));
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
                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
                store.loadSites({ path: null, isRequired: false });

                expect(service.getCurrentSiteAsTreeNodeItem).not.toHaveBeenCalled();
                expect(store.selectedSite().label).toBe(SYSTEM_HOST_NAME);
            });

            it('should select current site if required', fakeAsync(() => {
                const hostNode = TREE_SELECT_SITES_MOCK[1];
                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
                service.getCurrentSiteAsTreeNodeItem.mockReturnValue(of(hostNode));

                store.loadSites({ path: null, isRequired: true });
                tick();

                expect(service.getCurrentSiteAsTreeNodeItem).toHaveBeenCalled();
                expect(store.selectedSite().label).toBe(hostNode.label);
            }));
        });

        describe('when the resolved site cannot be found', () => {
            it('should surface an error instead of leaving the store silently uninitialized when there are no sites available', () => {
                service.getSitesTreePath.mockReturnValue(of([]));

                store.loadSites({ path: null, isRequired: false });

                expect(store.selectedSite()).toBe(null);
                expect(store.confirmedNode()).toBe(null);
                expect(store.sitesStatus()).toBe(ComponentStatus.ERROR);
            });

            it('should surface an error instead of leaving the store silently uninitialized when the persisted path resolves to a hostname not present in the sites list (e.g. an archived/inaccessible site)', fakeAsync(() => {
                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
                service.buildTreeByPaths.mockReturnValue(
                    of({
                        node: {
                            key: 'unknown-node',
                            label: 'unknown-site.dotcms.com/level1/',
                            data: {
                                id: 'unknown-node',
                                hostname: 'unknown-site.dotcms.com',
                                path: '/level1/',
                                type: 'folder'
                            }
                        },
                        tree: {
                            path: '/',
                            folders: [],
                            parent: {
                                hostName: 'unknown-site.dotcms.com',
                                id: 'unknown-site-id',
                                path: '/',
                                addChildrenAllowed: true
                            }
                        }
                    })
                );

                store.loadSites({ path: 'unknown-site.dotcms.com/level1', isRequired: false });
                tick();

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

        it('should format a nested folder path correctly', () => {
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

        it('should be a no-op when re-selecting the already-selected site, preserving folders and pendingNode', () => {
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
            service.searchFolders.mockClear();

            const pendingBeforeReclick = store.pendingNode();
            const foldersBeforeReclick = store.folders();

            store.selectSite(site);

            expect(service.searchFolders).not.toHaveBeenCalled();
            expect(store.pendingNode()).toBe(pendingBeforeReclick);
            expect(store.folders()).toBe(foldersBeforeReclick);
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
        it('should ignore terms shorter than the minimum length', fakeAsync(() => {
            const site = TREE_SELECT_SITES_MOCK[0];
            store.selectSite(site);
            service.searchFolders.mockClear();

            store.search('ab');
            tick(500);

            expect(service.searchFolders).not.toHaveBeenCalled();
            expect(store.searchResults()).toBe(null);
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
                { siteId: site.data.id, path: '/', recursive: true, name: 'match' },
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
    });

    describe('Error handling', () => {
        it('should surface loadSites HTTP errors and delegate to DotHttpErrorManagerService', () => {
            service.getSitesTreePath.mockReturnValue(throwError(() => httpError));

            store.loadSites({ path: null, isRequired: false });

            expect(store.sitesStatus()).toBe(ComponentStatus.ERROR);
            expect(store.sitesLoadFailed()).toBe(true);
            expect(httpErrorManager.handle).toHaveBeenCalledWith(httpError);
        });

        it('should surface loadFolders HTTP errors and delegate to DotHttpErrorManagerService', () => {
            const site = TREE_SELECT_SITES_MOCK[0];
            service.searchFolders.mockReturnValue(throwError(() => httpError));

            store.selectSite(site);

            expect(store.foldersStatus()).toBe(ComponentStatus.ERROR);
            expect(store.foldersLoadFailed()).toBe(true);
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
    });

    describe('filteredSites / setSiteSearchTerm', () => {
        beforeEach(() => {
            service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            store.loadSites({ path: null, isRequired: false });
        });

        it('should return all sites when siteSearchTerm is empty', () => {
            expect(store.filteredSites()).toEqual(store.sites());
        });

        it('should filter sites case-insensitively by label', () => {
            store.setSiteSearchTerm('DEMO');

            expect(store.filteredSites()).toEqual([TREE_SELECT_SITES_MOCK[0]]);
        });

        it('should return empty when no site matches the search term', () => {
            store.setSiteSearchTerm('no-match');

            expect(store.filteredSites()).toEqual([]);
        });

        it('should reset siteSearchTerm when selecting a different site', () => {
            store.setSiteSearchTerm('demo');
            store.selectSite(TREE_SELECT_SITES_MOCK[1]);

            expect(store.siteSearchTerm()).toBe('');
        });
    });
});
