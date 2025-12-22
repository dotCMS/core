import { createFakeEvent } from '@ngneat/spectator';
import { mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { unprotected } from '@ngrx/signals/testing';
import { of, throwError } from 'rxjs';

import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { delay } from 'rxjs/operators';

import { DotBrowsingService } from '@dotcms/data-access';
import { ComponentStatus, TreeNodeItem, TreeNodeSelectItem } from '@dotcms/dotcms-models';

import { PEER_PAGE_LIMIT, SiteFieldStore } from './site-field.store';

describe('SiteFieldStore', () => {
    let store: InstanceType<typeof SiteFieldStore>;
    let dotBrowsingService: SpyObject<DotBrowsingService>;

    const mockSites: TreeNodeItem[] = [
        {
            label: 'demo.dotcms.com',
            data: {
                id: '123',
                hostname: 'demo.dotcms.com',
                path: '',
                type: 'site'
            },
            icon: 'pi pi-globe',
            leaf: false,
            children: []
        }
    ];

    const mockFolders = {
        parent: {
            id: 'parent-id',
            hostName: 'demo.dotcms.com',
            path: '/parent',
            addChildrenAllowed: true
        },
        folders: [
            {
                label: 'folder1',
                data: {
                    id: 'folder1',
                    hostname: 'demo.dotcms.com',
                    path: 'folder1',
                    type: 'folder' as const
                },
                icon: 'pi pi-folder',
                leaf: true,
                children: []
            }
        ]
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                SiteFieldStore,
                mockProvider(DotBrowsingService, {
                    getSitesTreePath: jest.fn().mockReturnValue(of(mockSites)),
                    getFoldersTreeNode: jest.fn().mockReturnValue(of(mockFolders))
                })
            ]
        });

        store = TestBed.inject(SiteFieldStore);
        dotBrowsingService = TestBed.inject(DotBrowsingService) as SpyObject<DotBrowsingService>;
    });

    describe('Initial State', () => {
        it('should have initial state', () => {
            expect(store.nodeSelected()).toBeNull();
            expect(store.nodeExpanded()).toBeNull();
            expect(store.tree()).toEqual([]);
            expect(store.status()).toBe(ComponentStatus.INIT);
            expect(store.error()).toBeNull();
        });
    });

    describe('Computed Properties', () => {
        it('should compute isLoading as true when status is LOADING', () => {
            patchState(unprotected(store), { status: ComponentStatus.LOADING });
            expect(store.isLoading()).toBeTruthy();
        });

        it('should compute isLoading as false when status is not LOADING', () => {
            patchState(unprotected(store), { status: ComponentStatus.LOADED });
            expect(store.isLoading()).toBeFalsy();
        });

        it('should compute isLoading as false when status is ERROR', () => {
            patchState(unprotected(store), { status: ComponentStatus.ERROR });
            expect(store.isLoading()).toBeFalsy();
        });

        it('should compute isLoading as false when status is INIT', () => {
            patchState(unprotected(store), { status: ComponentStatus.INIT });
            expect(store.isLoading()).toBeFalsy();
        });

        it('should indicate loading state correctly', fakeAsync(() => {
            const mockObservable = of(mockSites).pipe(delay(100));
            dotBrowsingService.getSitesTreePath.mockReturnValue(mockObservable);

            store.loadSites();
            expect(store.isLoading()).toBeTruthy();
            expect(store.status()).toBe(ComponentStatus.LOADING);

            tick(100);
            expect(store.isLoading()).toBeFalsy();
            expect(store.status()).toBe(ComponentStatus.LOADED);
        }));

        it('should return correct value for valueToSave when node is selected (type: folder)', () => {
            const mockNode: TreeNodeItem = {
                label: 'Test Node',
                data: {
                    id: '123',
                    hostname: 'test.com',
                    path: 'test',
                    type: 'folder'
                },
                icon: 'pi pi-folder',
                leaf: true,
                children: []
            };
            patchState(unprotected(store), { nodeSelected: mockNode });
            expect(store.valueToSave()).toBe('folder:123');
        });

        it('should return correct value for valueToSave when node is selected (type: site)', () => {
            const mockNode: TreeNodeItem = {
                label: 'Test Node',
                data: {
                    id: '456',
                    hostname: 'test.com',
                    path: '',
                    type: 'site'
                },
                icon: 'pi pi-globe',
                leaf: true,
                children: []
            };
            patchState(unprotected(store), { nodeSelected: mockNode });
            expect(store.valueToSave()).toBe('site:456');
        });

        it('should return null for valueToSave when no node is selected', () => {
            patchState(unprotected(store), { nodeSelected: null });
            expect(store.valueToSave()).toBeNull();
        });

        it('should return null for valueToSave when node data is missing', () => {
            const mockNode: TreeNodeItem = {
                label: 'Invalid Node',
                data: null,
                icon: 'pi pi-folder',
                leaf: true,
                children: []
            };
            patchState(unprotected(store), { nodeSelected: mockNode });
            expect(store.valueToSave()).toBeNull();
        });

        it('should return null for valueToSave when node data id is missing', () => {
            const mockNode: TreeNodeItem = {
                label: 'Invalid Node',
                data: {
                    id: '',
                    hostname: 'test.com',
                    path: 'test',
                    type: 'folder'
                },
                icon: 'pi pi-folder',
                leaf: true,
                children: []
            };
            patchState(unprotected(store), { nodeSelected: mockNode });
            expect(store.valueToSave()).toBeNull();
        });

        it('should return null for valueToSave when node data type is missing', () => {
            const mockNode: TreeNodeItem = {
                label: 'Invalid Node',
                data: {
                    id: '123',
                    hostname: 'test.com',
                    path: 'test',
                    type: undefined as 'site' | 'folder' | undefined
                },
                icon: 'pi pi-folder',
                leaf: true,
                children: []
            };
            patchState(unprotected(store), { nodeSelected: mockNode });
            expect(store.valueToSave()).toBeNull();
        });
    });

    describe('loadSites', () => {
        it('should load sites successfully', () => {
            dotBrowsingService.getSitesTreePath.mockReturnValue(of(mockSites));

            store.loadSites();

            expect(dotBrowsingService.getSitesTreePath).toHaveBeenCalledWith({
                perPage: PEER_PAGE_LIMIT,
                filter: '*',
                page: 1
            });
            expect(store.tree()).toEqual(mockSites);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.error()).toBeNull();
        });

        it('should handle error when loading sites fails', () => {
            dotBrowsingService.getSitesTreePath.mockReturnValue(
                throwError(() => new Error('Failed to load sites'))
            );

            store.loadSites();

            expect(store.status()).toBe(ComponentStatus.ERROR);
            expect(store.error()).toBe('');
            expect(store.tree()).toEqual([]);
        });
    });

    describe('loadChildren', () => {
        it('should load children nodes successfully', () => {
            dotBrowsingService.getFoldersTreeNode.mockReturnValue(of(mockFolders));

            const mockEvent: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: {
                    label: 'Parent',
                    data: {
                        id: 'parent-id',
                        hostname: 'demo.dotcms.com',
                        path: 'parent',
                        type: 'folder' as const
                    },
                    icon: 'pi pi-folder',
                    leaf: false,
                    children: []
                }
            };

            store.loadChildren(mockEvent);

            expect(dotBrowsingService.getFoldersTreeNode).toHaveBeenCalledWith(
                'demo.dotcms.com/parent'
            );
            expect(store.nodeExpanded()).toEqual({
                ...mockEvent.node,
                leaf: true,
                icon: 'pi pi-folder-open',
                children: mockFolders.folders
            });
        });

        it('should handle error when loading children fails', () => {
            dotBrowsingService.getFoldersTreeNode.mockReturnValue(
                throwError(() => new Error('Failed to load folders'))
            );

            const mockEvent: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: {
                    label: 'Parent',
                    data: {
                        id: 'parent-id',
                        hostname: 'demo.dotcms.com',
                        path: 'parent',
                        type: 'folder' as const
                    },
                    icon: 'pi pi-folder',
                    leaf: false,
                    children: []
                }
            };

            store.loadChildren(mockEvent);

            expect(store.nodeExpanded()).toBeNull();
        });
    });

    describe('chooseNode', () => {
        it('should update selected node', () => {
            const mockEvent: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: {
                    label: 'Selected Node',
                    data: {
                        id: '123',
                        hostname: 'demo.dotcms.com',
                        path: 'selected',
                        type: 'folder' as const
                    },
                    icon: 'pi pi-folder',
                    leaf: true,
                    children: []
                }
            };

            store.chooseNode(mockEvent);
            expect(store.nodeSelected()).toEqual(mockEvent.node);
        });

        it('should not update selected node when data is missing', () => {
            const mockEvent: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: {
                    label: 'Invalid Node',
                    data: null,
                    icon: 'pi pi-folder',
                    leaf: true,
                    children: []
                }
            };

            store.chooseNode(mockEvent);
            expect(store.nodeSelected()).toBeNull();
        });
    });

    describe('clearSelection', () => {
        it('should clear the selected node', () => {
            // First select a node
            const mockEvent: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: {
                    label: 'Selected Node',
                    data: {
                        id: '123',
                        hostname: 'demo.dotcms.com',
                        path: 'selected',
                        type: 'folder' as const
                    },
                    icon: 'pi pi-folder',
                    leaf: true,
                    children: []
                }
            };

            store.chooseNode(mockEvent);
            expect(store.nodeSelected()).toEqual(mockEvent.node);
            expect(store.valueToSave()).toBe('folder:123');

            // Then clear the selection
            store.clearSelection();
            expect(store.nodeSelected()).toBeNull();
            expect(store.valueToSave()).toBeNull();
        });

        it('should handle clearing when no node is selected', () => {
            expect(store.nodeSelected()).toBeNull();

            store.clearSelection();
            expect(store.nodeSelected()).toBeNull();
            expect(store.valueToSave()).toBeNull();
        });
    });
});
