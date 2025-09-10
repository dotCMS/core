import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';

import { TreeNodeCollapseEvent, TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/tree';

import { delay } from 'rxjs/operators';

import { DotFolder, DotFolderService, DotMessageService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotTreeFolderComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveSidebarComponent } from './dot-content-drive-sidebar.component';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { ALL_FOLDER, TreeNodeItem } from '../../utils/tree-folder.utils';

describe('DotContentDriveSidebarComponent', () => {
    let spectator: Spectator<DotContentDriveSidebarComponent>;
    let component: DotContentDriveSidebarComponent;
    let contentDriveStore: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;
    let folderService: jest.Mocked<DotFolderService>;

    const mockSiteDetails = {
        hostname: 'demo.dotcms.com',
        identifier: 'site-123',
        siteName: 'Demo Site'
    };

    const mockFolders: DotFolder[] = [
        {
            id: 'parent-folder',
            path: '/documents/',
            hostName: 'demo.dotcms.com',
            addChildrenAllowed: true
        },
        {
            id: 'child-folder-1',
            path: '/documents/images/',
            hostName: 'demo.dotcms.com',
            addChildrenAllowed: true
        },
        {
            id: 'child-folder-2',
            path: '/documents/videos/',
            hostName: 'demo.dotcms.com',
            addChildrenAllowed: true
        }
    ];

    const mockTreeNodes: TreeNodeItem[] = [
        ALL_FOLDER,
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

    const createComponent = createComponentFactory({
        component: DotContentDriveSidebarComponent,
        imports: [DotTreeFolderComponent],
        providers: [
            mockProvider(DotMessageService, new MockDotMessageService({})),
            mockProvider(GlobalStore, {
                siteDetails: jest.fn().mockReturnValue(mockSiteDetails)
            }),
            mockProvider(DotContentDriveStore, {
                initContentDrive: jest.fn(),
                currentSite: jest.fn(),
                isTreeExpanded: jest.fn().mockReturnValue(true),
                removeFilter: jest.fn(),
                getFilterValue: jest.fn(),
                setIsTreeExpanded: jest.fn(),
                path: jest.fn().mockReturnValue('/test/path'),
                setItems: jest.fn(),
                setStatus: jest.fn(),
                setPagination: jest.fn(),
                setSort: jest.fn(),
                patchFilters: jest.fn(),
                setPath: jest.fn(),
                $searchParams: jest.fn(),
                contextMenu: jest.fn().mockReturnValue(null)
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            providers: [
                mockProvider(DotFolderService, {
                    getFolders: jest.fn().mockReturnValue(of(mockFolders))
                })
            ]
        });
        component = spectator.component;
        contentDriveStore = spectator.inject(DotContentDriveStore);
        folderService = spectator.inject(DotFolderService) as jest.Mocked<DotFolderService>;

        // Set up component state for testing
        component.$folders.set(mockTreeNodes);
        component.$selectedNode.set(mockTreeNodes[1]);
        component.$loading.set(false);

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('HTML Rendering', () => {
        it('should render dot-tree-folder component', () => {
            const treeComponent = spectator.query(DotTreeFolderComponent);
            expect(treeComponent).toBeTruthy();
        });

        it('should pass correct folders input to dot-tree-folder', () => {
            const treeComponent = spectator.query(DotTreeFolderComponent);
            expect(treeComponent?.$folders()).toEqual(mockTreeNodes);
        });

        it('should pass correct loading input to dot-tree-folder', () => {
            component.$loading.set(true);
            spectator.detectChanges();

            const treeComponent = spectator.query(DotTreeFolderComponent);
            expect(treeComponent?.$loading()).toBe(true);
        });

        it('should pass correct selectedNode input to dot-tree-folder', () => {
            const treeComponent = spectator.query(DotTreeFolderComponent);
            expect(treeComponent?.$selectedNode()).toEqual(mockTreeNodes[1]);
        });

        it('should pass showFolderIconOnFirstOnly as true to dot-tree-folder', () => {
            const treeComponent = spectator.query(DotTreeFolderComponent);
            expect(treeComponent?.$showFolderIconOnFirstOnly()).toBe(true);
        });

        it('should update dot-tree-folder inputs when signals change', () => {
            const newTreeNodes: TreeNodeItem[] = [
                {
                    key: 'new-folder',
                    label: '/new-folder/',
                    data: {
                        id: 'new-folder',
                        hostname: 'demo.dotcms.com',
                        path: '/new-folder/',
                        type: 'folder'
                    },
                    leaf: false
                }
            ];

            component.$folders.set(newTreeNodes);
            component.$selectedNode.set(newTreeNodes[0]);
            component.$loading.set(true);
            spectator.detectChanges();

            const treeComponent = spectator.query(DotTreeFolderComponent);
            expect(treeComponent?.$folders()).toEqual(newTreeNodes);
            expect(treeComponent?.$selectedNode()).toEqual(newTreeNodes[0]);
            expect(treeComponent?.$loading()).toBe(true);
        });
    });

    describe('Output Event Handling', () => {
        describe('onNodeSelect', () => {
            it('should handle onNodeSelect event and call store.setPath', () => {
                const mockEvent: TreeNodeSelectEvent = {
                    originalEvent: new Event('click'),
                    node: mockTreeNodes[1]
                };

                spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeSelect', mockEvent);

                expect(contentDriveStore.setPath).toHaveBeenCalledWith('/documents/');
            });

            it('should extract path from node data correctly', () => {
                const customNode: TreeNodeItem = {
                    key: 'custom-folder',
                    label: 'Custom',
                    data: {
                        id: 'custom-folder',
                        hostname: 'demo.dotcms.com',
                        path: '/custom/path/',
                        type: 'folder'
                    },
                    leaf: false
                };

                const mockEvent: TreeNodeSelectEvent = {
                    originalEvent: new Event('click'),
                    node: customNode
                };

                spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeSelect', mockEvent);

                expect(contentDriveStore.setPath).toHaveBeenCalledWith('/custom/path/');
            });
        });

        describe('onNodeExpand', () => {
            it('should handle onNodeExpand event when node has no children', () => {
                const nodeWithoutChildren: TreeNodeItem = {
                    key: 'expandable-folder',
                    label: '/expandable/',
                    data: {
                        id: 'expandable-folder',
                        hostname: 'demo.dotcms.com',
                        path: '/expandable/',
                        type: 'folder'
                    },
                    leaf: false,
                    children: []
                };

                const mockEvent: TreeNodeExpandEvent = {
                    originalEvent: new Event('click'),
                    node: nodeWithoutChildren
                };

                spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeExpand', mockEvent);

                expect(folderService.getFolders).toHaveBeenCalledWith(
                    'demo.dotcms.com/expandable/'
                );
            });

            it('should set node expanded to true if it already has children or is leaf', () => {
                // Reset the mock to clear any calls from component initialization
                jest.clearAllMocks();

                const nodeWithChildren: TreeNodeItem = {
                    key: 'parent-folder',
                    label: '/parent/',
                    data: {
                        id: 'parent-folder',
                        hostname: 'demo.dotcms.com',
                        path: '/parent/',
                        type: 'folder'
                    },
                    leaf: false,
                    children: [mockTreeNodes[0]]
                };

                const mockEvent: TreeNodeExpandEvent = {
                    originalEvent: new Event('click'),
                    node: nodeWithChildren
                };

                spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeExpand', mockEvent);

                expect(nodeWithChildren.expanded).toBe(true);
                expect(folderService.getFolders).not.toHaveBeenCalled();
            });

            it('should set loading state during expansion', fakeAsync(() => {
                // Delay the response to simulate a network request
                jest.spyOn(folderService, 'getFolders').mockReturnValue(of([]).pipe(delay(500)));
                const nodeWithoutChildren: TreeNodeItem = {
                    key: 'loading-folder',
                    label: '/loading/',
                    data: {
                        id: 'loading-folder',
                        hostname: 'demo.dotcms.com',
                        path: '/loading/',
                        type: 'folder'
                    },
                    leaf: false,
                    children: []
                };

                const mockEvent: TreeNodeExpandEvent = {
                    originalEvent: new Event('click'),
                    node: nodeWithoutChildren
                };

                spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeExpand', mockEvent);
                expect(nodeWithoutChildren.loading).toBe(true);
                tick(501);
                expect(nodeWithoutChildren.loading).toBe(false);
            }));
        });

        describe('onNodeCollapse', () => {
            it('should handle onNodeCollapse event for regular nodes', () => {
                const regularNode: TreeNodeItem = {
                    key: 'regular-folder',
                    label: '/regular/',
                    data: {
                        id: 'regular-folder',
                        hostname: 'demo.dotcms.com',
                        path: '/regular/',
                        type: 'folder'
                    },
                    leaf: false,
                    expanded: true
                };

                const mockEvent: TreeNodeCollapseEvent = {
                    originalEvent: new Event('click'),
                    node: regularNode
                };

                spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeCollapse', mockEvent);

                // Regular nodes should be able to collapse (no action needed)
                expect(regularNode.expanded).toBe(true); // No change for regular nodes
            });

            it('should prevent ALL_FOLDER from collapsing', () => {
                const allFolderNode: TreeNodeItem = {
                    ...ALL_FOLDER,
                    expanded: true
                };

                const mockEvent: TreeNodeCollapseEvent = {
                    originalEvent: new Event('click'),
                    node: allFolderNode
                };

                spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeCollapse', mockEvent);

                expect(allFolderNode.expanded).toBe(true);
            });
        });
    });

    describe('Event Handler Integration', () => {
        it('should have all event handlers properly bound', () => {
            const treeComponent = spectator.query(DotTreeFolderComponent);

            // Check that all event handlers are bound
            expect(treeComponent).toBeTruthy();

            // Verify event bindings exist by checking the component's event listeners
            const componentElement = spectator.query('dot-tree-folder');
            expect(componentElement).toBeTruthy();
        });

        it('should trigger events in correct sequence during node interaction', () => {
            const testNode: TreeNodeItem = {
                key: 'test-node',
                label: '/test/',
                data: {
                    id: 'test-node',
                    hostname: 'demo.dotcms.com',
                    path: '/test/',
                    type: 'folder'
                },
                leaf: false
            };

            // First select the node
            const selectEvent: TreeNodeSelectEvent = {
                originalEvent: new Event('click'),
                node: testNode
            };
            spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeSelect', selectEvent);
            expect(contentDriveStore.setPath).toHaveBeenCalledWith('/test/');

            // Then expand the node
            const expandEvent: TreeNodeExpandEvent = {
                originalEvent: new Event('click'),
                node: testNode
            };
            spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeExpand', expandEvent);
            expect(folderService.getFolders).toHaveBeenCalledWith('demo.dotcms.com/test/');
        });
    });
});
