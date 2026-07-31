import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import type { TreeNode } from 'primeng/api';
import { Tree } from 'primeng/tree';
import type { TreeNodeCollapseEvent, TreeNodeExpandEvent } from 'primeng/types/tree';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotFolderTreeComponent } from './dot-folder-tree.component';

describe('DotFolderTreeComponent', () => {
    let spectator: Spectator<DotFolderTreeComponent>;
    let component: DotFolderTreeComponent;

    const mockFolders: TreeNode[] = [
        {
            key: '1',
            label: '/application/content',
            data: { type: 'folder', path: '/application/content', hostname: 'demo.com', id: '1' },
            children: [
                {
                    key: '2',
                    label: '/application/content/images',
                    data: {
                        type: 'folder',
                        path: '/application/content/images',
                        hostname: 'demo.com',
                        id: '2'
                    }
                }
            ]
        },
        {
            key: '3',
            label: '/application/documents',
            data: {
                type: 'folder',
                path: '/application/documents',
                hostname: 'demo.com',
                id: '3'
            }
        }
    ];

    const mockSelectedNode: TreeNode = mockFolders[0].children![0];

    const createComponent = createComponentFactory({
        component: DotFolderTreeComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.tree.load-more': 'Load more',
                    'dot.file.field.host.folder.action.load.more': 'Load more folders'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;

        spectator.setInput('folders', mockFolders);
        spectator.setInput('loading', false);
        spectator.setInput('selectedNode', mockSelectedNode);
        spectator.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('p-tree bindings', () => {
        it('should default to single selection mode', () => {
            const tree = spectator.query(Tree);
            expect(tree.selectionMode).toBe('single');
            expect(component.$selection()).toEqual(mockSelectedNode);
        });

        it('should normalize selection to an array in multiple mode', () => {
            spectator.setInput('selectionMode', 'multiple');
            spectator.setInput('metaKeySelection', true);
            spectator.setInput('scrollHeight', 'auto');
            spectator.detectChanges();

            const tree = spectator.query(Tree);
            expect(tree.selectionMode).toBe('multiple');
            expect(tree.metaKeySelection).toBe(true);
            expect(tree.scrollHeight).toBe('auto');
            expect(component.$selection()).toEqual([mockSelectedNode]);
        });

        it('should apply chevron-only class by default', () => {
            const treeElement = spectator.query('p-tree');
            expect(treeElement.classList.contains('chevron-only')).toBe(true);
        });

        it('should apply first-only class when showFolderIconOnFirstOnly is true', () => {
            spectator.setInput('showFolderIconOnFirstOnly', true);
            spectator.detectChanges();

            const treeElement = spectator.query('p-tree');
            expect(treeElement.classList.contains('first-only')).toBe(true);
            expect(treeElement.classList.contains('chevron-only')).toBe(false);
        });

        it('should accept pt options input', () => {
            const pt = { root: { class: 'custom-root' } };
            spectator.setInput('pt', pt);
            spectator.detectChanges();

            expect(component.$pt()).toEqual(pt);
        });

        it('should set tree data-testid', () => {
            spectator.setInput('treeTestId', 'host-folder-tree');
            spectator.detectChanges();

            expect(spectator.query('[data-testid="host-folder-tree"]')).toBeTruthy();
        });
    });

    describe('outputs', () => {
        it('should emit select, expand, and collapse from p-tree', () => {
            const selectSpy = jest.spyOn(component.onNodeSelect, 'emit');
            const expandSpy = jest.spyOn(component.onNodeExpand, 'emit');
            const collapseSpy = jest.spyOn(component.onNodeCollapse, 'emit');

            const selectEvent: TreeNodeExpandEvent = {
                originalEvent: new Event('select'),
                node: mockFolders[0]
            };
            const expandEvent: TreeNodeExpandEvent = {
                originalEvent: new Event('expand'),
                node: mockFolders[0]
            };
            const collapseEvent: TreeNodeCollapseEvent = {
                originalEvent: new Event('collapse'),
                node: mockFolders[0]
            };

            spectator.triggerEventHandler(Tree, 'onNodeSelect', selectEvent);
            spectator.triggerEventHandler(Tree, 'onNodeExpand', expandEvent);
            spectator.triggerEventHandler(Tree, 'onNodeCollapse', collapseEvent);

            expect(selectSpy).toHaveBeenCalledWith(selectEvent);
            expect(expandSpy).toHaveBeenCalledWith(expandEvent);
            expect(collapseSpy).toHaveBeenCalledWith(collapseEvent);
        });
    });

    describe('load-more', () => {
        const loadMoreNode: TreeNode = {
            key: 'load-more:/application/',
            label: 'content-drive.tree.load-more',
            type: 'load-more',
            data: {
                type: 'load-more',
                path: '/application/',
                hostname: 'demo.com',
                id: 'load-more:/application/',
                nextPage: 2,
                remaining: 40
            },
            leaf: true,
            selectable: false
        };

        it('should emit loadMore on click without selecting', () => {
            const loadMoreSpy = jest.spyOn(component.loadMore, 'emit');
            const selectSpy = jest.spyOn(component.onNodeSelect, 'emit');

            spectator.setInput('folders', [loadMoreNode]);
            spectator.setInput('showLoadMoreRemaining', true);
            spectator.setInput('showLoadMorePlusIcon', false);
            spectator.detectChanges();

            const button = spectator.query(byTestId('tree-load-more'));
            expect(button).toBeTruthy();
            expect(button?.textContent).toContain('Load more');
            expect(button?.textContent).toContain('(40)');

            spectator.click(button as HTMLElement);

            expect(loadMoreSpy).toHaveBeenCalledWith(loadMoreNode);
            expect(selectSpy).not.toHaveBeenCalled();
        });

        it('should show plus icon for Host Folder variant', () => {
            const hfNode: TreeNode = {
                ...loadMoreNode,
                label: ''
            };

            spectator.setInput('folders', [hfNode]);
            spectator.setInput('showLoadMorePlusIcon', true);
            spectator.setInput('showLoadMoreRemaining', false);
            spectator.setInput('loadMoreTestId', 'host-folder-load-more');
            spectator.detectChanges();

            const button = spectator.query(byTestId('host-folder-load-more'));
            expect(button).toBeTruthy();
            expect(button?.querySelector('.pi-plus-circle')).toBeTruthy();
            expect(button?.textContent).toContain('Load more folders');
            expect(button?.textContent).not.toContain('(40)');
        });
    });

    describe('default label', () => {
        it('should render folder name from path for root nodes', () => {
            const labels = spectator.queryAll(byTestId('tree-node-label'));
            expect(labels.map((el) => el.textContent?.trim())).toEqual(['content', 'documents']);
        });
    });

    describe('public scroll helpers', () => {
        it('should expose Tree viewChild and ElementRef', () => {
            expect(component.tree()).toBeTruthy();
            expect(component.elementRef).toBeTruthy();
        });
    });
});
