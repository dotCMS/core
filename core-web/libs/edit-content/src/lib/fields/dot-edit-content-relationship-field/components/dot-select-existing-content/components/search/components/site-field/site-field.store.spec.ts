import { createFakeEvent } from '@ngneat/spectator';
import { mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { delay } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { PEER_PAGE_LIMIT, SiteFieldStore } from './site-field.store';

import {
    TreeNodeItem,
    TreeNodeSelectItem
} from '../../../../../../../../models/dot-edit-content-host-folder-field.interface';
import { DotEditContentService } from '../../../../../../../../services/dot-edit-content.service';

describe('SiteFieldStore', () => {
    let store: InstanceType<typeof SiteFieldStore>;
    let dotEditContentService: SpyObject<DotEditContentService>;

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
                mockProvider(DotEditContentService, {
                    getSitesTreePath: jest.fn().mockReturnValue(of(mockSites)),
                    getFoldersTreeNode: jest.fn().mockReturnValue(of(mockFolders))
                })
            ]
        });

        store = TestBed.inject(SiteFieldStore);
        dotEditContentService = TestBed.inject(
            DotEditContentService
        ) as SpyObject<DotEditContentService>;
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
        it('should indicate loading state correctly', fakeAsync(() => {
            const mockObservable = of(mockSites).pipe(delay(100));
            dotEditContentService.getSitesTreePath.mockReturnValue(mockObservable);

            store.loadSites();
            expect(store.isLoading()).toBeTruthy();
            expect(store.status()).toBe(ComponentStatus.LOADING);

            tick(100);
            expect(store.isLoading()).toBeFalsy();
            expect(store.status()).toBe(ComponentStatus.LOADED);
        }));

        it('should return correct value for valueToSave when node is selected (type: folder)', () => {
            const mockNode: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: {
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
                }
            };
            store.chooseNode(mockNode);
            expect(store.valueToSave()).toBe('folder:123');
        });

        it('should return correct value for valueToSave when node is selected (type: site)', () => {
            const mockNode: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: {
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
                }
            };
            store.chooseNode(mockNode);
            expect(store.valueToSave()).toBe('site:456');
        });

        it('should return null for valueToSave when no node is selected', () => {
            expect(store.valueToSave()).toBeNull();
        });

        it('should return null for valueToSave when node data is missing', () => {
            const mockNode: TreeNodeSelectItem = {
                originalEvent: createFakeEvent('click'),
                node: {
                    label: 'Invalid Node',
                    data: null,
                    icon: 'pi pi-folder',
                    leaf: true,
                    children: []
                }
            };
            store.chooseNode(mockNode);
            expect(store.valueToSave()).toBeNull();
        });
    });

    describe('loadSites', () => {
        it('should load sites successfully', () => {
            dotEditContentService.getSitesTreePath.mockReturnValue(of(mockSites));

            store.loadSites();

            expect(dotEditContentService.getSitesTreePath).toHaveBeenCalledWith({
                perPage: PEER_PAGE_LIMIT,
                filter: '*',
                page: 1
            });
            expect(store.tree()).toEqual(mockSites);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.error()).toBeNull();
        });

        it('should handle error when loading sites fails', () => {
            dotEditContentService.getSitesTreePath.mockReturnValue(
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
            dotEditContentService.getFoldersTreeNode.mockReturnValue(of(mockFolders));

            const mockEvent = {
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

            expect(dotEditContentService.getFoldersTreeNode).toHaveBeenCalledWith(
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
            dotEditContentService.getFoldersTreeNode.mockReturnValue(
                throwError(() => new Error('Failed to load folders'))
            );

            const mockEvent = {
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
            const mockEvent = {
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
            const mockEvent = {
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
            const mockEvent = {
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
