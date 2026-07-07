import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { DotPagination, TreeNodeItem, TreeNodeSelectItem } from '@dotcms/dotcms-models';
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

    const mockPagination: DotPagination = {
        currentPage: 1,
        perPage: FOLDER_PAGE_LIMIT,
        totalEntries: 0
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                HostFolderFiledStore,
                mockProvider(DotBrowsingService, {
                    getSitesTreePath: jest.fn(() => of(TREE_SELECT_SITES_MOCK)),
                    searchFolders: jest.fn(() => of({ folders: [], pagination: mockPagination }))
                })
            ]
        });

        store = TestBed.inject(HostFolderFiledStore);
        service = TestBed.inject(DotBrowsingService) as SpyObject<DotBrowsingService>;
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
        });
    });
});
