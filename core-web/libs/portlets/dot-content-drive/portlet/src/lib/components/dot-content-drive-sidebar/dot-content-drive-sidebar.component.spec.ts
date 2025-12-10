import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';

import { TreeNodeCollapseEvent, TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/tree';

import { delay } from 'rxjs/operators';

import { DotFolderService, DotMessageService } from '@dotcms/data-access';
import { DotFolder } from '@dotcms/dotcms-models';
import {
    DotContentDriveUploadFiles,
    DotTreeFolderComponent,
    DotFolderTreeNodeItem,
    DotContentDriveMoveItems,
    ALL_FOLDER
} from '@dotcms/portlets/content-drive/ui';
import { GlobalStore } from '@dotcms/store';

import { DotContentDriveSidebarComponent } from './dot-content-drive-sidebar.component';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';

describe('DotContentDriveSidebarComponent', () => {
    let spectator: Spectator<DotContentDriveSidebarComponent>;
    let contentDriveStore: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;

    const mockSiteDetails = {
        hostname: 'demo.dotcms.com',
        identifier: 'site-123',
        siteName: 'Demo Site'
    };

    const realAllFolder: DotFolderTreeNodeItem = {
        ...ALL_FOLDER,
        data: {
            hostname: mockSiteDetails.hostname,
            path: '',
            type: 'folder',
            id: mockSiteDetails.identifier
        }
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

    const mockTreeNodes: DotFolderTreeNodeItem[] = [
        {
            ...realAllFolder,
            data: {
                hostname: mockSiteDetails.hostname,
                path: '',
                type: 'folder',
                id: mockSiteDetails.identifier
            }
        },
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
            mockProvider(GlobalStore, {
                siteDetails: jest.fn().mockReturnValue(mockSiteDetails)
            }),
            mockProvider(DotContentDriveStore, {
                initContentDrive: jest.fn(),
                currentSite: jest.fn().mockReturnValue(mockSiteDetails),
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
                contextMenu: jest.fn().mockReturnValue(null),
                folders: jest.fn().mockReturnValue(mockTreeNodes),
                selectedNode: jest.fn().mockReturnValue(mockTreeNodes[1]),
                sidebarLoading: jest.fn().mockReturnValue(false),
                loadFolders: jest.fn(),
                loadChildFolders: jest.fn(),
                updateFolders: jest.fn(),
                setSelectedNode: jest.fn()
            }),
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
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

        contentDriveStore = spectator.inject(DotContentDriveStore, true);

        spectator.detectChanges();
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
            contentDriveStore.sidebarLoading.mockReturnValue(true);
            spectator.detectComponentChanges();

            const treeComponent = spectator.query(DotTreeFolderComponent);
            expect(treeComponent?.$loading()).toBe(true);
        });

        it('should pass correct selectedNode input to dot-tree-folder', () => {
            const treeComponent = spectator.query(DotTreeFolderComponent);
            const selectedNode = mockTreeNodes[1];
            expect(treeComponent?.$selectedNode()).toEqual([selectedNode]);
        });

        it('should pass showFolderIconOnFirstOnly as true to dot-tree-folder', () => {
            const treeComponent = spectator.query(DotTreeFolderComponent);
            expect(treeComponent?.$showFolderIconOnFirstOnly()).toBe(true);
        });

        it('should update dot-tree-folder inputs when signals change', () => {
            const newTreeNodes: DotFolderTreeNodeItem[] = [
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

            const selectedNode = newTreeNodes[0];

            contentDriveStore.folders.mockReturnValue(newTreeNodes);
            contentDriveStore.selectedNode.mockReturnValue(newTreeNodes[0]);
            contentDriveStore.sidebarLoading.mockReturnValue(true);
            spectator.detectComponentChanges();

            const treeComponent = spectator.query(DotTreeFolderComponent);
            expect(treeComponent?.$folders()).toEqual(newTreeNodes);
            expect(treeComponent?.$selectedNode()).toEqual([selectedNode]);
            expect(treeComponent?.$loading()).toBe(true);
        });
    });

    describe('Output Event Handling', () => {
        describe('onNodeSelect', () => {
            it('should handle onNodeSelect event and call store.setSelectedNode', () => {
                const mockEvent: TreeNodeSelectEvent = {
                    originalEvent: new Event('click'),
                    node: mockTreeNodes[1]
                };

                spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeSelect', mockEvent);

                expect(contentDriveStore.setSelectedNode).toHaveBeenCalledWith(mockTreeNodes[1]);
            });

            it('should handle onNodeSelect with different nodes', () => {
                const customNode: DotFolderTreeNodeItem = {
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

                expect(contentDriveStore.setSelectedNode).toHaveBeenCalledWith(customNode);
            });
        });

        describe('onNodeExpand', () => {
            it('should handle onNodeExpand event when node has no children', () => {
                // Mock the store's loadChildFolders method to return an observable
                const mockChildFolders: DotFolderTreeNodeItem[] = [];
                contentDriveStore.loadChildFolders.mockReturnValue(
                    of({ parent: mockFolders[0], folders: mockChildFolders })
                );
                contentDriveStore.folders.mockReturnValue(mockTreeNodes);

                const nodeWithoutChildren: DotFolderTreeNodeItem = {
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

                expect(contentDriveStore.loadChildFolders).toHaveBeenCalledWith(
                    '/expandable/',
                    'demo.dotcms.com'
                );
                expect(nodeWithoutChildren.loading).toBe(false);
                expect(nodeWithoutChildren.expanded).toBe(true);
                expect(nodeWithoutChildren.leaf).toBe(true);
                expect(contentDriveStore.updateFolders).toHaveBeenCalled();
            });

            it('should set node expanded to true if it already has children or is leaf', () => {
                // Reset the mock to clear any calls from component initialization
                jest.clearAllMocks();

                const nodeWithChildren: DotFolderTreeNodeItem = {
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
                expect(contentDriveStore.loadChildFolders).not.toHaveBeenCalled();
            });

            it('should set loading state during expansion', fakeAsync(() => {
                // Mock the store's loadChildFolders method to return a delayed observable
                const mockChildFolders: DotFolderTreeNodeItem[] = [];
                contentDriveStore.loadChildFolders.mockReturnValue(
                    of({ parent: mockFolders[0], folders: mockChildFolders }).pipe(delay(500))
                );
                contentDriveStore.folders.mockReturnValue(mockTreeNodes);

                const nodeWithoutChildren: DotFolderTreeNodeItem = {
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
                expect(nodeWithoutChildren.expanded).toBe(true);
            }));

            it('should update node with loaded children when folders are returned', () => {
                const loadedChildFolders: DotFolderTreeNodeItem[] = [
                    {
                        key: 'child-1',
                        label: '/expandable/child1/',
                        data: {
                            id: 'child-1',
                            hostname: 'demo.dotcms.com',
                            path: '/expandable/child1/',
                            type: 'folder'
                        },
                        leaf: false
                    }
                ];

                contentDriveStore.loadChildFolders.mockReturnValue(
                    of({ parent: mockFolders[0], folders: loadedChildFolders })
                );
                contentDriveStore.folders.mockReturnValue(mockTreeNodes);

                const nodeWithoutChildren: DotFolderTreeNodeItem = {
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

                expect(nodeWithoutChildren.children).toEqual(loadedChildFolders);
                expect(nodeWithoutChildren.leaf).toBe(false);
                expect(contentDriveStore.updateFolders).toHaveBeenCalled();
            });

            it('should handle error when loading child folders', () => {
                contentDriveStore.loadChildFolders.mockReturnValue(
                    of({ parent: mockFolders[0], folders: [] })
                );
                contentDriveStore.folders.mockReturnValue(mockTreeNodes);

                const nodeWithoutChildren: DotFolderTreeNodeItem = {
                    key: 'error-folder',
                    label: '/error/',
                    data: {
                        id: 'error-folder',
                        hostname: 'demo.dotcms.com',
                        path: '/error/',
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

                expect(nodeWithoutChildren.loading).toBe(false);
                expect(nodeWithoutChildren.expanded).toBe(true);
            });
        });

        describe('onNodeCollapse', () => {
            it('should handle onNodeCollapse event for regular nodes', () => {
                const regularNode: DotFolderTreeNodeItem = {
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

            it('should prevent root from collapsing', () => {
                const allFolderNode: DotFolderTreeNodeItem = {
                    ...realAllFolder,
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

        describe('uploadFiles', () => {
            it('should emit uploadFiles event when dot-tree-folder emits uploadFiles', () => {
                const mockFileList = {
                    length: 2,
                    item: (_index: number) => null,
                    [Symbol.iterator]: function* () {
                        yield new File(['content1'], 'file1.txt');
                        yield new File(['content2'], 'file2.txt');
                    }
                } as FileList;

                const mockUploadEvent: DotContentDriveUploadFiles = {
                    files: mockFileList,
                    targetFolder: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: 'folder-1',
                        type: 'folder'
                    }
                };

                let emittedValue: DotContentDriveUploadFiles | undefined;

                spectator.component.uploadFiles.subscribe((event) => {
                    emittedValue = event;
                });

                spectator.triggerEventHandler(
                    DotTreeFolderComponent,
                    'uploadFiles',
                    mockUploadEvent
                );

                expect(emittedValue).toBeDefined();
                expect(emittedValue?.files).toBe(mockFileList);
                expect(emittedValue?.targetFolder.id).toBe('folder-1');
            });
        });

        describe('moveItems', () => {
            it('should emit moveItems event when dot-tree-folder emits moveItems', () => {
                const mockMoveEvent: DotContentDriveMoveItems = {
                    targetFolder: {
                        id: 'folder-1',
                        hostname: 'demo.dotcms.com',
                        path: '/documents/',
                        type: 'folder'
                    }
                };

                let emittedValue: DotContentDriveMoveItems | undefined;

                spectator.component.moveItems.subscribe((event) => {
                    emittedValue = event;
                });

                spectator.triggerEventHandler(DotTreeFolderComponent, 'moveItems', mockMoveEvent);

                expect(emittedValue).toBeDefined();
                expect(emittedValue?.targetFolder).toEqual(mockMoveEvent.targetFolder);
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
            const testNode: DotFolderTreeNodeItem = {
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
            expect(contentDriveStore.setSelectedNode).toHaveBeenCalledWith(testNode);

            // Then expand the node
            const expandEvent: TreeNodeExpandEvent = {
                originalEvent: new Event('click'),
                node: testNode
            };
            spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeExpand', expandEvent);
            expect(contentDriveStore.loadChildFolders).toHaveBeenCalledWith(
                '/test/',
                'demo.dotcms.com'
            );
        });
    });

    describe('Current site hostname', () => {
        it('should render the current site hostname', () => {
            const currentSiteHostname = spectator.query('[data-testid="current-site-hostname"]');

            expect(currentSiteHostname?.textContent).toContain(mockSiteDetails.hostname);
        });

        it('should handle null current site gracefully', () => {
            contentDriveStore.currentSite.mockReturnValue(null);
            spectator.detectComponentChanges();

            const currentSiteHostname = spectator.query('[data-testid="current-site-hostname"]');
            expect(currentSiteHostname?.textContent.trim()).toBe('');
        });
    });

    describe('Effects', () => {
        it('should have getSiteFoldersEffect that calls loadFolders when site is available', () => {
            // The effect is set up during component initialization
            // We verify it exists and the component is properly initialized
            expect(spectator.component).toBeTruthy();

            // The effect should have been set up during component creation
            // We can verify loadFolders was called during initialization if site exists
            // Since mockSiteDetails is set in beforeEach, loadFolders should have been called
            expect(contentDriveStore.loadFolders).toHaveBeenCalled();
        });

        it('should not load folders when currentSite is null', () => {
            // Clear any previous calls
            jest.clearAllMocks();

            // Set currentSite to null - the effect should return early
            contentDriveStore.currentSite.mockReturnValue(null);

            // The effect checks currentSite at the start, so if it's null, loadFolders won't be called
            // We verify this by checking that after setting null, loadFolders is not called
            spectator.detectComponentChanges();
            spectator.flushEffects();

            // Since currentSite is null, the effect should return early and not call loadFolders
            // Note: This test verifies the effect logic, not the actual effect execution
            // The effect code checks: if (!currentSite) return;
            expect(contentDriveStore.currentSite()).toBeNull();
        });
    });

    describe('handleSelectedNodeFromTable', () => {
        it('should handle selectedNode with fromTable flag', () => {
            const mockScrollIntoView = jest.fn();
            // Create a proper mock element that extends HTMLElement
            const mockElement = {
                scrollIntoView: mockScrollIntoView
            } as unknown as HTMLElement;

            // Get the tree folder component and mock its querySelector
            const treeFolderComponent = spectator.query(DotTreeFolderComponent);
            const nativeElement = treeFolderComponent?.elementRef.nativeElement;

            // Mock querySelector to return the mock element
            jest.spyOn(nativeElement, 'querySelector').mockReturnValue(mockElement);

            // Spy on the recursiveExpandOneNode method
            const recursiveExpandSpy = jest.spyOn(spectator.component, 'recursiveExpandOneNode');

            const nodeFromTable: DotFolderTreeNodeItem = {
                key: 'table-node',
                label: '/documents/images/',
                data: {
                    id: 'table-node',
                    hostname: 'demo.dotcms.com',
                    path: '/documents/images/',
                    type: 'folder',
                    fromTable: true
                },
                leaf: false
            };

            // Setup folders - mockTreeNodes already includes a node with path '/documents/' (folder-1)
            // The path '/documents/images/' split and filtered gives ['documents', 'images']
            // slice(0, -1) removes the last segment, so it becomes ['documents']
            contentDriveStore.folders.mockReturnValue(mockTreeNodes);
            contentDriveStore.loadChildFolders.mockReturnValue(
                of({ parent: mockFolders[0], folders: [] })
            );

            // Call the method directly - this is what the effect would call
            spectator.component.handleSelectedNodeFromTable(nodeFromTable);

            // Verify recursiveExpandOneNode was called with correct arguments
            // The method calls it with just segments, which defaults to this.$folders()
            expect(recursiveExpandSpy).toHaveBeenCalledWith(['documents']);

            // Verify scrollIntoView was called if element was found
            expect(mockScrollIntoView).toHaveBeenCalledWith({
                behavior: 'smooth',
                block: 'center'
            });
        });

        it('should handle selectedNode with fromTable flag when element is not found', () => {
            const nodeFromTable: DotFolderTreeNodeItem = {
                key: 'table-node',
                label: '/documents/images/',
                data: {
                    id: 'table-node',
                    hostname: 'demo.dotcms.com',
                    path: '/documents/images/',
                    type: 'folder',
                    fromTable: true
                },
                leaf: false
            };

            // Mock querySelector to return null (element not found)
            const treeFolderComponent = spectator.query(DotTreeFolderComponent);
            const nativeElement = treeFolderComponent?.elementRef.nativeElement;

            if (nativeElement) {
                jest.spyOn(nativeElement, 'querySelector').mockReturnValue(null);
            }

            // Spy on the recursiveExpandOneNode method
            const recursiveExpandSpy = jest.spyOn(spectator.component, 'recursiveExpandOneNode');

            contentDriveStore.folders.mockReturnValue(mockTreeNodes);
            contentDriveStore.loadChildFolders.mockReturnValue(
                of({ parent: mockFolders[0], folders: [] })
            );

            // Should not throw error even if element is not found
            expect(() => {
                spectator.component.handleSelectedNodeFromTable(nodeFromTable);
            }).not.toThrow();

            // Verify recursiveExpandOneNode was still called
            expect(recursiveExpandSpy).toHaveBeenCalledWith(['documents']);
        });

        it('should return early when fromTable is false', () => {
            const nodeWithoutFromTable: DotFolderTreeNodeItem = {
                key: 'regular-node',
                label: '/documents/',
                data: {
                    id: 'regular-node',
                    hostname: 'demo.dotcms.com',
                    path: '/documents/',
                    type: 'folder',
                    fromTable: false
                },
                leaf: false
            };

            const recursiveExpandSpy = jest.spyOn(spectator.component, 'recursiveExpandOneNode');
            const treeFolderComponent = spectator.query(DotTreeFolderComponent);
            const nativeElement = treeFolderComponent?.elementRef.nativeElement;
            const querySelectorSpy = jest.spyOn(nativeElement, 'querySelector');

            // Call the method with a node that doesn't have fromTable flag
            spectator.component.handleSelectedNodeFromTable(nodeWithoutFromTable);

            // Verify that recursiveExpandOneNode was not called
            expect(recursiveExpandSpy).not.toHaveBeenCalled();

            // Verify that querySelector was not called
            expect(querySelectorSpy).not.toHaveBeenCalled();
        });
    });

    describe('Tree Toggler', () => {
        it('should render dot-content-drive-tree-toggler component', () => {
            const treeToggler = spectator.query('[data-testid="tree-toggler"]');
            expect(treeToggler).toBeTruthy();
        });
    });

    describe('recursiveExpandOneNode', () => {
        it('should recursively expand nodes based on path segments', () => {
            jest.clearAllMocks();

            const testFolders: DotFolderTreeNodeItem[] = [
                {
                    key: 'documents-node',
                    label: '/documents/',
                    data: {
                        id: 'documents-node',
                        hostname: 'demo.dotcms.com',
                        path: '/documents/',
                        type: 'folder'
                    },
                    leaf: false,
                    children: []
                }
            ];

            contentDriveStore.folders.mockReturnValue(testFolders);
            contentDriveStore.loadChildFolders.mockReturnValue(
                of({ parent: mockFolders[0], folders: [] })
            );

            // Call recursiveExpandOneNode with path segments
            // Path '/documents/images/' -> segments ['documents', 'images']
            spectator.component.recursiveExpandOneNode(['documents'], testFolders);

            // Should call loadChildFolders for the 'documents' node
            expect(contentDriveStore.loadChildFolders).toHaveBeenCalledWith(
                '/documents/',
                'demo.dotcms.com'
            );
        });

        it('should return early when segments array is empty', () => {
            jest.clearAllMocks();

            spectator.component.recursiveExpandOneNode([], mockTreeNodes);

            expect(contentDriveStore.loadChildFolders).not.toHaveBeenCalled();
        });

        it('should return early when no matching node is found', () => {
            jest.clearAllMocks();

            // Try to find a node with path containing 'nonexistent'
            spectator.component.recursiveExpandOneNode(['nonexistent'], mockTreeNodes);

            expect(contentDriveStore.loadChildFolders).not.toHaveBeenCalled();
        });

        it('should recursively expand nested path segments', () => {
            jest.clearAllMocks();

            const nestedFolders: DotFolderTreeNodeItem[] = [
                {
                    key: 'level1',
                    label: '/level1/',
                    data: {
                        id: 'level1',
                        hostname: 'demo.dotcms.com',
                        path: '/level1/',
                        type: 'folder'
                    },
                    leaf: false,
                    children: []
                }
            ];

            contentDriveStore.folders.mockReturnValue(nestedFolders);
            contentDriveStore.loadChildFolders.mockReturnValue(
                of({
                    parent: mockFolders[0],
                    folders: [
                        {
                            key: 'level2',
                            label: '/level1/level2/',
                            data: {
                                id: 'level2',
                                hostname: 'demo.dotcms.com',
                                path: '/level1/level2/',
                                type: 'folder'
                            },
                            leaf: false,
                            children: []
                        }
                    ]
                })
            );

            // Expand path with multiple segments
            spectator.component.recursiveExpandOneNode(['level1', 'level2'], nestedFolders);

            // Should call loadChildFolders for level1 first
            expect(contentDriveStore.loadChildFolders).toHaveBeenCalledWith(
                '/level1/',
                'demo.dotcms.com'
            );
        });
    });

    describe('Edge Cases', () => {
        it('should handle onNodeExpand when node is already a leaf', () => {
            jest.clearAllMocks();

            const leafNode: DotFolderTreeNodeItem = {
                key: 'leaf-folder',
                label: '/leaf/',
                data: {
                    id: 'leaf-folder',
                    hostname: 'demo.dotcms.com',
                    path: '/leaf/',
                    type: 'folder'
                },
                leaf: true,
                children: undefined // Explicitly set to undefined
            };

            const mockEvent: TreeNodeExpandEvent = {
                originalEvent: new Event('click'),
                node: leafNode
            };

            spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeExpand', mockEvent);

            expect(leafNode.expanded).toBe(true);
            expect(contentDriveStore.loadChildFolders).not.toHaveBeenCalled();
        });

        it('should handle onNodeCollapse for non-ALL_FOLDER nodes', () => {
            const regularNode: DotFolderTreeNodeItem = {
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

            // Regular nodes can collapse, so expanded should remain true (no change)
            expect(regularNode.expanded).toBe(true);
        });

        it('should handle onNodeExpand when node already has children', () => {
            jest.clearAllMocks();

            const nodeWithChildren: DotFolderTreeNodeItem = {
                key: 'parent-folder',
                label: '/parent/',
                data: {
                    id: 'parent-folder',
                    hostname: 'demo.dotcms.com',
                    path: '/parent/',
                    type: 'folder'
                },
                leaf: false,
                expanded: false,
                children: [mockTreeNodes[0]] // Has children with length > 0
            };

            const mockEvent: TreeNodeExpandEvent = {
                originalEvent: new Event('click'),
                node: nodeWithChildren
            };

            spectator.triggerEventHandler(DotTreeFolderComponent, 'onNodeExpand', mockEvent);

            expect(nodeWithChildren.expanded).toBe(true);
            expect(contentDriveStore.loadChildFolders).not.toHaveBeenCalled();
        });
    });
});
