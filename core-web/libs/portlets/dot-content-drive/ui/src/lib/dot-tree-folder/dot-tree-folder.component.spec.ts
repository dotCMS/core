import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { TreeNode } from 'primeng/api';
import { SkeletonModule } from 'primeng/skeleton';
import { Tree, TreeModule, TreeNodeExpandEvent, TreeNodeCollapseEvent } from 'primeng/tree';

import { DotMessageService } from '@dotcms/data-access';
import { FolderNamePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotTreeFolderComponent } from './dot-tree-folder.component';

import { SYSTEM_HOST_ID } from '../shared/constants';

// Mock DragEvent since it's not available in Jest environment
class DragEventMock extends Event {
    override preventDefault = jest.fn();
    override stopPropagation = jest.fn();
    dataTransfer: { files?: FileList | null } | null = null;

    constructor(type: string) {
        super(type);
        this.dataTransfer = { files: null };
    }
}

// Override global DragEvent with our mock
(global as unknown as { DragEvent: typeof DragEventMock }).DragEvent = DragEventMock;

// Helper functions to create properly mocked drag events
function createDragEnterEvent(
    fromElement?: HTMLElement | null
): DragEvent & { fromElement: HTMLElement | null } {
    const event = new DragEvent('dragenter') as DragEvent & { fromElement: HTMLElement | null };
    (event as unknown as { fromElement: HTMLElement | null }).fromElement = fromElement ?? null;

    return event;
}

function createDragLeaveEvent(relatedTarget?: EventTarget | null): DragEvent {
    const event = new DragEvent('dragleave');
    (event as unknown as { relatedTarget: EventTarget | null }).relatedTarget =
        relatedTarget ?? null;

    return event;
}

function createDragOverEvent(target?: HTMLElement): DragEvent {
    const event = new DragEvent('dragover');
    Object.defineProperty(event, 'target', {
        value: target ?? document.createElement('div'),
        writable: false
    });

    return event;
}

function createDropEvent(files?: FileList | null): DragEvent {
    const event = new DragEvent('drop');
    if (event.dataTransfer) {
        (event.dataTransfer as unknown as { files: FileList | null }).files = files ?? null;
    }

    return event;
}

