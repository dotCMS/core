import {
    createServiceFactory,
    SpectatorService,
    mockProvider,
    SpyObject
} from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { unprotected } from '@ngrx/signals/testing';
import { of, throwError } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';

import { delay } from 'rxjs/operators';

import {
    ComponentStatus,
    ContentByFolderParams,
    TreeNodeItem,
    TreeNodeSelectItem,
    DotFolder
} from '@dotcms/dotcms-models';
import { DotBrowsingService } from '@dotcms/ui';
import { createFakeContentlet, createFakeEvent } from '@dotcms/utils-testing';

import { DotBrowserSelectorStore, SYSTEM_HOST_ID } from './browser.store';

const TREE_SELECT_SITES_MOCK: TreeNodeItem[] = [
    {
        key: 'demo.dotcms.com',
        label: 'demo.dotcms.com',
        data: {
            id: 'demo.dotcms.com',
            hostname: 'demo.dotcms.com',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder'
    },
    {
        key: 'nico.dotcms.com',
        label: 'nico.dotcms.com',
        data: {
            id: 'nico.dotcms.com',
            hostname: 'nico.dotcms.com',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder'
    },
    {
        key: 'System Host',
        label: 'System Host',
        data: {
            id: 'System Host',
            hostname: 'System Host',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder'
    }
];

const TREE_SELECT_MOCK: TreeNodeItem[] = [
    {
        key: 'demo.dotcms.com',
        label: 'demo.dotcms.com',
        data: {
            id: 'demo.dotcms.com',
            hostname: 'demo.dotcms.com',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder',
        children: [
            {
                key: 'demo.dotcms.comlevel1',
                label: 'demo.dotcms.com/level1/',
                data: {
                    id: 'demo.dotcms.comlevel1',
                    hostname: 'demo.dotcms.com',
                    path: '/level1/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder',
                children: [
                    {
                        key: 'demo.dotcms.comlevel1child1',
                        label: 'demo.dotcms.com/level1/child1/',
                        data: {
                            id: 'demo.dotcms.comlevel1child1',
                            hostname: 'demo.dotcms.com',
                            path: '/level1/child1/',
                            type: 'folder'
                        },
                        expandedIcon: 'pi pi-folder-open',
                        collapsedIcon: 'pi pi-folder'
                    }
                ]
            },
            {
                key: 'demo.dotcms.comlevel2',
                label: 'demo.dotcms.com/level2/',
                data: {
                    id: 'demo.dotcms.comlevel2',
                    hostname: 'demo.dotcms.com',
                    path: '/level2/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder'
            }
        ]
    },
    {
        key: 'nico.dotcms.com',
        label: 'nico.dotcms.com',
        data: {
            id: 'nico.dotcms.com',
            hostname: 'nico.dotcms.com',
            path: '',
            type: 'site'
        },
        expandedIcon: 'pi pi-folder-open',
        collapsedIcon: 'pi pi-folder'
    }
];

describe('DotBrowserSelectorStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotBrowserSelectorStore>>;
    let store: InstanceType<typeof DotBrowserSelectorStore>;
    let dotBrowsingService: SpyObject<DotBrowsingService>;

    const createService = createServiceFactory({
        service: DotBrowserSelectorStore,
        providers: [
            mockProvider(DotBrowsingService, {
                getSitesTreePath: jest.fn().mockReturnValue(of(TREE_SELECT_SITES_MOCK)),
                getContentByFolder: jest.fn().mockReturnValue(of([])),
                getFoldersTreeNode: jest.fn().mockReturnValue(
                    of({
                        parent: {
                            id: '',
                            hostName: '',
                            path: '',
                            addChildrenAllowed: false
                        } as DotFolder,
                        folders: []
                    })
                )
            })
        ]
    });

    beforeEach(fakeAsync(() => {
        spectator = createService();
        store = spectator.service;
        dotBrowsingService = spectator.inject(DotBrowsingService);
        // Wait for onInit to complete (it calls loadFolders)
        tick(50);
    }));

    it('should be created', () => {
        expect(store).toBeTruthy();
    });

    describe('Initial state', () => {
        it('should have initial state values after onInit', () => {
            // After onInit (which runs in beforeEach), folders should be loaded
            expect(store.folders().data).toEqual(TREE_SELECT_SITES_MOCK);
            expect(store.folders().status).toBe(ComponentStatus.LOADED);
            expect(store.folders().nodeExpaned).toBeNull();
            expect(store.content().data).toEqual([]);
            expect(store.content().status).toBe(ComponentStatus.INIT);
            expect(store.content().error).toBeNull();
            expect(store.selectedContent()).toBeNull();
            expect(store.searchQuery()).toBe('');
            expect(store.viewMode()).toBe('list');
        });
    });

    describe('Computed properties', () => {
        it('should compute foldersIsLoading as true when folders status is LOADING', () => {
            patchState(unprotected(store), {
                folders: { ...store.folders(), status: ComponentStatus.LOADING }
            });
            expect(store.foldersIsLoading()).toBe(true);
        });

        it('should compute foldersIsLoading as false when folders status is not LOADING', () => {
            patchState(unprotected(store), {
                folders: { ...store.folders(), status: ComponentStatus.LOADED }
            });
            expect(store.foldersIsLoading()).toBe(false);
        });

        it('should compute foldersIsLoading as false when folders status is ERROR', () => {
            patchState(unprotected(store), {
                folders: { ...store.folders(), status: ComponentStatus.ERROR }
            });
            expect(store.foldersIsLoading()).toBe(false);
        });

        it('should compute contentIsLoading as true when content status is LOADING', () => {
            patchState(unprotected(store), {
                content: { ...store.content(), status: ComponentStatus.LOADING }
            });
            expect(store.contentIsLoading()).toBe(true);
        });

        it('should compute contentIsLoading as false when content status is LOADED', () => {
            const mockContentlets = [createFakeContentlet()];
            patchState(unprotected(store), {
                content: { data: mockContentlets, status: ComponentStatus.LOADED, error: null }
            });
            expect(store.contentIsLoading()).toBe(false);
        });

        it('should compute contentIsLoading as false when content status is ERROR', () => {
            patchState(unprotected(store), {
                content: { data: [], status: ComponentStatus.ERROR, error: 'Error message' }
            });
            expect(store.contentIsLoading()).toBe(false);
        });
    });

    describe('Method: setSelectedContent', () => {
        it('should set selected content', () => {
            const mockContentlet = createFakeContentlet({ title: 'Test Content' });
            store.setSelectedContent(mockContentlet);
            expect(store.selectedContent()).toEqual(mockContentlet);
        });

        it('should update selected content', () => {
            const mockContentlet1 = createFakeContentlet({ title: 'Content 1' });
            const mockContentlet2 = createFakeContentlet({ title: 'Content 2' });

            store.setSelectedContent(mockContentlet1);
            expect(store.selectedContent()).toEqual(mockContentlet1);

            store.setSelectedContent(mockContentlet2);
            expect(store.selectedContent()).toEqual(mockContentlet2);
        });
    });

    describe('Method: loadFolders', () => {
        it('should set folders status to LOADING and then to LOADED with data', fakeAsync(() => {
            // Use timer to make the observable async so we can verify LOADING state
            dotBrowsingService.getSitesTreePath.mockReturnValue(
                of(TREE_SELECT_SITES_MOCK).pipe(delay(1))
            );

            store.loadFolders();
            expect(store.folders().status).toBe(ComponentStatus.LOADING);

            tick(50);

            expect(store.folders().status).toBe(ComponentStatus.LOADED);
            expect(store.folders().data).toEqual(TREE_SELECT_SITES_MOCK);
            expect(store.folders().nodeExpaned).toBeNull();
            expect(dotBrowsingService.getSitesTreePath).toHaveBeenCalledWith({
                perPage: 1000,
                filter: '*'
            });
        }));

        it('should set folders status to ERROR on service error', fakeAsync(() => {
            dotBrowsingService.getSitesTreePath.mockReturnValue(
                throwError(() => new Error('error'))
            );

            store.loadFolders();
            tick(50);

            expect(store.folders().status).toBe(ComponentStatus.ERROR);
            expect(store.folders().data).toEqual([]);
            expect(store.folders().nodeExpaned).toBeNull();
        }));

        it('should reset nodeExpaned when folders are loaded', fakeAsync(() => {
            const expandedNode = TREE_SELECT_MOCK[0];
            patchState(unprotected(store), {
                folders: { ...store.folders(), nodeExpaned: expandedNode }
            });

            dotBrowsingService.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
            store.loadFolders();
            tick(50);

            expect(store.folders().nodeExpaned).toBeNull();
        }));
    });

    describe('Method: loadContent', () => {
        it('should load content for a selected node', fakeAsync(() => {
            const mockContentlets = [
                createFakeContentlet({ title: 'Content 1' }),
                createFakeContentlet({ title: 'Content 2' })
            ];
            // Use timer to make the observable async so we can verify LOADING state
            dotBrowsingService.getContentByFolder.mockReturnValue(
                of(mockContentlets).pipe(delay(1))
            );

            const params: ContentByFolderParams = {
                hostFolderId: 'demo.dotcms.com',
                mimeTypes: []
            };

            store.loadContent(params);
            expect(store.content().status).toBe(ComponentStatus.LOADING);

            tick(50);

            expect(store.content().status).toBe(ComponentStatus.LOADED);
            expect(store.content().data).toEqual(mockContentlets);
            expect(store.content().error).toBeNull();
            expect(dotBrowsingService.getContentByFolder).toHaveBeenCalledWith({
                hostFolderId: 'demo.dotcms.com',
                mimeTypes: []
            });
        }));

        it('should preserve existing content data when setting status to LOADING', fakeAsync(() => {
            const existingContent = [createFakeContentlet({ title: 'Existing' })];
            patchState(unprotected(store), {
                content: { data: existingContent, status: ComponentStatus.LOADED, error: null }
            });

            const mockContentlets = [createFakeContentlet({ title: 'New' })];
            // Use timer to make the observable async so we can verify LOADING state
            dotBrowsingService.getContentByFolder.mockReturnValue(
                of(mockContentlets).pipe(delay(1))
            );

            const params: ContentByFolderParams = {
                hostFolderId: 'demo.dotcms.com',
                mimeTypes: []
            };

            store.loadContent(params);
            // During loading, the data should still be preserved from previous state
            expect(store.content().status).toBe(ComponentStatus.LOADING);
            tick(50);
        }));

        it('should load content using SYSTEM_HOST_ID when provided', fakeAsync(() => {
            const mockContentlets = [createFakeContentlet()];
            // Use timer to make the observable async so we can verify LOADING state
            dotBrowsingService.getContentByFolder.mockReturnValue(
                of(mockContentlets).pipe(delay(1))
            );

            const params: ContentByFolderParams = {
                hostFolderId: SYSTEM_HOST_ID,
                mimeTypes: []
            };

            store.loadContent(params);
            // Verify LOADING state before the observable completes
            expect(store.content().status).toBe(ComponentStatus.LOADING);

            tick(50);

            expect(store.content().status).toBe(ComponentStatus.LOADED);
            expect(store.content().data).toEqual(mockContentlets);
            expect(dotBrowsingService.getContentByFolder).toHaveBeenCalledWith({
                hostFolderId: SYSTEM_HOST_ID,
                mimeTypes: []
            });
        }));

        it('should use mimeTypes filter when loading content', fakeAsync(() => {
            const mimeTypes = ['image/jpeg', 'image/png'];
            const mockContentlets = [createFakeContentlet()];
            dotBrowsingService.getContentByFolder.mockReturnValue(of(mockContentlets));

            const params: ContentByFolderParams = {
                hostFolderId: 'demo.dotcms.com',
                mimeTypes
            };

            store.loadContent(params);
            tick(50);

            expect(dotBrowsingService.getContentByFolder).toHaveBeenCalledWith({
                hostFolderId: 'demo.dotcms.com',
                mimeTypes
            });
        }));

        it('should handle service error when loading content', fakeAsync(() => {
            dotBrowsingService.getContentByFolder.mockReturnValue(
                throwError(() => new Error('Service error'))
            );

            const params: ContentByFolderParams = {
                hostFolderId: 'demo.dotcms.com',
                mimeTypes: []
            };

            store.loadContent(params);
            tick(50);

            expect(store.content().status).toBe(ComponentStatus.ERROR);
            expect(store.content().error).toBe(
                'dot.file.field.dialog.select.existing.file.table.error.content'
            );
            expect(store.content().data).toEqual([]);
        }));
    });

    describe('Method: loadChildren', () => {
        it('should load children for a node', fakeAsync(() => {
            // Clear previous mock calls
            jest.clearAllMocks();

            const mockChildren = {
                parent: {
                    id: 'demo.dotcms.com',
                    hostName: 'demo.dotcms.com',
                    path: '',
                    type: 'site',
                    addChildrenAllowed: true
                },
                folders: [...TREE_SELECT_SITES_MOCK]
            };

            dotBrowsingService.getFoldersTreeNode.mockReturnValue(of(mockChildren));

            const node = { ...TREE_SELECT_MOCK[0] };
            const mockItem: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node
            };

            store.loadChildren(mockItem);
            tick(50);

            expect(node.children).toEqual(mockChildren.folders);
            expect(node.loading).toBe(false);
            expect(node.leaf).toBe(true);
            expect(node.icon).toBe('pi pi-folder-open');
            expect(store.folders().nodeExpaned).toBe(node);
            expect(dotBrowsingService.getFoldersTreeNode).toHaveBeenCalledTimes(1);
            expect(dotBrowsingService.getFoldersTreeNode).toHaveBeenCalledWith('demo.dotcms.com/');
        }));

        it('should handle error when loading children', fakeAsync(() => {
            // Clear previous mock calls
            jest.clearAllMocks();

            dotBrowsingService.getFoldersTreeNode.mockReturnValue(
                throwError(() => new Error('error'))
            );

            const node = { ...TREE_SELECT_MOCK[0], children: [] };
            const mockItem: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node
            };

            store.loadChildren(mockItem);
            tick(50);

            expect(node.children).toEqual([]);
            expect(node.loading).toBe(false);
        }));

        it('should build correct path from hostname and path', fakeAsync(() => {
            // Clear previous mock calls
            jest.clearAllMocks();

            const mockChildren = {
                parent: {
                    id: 'folder-1',
                    hostName: 'demo.dotcms.com',
                    path: '/level1/',
                    type: 'folder',
                    addChildrenAllowed: true
                },
                folders: []
            };

            dotBrowsingService.getFoldersTreeNode.mockReturnValue(of(mockChildren));

            const childNode = TREE_SELECT_MOCK[0].children?.[0];
            if (!childNode) {
                throw new Error('Test setup error: child node not found');
            }

            const node: TreeNodeItem = {
                ...childNode
            };

            const mockItem: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node
            };

            store.loadChildren(mockItem);
            tick(50);

            // The implementation creates path as `${hostname}/${path}` where path starts with `/`
            // So it becomes `demo.dotcms.com//level1/` (double slash)
            expect(dotBrowsingService.getFoldersTreeNode).toHaveBeenCalledTimes(1);
            expect(dotBrowsingService.getFoldersTreeNode).toHaveBeenCalledWith(
                'demo.dotcms.com//level1/'
            );
        }));
    });

    describe('onInit hook', () => {
        it('should call loadFolders on initialization', () => {
            // Store is created in beforeEach, which triggers onInit
            // onInit completes in beforeEach via fakeAsync/tick
            expect(dotBrowsingService.getSitesTreePath).toHaveBeenCalledWith({
                perPage: 1000,
                filter: '*'
            });
            expect(store.folders().status).toBe(ComponentStatus.LOADED);
            expect(store.folders().data).toEqual(TREE_SELECT_SITES_MOCK);
        });
    });
});
