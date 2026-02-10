import { createFakeEvent } from '@ngneat/spectator';
import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { TreeNodeItem } from '@dotcms/dotcms-models';
import { DotBrowsingService } from '@dotcms/ui';

import { HostFolderFiledStore, SYSTEM_HOST_NAME } from './host-folder-field.store';

import { TREE_SELECT_MOCK, TREE_SELECT_SITES_MOCK } from '../../../utils/mocks';

describe('HostFolderFiledStore', () => {
    let store: InstanceType<typeof HostFolderFiledStore>;
    let service: SpyObject<DotBrowsingService>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                HostFolderFiledStore,
                mockProvider(DotBrowsingService, {
                    getSitesTreePath: jest.fn(() => of(TREE_SELECT_SITES_MOCK))
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
                const props = {
                    path: null,
                    isRequired: false
                };
                store.loadSites(props);
                const hasSystemHost = store.tree().some((item) => item.label === SYSTEM_HOST_NAME);
                expect(hasSystemHost).toBe(true);
            });

            it('should not include System Host when isRequired is true.', () => {
                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
                const props = {
                    path: null,
                    isRequired: true
                };
                store.loadSites(props);
                const hasSystemHost = store.tree().some((item) => item.label === SYSTEM_HOST_NAME);
                expect(hasSystemHost).toBe(false);
            });
        });

        describe('when path is not empty', () => {
            it('should select the node if the path is not empty and not required', () => {
                const node = TREE_SELECT_SITES_MOCK[0];
                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
                const props = {
                    path: node.label,
                    isRequired: false
                };
                store.loadSites(props);
                expect(service.getCurrentSiteAsTreeNodeItem).not.toHaveBeenCalled();
                expect(store.nodeSelected().key).toBe(node.key);
            });

            it('should select the node if the path is not empty and is required', () => {
                const node = TREE_SELECT_SITES_MOCK[0];
                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
                const props = {
                    path: node.label,
                    isRequired: true
                };
                store.loadSites(props);
                expect(service.getCurrentSiteAsTreeNodeItem).not.toHaveBeenCalled();
                expect(store.nodeSelected().key).toBe(node.key);
            });
        });

        describe('when path is empty', () => {
            it('should select System Host if the path is not empty and not required', () => {
                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
                const props = {
                    path: null,
                    isRequired: false
                };
                store.loadSites(props);
                expect(service.getCurrentSiteAsTreeNodeItem).not.toHaveBeenCalled();
                expect(store.nodeSelected().label).toBe(SYSTEM_HOST_NAME);
            });

            it('should select current site if the path is not empty and is required', () => {
                const hostNode = TREE_SELECT_SITES_MOCK[1];
                service.getSitesTreePath.mockReturnValue(of(TREE_SELECT_SITES_MOCK));
                service.getCurrentSiteAsTreeNodeItem.mockReturnValue(of(hostNode));
                const props = {
                    path: null,
                    isRequired: true
                };
                store.loadSites(props);
                expect(service.getCurrentSiteAsTreeNodeItem).toHaveBeenCalled();
                expect(store.nodeSelected().label).toBe(hostNode.label);
            });
        });
    });

    describe('Method: chooseNode', () => {
        it('should update the form value with the correct format with root path', () => {
            const mockItem = {
                originalEvent: createFakeEvent('input'),
                node: { ...TREE_SELECT_MOCK[0] }
            };
            store.chooseNode(mockItem);
            const value = store.pathToSave();
            expect(value).toBe('demo.dotcms.com:/');
        });

        it('should update the form value with the correct format with one level', () => {
            const mockItem = {
                originalEvent: createFakeEvent('input'),
                node: { ...TREE_SELECT_MOCK[0].children[0] }
            };
            store.chooseNode(mockItem);
            const value = store.pathToSave();
            expect(value).toBe('demo.dotcms.com:/level1/');
        });

        it('should update the form value with the correct format with two level', () => {
            const mockItem = {
                originalEvent: createFakeEvent('input'),
                node: { ...TREE_SELECT_MOCK[0].children[0].children[0] }
            };
            store.chooseNode(mockItem);
            const value = store.pathToSave();
            expect(value).toBe('demo.dotcms.com:/level1/child1/');
        });

        it('should be null when data is null', () => {
            const mockItem = {
                originalEvent: createFakeEvent('input'),
                node: { ...TREE_SELECT_MOCK[0].children[0].children[0] }
            };
            delete mockItem.node.data;
            store.chooseNode(mockItem);
            const value = store.pathToSave();
            expect(value).toBe(null);
        });
    });

    describe('Method: loadChildren', () => {
        const mockFolders: TreeNodeItem[] = [
            {
                key: 'folder-1',
                label: 'demo.dotcms.com/level1/',
                data: {
                    id: 'folder-1',
                    hostname: 'demo.dotcms.com',
                    path: '/level1/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder'
            }
        ];

        it('should call getFoldersTreeNode and update node when node has no children', fakeAsync(() => {
            service.getFoldersTreeNode.mockReturnValue(
                of({
                    parent: {
                        id: 'parent-1',
                        hostName: 'demo.dotcms.com',
                        path: '/level1',
                        addChildrenAllowed: true
                    },
                    folders: mockFolders
                })
            );

            const nodeWithoutChildren = {
                ...TREE_SELECT_MOCK[0].children[0],
                children: undefined
            };
            const event = {
                originalEvent: createFakeEvent('click'),
                node: nodeWithoutChildren
            };

            store.loadChildren(event);
            tick();

            expect(service.getFoldersTreeNode).toHaveBeenCalledWith('demo.dotcms.com//level1/');
            expect(nodeWithoutChildren.leaf).toBe(true);
            expect(nodeWithoutChildren.icon).toBe('pi pi-folder-open');
            expect(nodeWithoutChildren.children).toEqual(mockFolders);
            expect(store.nodeExpaned()).toBe(nodeWithoutChildren);
        }));

        it('should call getFoldersTreeNode when node has empty children array (e.g. from buildTreeByPaths placeholder)', fakeAsync(() => {
            service.getFoldersTreeNode.mockReturnValue(
                of({
                    parent: {
                        id: 'parent-1',
                        hostName: 'demo.dotcms.com',
                        path: '/level1',
                        addChildrenAllowed: true
                    },
                    folders: mockFolders
                })
            );

            const nodeWithEmptyChildren = {
                ...TREE_SELECT_MOCK[0].children[0],
                children: [] as TreeNodeItem[]
            };
            const event = {
                originalEvent: createFakeEvent('click'),
                node: nodeWithEmptyChildren
            };

            store.loadChildren(event);
            tick();

            expect(service.getFoldersTreeNode).toHaveBeenCalledWith('demo.dotcms.com//level1/');
            expect(nodeWithEmptyChildren.children).toEqual(mockFolders);
        }));

        it('should not call getFoldersTreeNode when node already has children (avoids overwriting tree from buildTreeByPaths)', () => {
            const existingChild: TreeNodeItem = {
                key: 'existing-child',
                label: 'Existing',
                data: {
                    id: 'existing-child',
                    hostname: 'demo.dotcms.com',
                    path: '/level1/existing/',
                    type: 'folder'
                },
                expandedIcon: 'pi pi-folder-open',
                collapsedIcon: 'pi pi-folder'
            };
            const nodeWithChildren = {
                ...TREE_SELECT_MOCK[0].children[0],
                children: [existingChild]
            };
            const event = {
                originalEvent: createFakeEvent('click'),
                node: nodeWithChildren
            };

            store.loadChildren(event);

            expect(service.getFoldersTreeNode).not.toHaveBeenCalled();
        });
    });
});