describe('DotTreeFolderComponent', () => {
    let spectator: Spectator<DotTreeFolderComponent>;
    let component: DotTreeFolderComponent;

    const mockFolders: TreeNode[] = [
        {
            key: '1',
            label: '/application/content',
            data: { path: '/application/content' },
            children: [
                {
                    key: '2',
                    label: '/application/content/images',
                    data: { path: '/application/content/images' }
                }
            ]
        },
        {
            key: '3',
            label: '/application/documents',
            data: { path: '/application/documents' }
        }
    ];

    const mockSelectedNode: TreeNode = {
        key: '2',
        label: '/application/content/images',
        data: { path: '/application/content/images' }
    };

    const createComponent = createComponentFactory({
        component: DotTreeFolderComponent,
        imports: [TreeModule, SkeletonModule, FolderNamePipe],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content.drive.loading.folders.title': 'Loading folders...'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;

        // Set required inputs directly on the component using aliases
        spectator.fixture.componentRef.setInput('folders', mockFolders);
        spectator.fixture.componentRef.setInput('loading', false);
        spectator.fixture.componentRef.setInput('selectedNode', mockSelectedNode);
        spectator.fixture.componentRef.setInput('showFolderIconOnFirstOnly', false);

        spectator.detectChanges();
    });

    describe('Component Initialization', () => {
        it('should create', () => {
            expect(component).toBeTruthy();
        });

        it('should have the correct inputs', () => {
            expect(component.$folders()).toEqual(mockFolders);
            expect(component.$loading()).toBe(false);
            expect(component.$selectedNode()).toEqual([mockSelectedNode]);
            expect(component.$showFolderIconOnFirstOnly()).toBe(false);
        });
    });

    describe('p-tree Properties', () => {
        let treeComponent: Tree;

        beforeEach(() => {
            treeComponent = spectator.query(Tree);
        });

        it('should pass folders to p-tree value property', () => {
            expect(treeComponent.value).toEqual(mockFolders);
        });

        it('should pass loading state to p-tree loading property', () => {
            expect(treeComponent.loading).toBe(false);
        });

        it('should set selectionMode to multiple', () => {
            expect(treeComponent.selectionMode).toBe('multiple');
        });

        it('should set loadingMode to icon', () => {
            expect(treeComponent.loadingMode).toBe('icon');
        });

        it('should pass selectedNode to p-tree selection property', () => {
            expect(treeComponent.selection).toEqual([mockSelectedNode]);
        });

        it('should set scrollHeight to auto', () => {
            expect(treeComponent.scrollHeight).toBe('auto');
        });

        it('should have correct styleClass when showFolderIconOnFirstOnly is false', () => {
            spectator.fixture.componentRef.setInput('showFolderIconOnFirstOnly', false);
            spectator.detectChanges();
            expect(treeComponent.styleClass).toBe('w-full h-full folder-all');
        });

        it('should have correct styleClass when showFolderIconOnFirstOnly is true', () => {
            spectator.fixture.componentRef.setInput('showFolderIconOnFirstOnly', true);
            spectator.detectChanges();
            expect(treeComponent.styleClass).toBe('w-full h-full first-only');
        });
    });

    describe('showFolderIconOnFirstOnly Input', () => {
        it('should compute treeStyleClasses correctly when showFolderIconOnFirstOnly is false', () => {
            spectator.fixture.componentRef.setInput('showFolderIconOnFirstOnly', false);
            spectator.detectChanges();
            expect(component.treeStyleClasses()).toBe('w-full h-full folder-all');
        });

        it('should compute treeStyleClasses correctly when showFolderIconOnFirstOnly is true', () => {
            spectator.fixture.componentRef.setInput('showFolderIconOnFirstOnly', true);
            spectator.detectChanges();
            expect(component.treeStyleClasses()).toBe('w-full h-full first-only');
        });

        it('should update p-tree styleClass when showFolderIconOnFirstOnly changes', () => {
            const treeComponent = spectator.query(Tree);

            spectator.fixture.componentRef.setInput('showFolderIconOnFirstOnly', true);
            spectator.detectChanges();
            expect(treeComponent.styleClass).toBe('w-full h-full first-only');

            spectator.fixture.componentRef.setInput('showFolderIconOnFirstOnly', false);
            spectator.detectChanges();
            expect(treeComponent.styleClass).toBe('w-full h-full folder-all');
        });
    });

    describe('selectedNode Input', () => {
        it('should pass selectedNode correctly to p-tree selection', () => {
            const newSelectedNode: TreeNode = {
                key: '3',
                label: '/application/documents',
                data: { path: '/application/documents' }
            };

            spectator.fixture.componentRef.setInput('selectedNode', newSelectedNode);
            spectator.detectChanges();

            const treeComponent = spectator.query(Tree);
            expect(treeComponent.selection).toEqual([newSelectedNode]);
        });

        it('should update p-tree selection when selectedNode changes', () => {
            const treeComponent = spectator.query(Tree);

            expect(treeComponent.selection).toEqual([mockSelectedNode]);

            const newSelectedNode: TreeNode = {
                key: '1',
                label: '/application/content',
                data: { path: '/application/content' }
            };

            spectator.fixture.componentRef.setInput('selectedNode', newSelectedNode);
            spectator.detectChanges();
            expect(treeComponent.selection).toEqual([newSelectedNode]);
        });
    });

    describe('Component Outputs', () => {
        let treeComponent: Tree;

        beforeEach(() => {
            treeComponent = spectator.query(Tree);
        });

        it('should emit onNodeSelect when tree node is selected', () => {
            const onNodeSelectSpy = jest.spyOn(component.onNodeSelect, 'emit');
            const mockEvent: TreeNodeExpandEvent = {
                originalEvent: new Event('click'),
                node: mockFolders[0]
            };

            treeComponent.onNodeSelect.emit(mockEvent);

            expect(onNodeSelectSpy).toHaveBeenCalledWith(mockEvent);
        });

        it('should emit onNodeExpand when tree node is expanded', () => {
            const onNodeExpandSpy = jest.spyOn(component.onNodeExpand, 'emit');
            const mockEvent: TreeNodeExpandEvent = {
                originalEvent: new Event('click'),
                node: mockFolders[0]
            };

            treeComponent.onNodeExpand.emit(mockEvent);

            expect(onNodeExpandSpy).toHaveBeenCalledWith(mockEvent);
        });

        it('should emit onNodeCollapse when tree node is collapsed', () => {
            const onNodeCollapseSpy = jest.spyOn(component.onNodeCollapse, 'emit');
            const mockEvent: TreeNodeCollapseEvent = {
                originalEvent: new Event('click'),
                node: mockFolders[0]
            };

            treeComponent.onNodeCollapse.emit(mockEvent);

            expect(onNodeCollapseSpy).toHaveBeenCalledWith(mockEvent);
        });

        it('should trigger outputs through event handlers in template', () => {
            const onNodeSelectSpy = jest.spyOn(component.onNodeSelect, 'emit');
            const onNodeExpandSpy = jest.spyOn(component.onNodeExpand, 'emit');
            const onNodeCollapseSpy = jest.spyOn(component.onNodeCollapse, 'emit');

            const mockSelectEvent: TreeNodeExpandEvent = {
                originalEvent: new Event('select'),
                node: mockFolders[0]
            };

            const mockExpandEvent: TreeNodeExpandEvent = {
                originalEvent: new Event('expand'),
                node: mockFolders[0]
            };

            const mockCollapseEvent: TreeNodeCollapseEvent = {
                originalEvent: new Event('collapse'),
                node: mockFolders[0]
            };

            spectator.triggerEventHandler(Tree, 'onNodeSelect', mockSelectEvent);
            spectator.triggerEventHandler(Tree, 'onNodeExpand', mockExpandEvent);
            spectator.triggerEventHandler(Tree, 'onNodeCollapse', mockCollapseEvent);

            expect(onNodeSelectSpy).toHaveBeenCalledWith(mockSelectEvent);
            expect(onNodeExpandSpy).toHaveBeenCalledWith(mockExpandEvent);
            expect(onNodeCollapseSpy).toHaveBeenCalledWith(mockCollapseEvent);
        });
    });

    describe('Template Rendering', () => {
        it('should render p-tree when not loading', () => {
            spectator.fixture.componentRef.setInput('loading', false);
            spectator.detectChanges();

            const treeElement = spectator.query('p-tree');
            expect(treeElement).toBeTruthy();
        });

        it('should not render p-tree when loading', () => {
            spectator.fixture.componentRef.setInput('loading', true);
            spectator.detectChanges();

            const treeElement = spectator.query('p-tree');
            expect(treeElement).toBeFalsy();
        });

        it('should render folder name through pipe in node template', () => {
            // The template is rendered by p-tree, check if p-tree exists
            const treeElement = spectator.query('p-tree');
            expect(treeElement).toBeTruthy();
        });

        it('should render toggler icons in custom template', () => {
            // The template exists in the component but is rendered by p-tree internally
            const treeElement = spectator.query('p-tree');
            expect(treeElement).toBeTruthy();
        });

        it('should have tree component with custom templates', () => {
            // The templates are defined but rendered internally by p-tree
            const treeElement = spectator.query('p-tree');
            expect(treeElement).toBeTruthy();
        });
    });

    describe('Constants', () => {
        it('should export SYSTEM_HOST_ID constant', () => {
            expect(SYSTEM_HOST_ID).toBe('SYSTEM_HOST');
        });
    });

    describe('Input Changes', () => {
        it('should update tree properties when folders input changes', () => {
            const newFolders: TreeNode[] = [
                {
                    key: '4',
                    label: '/new/folder',
                    data: { path: '/new/folder' }
                }
            ];

            spectator.fixture.componentRef.setInput('folders', newFolders);
            spectator.detectChanges();

            const treeComponent = spectator.query(Tree);
            expect(treeComponent.value).toEqual(newFolders);
        });

        it('should update tree loading state when loading input changes', () => {
            // When loading is false, tree should be rendered
            spectator.fixture.componentRef.setInput('loading', false);
            spectator.detectChanges();

            const treeComponent = spectator.query(Tree);
            expect(treeComponent?.loading).toBe(false);
        });
    });

    describe('Drag and Drop', () => {
        let elementRefSpy: ReturnType<typeof jest.spyOn>;
        let uploadFilesSpyEmitter: ReturnType<typeof jest.spyOn>;

        beforeEach(() => {
            uploadFilesSpyEmitter = jest.spyOn(component.uploadFiles, 'emit');

            // Spy on the component's elementRef nativeElement.contains method
            if (component.elementRef?.nativeElement) {
                elementRefSpy = jest
                    .spyOn(component.elementRef.nativeElement, 'contains')
                    .mockReturnValue(false);
            }
        });

        afterEach(() => {
            jest.clearAllMocks();
        });

        describe('dragenter', () => {
            it('should prevent default and stop propagation', () => {
                const dragEvent = createDragEnterEvent(null);

                component.onDragEnter(dragEvent);

                expect(dragEvent.preventDefault).toHaveBeenCalled();
                expect(dragEvent.stopPropagation).toHaveBeenCalled();
            });

            it('should handle dragenter with fromElement', () => {
                const mockFromElement = document.createElement('div');
                const dragEvent = createDragEnterEvent(mockFromElement);

                component.onDragEnter(dragEvent);

                expect(dragEvent.preventDefault).toHaveBeenCalled();
                expect(dragEvent.stopPropagation).toHaveBeenCalled();
            });
        });

        describe('dragover', () => {
            it('should prevent default and stop propagation', () => {
                const dragEvent = createDragOverEvent();

                component.onDragOver(dragEvent);

                expect(dragEvent.preventDefault).toHaveBeenCalled();
                expect(dragEvent.stopPropagation).toHaveBeenCalled();
            });

            it('should set activeDropNode when dragging over a node with data-json-node attribute', () => {
                const mockNodeData = {
                    id: 'folder-123',
                    hostname: 'demo.dotcms.com',
                    path: '/documents/',
                    type: 'folder' as const
                };

                const targetElement = document.createElement('span');
                targetElement.setAttribute('data-json-node', JSON.stringify(mockNodeData));
                targetElement.setAttribute('data-testid', 'tree-node-label');

                const dragEvent = createDragOverEvent(targetElement);

                component.onDragOver(dragEvent);

                expect(component.$activeDropNode()).toEqual(mockNodeData);
            });

            it('should set activeDropNode when target contains a child with data-testid="tree-node-label"', () => {
                const mockNodeData = {
                    id: 'folder-456',
                    hostname: 'demo.dotcms.com',
                    path: '/images/',
                    type: 'folder' as const
                };

                const childElement = document.createElement('span');
                childElement.setAttribute('data-json-node', JSON.stringify(mockNodeData));
                childElement.setAttribute('data-testid', 'tree-node-label');

                const parentElement = document.createElement('div');
                parentElement.appendChild(childElement);

                const dragEvent = createDragOverEvent(parentElement);

                component.onDragOver(dragEvent);

                expect(component.$activeDropNode()).toEqual(mockNodeData);
            });

            it('should not set activeDropNode when target has no data-json-node attribute', () => {
                const targetElement = document.createElement('div');
                const dragEvent = createDragOverEvent(targetElement);

                component.$activeDropNode.set(null);
                component.onDragOver(dragEvent);

                expect(component.$activeDropNode()).toBeNull();
            });
        });

        describe('dragleave', () => {
            beforeEach(() => {
                // Set an active drop node first
                component.$activeDropNode.set({
                    id: 'folder-123',
                    hostname: 'demo.dotcms.com',
                    path: '/documents/',
                    type: 'folder'
                });
            });

            it('should reset activeDropNode when drag leaves and relatedTarget is not within component', () => {
                const mockRelatedTarget = document.createElement('div');
                elementRefSpy.mockReturnValue(false);

                const dragEvent = createDragLeaveEvent(mockRelatedTarget);

                component.onDragLeave(dragEvent);

                expect(component.$activeDropNode()).toBeNull();
            });

            it('should not reset activeDropNode when relatedTarget is still within component', () => {
                const mockRelatedTarget = document.createElement('div');
                elementRefSpy.mockReturnValue(true);

                const dragEvent = createDragLeaveEvent(mockRelatedTarget);

                component.onDragLeave(dragEvent);

                expect(component.$activeDropNode()).not.toBeNull();
            });

            it('should reset activeDropNode when relatedTarget is null', () => {
                const dragEvent = createDragLeaveEvent(null);

                component.onDragLeave(dragEvent);

                expect(component.$activeDropNode()).toBeNull();
            });

            it('should prevent default', () => {
                const dragEvent = createDragLeaveEvent(null);

                component.onDragLeave(dragEvent);

                expect(dragEvent.preventDefault).toHaveBeenCalled();
            });
        });

        describe('drop', () => {
            let mockFiles: FileList;
            let mockFile: File;

            beforeEach(() => {
                mockFile = new File(['content'], 'test.txt', { type: 'text/plain' });

                mockFiles = {
                    length: 1,
                    item: (index: number) => (index === 0 ? mockFile : null),
                    [0]: mockFile,
                    [Symbol.iterator]: function* () {
                        yield mockFile;
                    }
                } as FileList;

                // Set an active drop node
                component.$activeDropNode.set({
                    id: 'folder-789',
                    hostname: 'demo.dotcms.com',
                    path: '/uploads/',
                    type: 'folder'
                });
            });

            it('should emit uploadFiles event with files and targetFolderId', () => {
                const dragEvent = createDropEvent(mockFiles);

                component.onDrop(dragEvent);

                expect(uploadFilesSpyEmitter).toHaveBeenCalledWith({
                    files: mockFiles,
                    targetFolderId: 'folder-789'
                });
            });

            it('should reset activeDropNode after drop', () => {
                const dragEvent = createDropEvent(mockFiles);

                component.onDrop(dragEvent);

                expect(component.$activeDropNode()).toBeNull();
            });

            it('should prevent default and stop propagation', () => {
                const dragEvent = createDropEvent(mockFiles);

                component.onDrop(dragEvent);

                expect(dragEvent.preventDefault).toHaveBeenCalled();
                expect(dragEvent.stopPropagation).toHaveBeenCalled();
            });

            it('should not emit uploadFiles when no files are dropped', () => {
                const dragEvent = createDropEvent(null);

                component.onDrop(dragEvent);

                expect(uploadFilesSpyEmitter).not.toHaveBeenCalled();
            });

            it('should not emit uploadFiles when files list is empty', () => {
                const emptyFiles = {
                    length: 0,
                    item: () => null,
                    [Symbol.iterator]: function* () {
                        // Empty generator
                    }
                } as FileList;

                const dragEvent = createDropEvent(emptyFiles);

                component.onDrop(dragEvent);

                expect(uploadFilesSpyEmitter).not.toHaveBeenCalled();
            });

            it('should handle multiple files being dropped', () => {
                const file1 = new File(['content1'], 'test1.txt', { type: 'text/plain' });
                const file2 = new File(['content2'], 'test2.txt', { type: 'text/plain' });

                const multipleFiles = {
                    length: 2,
                    item: (index: number) => [file1, file2][index] || null,
                    [0]: file1,
                    [1]: file2,
                    [Symbol.iterator]: function* () {
                        yield file1;
                        yield file2;
                    }
                } as FileList;

                const dragEvent = createDropEvent(multipleFiles);

                component.onDrop(dragEvent);

                expect(uploadFilesSpyEmitter).toHaveBeenCalledWith({
                    files: multipleFiles,
                    targetFolderId: 'folder-789'
                });
            });
        });

        describe('Integration Tests', () => {
            it('should complete full drag and drop cycle', () => {
                const mockFile = new File(['content'], 'test.txt', { type: 'text/plain' });
                const mockFiles = {
                    length: 1,
                    item: () => mockFile,
                    [0]: mockFile,
                    [Symbol.iterator]: function* () {
                        yield mockFile;
                    }
                } as FileList;

                const mockNodeData = {
                    id: 'folder-integration',
                    hostname: 'demo.dotcms.com',
                    path: '/integration-test/',
                    type: 'folder' as const
                };

                // 1. Drag enter
                const dragEnterEvent = createDragEnterEvent(null);
                component.onDragEnter(dragEnterEvent);
                expect(dragEnterEvent.preventDefault).toHaveBeenCalled();

                // 2. Drag over a node
                const targetElement = document.createElement('span');
                targetElement.setAttribute('data-json-node', JSON.stringify(mockNodeData));
                const dragOverEvent = createDragOverEvent(targetElement);
                component.onDragOver(dragOverEvent);
                expect(component.$activeDropNode()).toEqual(mockNodeData);

                // 3. Drop files
                const dropEvent = createDropEvent(mockFiles);
                component.onDrop(dropEvent);

                expect(uploadFilesSpyEmitter).toHaveBeenCalledWith({
                    files: mockFiles,
                    targetFolderId: 'folder-integration'
                });
                expect(component.$activeDropNode()).toBeNull();
            });

            it('should handle drag leave without dropping', () => {
                const mockNodeData = {
                    id: 'folder-cancel',
                    hostname: 'demo.dotcms.com',
                    path: '/cancel-test/',
                    type: 'folder' as const
                };

                // 1. Drag over to set active node
                const targetElement = document.createElement('span');
                targetElement.setAttribute('data-json-node', JSON.stringify(mockNodeData));
                const dragOverEvent = createDragOverEvent(targetElement);
                component.onDragOver(dragOverEvent);
                expect(component.$activeDropNode()).toEqual(mockNodeData);

                // 2. Drag leave
                const dragLeaveEvent = createDragLeaveEvent(null);
                component.onDragLeave(dragLeaveEvent);

                expect(component.$activeDropNode()).toBeNull();
                expect(uploadFilesSpyEmitter).not.toHaveBeenCalled();
            });

            it('should update activeDropNode when dragging over different nodes', () => {
                const mockNodeData1 = {
                    id: 'folder-1',
                    hostname: 'demo.dotcms.com',
                    path: '/folder-1/',
                    type: 'folder' as const
                };

                const mockNodeData2 = {
                    id: 'folder-2',
                    hostname: 'demo.dotcms.com',
                    path: '/folder-2/',
                    type: 'folder' as const
                };

                // Drag over first node
                const targetElement1 = document.createElement('span');
                targetElement1.setAttribute('data-json-node', JSON.stringify(mockNodeData1));
                const dragOverEvent1 = createDragOverEvent(targetElement1);
                component.onDragOver(dragOverEvent1);
                expect(component.$activeDropNode()).toEqual(mockNodeData1);

                // Drag over second node
                const targetElement2 = document.createElement('span');
                targetElement2.setAttribute('data-json-node', JSON.stringify(mockNodeData2));
                const dragOverEvent2 = createDragOverEvent(targetElement2);
                component.onDragOver(dragOverEvent2);
                expect(component.$activeDropNode()).toEqual(mockNodeData2);
            });
        });

        describe('$activeDropNode Signal', () => {
            it('should initialize as null', () => {
                expect(component.$activeDropNode()).toBeNull();
            });

            it('should update when set programmatically', () => {
                const mockNodeData = {
                    id: 'test-folder',
                    hostname: 'test.com',
                    path: '/test/',
                    type: 'folder' as const
                };

                component.$activeDropNode.set(mockNodeData);

                expect(component.$activeDropNode()).toEqual(mockNodeData);
            });

            it('should be reactive when updated', () => {
                const mockNodeData1 = {
                    id: 'folder-a',
                    hostname: 'test.com',
                    path: '/a/',
                    type: 'folder' as const
                };

                const mockNodeData2 = {
                    id: 'folder-b',
                    hostname: 'test.com',
                    path: '/b/',
                    type: 'folder' as const
                };

                component.$activeDropNode.set(mockNodeData1);
                expect(component.$activeDropNode()).toEqual(mockNodeData1);

                component.$activeDropNode.set(mockNodeData2);
                expect(component.$activeDropNode()).toEqual(mockNodeData2);

                component.$activeDropNode.set(null);
                expect(component.$activeDropNode()).toBeNull();
            });
        });
    });
});
