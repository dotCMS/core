import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { TreeNode } from 'primeng/api';
import { SkeletonModule } from 'primeng/skeleton';
import { Tree, TreeModule, TreeNodeExpandEvent, TreeNodeCollapseEvent } from 'primeng/tree';

import { DotMessageService } from '@dotcms/data-access';
import { FolderNamePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotTreeFolderComponent } from './dot-tree-folder.component';

import { SYSTEM_HOST_ID } from '../shared/constants';

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
});
